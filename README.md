This project is an Android companion to the [RoboGaggia](https://github.com/ndipatri/RoboGaggia) Arduino project.

This application subscribes to the 'telemetry' Adafruit.IO MQTT topic ***{username]/feeds/robogaggiatelemetry***

This RoboGaggia publishes to this same topic while it is in its PREINFUSION and BREW states.  This application renders a life graph of critical brew metrics in real time.  These metrics include:

***state, measuredWeight, measuredPressure, dutyCycle, flowRate, tempC***

