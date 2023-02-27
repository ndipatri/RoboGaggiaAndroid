package com.ndipatri.robogaggia

fun renderDummyTelemetry(): List<TelemetryMessage> {
    return rawTelemetryString.split("\n").map { line ->
        line.split(",").let {
            TelemetryMessage(
                description = it[1],
                weightGrams = it[2],
                pressureBars = it[3],
                dutyCyclePercent = it[4],
                flowRateGPS = it[5],
                brewTempC = it[6],
            )
        }
    }
}

val rawTelemetryString = "preInfusion, PID(0.200000:1.000000:2.000000), 0, -1.000000, 40.400000, 0.000000, 105.750000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 105.000000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 104.750000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 104.250000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 104.250000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 103.250000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 103.000000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 101.500000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 100.750000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 100.000000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 0.000000, 40.400000, 0.000000, 100.500000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 1.000000, 40.400000, 0.000000, 101.500000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 1.000000, 40.400000, 0.000000, 104.000000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 0, 1.000000, 40.400000, 0.000000, 106.250000\n" +
        "preInfusion, PID(0.200000:1.000000:2.000000), 1, 2.000000, 40.400000, 0.833333, 109.250000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 2, 2.000000, 41.266667, 0.833333, 110.250000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 4, 2.000000, 43.000000, 1.666667, 111.750000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 5, 2.000000, 43.900000, 0.833333, 111.750000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 7, 4.000000, 45.800000, 1.666667, 111.000000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 9, 4.000000, 46.700000, 1.666667, 110.250000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 11, 4.000000, 47.766667, 1.666667, 109.000000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 14, 5.000000, 48.833333, 2.500000, 107.500000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 16, 5.000000, 49.066667, 1.666667, 106.000000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 18, 6.000000, 50.300000, 1.666667, 105.000000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 21, 6.000000, 51.366667, 2.500000, 103.250000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 24, 5.000000, 51.600000, 2.500000, 101.500000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 26, 5.000000, 52.100000, 1.666667, 100.250000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 29, 6.000000, 53.333333, 2.500000, 99.250000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 30, 5.000000, 53.566667, 0.833333, 99.000000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 32, 6.000000, 55.633333, 1.666667, 99.750000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 34, 6.000000, 56.533333, 1.666667, 101.250000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 36, 8.000000, 57.600000, 1.666667, 103.250000\n" +
        "brewing, PID(0.200000:1.000000:2.000000), 38, 8.000000, 57.866667, 1.666667, 104.000000"
