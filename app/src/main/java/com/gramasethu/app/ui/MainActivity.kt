package com.gramasethu.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.gramasethu.app.R
import com.gramasethu.app.databinding.ActivityMainBinding
import com.gramasethu.app.service.FcmService
import com.gramasethu.app.ui.fragment.AlertsFragment
import com.gramasethu.app.ui.fragment.BridgeListFragment
import com.gramasethu.app.ui.fragment.MapFragment

/**
 * Host activity.
 * - Signs in anonymously to Firebase Auth
 * - Subscribes to FCM topics (Sender ID: 177826766555)
 * - Hosts bottom navigation: Map | Bridges | Alerts
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    private val locLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { /* handled per-fragment */ }

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) Log.d(TAG, "Notification permission granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFirebase()
        requestPerms()
        setupNav()

        if (savedInstanceState == null) show(MapFragment())
    }

    // ── Firebase init ──────────────────────────────────────────────────────────

    private fun initFirebase() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            // Anonymous sign-in so Firestore security rules can identify reporters
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d(TAG, "Signed in anonymously: ${it.user?.uid}")
                    subscribeToFcmTopics()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Anonymous sign-in failed", e)
                    // Still subscribe even if auth fails
                    subscribeToFcmTopics()
                }
        } else {
            subscribeToFcmTopics()
        }

        // Log current FCM token for debugging
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d(TAG, "FCM Token (Sender: ${FcmService.SENDER_ID}): ${token.take(30)}…")
        }
    }

    /**
     * Subscribe to FCM topics so this device receives push notifications
     * for bridge status changes and monsoon alerts.
     *
     * Topics:
     *  - all_bridges      → every bridge status update
     *  - submerged_alerts → SUBMERGED-only high-priority alarm
     *  - karnataka_monsoon→ rainfall / flood warnings
     */
    private fun subscribeToFcmTopics() {
        FcmService.subscribeToTopics()
    }

    // ── Permissions ────────────────────────────────────────────────────────────

    private fun requestPerms() {
        val locs = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (locs.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            locLauncher.launch(locs)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    private fun setupNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            show(when (item.itemId) {
                R.id.nav_map    -> MapFragment()
                R.id.nav_list   -> BridgeListFragment()
                R.id.nav_alerts -> AlertsFragment()
                else            -> return@setOnItemSelectedListener false
            })
            true
        }
    }

    private fun show(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, f).commit()
    }
}
