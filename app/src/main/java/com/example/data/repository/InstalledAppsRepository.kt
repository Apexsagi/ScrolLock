package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.domain.model.DiscoveredApp
import com.example.domain.model.MonitoredAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository(
    private val context: Context,
    private val usageStatsRepository: UsageStatsRepository
) {

    suspend fun getDiscoveredApps(): List<DiscoveredApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val installedResolveInfos = try {
            packageManager.queryIntentActivities(mainIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val installedMap = installedResolveInfos.associateBy { it.activityInfo.packageName }

        val resultList = mutableListOf<DiscoveredApp>()
        val processedPackages = mutableSetOf<String>()

        // 1. First add/match the popular social & video catalog apps
        MonitoredAppInfo.POPULAR_CATALOG.forEach { catalogApp ->
            val resolveInfo = installedMap[catalogApp.packageName]
            val isInstalled = resolveInfo != null
            val icon = resolveInfo?.loadIcon(packageManager)
            val usageMillis = usageStatsRepository.getScreenTimeTodayMillis(catalogApp.packageName)

            resultList.add(
                DiscoveredApp(
                    packageName = catalogApp.packageName,
                    appName = resolveInfo?.loadLabel(packageManager)?.toString() ?: catalogApp.displayName,
                    category = catalogApp.category,
                    isInstalled = isInstalled,
                    isScrollTrackingSupported = catalogApp.isScrollTrackingSupported,
                    currentUsageMillis = usageMillis,
                    iconDrawable = icon
                )
            )
            processedPackages.add(catalogApp.packageName)
        }

        // 2. Discover all other installed launchable apps on the user's device
        installedResolveInfos.forEach { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (!processedPackages.contains(pkg) && pkg != context.packageName) {
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager)
                val category = detectAppCategory(resolveInfo.activityInfo.applicationInfo, pkg, appName)
                val usageMillis = usageStatsRepository.getScreenTimeTodayMillis(pkg)
                val scrollSupported = MonitoredAppInfo.isScrollTrackingSupportedFor(pkg)

                resultList.add(
                    DiscoveredApp(
                        packageName = pkg,
                        appName = appName,
                        category = category,
                        isInstalled = true,
                        isScrollTrackingSupported = scrollSupported,
                        currentUsageMillis = usageMillis,
                        iconDrawable = icon
                    )
                )
                processedPackages.add(pkg)
            }
        }

        resultList.sortedWith(
            compareByDescending<DiscoveredApp> { it.isInstalled }
                .thenBy { it.appName.lowercase() }
        )
    }

    private fun detectAppCategory(appInfo: ApplicationInfo?, packageName: String, appName: String): String {
        val lowerPkg = packageName.lowercase()
        val lowerName = appName.lowercase()

        // 1. Android ApplicationInfo Category
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && appInfo != null) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return "Games"
                ApplicationInfo.CATEGORY_VIDEO -> return "Video"
                ApplicationInfo.CATEGORY_SOCIAL -> return "Social"
                ApplicationInfo.CATEGORY_NEWS -> return "News"
                ApplicationInfo.CATEGORY_MAPS -> return "Maps"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return "Productivity"
                ApplicationInfo.CATEGORY_AUDIO -> return "Audio"
                ApplicationInfo.CATEGORY_IMAGE -> return "Photos"
            }
        }

        // 2. Package Name & Label Heuristics
        return when {
            lowerPkg.contains("game") || lowerName.contains("game") || lowerPkg.contains("play") -> "Games"
            lowerPkg.contains("video") || lowerPkg.contains("tube") || lowerPkg.contains("stream") ||
                    lowerPkg.contains("netflix") || lowerPkg.contains("twitch") || lowerPkg.contains("hulu") -> "Video"
            lowerPkg.contains("social") || lowerPkg.contains("chat") || lowerPkg.contains("messenger") ||
                    lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("snapchat") ||
                    lowerPkg.contains("reddit") || lowerPkg.contains("twitter") || lowerPkg.contains("discord") ||
                    lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("tiktok") -> "Social"
            lowerPkg.contains("music") || lowerPkg.contains("spotify") || lowerPkg.contains("audio") || lowerPkg.contains("podcast") -> "Audio"
            lowerPkg.contains("doc") || lowerPkg.contains("sheet") || lowerPkg.contains("note") || lowerPkg.contains("office") -> "Productivity"
            else -> "General"
        }
    }
}
