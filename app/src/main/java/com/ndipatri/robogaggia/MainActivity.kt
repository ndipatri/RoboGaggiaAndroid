package com.ndipatri.robogaggia

import android.content.Context
import android.content.res.Configuration
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import com.ndipatri.robogaggia.theme.Purple40
import com.ndipatri.robogaggia.theme.RoboGaggiaTheme
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.QoS
import kotlinx.coroutines.delay
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.time.format.TextStyle


@OptIn(ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    private val TEST_MODE = BuildConfig.TEST_MODE

    private lateinit var mqttInstance: Mqtt

    // prevent orientation change.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.apply {
            // Hide both the navigation bar and the status bar.
            // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
            // a general rule, you should design your app to hide the status bar whenever you
            // hide the navigation bar.
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        mqttInstance = Mqtt().also {
            if (!TEST_MODE) {
                it.start(applicationContext)
            }
        }

        setContent {
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
                                val intent = intent
                                finish()
                                startActivity(intent)
                                //mqttInstance.incomingTelemetryLD.postValue(null)
                            }
                        ),
                    color = Color.Black,
                ) {
                    val latestMessageList by mqttInstance.incomingTelemetryLD.observeAsState()

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
                                style = TextStyle(
                                    fontSize = 14.sp, color = White
                                )
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
                            heightMultiplier = .6F
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

            if (TEST_MODE) {
                LaunchedEffect(true) {

                    var index: Int = 0

                    val description = "PID(1,2,4)"
                    val weightList = listOf(0, 0, 0, 0, 0, 0, 1, 4, 11, 17, 19, 22, 25, 31, 34, 38, 41, 41)
                    val pressureList = listOf(8, 0, 1, 0, 0, 0, 1, 4, 7, 5, 3, 8, 11, 11, 8, 9, 7, 7)
                    val flowRateList = listOf(0, 0, 0, 0, 0, 0, 0.833333, 2.5, 1.833333, 5, 1.666667, 2.5, 2.5, 5, 2.5, 1.333333, 2.5, 2.5)
                    val tempList = listOf(
                        103.75,
                        106.5,
                        109.75,
                        112.75,
                        114.5,
                        116.25,
                        117,
                        116,
                        114.25,
                        111,
                        108.25,
                        105.25,
                        102.5,
                        100,
                        98,
                        96.75,
                        96.25,
                        96.25
                    )
                    val dutyCycleList = listOf(0, 50, 20, 0, 0, 0, 0.833333, 80, 1.833333, 5, 17, 25, 20, 5, 20, 10, 25, 25)

                    println("description: ${description})")
                    println("weightList: size=${weightList.size}")
                    println("pressureList: size=${pressureList.size}")
                    println("flowRateList: size=${flowRateList.size}")
                    println("tempList: size=${tempList.size}")
                    println("dutyCycleList: size=${dutyCycleList.size}")

                    while (true) {
                        delay(250)

                        val existingList = mutableListOf<TelemetryMessage>().also {
                            it.addAll(mqttInstance.incomingTelemetryLD.value ?: emptyList())
                        }

                        var nextWeightValue = weightList[index]
                        var nextPressueValue = pressureList[index]
                        var nextFlowRateValue = flowRateList[index]
                        var nextTempValue = tempList[index]
                        var nextDutyCycleValue = dutyCycleList[index]
                        if (++index == weightList.size) break

                        val newRoboGaggiaMessage = TelemetryMessage(
                            weightGrams = "$nextWeightValue",
                            pressureBars = "$nextPressueValue",
                            dutyCyclePercent = "$nextDutyCycleValue",
                            flowRateGPS = "$nextFlowRateValue",
                            brewTempC = "$nextTempValue",
                            description = "$description"
                        )

                        existingList.add(newRoboGaggiaMessage)

                        mqttInstance.incomingTelemetryLD.postValue(existingList)
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
                style = TextStyle(
                    fontSize = 26.sp, color = seriesColor
                )
            )
            Text(
                text = " ($seriesMax)",
                style = TextStyle(
                    fontSize = 18.sp, color = seriesColor
                )
            )
        }
    }

    class Mqtt {

        val incomingTelemetryLD = MutableLiveData(mutableListOf<TelemetryMessage>())

        lateinit var mqttAndroidClient: MqttAndroidClient

        fun start(applicationContext: Context) {

            clientId += System.currentTimeMillis()
            mqttAndroidClient = MqttAndroidClient(applicationContext, serverUri, clientId)
            mqttAndroidClient.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String) {
                    if (reconnect) {
                        println("Reconnected: $serverURI")
                        // Because Clean Session is true, we need to re-subscribe
                        subscribeToTopic(mqttAndroidClient)
                    } else {
                        println("Connected: $serverURI")
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    println("The Connection was lost.")
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    println("Incoming message: " + String(message.payload))

                    val existingList = mutableListOf<TelemetryMessage>().also {
                        it.addAll(incomingTelemetryLD.value ?: emptyList())
                    }

                    lateinit var stateName: String
                    lateinit var description: String
                    lateinit var measuredWeightGrams: String
                    lateinit var measuredPressureBars: String
                    lateinit var pumpDutyCycle: String
                    lateinit var flowRateGPS: String
                    lateinit var brewTempC: String

                    message.toString().split(",").forEachIndexed() { index, element ->
                        when (index) {
                            0 -> stateName = element
                            1 -> description = element
                            2 -> measuredWeightGrams = element
                            3 -> measuredPressureBars = element
                            4 -> pumpDutyCycle = element
                            5 -> flowRateGPS = element
                            6 -> brewTempC = element
                        }
                    }

                    val newRoboGaggiaMessage = TelemetryMessage(
                        weightGrams = measuredWeightGrams,
                        pressureBars = measuredPressureBars,
                        dutyCyclePercent = pumpDutyCycle,
                        flowRateGPS = flowRateGPS,
                        brewTempC = brewTempC,
                        description = description
                    )

                    existingList.add(newRoboGaggiaMessage)

                    incomingTelemetryLD.postValue(existingList)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken) {}
            })

            val mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.isAutomaticReconnect = true
            mqttConnectOptions.isCleanSession = false
            mqttConnectOptions.userName = BuildConfig.AIO_USERNAME
            mqttConnectOptions.password = BuildConfig.AIO_PASSWORD.toCharArray()
            println("Connecting: $serverUri")
            mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)

                    subscribeToTopic(mqttAndroidClient)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    println("Failed to connect: ${serverUri}")
                }
            })
        }

        fun subscribeToTopic(mqttAndroidClient: MqttAndroidClient) {
            mqttAndroidClient.subscribe(subscriptionTopic, QoS.AtMostOnce.value, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    println("Subscribed! ${subscriptionTopic}")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    println("Failed to subscribe $exception")
                }
            })
        }

        companion object {
            private const val serverUri = "tcp://io.adafruit.com:1883"
            private const val subscriptionTopic = "ndipatri/feeds/robogaggiatelemetry"
            private const val publishTopic = "exampleAndroidPublishTopic"
            private const val publishMessage = "Hello World"
            private var clientId = ""
        }
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
        /** filling the area under the path */
        val fillPath = Path(stroke.asAndroidPath())
            .asComposePath()
            .apply {
                lineTo(xAxisSpace * xValues.last(), size.height - heightDP)
                lineTo(xAxisSpace, size.height - heightDP)
                close()
            }
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


data class TelemetryMessage(
    val description: String,
    val weightGrams: String,
    val pressureBars: String,
    val dutyCyclePercent: String,
    val flowRateGPS: String,
    val brewTempC: String
)