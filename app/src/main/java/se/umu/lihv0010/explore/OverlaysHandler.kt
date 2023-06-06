package se.umu.lihv0010.explore

import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.osmdroid.bonuspack.kml.Style
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File


class OverlaysHandler(private val map: MapView) {
    private val tag = "DebugExploreOverlaysHandlerClass"
    lateinit var fog: FogOverlay

    private val provider = GpsMyLocationProvider(MainActivity.map.context)
    val myLocationOverlay = MyLocationNewOverlay(provider, MainActivity.map)

    fun setup() {
        showSavedMapData()
        showMyLocationMarker()
        showFog()
    }

    fun showSavedMapData() {
        Log.d(tag, "Parsing KMLdocument")
        val localFile: File = MainActivity.kmlDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
        MainActivity.kmlDocument.parseKMLFile(localFile) // Parses the saved local file into current kmlDocument object
        val icon = ContextCompat.getDrawable(map.context, R.drawable.ic_flag_checkered) // Goal icon
        val defaultBitmap = icon?.toBitmap()
        val overlayStyle = Style(defaultBitmap, 0x00F, 3.0f, 0x000) // Styling for icons and path
        val kmlOverlay = MainActivity.kmlDocument.mKmlRoot.buildOverlay(map, overlayStyle, null,
            MainActivity.kmlDocument
        ) // Overlay for icons and path
        map.overlays.add(kmlOverlay)
        map.invalidate()
    }

    private fun showMyLocationMarker() {
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        MainActivity.map.overlays.add(myLocationOverlay)
        MainActivity.map.invalidate()
    }

    private fun showFog() {
        fog = FogOverlay(map.context)
        map.overlays.add(fog)
    }

    fun removeGoalAndPathFromOverlays() {
        map.overlays.removeIf { it is Goal }
        map.overlays.removeIf { it is Polyline }

        MainActivity.kmlDocument.mKmlRoot.mItems.clear()
        val localFile: File = MainActivity.kmlDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
        MainActivity.kmlDocument.saveAsKML(localFile)

        map.invalidate()
        Log.d(tag, "After deletion:")
        debugLog()
    }

    private fun debugLog() {
        Log.d(tag, "KML Overlays: " + MainActivity.kmlDocument.mKmlRoot.mItems.toString())
        Log.d(tag, "Map Overlays: " + map.overlays.toString())
    }
}