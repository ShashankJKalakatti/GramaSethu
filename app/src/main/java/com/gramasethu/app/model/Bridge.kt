package com.gramasethu.app.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import java.util.concurrent.TimeUnit

enum class BridgeStatus { OPEN, DAMAGED, SUBMERGED, UNKNOWN }

data class Bridge(
    @DocumentId val id: String = "",
    val name: String = "",
    val village: String = "",
    val district: String = "",
    val taluk: String = "",
    val location: GeoPoint? = null,
    val status: String = BridgeStatus.UNKNOWN.name,
    val waterLevel: Double = 0.0,
    val lastReportedBy: String = "",
    val lastUpdated: Timestamp? = null,
    val alternativeRoute: String = "",
    val catchmentArea: String = ""
) {
    val statusEnum: BridgeStatus
        get() = runCatching { BridgeStatus.valueOf(status) }.getOrDefault(BridgeStatus.UNKNOWN)

    val isStale: Boolean
        get() {
            val ts = lastUpdated ?: return true
            return TimeUnit.MILLISECONDS.toHours(
                System.currentTimeMillis() - ts.toDate().time
            ) >= 6
        }

    val effectiveStatus: BridgeStatus
        get() = if (isStale) BridgeStatus.UNKNOWN else statusEnum

    val freshnessLabel: String
        get() {
            val ts = lastUpdated ?: return "No reports yet"
            val diffMs = System.currentTimeMillis() - ts.toDate().time
            val mins  = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
            return when {
                hours >= 6 -> "Status Unknown (>6 hrs old)"
                hours >= 1 -> "Updated ${hours}h ago"
                mins  < 1  -> "Updated just now"
                else       -> "Updated ${mins} min${if (mins > 1) "s" else ""} ago"
            }
        }
}
