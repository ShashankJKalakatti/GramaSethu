package com.gramasethu.app.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.gramasethu.app.model.Bridge
import com.gramasethu.app.model.BridgeReport
import com.gramasethu.app.model.BridgeStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BridgeRepository private constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("bridges")

    companion object {
        private const val TAG = "BridgeRepo"
        @Volatile private var INSTANCE: BridgeRepository? = null
        fun get() = INSTANCE ?: synchronized(this) {
            INSTANCE ?: BridgeRepository().also { INSTANCE = it }
        }
    }

    /** Real-time Flow – emits on every Firestore change (< 3 s latency). */
    val bridgesFlow: Flow<List<Bridge>> = callbackFlow {
        val reg: ListenerRegistration = col.orderBy("name")
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e(TAG, "listener error", err); return@addSnapshotListener }
                trySend(snap?.toObjects(Bridge::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun submitReport(report: BridgeReport): Result<Unit> = runCatching {
        db.runTransaction { tx ->
            val bridgeRef = col.document(report.bridgeId)
            val reportRef = db.collection("reports").document()
            tx.set(reportRef, report)
            tx.update(bridgeRef, mapOf(
                "status"         to report.reportedStatus,
                "lastUpdated"    to Timestamp.now(),
                "lastReportedBy" to report.reporterName,
                "waterLevel"     to report.waterLevelObserved
            ))
        }.await()
    }

    suspend fun seedIfEmpty() {
        if (col.limit(1).get().await().isEmpty) {
            Log.d(TAG, "Seeding demo data…")
            listOf(
                Bridge("br_001","Hemavathi Crossing","Gorur","Hassan","Hassan",
                    GeoPoint(13.0827,76.0950),BridgeStatus.OPEN.name,0.4,
                    alternativeRoute="Via Sakleshpur NH-75 (+12 km)",lastUpdated=Timestamp.now()),
                Bridge("br_002","Cauvery Minor Bridge","Kushalnagar","Kodagu","Somwarpet",
                    GeoPoint(12.4600,75.9700),BridgeStatus.SUBMERGED.name,1.9,
                    alternativeRoute="Via Madikeri-Mysore Road (+18 km)",lastUpdated=Timestamp.now()),
                Bridge("br_003","Bhadra Culvert","Tarikere","Chikkamagaluru","Tarikere",
                    GeoPoint(13.7167,75.8167),BridgeStatus.DAMAGED.name,0.9,
                    alternativeRoute="Via Birur-Kadur Road (+8 km)",lastUpdated=Timestamp.now()),
                Bridge("br_004","Tungabhadra Ford","Harihar","Davangere","Harihar",
                    GeoPoint(14.5167,75.7167),BridgeStatus.OPEN.name,0.2,
                    alternativeRoute="No alternative needed",lastUpdated=Timestamp.now()),
                Bridge("br_005","Netravathi Footbridge","Bantwal","Dakshina Kannada","Bantwal",
                    GeoPoint(12.8900,75.0300),BridgeStatus.SUBMERGED.name,2.1,
                    alternativeRoute="Via NH-75 Bantwal bypass (+6 km)",lastUpdated=Timestamp.now()),
                Bridge("br_006","Malaprabha Crossing","Saundatti","Belagavi","Saundatti",
                    GeoPoint(15.7700,75.1200),BridgeStatus.OPEN.name,0.3,
                    alternativeRoute="No alternative needed",lastUpdated=Timestamp.now()),
                Bridge("br_007","Sharavathi Culvert","Sagar","Shivamogga","Sagar",
                    GeoPoint(14.1667,75.0333),BridgeStatus.DAMAGED.name,0.7,
                    alternativeRoute="Via Jog Falls Road (+15 km)",lastUpdated=Timestamp.now()),
                Bridge("br_008","Kabini Bridge","Nanjangud","Mysuru","Nanjangud",
                    GeoPoint(11.8700,76.6800),BridgeStatus.OPEN.name,0.5,
                    alternativeRoute="No alternative needed",lastUpdated=Timestamp.now())
            ).forEach { col.document(it.id).set(it).await() }
        }
    }
}
