package com.gramasethu.app.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gramasethu.app.R
import com.gramasethu.app.model.Bridge
import com.gramasethu.app.model.BridgeStatus
import com.gramasethu.app.ui.BridgeDetailActivity
import com.gramasethu.app.viewmodel.BridgeViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    private val vm: BridgeViewModel by activityViewModels()
    private lateinit var map: MapView
    private var myLoc: MyLocationNewOverlay? = null
    private val markers = mutableMapOf<String, Marker>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().apply {
            load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
            userAgentValue = requireContext().packageName
        }
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map = view.findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(true)
        map.controller.setZoom(7.0)
        map.controller.setCenter(GeoPoint(14.5204, 75.7224))

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myLoc = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map).also {
                it.enableMyLocation(); map.overlays.add(it)
            }
        }

        vm.allBridges.observe(viewLifecycleOwner) { bridges ->
            bridges.forEach { b -> renderMarker(b) }
            map.invalidate()
        }
    }

    private fun renderMarker(b: Bridge) {
        val loc = b.location ?: return
        val pt  = GeoPoint(loc.latitude, loc.longitude)
        val ico = makeIcon(b.effectiveStatus)
        markers[b.id]?.let { m ->
            m.position = pt; m.icon = ico; m.snippet = b.freshnessLabel; return
        }
        val m = Marker(map).apply {
            position = pt; title = b.name
            subDescription = "${b.village} · ${b.district}"
            snippet = b.freshnessLabel; icon = ico
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            val id = b.id
            setOnMarkerClickListener { mk, _ ->
                mk.showInfoWindow()
                startActivity(Intent(requireContext(), BridgeDetailActivity::class.java)
                    .putExtra(BridgeDetailActivity.EXTRA_ID, id))
                true
            }
        }
        map.overlays.add(m); markers[b.id] = m
    }

    private fun makeIcon(s: BridgeStatus): BitmapDrawable {
        val c = when (s) {
            BridgeStatus.OPEN      -> Color.parseColor("#43A047")
            BridgeStatus.DAMAGED   -> Color.parseColor("#FDD835")
            BridgeStatus.SUBMERGED -> Color.parseColor("#E53935")
            BridgeStatus.UNKNOWN   -> Color.parseColor("#9E9E9E")
        }
        val sz = 80
        val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val cv  = Canvas(bmp)
        cv.drawCircle(sz/2f, sz/2f, sz/2f-4, Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = c })
        cv.drawCircle(sz/2f, sz/2f, sz/2f-4, Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.WHITE; it.style = Paint.Style.STROKE; it.strokeWidth = 5f })
        return BitmapDrawable(resources, bmp)
    }

    override fun onResume()  { super.onResume();  map.onResume();  myLoc?.enableMyLocation() }
    override fun onPause()   { super.onPause();   map.onPause();   myLoc?.disableMyLocation() }
    override fun onDestroyView() { super.onDestroyView(); map.onDetach() }
}
