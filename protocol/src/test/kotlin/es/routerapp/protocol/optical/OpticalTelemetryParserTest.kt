package es.routerapp.protocol.optical

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpticalTelemetryParserTest {

    @Test
    fun parseOptimalOpticalPower() {
        val input = mapOf(
            "rxPower" to "-19.50 dBm",
            "txPower" to "+2.80 dBm",
            "temperature" to "44.2 C",
            "voltage" to "3.31 V",
            "biasCurrent" to "14.8 mA",
        )
        val telemetry = OpticalTelemetryParser.parse(input)

        assertEquals(-19.50f, telemetry.rxPowerDbm ?: 0f, 0.01f)
        assertEquals(2.80f, telemetry.txPowerDbm ?: 0f, 0.01f)
        assertEquals(44.2f, telemetry.temperatureC ?: 0f, 0.1f)
        assertEquals(3.31f, telemetry.voltageV ?: 0f, 0.01f)
        assertEquals(14.8f, telemetry.biasCurrentMa ?: 0f, 0.1f)
        assertEquals(OpticalHealthStatus.OPTIMAL, telemetry.status)
        assertTrue(telemetry.hasValidSignal)
        assertFalse(telemetry.isExternalOnt)
    }

    @Test
    fun parseOptimalBoundaryThresholds() {
        val atBoundary = OpticalTelemetryParser.parse(mapOf("rxPower" to "-25.0"))
        assertEquals(OpticalHealthStatus.OPTIMAL, atBoundary.status)

        val atMaxBoundary = OpticalTelemetryParser.parse(mapOf("rxPower" to "-8.0"))
        assertEquals(OpticalHealthStatus.OPTIMAL, atMaxBoundary.status)
    }

    @Test
    fun parseMarginalOpticalPower() {
        val input = mapOf("rx_power" to "-26.40")
        val telemetry = OpticalTelemetryParser.parse(input)

        assertEquals(-26.40f, telemetry.rxPowerDbm ?: 0f, 0.01f)
        assertEquals(OpticalHealthStatus.MARGINAL, telemetry.status)
        assertTrue(telemetry.hasValidSignal)
    }

    @Test
    fun parseCriticalOpticalPower() {
        val input = mapOf("opticalRxPower" to "-28.50 dBm")
        val telemetry = OpticalTelemetryParser.parse(input)

        assertEquals(-28.50f, telemetry.rxPowerDbm ?: 0f, 0.01f)
        assertEquals(OpticalHealthStatus.CRITICAL, telemetry.status)
        assertTrue(telemetry.hasValidSignal)
    }

    @Test
    fun parseSaturatedOpticalPower() {
        val input = mapOf("rxPower" to "-5.2")
        val telemetry = OpticalTelemetryParser.parse(input)

        assertEquals(-5.2f, telemetry.rxPowerDbm ?: 0f, 0.01f)
        assertEquals(OpticalHealthStatus.SATURATED, telemetry.status)
    }

    @Test
    fun parseScaledHundredthsAndThousandths() {
        // Router returns -2145 (representing -21.45 dBm)
        val scaledHundredths = OpticalTelemetryParser.parse(mapOf("rxPower" to "-2145"))
        assertEquals(-21.45f, scaledHundredths.rxPowerDbm ?: 0f, 0.01f)
        assertEquals(OpticalHealthStatus.OPTIMAL, scaledHundredths.status)

        // Router returns -28900 (representing -28.9 dBm)
        val scaledThousandths = OpticalTelemetryParser.parse(mapOf("rxPower" to "-28900"))
        assertEquals(-28.9f, scaledThousandths.rxPowerDbm ?: 0f, 0.01f)
        assertEquals(OpticalHealthStatus.CRITICAL, scaledThousandths.status)
    }

    @Test
    fun parseExternalOntFallback() {
        val opticalEmpty = emptyMap<String, String>()
        val wanEthernet = mapOf(
            "name" to "IP_Routed_ETH",
            "type" to "Ethernet",
            "connStatusV4" to "Connected",
        )
        val telemetry = OpticalTelemetryParser.parse(opticalEmpty, wanEthernet)

        assertNull(telemetry.rxPowerDbm)
        assertEquals(OpticalHealthStatus.EXTERNAL_ONT, telemetry.status)
        assertTrue(telemetry.isExternalOnt)
        assertFalse(telemetry.hasValidSignal)
    }

    @Test
    fun parseEmptyOrUnknown() {
        val empty = OpticalTelemetryParser.parse(emptyMap())
        assertEquals(OpticalHealthStatus.UNKNOWN, empty.status)
        assertNull(empty.rxPowerDbm)
        assertFalse(empty.hasValidSignal)
    }

    @Test
    fun parseTr181StandardPropertyNames() {
        val tr181Input = mapOf(
            "opticalSignalLevel" to "-22.30",
            "transmitOpticalLevel" to "+2.15",
            "transceiverTemp" to "46.5",
            "supplyVoltage" to "3.28",
            "laserBiasCurrent" to "13.2",
        )
        val telemetry = OpticalTelemetryParser.parse(tr181Input)

        assertEquals(-22.30f, telemetry.rxPowerDbm ?: 0f, 0.01f)
        assertEquals(2.15f, telemetry.txPowerDbm ?: 0f, 0.01f)
        assertEquals(46.5f, telemetry.temperatureC ?: 0f, 0.1f)
        assertEquals(3.28f, telemetry.voltageV ?: 0f, 0.01f)
        assertEquals(13.2f, telemetry.biasCurrentMa ?: 0f, 0.1f)
        assertEquals(OpticalHealthStatus.OPTIMAL, telemetry.status)
    }

    @Test
    fun parseAlternativeRxTxAliases() {
        val input = mapOf(
            "rxOpticalPower" to "-18.90 dBm",
            "txOpticalPower" to "+3.10 dBm",
        )
        val telemetry = OpticalTelemetryParser.parse(input)

        assertEquals(-18.90f, telemetry.rxPowerDbm ?: 0f, 0.01f)
        assertEquals(3.10f, telemetry.txPowerDbm ?: 0f, 0.01f)
        assertEquals(OpticalHealthStatus.OPTIMAL, telemetry.status)
    }

    @Test
    fun parseZeroDbmTreatedAsUnknownNotSaturated() {
        val zeroInput = mapOf("rxPower" to "0.0 dBm")
        val telemetry = OpticalTelemetryParser.parse(zeroInput)

        assertNull(telemetry.rxPowerDbm)
        assertEquals(OpticalHealthStatus.UNKNOWN, telemetry.status)
        assertFalse(telemetry.hasValidSignal)
    }

    @Test
    fun parseZeroStringIntegerTreatedAsUnknown() {
        val zeroInput = mapOf("rxPower" to "0")
        val telemetry = OpticalTelemetryParser.parse(zeroInput)

        assertNull(telemetry.rxPowerDbm)
        assertEquals(OpticalHealthStatus.UNKNOWN, telemetry.status)
        assertFalse(telemetry.hasValidSignal)
    }

    @Test
    fun parsePositiveDummyMagnitudeTreatedAsUnknown() {
        val dummyPositive = mapOf("rxPower" to "+0.00")
        val telemetry = OpticalTelemetryParser.parse(dummyPositive)

        assertNull(telemetry.rxPowerDbm)
        assertEquals(OpticalHealthStatus.UNKNOWN, telemetry.status)
    }
}

