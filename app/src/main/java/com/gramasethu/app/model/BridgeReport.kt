package com.gramasethu.app.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class BridgeReport(
    @DocumentId val id: String = "",
    val bridgeId: String = "",
    val reportedStatus: String = "",
    val reporterName: String = "Anonymous",
    val notes: String = "",
    val reporterLocation: GeoPoint? = null,
    val waterLevelObserved: Double = 0.0,
    val reportedAt: Timestamp? = null
)
