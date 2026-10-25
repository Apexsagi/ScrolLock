package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.repository.UserPreferences
import com.example.domain.model.AppBudget
import com.example.domain.model.CapabilityState
import com.example.domain.model.LockMode
import com.example.domain.model.MonitoringHealthInfo
import com.example.ui.ScreenDestination
import com.example.ui.components.ScreenLockBottomBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.settings.SettingsLimitsSection
import com.example.ui.theme.ScrollLockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ResponsiveLayoutRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleBudgets = listOf(
        AppBudget(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            category = "Social",
            dailyLimit = 50,
            screenTimeLimitMinutes = 60,
            launchLimit = 20,
            scrollCountingSupported = true,
            isEnabled = true,
            lockMode = LockMode.STANDARD
        ),
        AppBudget(
            packageName = "com.google.android.youtube",
            displayName = "YouTube",
            category = "Video",
            dailyLimit = 30,
            screenTimeLimitMinutes = 45,
            launchLimit = 15,
            scrollCountingSupported = true,
            isEnabled = true,
            lockMode = LockMode.STRICT
        )
    )

    private val sampleHealth = MonitoringHealthInfo(
        capabilityState = CapabilityState.AVAILABLE,
        usageAccessGranted = true,
        accessibilityGranted = true,
        overlayGranted = true,
        notificationGranted = true,
        lastEventTimestamp = System.currentTimeMillis(),
        diagnosticMessage = "All services operational",
        observedEventsToday = 15
    )

    @Test
    @Config(qualifiers = "w320dp-h640dp-normal-port")
    fun testDashboardRendersOnNarrowScreen() {
        var navigatedTo: ScreenDestination? = null
        var refreshClicked = false

        composeTestRule.setContent {
            ScrollLockTheme {
                DashboardScreen(
                    budgets = sampleBudgets,
                    scrollCountsMap = mapOf("com.instagram.android" to 12, "com.google.android.youtube" to 5),
                    screenTimesMap = mapOf("com.instagram.android" to 15 * 60 * 1000L, "com.google.android.youtube" to 20 * 60 * 1000L),
                    launchesMap = mapOf("com.instagram.android" to 4, "com.google.android.youtube" to 2),
                    health = sampleHealth,
                    onNavigate = { dest -> navigatedTo = dest },
                    onSelectAppDetail = {},
                    onRefresh = { refreshClicked = true }
                )
            }
        }

        // Verify header elements are present and clickable
        composeTestRule.onNodeWithTag("dashboard_refresh_button").assertIsDisplayed().performClick()
        assert(refreshClicked)

        composeTestRule.onNodeWithTag("dashboard_settings_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_add_app_text_button").assertIsDisplayed()

        // Verify bottom action button is present and clickable
        composeTestRule.onNodeWithTag("fab_add_apps").assertIsDisplayed().performClick()
        assertEquals(ScreenDestination.APP_SELECTION, navigatedTo)

        // Verify app cards are rendered and accessible without clipping
        composeTestRule.onNodeWithTag("budget_card_com.instagram.android").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-normal-port")
    fun testDashboardRendersOnStandardPhone() {
        var navigatedTo: ScreenDestination? = null

        composeTestRule.setContent {
            ScrollLockTheme {
                DashboardScreen(
                    budgets = sampleBudgets,
                    scrollCountsMap = emptyMap(),
                    screenTimesMap = emptyMap(),
                    launchesMap = emptyMap(),
                    health = sampleHealth,
                    onNavigate = { dest -> navigatedTo = dest },
                    onSelectAppDetail = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("fab_add_apps").assertIsDisplayed()
        composeTestRule.onNodeWithTag("budget_card_com.instagram.android").assertIsDisplayed()
        composeTestRule.onNodeWithTag("budget_card_com.google.android.youtube").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w430dp-h932dp-normal-port")
    fun testDashboardRendersOnLargePhone() {
        composeTestRule.setContent {
            ScrollLockTheme {
                DashboardScreen(
                    budgets = sampleBudgets,
                    scrollCountsMap = emptyMap(),
                    screenTimesMap = emptyMap(),
                    launchesMap = emptyMap(),
                    health = sampleHealth,
                    onNavigate = {},
                    onSelectAppDetail = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("fab_add_apps").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_refresh_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_settings_button").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h540dp-normal-port")
    fun testDashboardRendersOnShortDisplay() {
        composeTestRule.setContent {
            ScrollLockTheme {
                DashboardScreen(
                    budgets = sampleBudgets,
                    scrollCountsMap = emptyMap(),
                    screenTimesMap = emptyMap(),
                    launchesMap = emptyMap(),
                    health = sampleHealth,
                    onNavigate = {},
                    onSelectAppDetail = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("fab_add_apps").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-normal-port")
    fun testBottomNavigationOnNarrowScreen() {
        var selectedDest: ScreenDestination? = null

        composeTestRule.setContent {
            ScrollLockTheme {
                ScreenLockBottomBar(
                    currentDestination = ScreenDestination.DASHBOARD,
                    onNavigate = { dest -> selectedDest = dest }
                )
            }
        }

        composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_stats").assertIsDisplayed().performClick()
        assertEquals(ScreenDestination.ANALYTICS, selectedDest)

        composeTestRule.onNodeWithTag("nav_rules").assertIsDisplayed().performClick()
        assertEquals(ScreenDestination.BUDGET_SETTINGS, selectedDest)

        composeTestRule.onNodeWithTag("nav_profile").assertIsDisplayed().performClick()
        assertEquals(ScreenDestination.PRIVACY, selectedDest)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-normal-port")
    fun testPermissionBannerOnNarrowScreen() {
        var fixClicked = false
        val degradedHealth = sampleHealth.copy(capabilityState = CapabilityState.PERMISSION_REQUIRED)

        composeTestRule.setContent {
            ScrollLockTheme {
                DashboardScreen(
                    budgets = sampleBudgets,
                    scrollCountsMap = emptyMap(),
                    screenTimesMap = emptyMap(),
                    launchesMap = emptyMap(),
                    health = degradedHealth,
                    onNavigate = { if (it == ScreenDestination.MONITORING_HEALTH) fixClicked = true },
                    onSelectAppDetail = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("dashboard_health_banner").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-normal-port")
    fun testAppCardPresetsAndCustomInputOnNarrowScreen() {
        var updatedPkg: String? = null
        var updatedLimit: Int? = null

        composeTestRule.setContent {
            ScrollLockTheme {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        SettingsLimitsSection(
                            budgets = sampleBudgets,
                            userPreferences = UserPreferences(),
                            onUpdateLimit = { pkg, limit ->
                                updatedPkg = pkg
                                updatedLimit = limit
                            },
                            onUpdateLockMode = { _, _ -> },
                            onToggleAppEnabled = { _, _ -> },
                            onToggleStrictModeProtection = {},
                            onSetDefaultLimits = { _, _ -> },
                            onSetLimitReachedAction = {},
                            onSetEmergencyGraceSettings = { _, _ -> },
                            onOpenSetPin = {},
                            onOpenChangePin = {},
                            onOpenRemovePin = {},
                            onUpdateSecurityControl = { _, _, _, _, _ -> },
                            onUpdateRandomReset = { _, _, _ -> },
                            executeWithPinCheck = { _, action -> action() }
                        )
                    }
                }
            }
        }

        // Verify Instagram toggle switch is displayed and accessible
        composeTestRule.onNodeWithTag("toggle_app_com.instagram.android")
            .performScrollTo()
            .assertIsDisplayed()

        // Verify Custom scroll input is displayed and accepts input
        composeTestRule.onNodeWithTag("custom_scroll_input_com.instagram.android")
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput("150")

        // Verify Apply button is displayed and triggers limit update
        composeTestRule.onNodeWithTag("apply_custom_scroll_com.instagram.android")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals("com.instagram.android", updatedPkg)
        assertEquals(150, updatedLimit)
    }
}
