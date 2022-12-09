package com.ndipatri.robogaggia

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ndipatri.robogaggia.theme.RoboGaggiaTheme
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.QoS
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mqtt().getClient(applicationContext)

        setContent {
            RoboGaggiaTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RoboGaggiaTheme {
        Greeting("Android")
    }
}




class Mqtt {

    fun getClient(applicationContext: Context): MqttAndroidClient {

        clientId += System.currentTimeMillis()
        val mqttAndroidClient = MqttAndroidClient(applicationContext, serverUri, clientId)
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
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false
        mqttConnectOptions.userName = "ndipatri"
        mqttConnectOptions.password = "aio_besk78mc7BC8GL37grhZSWCAlj7J".toCharArray()
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

        return mqttAndroidClient
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

    private fun publishMessage(mqttAndroidClient: MqttAndroidClient) {
        val message = MqttMessage()
        message.payload = publishMessage.toByteArray()
        if (mqttAndroidClient.isConnected) {
            mqttAndroidClient.publish(publishTopic, message)
            println("Message Published >${publishMessage}<")
            if (!mqttAndroidClient.isConnected) {
                println(mqttAndroidClient.bufferedMessageCount.toString() + " messages in buffer.")
            }
        } else {
            println("Not Connected")
        }
    }

    companion object {
        private const val serverUri = "tcp://io.adafruit.com:1883"
        private const val subscriptionTopic = "ndipatri/feeds/robogaggiatelemetry"
        private const val publishTopic = "exampleAndroidPublishTopic"
        private const val publishMessage = "Hello World"
        private var clientId = ""
    }
}

