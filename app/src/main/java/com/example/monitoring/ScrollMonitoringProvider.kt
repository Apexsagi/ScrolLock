package com.example.monitoring

import android.content.Context
import com.example.domain.model.CapabilityState
import com.example.domain.model.MonitoredAppInfo
import com.example.domain.model.MonitoringHealthInfo
import com.example.domain.model.ScrollObservation
import kotlinx.coroutines.flow.Flow

interface ScrollMonitoringProvider {
    fun getCapabilityState(context: Context): CapabilityState
    fun getMonitoringHealthInfo(context: Context, observedCountToday: Int): MonitoringHealthInfo
    fun getSupportedApps(): List<MonitoredAppInfo>
    val observationEvents: Flow<ScrollObservation>
    val lastObservationTimestamp: Long?
    fun isAccessibilityServiceEnabled(context: Context): Boolean
    fun isUsageAccessEnabled(context: Context): Boolean
    fun isOverlayPermissionEnabled(context: Context): Boolean
}
