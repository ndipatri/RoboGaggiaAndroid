# RoboGaggia Android

This project is an Android companion to the [RoboGaggia](https://github.com/ndipatri/RoboGaggia) Arduino project.

This application subscribes to the 'telemetry' Adafruit.IO MQTT topic ***{username]/feeds/robogaggiatelemetry***

This RoboGaggia publishes to this same topic while it is in its PREINFUSION and BREW states.  This application renders a life graph of critical brew metrics in real time.  These metrics include:

***state, measuredWeight, measuredPressure, dutyCycle, flowRate, tempC***

# Setup

You must include a ***secrets.properties*** file in the root directory of this project with the following contents:

```
TEST_MODE=false
AIO_PASSWORD="{your adafruit username}"
AIO_USERNAME="{your adafruit api key}"
```

If you set ***TEST_MODE*** to true, this app will create local fake telemetry data for testing purposes and will NOT subcribe to the Adafruit.IO MQTT broker. 

