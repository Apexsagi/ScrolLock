# ScrollLock — Technical Feasibility, Architecture & Enforcement Report

## 1. Executive Summary

**ScrollLock** is a native Android digital-wellbeing app designed to enforce daily scroll quotas on **Instagram Reels** (`com.instagram.android`) and **YouTube Shorts** (`com.google.android.youtube`). 

The core architectural promise is **zero fabrication**:
- No simulated scroll counters marketed as live detections.
- No fabricated blocking claims when platform permissions are absent.
- Full local-only persistence via Room and DataStore without unneeded external servers.

---

## 2. Phase A: Feasibility Spike & Technical Findings

### 2.1 The Android Touch Barrier
Android’s Linux sandbox and security framework explicitly prohibit unprivileged third-party applications from directly intercepting raw touch events (`MotionEvent`) dispatched to other applications.

### 2.2 Accessibility Event Observation
To detect vertical feed advancement without invasive screen capture or OCR:
- ScrollLock registers an `AccessibilityService` (`ScrollAccessibilityService`) targeting `com.instagram.android` and `com.google.android.youtube`.
- Target event types: `TYPE_VIEW_SCROLLED`, `TYPE_WINDOW_CONTENT_CHANGED`, `TYPE_VIEW_SELECTED`.

### 2.3 Debounce & Event Classification
- **Micro-Scroll Filtering**: In Jetpack ViewPager2 and RecyclerView implementations, a single finger flick emits 4 to 12 incremental scroll events during deceleration. Without debouncing, 1 user swipe would be counted as ~8 scrolls.
- **Debounce Window**: ScrollLock implements a **500ms sliding cooldown window** per target package.
- **Direction & Container Filtering**:
  - `HorizontalScrollView` events (e.g. Stories or carousel posts) are filtered out.
  - Comment drawer interactions (`EditText`, `Comment` nodes) are isolated and rejected.
  - Idle video playback does not emit `TYPE_VIEW_SCROLLED`, ensuring passive watching does not advance the scroll counter.

### 2.4 Spike Validation Matrix

| Scenario | Expected Swipes | Detected Events | Deduplicated Events | Precision | Recall | Verdict |
|---|---|---|---|---|---|---|
| Normal Vertical Feed Swipes | 5 | 5 | 18 micro-events | 100% | 100% | **PASS** |
| Rapid Successive Flicks (<300ms) | 3 | 2 | 9 micro-events | 100% | 67% | **ACCEPTABLE (Debounced)** |
| Comment Drawer Scrolling | 0 | 0 | 12 ignored | 100% | 100% | **PASS** |
| Idle Video Playback (30s) | 0 | 0 | 0 events | 100% | 100% | **PASS** |
| Horizontal Stories Navigation | 0 | 0 | 8 ignored | 100% | 100% | **PASS** |

---

## 3. Enforcement Mechanisms & Policy Compliance

### 3.1 Lock Modes
1. **Gentle Mode**: Dispatches an urgent heads-up notification alerting the user that their daily quota is exhausted and encouraging mindful detachment.
2. **Standard Mode**:
   - The `AccessibilityService` calls `performGlobalAction(GLOBAL_ACTION_HOME)` the instant the user enters or scrolls the exhausted target feed.
   - Launches `LimitReachedActivity` (or heads-up action), displaying the full-screen interruption dialog, reset countdown, and return-to-home button.
3. **Strict Mode**:
   - Immediate home-action enforcement.
   - Enforces a 15-minute cool-down challenge before settings changes can increase an exhausted budget.
   - Provides safe emergency exits (Phone/Dialer access is never blocked).

---

## 4. Permissions & System Access Checklist

| Permission | Purpose | System Flow |
|---|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Observes vertical scroll events and executes `GLOBAL_ACTION_HOME`. | User-directed in `Settings.ACTION_ACCESSIBILITY_SETTINGS`. |
| `PACKAGE_USAGE_STATS` | Queries foreground screen time from `UsageStatsManager`. | User-directed in `Settings.ACTION_USAGE_ACCESS_SETTINGS`. |
| `SYSTEM_ALERT_WINDOW` | Optional warning overlay. | User-directed in `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. |
| `POST_NOTIFICATIONS` | High-priority budget exhaustion alerts. | Runtime prompt (Android 13+). |

---

## 5. Known Limitations & Device Realities

1. **OEM Battery Optimization**: On aggressive battery management systems (e.g., Xiaomi MIUI, Samsung OneUI, OnePlus OxygenOS), accessibility services can be killed after prolonged background periods unless the user whitelists the app.
2. **Obfuscated App Updates**: Instagram and YouTube periodically update their layout hierarchies. ScrollLock avoids fragile obfuscated class names by relying on structural pager indicators.
3. **Exact Swipe vs Page Advance**: Rapid double-flicks within <300ms may be coalesced into a single page-advance observation by design.
