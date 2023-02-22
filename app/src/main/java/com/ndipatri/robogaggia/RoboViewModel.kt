package com.ndipatri.robogaggia

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.QoS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class RoboViewModel() : ViewModel() {

    val uiStateFlow: MutableStateFlow<UIState> = MutableStateFlow(UIState.Loading)

    lateinit var mqttAndroidClient: MqttAndroidClient

    fun start(context: Context) {
        clientId += System.currentTimeMillis()
        mqttAndroidClient = MqttAndroidClient(context, BuildConfig.MQTT_SERVER, clientId)
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

                val newTelemetry = TelemetryMessage(
                    weightGrams = measuredWeightGrams,
                    pressureBars = measuredPressureBars,
                    dutyCyclePercent = pumpDutyCycle,
                    flowRateGPS = flowRateGPS,
                    brewTempC = brewTempC,
                    description = description
                )

                val accumulatedTelemetry = mutableListOf<TelemetryMessage>()
                if (uiStateFlow.value is UIState.Data) {
                    accumulatedTelemetry.addAll((uiStateFlow.value as UIState.Data).accumulatedTelemetry)
                }
                accumulatedTelemetry.add(newTelemetry)

                viewModelScope.launch {
                    uiStateFlow.emit(UIState.Data(accumulatedTelemetry))
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })

        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false
        mqttConnectOptions.userName = BuildConfig.AIO_USERNAME
        mqttConnectOptions.password = BuildConfig.AIO_PASSWORD.toCharArray()
        println("Connecting: ${BuildConfig.MQTT_SERVER}")

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
                println("Failed to connect: ${BuildConfig.MQTT_SERVER}, error: $exception")
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
        private const val subscriptionTopic = "ndipatri/feeds/robogaggiatelemetry"
        private var clientId = ""
    }
}

sealed interface UIState {
    object Loading : UIState
    data class Data(
        val accumulatedTelemetry: List<TelemetryMessage>
    ) : UIState
}

data class TelemetryMessage(
    val description: String,
    val weightGrams: String,
    val pressureBars: String,
    val dutyCyclePercent: String,
    val flowRateGPS: String,
    val brewTempC: String
)