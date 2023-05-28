package se.umu.lihv0010.explore

import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.osmdroid.bonuspack.kml.Style
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.File

class OverlaysHandler(private val map: MapView) {
    private val tag = "DebugExploreOverlaysHandlerClass"
    val fog = FogOverlay()

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
    fun showFog() {
        map.overlays.add(fog)
        // TODO: Save and load
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