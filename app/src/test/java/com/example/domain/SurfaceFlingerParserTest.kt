package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceFlingerParserTest {

    @Test
    fun testFindLayerForPackage_prefersSurfaceView() {
        val sampleListOutput = """
            StatusBar#0
            NavigationBar0#0
            com.example.game/com.example.game.MainActivity#0
            SurfaceView[com.example.game/com.example.game.MainActivity]#0
            Background for - SurfaceView[com.example.game/com.example.game.MainActivity]#0
        """.trimIndent()

        val layer = SurfaceFlingerParser.findLayerForPackage(sampleListOutput, "com.example.game")
        assertEquals("SurfaceView[com.example.game/com.example.game.MainActivity]#0", layer)
    }

    @Test
    fun testFindLayerForPackage_fallsBackToMainActivity() {
        val sampleListOutput = """
            StatusBar#0
            NavigationBar0#0
            com.miHoYo.GenshinImpact/com.miHoYo.GetMobileActivity#0
        """.trimIndent()

        val layer = SurfaceFlingerParser.findLayerForPackage(sampleListOutput, "com.miHoYo.GenshinImpact")
        assertEquals("com.miHoYo.GenshinImpact/com.miHoYo.GetMobileActivity#0", layer)
    }

    @Test
    fun testFindLayerForPackage_returnsNullWhenNotFound() {
        val sampleListOutput = """
            StatusBar#0
            NavigationBar0#0
        """.trimIndent()

        val layer = SurfaceFlingerParser.findLayerForPackage(sampleListOutput, "com.nonexistent.game")
        assertNull(layer)
    }

    @Test
    fun testParseLatencyOutput_valid60FpsStream() {
        // 60 Hz = refresh period 16666666 ns
        val periodNs = 16_666_666L
        val basePresentTimeNs = 100_000_000_000L

        val sb = StringBuilder()
        sb.append(periodNs).append("\n")

        // Generate 60 frames spaced by 16.666ms within 1 second
        for (i in 0 until 60) {
            val appSample = basePresentTimeNs + (i * periodNs) - 10_000_000L
            val readyTime = basePresentTimeNs + (i * periodNs) - 2_000_000L
            val presentTime = basePresentTimeNs + (i * periodNs)
            sb.append("$appSample $readyTime $presentTime\n")
        }

        val result = SurfaceFlingerParser.parseLatencyOutput(sb.toString())
        assertNotNull(result)
        assertEquals(60f, result!!.fps, 1.0f)
        assertEquals(16.66f, result.frameTimeMs, 0.5f)
        assertEquals(periodNs, result.refreshPeriodNs)
        assertEquals(0f, result.jankPercent, 0.1f)
    }

    @Test
    fun testParseLatencyOutput_handlesJankDetection() {
        val periodNs = 16_666_666L
        val basePresentTimeNs = 100_000_000_000L
        val sb = StringBuilder()
        sb.append(periodNs).append("\n")

        // 10 normal frames
        var currentTime = basePresentTimeNs
        for (i in 0 until 10) {
            currentTime += periodNs
            sb.append("0 0 $currentTime\n")
        }

        // 1 jank frame (delta > 1.5 * periodNs e.g. 35ms)
        currentTime += (periodNs * 2.2).toLong()
        sb.append("0 0 $currentTime\n")

        val result = SurfaceFlingerParser.parseLatencyOutput(sb.toString())
        assertNotNull(result)
        assertTrue("Jank percent should be greater than 0", result!!.jankPercent > 0f)
    }

    @Test
    fun testParseLatencyOutput_handlesEmptyAndCorruptOutputGracefully() {
        val emptyResult = SurfaceFlingerParser.parseLatencyOutput("")
        assertNull(emptyResult)

        val corruptResult = SurfaceFlingerParser.parseLatencyOutput("not_a_number\nsome random text")
        assertNotNull(corruptResult)
        assertEquals(0f, corruptResult!!.fps, 0.01f)
    }
}
