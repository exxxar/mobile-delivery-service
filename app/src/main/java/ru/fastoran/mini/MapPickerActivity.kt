package ru.fastoran.mini

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapPickerActivity : Activity() {
    companion object {
        const val EXTRA_LAT = "selected_lat"
        const val EXTRA_LNG = "selected_lng"
        const val EXTRA_ADDRESS = "selected_address"
        private const val LOCATION_PERMISSION_REQUEST = 71
    }

    private lateinit var map: MapView
    private lateinit var hint: TextView
    private var selectedPoint: GeoPoint? = null
    private var selectedAddress: String = ""
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        hint = TextView(this).apply {
            text = "Нажмите на карту, чтобы выбрать адрес доставки"
            textSize = 15f
            setPadding(18, 14, 18, 14)
        }
        root.addView(hint, LinearLayout.LayoutParams(-1, -2))
        map = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(55.751244, 37.618423))
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    choosePoint(p)
                    return true
                }
                override fun longPressHelper(p: GeoPoint): Boolean {
                    choosePoint(p)
                    return true
                }
            }))
        }
        root.addView(map, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 10, 12, 10)
        }
        val myLocation = Button(this).apply {
            text = "Моё местоположение"
            setOnClickListener { moveToCurrentLocation() }
        }
        val confirm = Button(this).apply {
            text = "Подтвердить адрес"
            isEnabled = false
            setOnClickListener { confirmSelection() }
            tag = "confirm"
        }
        actions.addView(myLocation, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(confirm, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions)
        setContentView(root)

        if (hasLocationPermission()) moveToCurrentLocation()
    }

    private fun choosePoint(point: GeoPoint) {
        selectedPoint = point
        map.overlays.removeAll { it is Marker }
        val newMarker = Marker(map).apply {
            position = point
            title = "Адрес доставки"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        marker = newMarker
        map.overlays.add(newMarker)
        map.controller.animateTo(point)
        map.invalidate()
        selectedAddress = reverseGeocode(point) ?: "${"%.6f".format(Locale.US, point.latitude)}, ${"%.6f".format(Locale.US, point.longitude)}"
        hint.text = selectedAddress
        findConfirmButton()?.isEnabled = true
    }

    private fun reverseGeocode(point: GeoPoint): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            @Suppress("DEPRECATION")
            val list = Geocoder(this, Locale.getDefault()).getFromLocation(point.latitude, point.longitude, 1)
            val a = list?.firstOrNull() ?: return null
            listOfNotNull(a.thoroughfare, a.subThoroughfare, a.subLocality, a.locality, a.adminArea)
                .filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { null }
        } catch (_: Exception) { null }
    }

    private fun findConfirmButton(): Button? {
        fun search(view: android.view.View): Button? {
            if (view is Button && view.tag == "confirm") return view
            if (view is android.view.ViewGroup) for (i in 0 until view.childCount) search(view.getChildAt(i))?.let { return it }
            return null
        }
        return search(window.decorView)
    }

    private fun confirmSelection() {
        val point = selectedPoint ?: return
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_LAT, point.latitude)
            putExtra(EXTRA_LNG, point.longitude)
            putExtra(EXTRA_ADDRESS, selectedAddress)
        })
        finish()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun moveToCurrentLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
            return
        }
        try {
            val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = manager.getProviders(true)
            val location = providers.mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }.maxByOrNull { it.time }
            if (location != null) {
                val point = GeoPoint(location.latitude, location.longitude)
                map.controller.animateTo(point)
                map.controller.setZoom(16.0)
                choosePoint(point)
            } else {
                hint.text = "Местоположение пока недоступно. Выберите точку касанием по карте."
            }
        } catch (_: Exception) {
            hint.text = "Не удалось получить местоположение. Выберите точку на карте вручную."
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) moveToCurrentLocation()
    }

    override fun onResume() { super.onResume(); if (::map.isInitialized) map.onResume() }
    override fun onPause() { if (::map.isInitialized) map.onPause(); super.onPause() }
    override fun onDestroy() { if (::map.isInitialized) map.onDetach(); super.onDestroy() }
}
