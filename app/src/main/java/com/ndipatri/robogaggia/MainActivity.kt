package com.ndipatri.robogaggia

import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ndipatri.robogaggia.theme.Purple40
import com.ndipatri.robogaggia.theme.RoboGaggiaTheme


@OptIn(ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the status bar.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        setContent {
            val roboViewModel = viewModel<RoboViewModel>()

            RoboGaggiaTheme {
                // A surface container using the 'background' color from the theme
                Surface(
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
                    val latestMessageList by roboViewModel.mqttInstance.incomingTelemetryLD.observeAsState()

                    if (latestMessageList.isNullOrEmpty()) {
                        Text("Sorry, no messages!", color = Color.White)
                    } else {
                        val weightSeries = latestMessageList!!.map { it.weightGrams.toFloat() }
                        val pressureSeries = latestMessageList!!.map { it.pressureBars.toFloat() }
                        val flowRateSeries = latestMessageList!!.map { it.flowRateGPS.toFloat() }
                        val tempSeries = latestMessageList!!.map { it.brewTempC.toFloat() }
                        val dutyCycleSeries = latestMessageList!!.map { it.dutyCyclePercent.toFloat() }

                        val gramsColor = Yellow
                        val barsColor = Red
                        val gramsPerSecColor = Magenta
                        val tempColor = Green
                        val dutyCyleColor = Purple40

                        Box(modifier = Modifier.fillMaxSize()) {

                            Column {
                                seriesTitleRow("grams", gramsColor, maxValue(latestMessageList!!) { it.weightGrams.toFloat() })
                                seriesTitleRow("bars", barsColor, maxValue(latestMessageList!!) { it.pressureBars.toFloat() })
                                seriesTitleRow("grams/sec", gramsPerSecColor, maxValue(latestMessageList!!) { it.flowRateGPS.toFloat() })
                                seriesTitleRow("tempC", tempColor, maxValue(latestMessageList!!) { it.brewTempC.toFloat() })
                                seriesTitleRow("dutyCycle", dutyCyleColor, maxValue(latestMessageList!!) { it.dutyCyclePercent.toFloat() })
                            }

                            Text(
                                modifier = Modifier.align(TopEnd),
                                text = latestMessageList!!.last().description,
                                color = White
                            )
                        }

                        // grams
                        Graph(
                            pathColor = gramsColor,
                            points = weightSeries,
                            heightMultiplier = .08F
                        )

                        // bars
                        Graph(
                            pathColor = barsColor,
                            points = pressureSeries,
                            heightMultiplier = .4F
                        )

                        // grams per second
                        Graph(
                            pathColor = gramsPerSecColor,
                            points = flowRateSeries,
                            heightMultiplier = .6F
                        )

                        // celsius
                        Graph(
                            pathColor = tempColor,
                            points = tempSeries,
                            heightMultiplier = .04F
                        )

                        // dutyCycle
                        Graph(
                            pathColor = dutyCyleColor,
                            points = dutyCycleSeries,
                            heightMultiplier = .03F
                        )
                    }
                }
            }
        }
    }

    private fun maxValue(list: List<TelemetryMessage>, extractElementFromList: (TelemetryMessage) -> Float): Float {
        return list
            .map { extractElementFromList.invoke(it) }
            .reduce { max, next ->
                if (next > max) {
                    next
                } else {
                    max
                }
            }
    }

    @Composable
    private fun seriesTitleRow(seriesTitle: String, seriesColor: Color, seriesMax: Float) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = seriesTitle,
                color = seriesColor,
                //modifier = Modifier.padding(top = 50.dp)
                //modifier = Modifier.paddingFromBaseline(50.dp)
            )
            Text(
                text = " ($seriesMax)",
                color = seriesColor
            )
        }
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
