package com.gramasethu.app.ui

import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.gramasethu.app.R
import com.gramasethu.app.databinding.ActivityBridgeDetailBinding
import com.gramasethu.app.model.Bridge
import com.gramasethu.app.model.BridgeStatus
import com.gramasethu.app.viewmodel.BridgeViewModel

class BridgeDetailActivity : AppCompatActivity() {

    companion object { const val EXTRA_ID = "bridge_id" }

    private lateinit var b: ActivityBridgeDetailBinding
    private val vm: BridgeViewModel by viewModels()
    private var bridgeId = ""
    private lateinit var pool: SoundPool
    private var soundId = -1
    private var soundReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBridgeDetailBinding.inflate(layoutInflater)
        setContentView(b.root)
        bridgeId = intent.getStringExtra(EXTRA_ID) ?: run { finish(); return }
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        pool = SoundPool.Builder().setMaxStreams(1)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
        pool.setOnLoadCompleteListener { _, _, s -> soundReady = s == 0 }
        soundId = pool.load(this, R.raw.warning_beep, 1)

        vm.allBridges.observe(this) { list ->
            list.find { it.id == bridgeId }?.let { render(it) }
        }
        b.btnReport.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java)
                .putExtra(ReportActivity.EXTRA_ID, bridgeId))
        }
    }

    private fun render(bridge: Bridge) {
        supportActionBar?.title = bridge.name
        b.textVillage.text    = "${bridge.village}, ${bridge.taluk} Taluk"
        b.textDistrict.text   = "${bridge.district} District"
        b.textFreshness.text  = bridge.freshnessLabel
        b.textReporter.text   = "Reported by: ${bridge.lastReportedBy.ifBlank { "—" }}"
        b.textWaterLevel.text = "Water level: ${"%.1f".format(bridge.waterLevel)} m"
        b.textRoute.text      = "🗺 ${bridge.alternativeRoute}"
        b.waterBar.progress   = ((bridge.waterLevel / 3.0) * 100).toInt().coerceIn(0, 100)

        when (bridge.effectiveStatus) {
            BridgeStatus.OPEN -> {
                b.statusBadge.text = "✓  OPEN – Safe to Cross"
                b.statusBadge.setBackgroundResource(R.drawable.bg_open)
                b.warningBanner.visibility = View.GONE
            }
            BridgeStatus.DAMAGED -> {
                b.statusBadge.text = "⚡  DAMAGED – Use Alternate Route"
                b.statusBadge.setBackgroundResource(R.drawable.bg_damaged)
                b.warningBanner.visibility = View.VISIBLE
                b.warningBanner.setBackgroundResource(R.drawable.bg_warn_yellow)
                b.textWarning.text = "⚠ Bridge is damaged. Use the alternative route shown below."
                b.textWarning.setTextColor(getColor(R.color.warn_yellow_text))
            }
            BridgeStatus.SUBMERGED -> {
                b.statusBadge.text = "⚠  SUBMERGED – Do NOT Cross"
                b.statusBadge.setBackgroundResource(R.drawable.bg_submerged)
                b.warningBanner.visibility = View.VISIBLE
                b.warningBanner.setBackgroundResource(R.drawable.bg_warn_red)
                b.textWarning.text = "🚨 DANGER: Bridge is under water. Do not attempt to cross!"
                b.textWarning.setTextColor(getColor(R.color.warn_red_text))
                if (soundReady) pool.play(soundId, 1f, 1f, 1, 0, 1f)
            }
            BridgeStatus.UNKNOWN -> {
                b.statusBadge.text = "?  STATUS UNKNOWN"
                b.statusBadge.setBackgroundResource(R.drawable.bg_unknown)
                b.warningBanner.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
    override fun onDestroy() { super.onDestroy(); pool.release() }
}
