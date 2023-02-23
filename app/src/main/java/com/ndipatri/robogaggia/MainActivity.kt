package com.ndipatri.robogaggia

import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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

                    val visibleSeriesMap = remember { mutableStateMapOf(0 to true, 1 to true, 2 to true, 3 to true, 4 to true) }

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
                                    Graph2(
                                        modifier = Modifier.weight(1f),
                                        pathColor = colorList[index],
                                        yValues = series,
                                        yMaxValue = maxValueList[index],
                                        xStepsPerScreen = 40 // 1.2 second per step
                                    )
                                }
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    seriesList.forEachIndexed { index, series ->
                        if (visibleSeriesMap[index] == false) {
                            Text(
                                modifier = Modifier
                                    .padding(end = 20.dp)
                                    .clickable(onClick = { onSeriesSelected(index) }),
                                text = unitList[index],
                                color = colorList[index],
                            )
                        }
                    }
                }

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
                modifier = Modifier.align(TopEnd),
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
                .weight(1f) // since this is a 'scoped' modifier, we need to extend ColumnScope for this Composable
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = seriesTitle,
                color = seriesColor,
                fontSize = 14.sp
            )
            Text(
                text = " ($seriesMax)",
                color = seriesColor,
                fontSize = 14.sp
            )
        }
    }

    @Composable
    fun Graph2(
        modifier: Modifier,
        pathColor: Color,
        yValues: List<Float>,
        yMaxValue: Float,
        xStepsPerScreen: Int,
    ) {
        val path = Path()
        Canvas(
            // We need fillMaxSize.. otherwise, we are defining our size internally here.
            modifier = modifier.fillMaxSize()//.border(width = 1.dp, color = PurpleGrey80)
        ) {
            val xStepPx = size.width / xStepsPerScreen
            val yZeroPx = size.height

            // We divide up our available plot height based on the highest possible value that we would
            // need to draw.. This is so we don't exceed our bounds when drawing...
            val yScaleFactor = yZeroPx / yMaxValue

            yValues.forEachIndexed { index, value ->
                // We start creating our path far enough to the
                // left so all values will fit.. we add one extra value which is
                // our artificial zero starting point.
                val xPosition = size.width - (xStepPx * (yValues.size - index + 1))

                if (index == 0) {
                    // In order to draw a line to first point, we have to start
                    // at origin for this graph
                    path.moveTo(xPosition, yZeroPx)
                }

                val yPosition = yZeroPx - (value * yScaleFactor)

                path.lineTo(xPosition + xStepPx, yPosition)
            }

            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(2.dp.toPx())
            )
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

    /**
     * Heavily inspired by an article by Saurabh Pant
     * https://proandroiddev.com/creating-graph-in-jetpack-compose-312957b11b2
     */
    @Composable
    fun Graph(
        pathColor: Color,
        points: List<Float>,
        // the lower the number, the larger the scale..
        heightMultiplier: Float = 0.2F
    ) {
        val MAX_SAMPLES = 50

        var _points = mutableListOf<Float>().also {
            it.addAll(points)
        }

        if (_points.size > MAX_SAMPLES) {
            _points = _points.subList(_points.size - (_points.size % MAX_SAMPLES) - 1, _points.size)
        }

        val paddingSpace = 16.dp
        val widthMultiplier = .8F
        val heightDP = 200
        val xValues = (0..MAX_SAMPLES).map { it + 1 }

        val controlPoints1 = mutableListOf<PointF>()
        val controlPoints2 = mutableListOf<PointF>()
        val coordinates = mutableListOf<PointF>()
        val density = LocalDensity.current
        val textPaint = remember(density) {
            Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = density.run { 12.sp.toPx() }
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val xAxisSpace = widthMultiplier * ((size.width - paddingSpace.toPx()) / xValues.size)
            /** placing x axis points */
            for (i in xValues.indices step 5) {
                drawContext.canvas.nativeCanvas.drawText(
                    "${xValues[i]}",
                    xAxisSpace * (i + 1),
                    size.height - MAX_SAMPLES,
                    textPaint
                )
            }
            /** placing our x axis points */
            for (i in _points.indices) {
                val x1 = xAxisSpace * xValues[i]
                val y1 = size.height - (heightDP * (_points[i] * heightMultiplier.toFloat()))
                coordinates.add(PointF(x1, y1))
                /** drawing circles to indicate all the points */
//                drawCircle(
//                    color = Color.Red,
//                    radius = 10f,
//                    center = Offset(x1,y1)
//                )
            }
            /** calculating the connection points */
            for (i in 1 until coordinates.size) {
                controlPoints1.add(PointF((coordinates[i].x + coordinates[i - 1].x) / 2, coordinates[i - 1].y))
                controlPoints2.add(PointF((coordinates[i].x + coordinates[i - 1].x) / 2, coordinates[i].y))
            }
            /** drawing the path */
            val stroke = Path().apply {
                reset()
                moveTo(coordinates.first().x, coordinates.first().y)
                for (i in 0 until coordinates.size - 1) {
                    cubicTo(
                        controlPoints1[i].x, controlPoints1[i].y,
                        controlPoints2[i].x, controlPoints2[i].y,
                        coordinates[i + 1].x, coordinates[i + 1].y
                    )
                }
            }

            /** filling the area under the path */
            // NJD unused anyway
//        val fillPath = Path(stroke.asAndroidPath())
//            .asComposePath()
//            .apply {
//                lineTo(xAxisSpace * xValues.last(), size.height - heightDP)
//                lineTo(xAxisSpace, size.height - heightDP)
//                close()
//            }
            drawPath(
                stroke,
                color = pathColor,
                style = Stroke(
                    width = 10f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
