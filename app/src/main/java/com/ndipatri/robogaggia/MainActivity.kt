package com.ndipatri.robogaggia

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ndipatri.robogaggia.theme.Purple40
import com.ndipatri.robogaggia.theme.PurpleGrey40_50
import com.ndipatri.robogaggia.theme.PurpleGrey80
import com.ndipatri.robogaggia.theme.RoboGaggiaTheme

/**
 * Inspired by work from
 *
 * Heavily inspired by an article by Saurabh Pant
 *
 * https://proandroiddev.com/creating-graph-in-jetpack-compose-312957b11b2
 *
 * and
 *
 * https://github.com/riggaroo/compose-playtime/blob/main/app/src/main/java/dev/riggaroo/composeplaytime/WeTradeGraph.kt
 */

@OptIn(ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NoStatusBar()

            val context = LocalContext.current

            val viewModel = viewModel<RoboViewModel>()

            val uiState by viewModel.uiStateFlow.collectAsState()
            MainContent(uiState)

            // Only run on initial composition
            LaunchedEffect(true) {
                viewModel.start(context)
            }
        }
    }

    @Preview(
        // Pixel 3
        device = "spec:width=2160px,height=1080px,dpi=443,orientation=landscape"
    )
    @Composable
    fun MainContentPreview() {

        val uiState = UIState.Data(
            accumulatedTelemetry =
            renderDummyTelemetry()
        )

        MainContent(uiState)
    }

    @Composable
    private fun MainContent(uiState: UIState) {
        RoboGaggiaTheme {
            Surface(
                // uses theme
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = {
                            // Regular
                        },
                        onLongClick = {
                            finish()
                            startActivity(intent)
                        }
                    ),
                color = Color.Black,
            ) {

                if (uiState is UIState.Loading) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Sorry, no telemetry!",
                            color = White,
                            modifier = Modifier.align(Center)
                        )
                    }
                } else {
                    val accumulatedTelemetry = (uiState as UIState.Data).accumulatedTelemetry

                    val (seriesList, maxValueList, unitList, colorList) = SeriesData(accumulatedTelemetry)

                    //val visibleSeriesMap = remember { mutableStateMapOf(0 to true, 1 to true, 2 to true, 3 to true, 4 to true) }
                    val visibleSeriesMap = remember { mutableStateMapOf(0 to true, 1 to false, 2 to false, 3 to false, 4 to false) }

                    val xStepsPerScreen = 40
                    val secondsPerStep = 1.2

                    Box(modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 30.dp)) {
                        val numberOfVisibleRows = visibleSeriesMap.filterValues { value -> value }.size

                        GridBackground(
                            numberOfColumns = numberOfVisibleRows + 2,
                            numberOfRows = numberOfVisibleRows
                        )

                        Legend(
                            modifier = Modifier.padding(start = 30.dp),
                            seriesList = seriesList,
                            visibleSeriesMap = visibleSeriesMap,
                            onSeriesSelected = {
                                visibleSeriesMap[it] = visibleSeriesMap[it]?.not() ?: true
                            },
                            unitList = unitList,
                            colorList = colorList,
                            accumulatedTelemetry = accumulatedTelemetry
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            seriesList.forEachIndexed() { index, series ->
                                if (visibleSeriesMap[index] != false) {
                                    LineGraph(
                                        modifier = Modifier.weight(1f),
                                        pathColor = colorList[index],
                                        yValues = series,
                                        yMaxValue = maxValueList[index],
                                        xStepsPerScreen = xStepsPerScreen
                                    )
                                }
                            }
                        }
                    }

                    val screenDensity = LocalDensity.current
                    val textPaint = remember(screenDensity) {
                        Paint().apply {
                            color = White.toArgb()
                            textAlign = Paint.Align.CENTER
                            textSize = screenDensity.run { 12.sp.toPx() }
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .padding(bottom = 15.dp)
                            .fillMaxSize()
                    ) {

                        val xStepPx = size.width / xStepsPerScreen
                        val totalValues = seriesList[0].size
                        for (index in 1..totalValues) {
                            // We only draw text at certain time intervals
                            if (index % 5 == 0 || index == 1 || index == totalValues) {

                                drawContext.canvas.nativeCanvas.drawText(
                                    "${(index * secondsPerStep).toInt()}s",
                                    size.width - (xStepPx * (totalValues - index) + 35),
                                    size.height,
                                    textPaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainActivity.GridBackground(
        numberOfColumns: Int,
        numberOfRows: Int
    ) {
        // we cache this so the only time it gets recomposed is if
        // the size of the drawing area changes
        Spacer(modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    val borderWidth = 2.dp.toPx()

                    // vertical axis
                    drawLine(
                        PurpleGrey80,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = borderWidth
                    )

                    // vertical ticks
                    val verticalTickSpacing = 10.dp.toPx()
                    repeat((size.height / verticalTickSpacing).toInt()) { tickIndex ->
                        drawLine(
                            PurpleGrey80,
                            start = Offset(0f, tickIndex * verticalTickSpacing),
                            end = Offset(5.dp.toPx(), tickIndex * verticalTickSpacing),
                            strokeWidth = borderWidth
                        )
                    }

                    // horizontal axis
                    drawLine(
                        PurpleGrey80,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = borderWidth
                    )

                    // horizontal ticks
                    val horizontalTickSpacing = 10.dp.toPx()
                    repeat((size.width / horizontalTickSpacing).toInt()) { tickIndex ->
                        drawLine(
                            PurpleGrey80,
                            start = Offset(tickIndex * horizontalTickSpacing, size.height),
                            end = Offset(tickIndex * horizontalTickSpacing, size.height - 10f),
                            strokeWidth = borderWidth
                        )
                    }

                    // vertical lines
                    repeat(numberOfColumns - 1) { columnIndex -> // vertical lines
                        val startX = (size.width / numberOfColumns) * (columnIndex + 1)
                        drawLine(
                            PurpleGrey40_50,
                            start = Offset(startX, 0f),
                            end = Offset(startX, size.height),
                            strokeWidth = borderWidth
                        )
                    }

                    // horizontal lines
                    repeat(numberOfRows - 1) { columnIndex -> // vertical lines
                        val startY = (size.height / numberOfRows) * (columnIndex + 1)
                        drawLine(
                            PurpleGrey40_50,
                            start = Offset(0f, startY),
                            end = Offset(size.width, startY),
                            strokeWidth = borderWidth
                        )
                    }
                }
            }
        )
    }

    @Composable
    private fun MainActivity.Legend(
        modifier: Modifier,
        seriesList: List<List<Float>>,
        visibleSeriesMap: Map<Int, Boolean>,
        onSeriesSelected: (Int) -> Unit,
        unitList: List<String>,
        colorList: List<Color>,
        accumulatedTelemetry: List<TelemetryMessage>
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {

            Column(modifier = Modifier.fillMaxHeight()) {
                // Single head row with all series not currently being displayed
                Row(modifier = Modifier.border(BorderStroke(1.dp, PurpleGrey80), shape = RoundedCornerShape(20.dp))) {
                    seriesList.forEachIndexed { index, series ->
                        if (visibleSeriesMap[index] == false) {
                            DelayedClickButton(onClick = { onSeriesSelected(index) }) {
                                Text(
                                    modifier = Modifier
                                        .padding(end = 20.dp),
                                    text = unitList[index],
                                    color = colorList[index],
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // Row for each series being displayed
                seriesList.forEachIndexed { index, series ->
                    if (visibleSeriesMap[index] != false) {
                        SeriesTitleRow(unitList[index],
                            colorList[index],
                            series.max(),
                            onClick = { onSeriesSelected(index) })
                    }
                }
            }

            Text(
                modifier = Modifier
                    .align(TopEnd)
                    .padding(20.dp),
                text = accumulatedTelemetry.last().description,
                color = White,
                fontSize = 14.sp
            )
        }
    }

    @Composable
    private fun ColumnScope.SeriesTitleRow(seriesTitle: String, seriesColor: Color, seriesMax: Float, onClick: () -> Unit) {

        Row(
            modifier = Modifier
                .padding(top = 5.dp, bottom = 5.dp)
                .weight(1f), // since this is a 'scoped' modifier, we need to extend ColumnScope for this Composable
            verticalAlignment = Alignment.CenterVertically
        ) {
            DelayedClickButton(
                onClick = onClick,
            ) {
                Text(
                    text = seriesTitle,
                    color = seriesColor,
                    fontSize = 16.sp
                )
                Text(
                    text = " ($seriesMax)",
                    color = seriesColor,
                    fontSize = 16.sp
                )
            }
        }
    }

    @Composable
    fun LineGraph(
        modifier: Modifier,
        pathColor: Color,
        yValues: List<Float>,
        yMaxValue: Float,
        xStepsPerScreen: Int,
    ) {
        // This whole graph is just about rendering a Path...
        val path = Path()
        Canvas(
            // We need fillMaxSize.. otherwise, we are defining our size internally here.
            modifier = modifier.fillMaxSize()//.border(width = 1.dp, color = PurpleGrey80)
        ) {
            val xStepPx = size.width / xStepsPerScreen
            val yZeroPx = size.height // y value representing the x-axis of the graph

            // We divide up our available plot height based on the highest possible value that we would
            // need to draw.. This is so we don't exceed our bounds when drawing...
            val yScaleFactor = yZeroPx / yMaxValue

            // so we always start at 0 when drawing our graph
            val values = mutableListOf<Float>().apply {
                add(0F)
                addAll(yValues)
            }

            var previousXPosition = 0F
            var previousYPosition = 0F
            values.forEachIndexed { index, value ->
                // We start creating our path far enough to the
                // left so all values will fit.


//              NJD - this approach uses every other data point as control point
//                println("*** NJD: index: $index, value: $value")
//                val xPosition = size.width - (xStepPx * (values.size - index))
//
//                val yPosition = yZeroPx - (value * yScaleFactor)
//
//                if (index == 0) {
//                    println("*** NJD: moveTo: ($xPosition, $yPosition")
//                    path.moveTo(xPosition, yPosition)
//                } else {
//                    println("*** NJD: other ($xPosition, $yPosition")
//
//                    // every other value
//                    if (index % 2 == 0) { // 2,4,6,8
//                        println("*** NJD: mod: ($xPosition, $yPosition")
//                        // every other point we actually use as a destination
//                        // for bezier quadratic curve
//                        //              cp
//                        //           *  *  *
//                        //        *           *p2
//                        //    p1
//                        //
//                        path.quadraticBezierTo(
//                            previousXPosition,
//                            previousYPosition,
//                            xPosition,
//                            yPosition
//                        )
//                    }
//                    // otherwise, this point will be used as a control point
//                }

                // NJD - this approach we create two fake control points in between data points..
                // We start creating our path far enough to the
                // left so all values will fit.
                val xPosition = size.width - (xStepPx * (values.size - index))
                var yPosition = 0F

                if (index == 0) {
                    yPosition = yZeroPx

                    // In order to draw a line to first point, we have to start
                    // at origin for this graph
                    path.moveTo(xPosition, yPosition)
                } else {
                    yPosition = yZeroPx - (value * yScaleFactor)

                    // We create two control points so the path between last point and
                    // this point is not a straight one:
                    //                      *   *
                    //                cp2         p2
                    //                 *
                    //    p1          cp1
                    //        *   *
                    path.cubicTo(
                        (xPosition + previousXPosition) / 2,
                        previousYPosition,
                        (xPosition + previousXPosition) / 2,
                        yPosition,
                        xPosition,
                        yPosition
                    )
                }

                previousXPosition = xPosition
                previousYPosition = yPosition

                drawCircle(
                    color = Color.Red,
                    radius = 10f,
                    center = Offset(xPosition, yPosition)
                )








                drawPath(
                    path = path,
                    color = pathColor,
                    style = Stroke(2.dp.toPx())
                )
            }
        }
    }

    @Composable
    private fun NoStatusBar() {
        val systemUiController: SystemUiController = rememberSystemUiController()
        systemUiController.isStatusBarVisible = false // Status bar
        systemUiController.isNavigationBarVisible = false // Navigation bar
        systemUiController.isSystemBarsVisible = false // Status & Navigation bars
    }

    data class SeriesData constructor(
        val seriesList: List<List<Float>>,
        val maxValueList: List<Float>,
        val unitList: List<String>,
        val colorList: List<Color>
    ) {
        constructor(accumulatedTelemetry: List<TelemetryMessage>) :
                this(
                    seriesList = listOf(
                        accumulatedTelemetry.map { it.weightGrams.toFloat() },
                        accumulatedTelemetry.map { it.pressureBars.toFloat() },
                        accumulatedTelemetry.map { it.flowRateGPS.toFloat() },
                        accumulatedTelemetry.map { it.brewTempC.toFloat() },
                        accumulatedTelemetry.map { it.dutyCyclePercent.toFloat() }),

                    maxValueList = listOf(50f, 15f, 5f, 150f, 100f),
                    unitList = listOf("grams", "bars", "grams/sec", "tempC", "pumpPower"),
                    colorList = listOf(Yellow, Red, Magenta, Green, Purple40)
                )
    }
}
