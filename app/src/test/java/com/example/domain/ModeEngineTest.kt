package com.example.domain

import com.example.model.PerformanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeEngineTest {

    private val engine = ModeEngine()

    @Test
    fun testSafetyRule_blocksSystemAndOemPrefixes() {
        // Forbidden prefixes: android, com.android, com.google.android.gms, com.mediatek, com.transsion, com.hoffnung
        assertFalse(ModeEngine.isSafeThirdPartyPackage("android"))
        assertFalse(ModeEngine.isSafeThirdPartyPackage("com.android.systemui"))
        assertFalse(ModeEngine.isSafeThirdPartyPackage("com.android.settings"))
        assertFalse(ModeEngine.isSafeThirdPartyPackage("com.google.android.gms"))
        assertFalse(ModeEngine.isSafeThirdPartyPackage("com.google.android.gsf"))
        assertFalse(ModeEngine.isSafeThirdPartyPackage("com.mediatek.engineermode"))
        assertFalse(ModeEngine.isSafeThirdPartyPackage("com.transsion.hilauncher"))
        assertFalse(ModeEngine.isSafeThirdPartyPackage("com.transsion.hios"))
        assertFalse(ModeEngine.isSafeThirdPartyPackage("com.transsion.xos"))
        assertFalse(ModeEngine.isSafeThirdPartyPackage("com.hoffnung.security"))

        // Legitimate third-party apps should be allowed
        assertTrue(ModeEngine.isSafeThirdPartyPackage("com.spotify.music"))
        assertTrue(ModeEngine.isSafeThirdPartyPackage("org.telegram.messenger"))
        assertTrue(ModeEngine.isSafeThirdPartyPackage("com.discord"))
    }

    @Test
    fun testBuildActionPlan_performanceFiltersOutSystemPackages() {
        val mixedPackages = setOf(
            "com.spotify.music",
            "com.transsion.hios",
            "com.mediatek.telephony",
            "com.discord",
            "com.android.systemui"
        )

        val plan = engine.buildActionPlan(
            targetMode = PerformanceMode.PERFORMANCE,
            allowedThirdPartyPackages = mixedPackages,
            baselineDnd = 1,
            baselineRefreshRate = "60.0"
        )

        // Find KillWhitelistedThirdPartyAction
        val killAction = plan.filterIsInstance<KillWhitelistedThirdPartyAction>().firstOrNull()
        assertTrue("Kill action should exist in Performance mode", killAction != null)

        // Ensure only Spotify and Discord are included
        // Description includes count
        assertTrue(killAction!!.description.contains("2 user-whitelisted apps"))
    }

    @Test
    fun testBuildActionPlan_balancedRestoresDefaults() {
        val plan = engine.buildActionPlan(
            targetMode = PerformanceMode.BALANCED,
            allowedThirdPartyPackages = setOf("com.spotify.music"),
            baselineDnd = 1,
            baselineRefreshRate = "60.0"
        )

        assertEquals(1, plan.size)
        assertTrue(plan[0] is RestoreDefaultsAction)
        assertTrue(plan[0].isReversible)
    }

    @Test
    fun testBuildActionPlan_batterySaverSetsLowerRefreshRate() {
        val plan = engine.buildActionPlan(
            targetMode = PerformanceMode.BATTERY_SAVER,
            allowedThirdPartyPackages = emptySet(),
            baselineDnd = 1,
            baselineRefreshRate = "60.0"
        )

        val refreshRateAction = plan.filterIsInstance<RefreshRateAction>().firstOrNull()
        assertTrue(refreshRateAction != null)
        assertTrue(refreshRateAction!!.description.contains("60.0 Hz"))
    }
}
