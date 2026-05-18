package com.gramasethu.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import com.gramasethu.app.databinding.ActivityReportBinding
import com.gramasethu.app.viewmodel.BridgeViewModel

class ReportActivity : AppCompatActivity() {

    companion object { const val EXTRA_ID = "bridge_id" }

    private lateinit var b: ActivityReportBinding
    private val vm: BridgeViewModel by viewModels()
    private var bridgeId = ""
    private var selected: String? = null
    private var geoLoc: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReportBinding.inflate(layoutInflater)
        setContentView(b.root)
        bridgeId = intent.getStringExtra(EXTRA_ID) ?: run { finish(); return }
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Quick Report"

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this).lastLocation
                .addOnSuccessListener { loc -> loc?.let { geoLoc = GeoPoint(it.latitude, it.longitude) } }
        }

        b.btnOpen.setOnClickListener      { pick("OPEN") }
        b.btnDamaged.setOnClickListener   { pick("DAMAGED") }
        b.btnSubmerged.setOnClickListener { pick("SUBMERGED") }
        b.btnSubmit.isEnabled = false
        b.btnSubmit.setOnClickListener { submit() }

        vm.reportState.observe(this) { state ->
            when (state) {
                is BridgeViewModel.ReportState.Loading -> {
                    b.progressBar.visibility = View.VISIBLE; b.btnSubmit.isEnabled = false
                }
                is BridgeViewModel.ReportState.Success -> {
                    b.progressBar.visibility = View.GONE
                    Toast.makeText(this, "✓ Report submitted! Thank you.", Toast.LENGTH_LONG).show()
                    vm.resetReport(); finish()
                }
                is BridgeViewModel.ReportState.Error -> {
                    b.progressBar.visibility = View.GONE; b.btnSubmit.isEnabled = true
                    Toast.makeText(this, "Error: ${state.msg}", Toast.LENGTH_LONG).show()
                    vm.resetReport()
                }
                else -> b.progressBar.visibility = View.GONE
            }
        }
    }

    private fun pick(s: String) {
        selected = s
        b.btnOpen.alpha      = if (s == "OPEN")      1f else 0.4f
        b.btnDamaged.alpha   = if (s == "DAMAGED")   1f else 0.4f
        b.btnSubmerged.alpha = if (s == "SUBMERGED") 1f else 0.4f
        b.btnSubmit.isEnabled = true
    }

    private fun submit() {
        val s = selected ?: return
        vm.submitReport(
            bridgeId = bridgeId, status = s,
            name  = b.editName.text.toString(),
            notes = b.editNotes.text.toString(),
            wl    = b.editWaterLevel.text.toString().toDoubleOrNull() ?: 0.0,
            loc   = geoLoc
        )
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
