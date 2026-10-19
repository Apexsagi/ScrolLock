package com.example

import android.app.Application
import com.example.data.local.ScrollLockDatabase
import com.example.data.repository.BudgetRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.data.repository.UsageStatsRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.enforcement.EnforcementManager
import com.example.monitoring.AccessibilityScrollMonitor
import com.example.monitoring.FeasibilitySpikeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScrollLockApplication : Application() {

    lateinit var database: ScrollLockDatabase
        private set

    lateinit var budgetRepository: BudgetRepository
        private set

    lateinit var usageStatsRepository: UsageStatsRepository
        private set

    lateinit var installedAppsRepository: InstalledAppsRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var feasibilitySpikeManager: FeasibilitySpikeManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = ScrollLockDatabase.getInstance(this)
        budgetRepository = BudgetRepository(database)
        usageStatsRepository = UsageStatsRepository(this)
        installedAppsRepository = InstalledAppsRepository(this, usageStatsRepository)
        userPreferencesRepository = UserPreferencesRepository(this)

        val monitor = AccessibilityScrollMonitor(this, budgetRepository, usageStatsRepository)
        scrollMonitor = monitor

        val enforcement = EnforcementManager(this, budgetRepository, usageStatsRepository, userPreferencesRepository)
        enforcementManager = enforcement

        feasibilitySpikeManager = FeasibilitySpikeManager(budgetRepository, monitor)

        // Initialize default budgets in database if first run
        CoroutineScope(Dispatchers.IO).launch {
            budgetRepository.initializeDefaultBudgetsIfEmpty()
        }
    }

    companion object {
        lateinit var instance: ScrollLockApplication
            private set

        var scrollMonitor: AccessibilityScrollMonitor? = null
            private set

        var enforcementManager: EnforcementManager? = null
            private set
    }
}
