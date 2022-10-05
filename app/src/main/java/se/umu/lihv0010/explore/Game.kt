package se.umu.lihv0010.explore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.bonuspack.kml.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import java.lang.Exception
import se.umu.lihv0010.explore.MainActivity.Companion
import java.io.File


class Game(inputMap: MapView) {
    private val tag = "DebugExploreGameClass"
    private val map = inputMap

    var prefs: SharedPreferences = map.context.getSharedPreferences(
        "preferences", Context.MODE_PRIVATE
    )

    var points: MutableLiveData<Int>
    var totalDistanceTravelled: Int
    var goals: MutableList<GeoPoint> = mutableListOf() // List of current goals
    var goalsWorthPoints: MutableList<Int> = mutableListOf() // List of what points goals are worth depending on index
    var goalExists: MutableLiveData<Boolean> = MutableLiveData(goals.isNotEmpty()) // Checking if goal exists or not, used by UI
    private val goalGenerator = GoalGenerator(map)

    init {
        //clearData()
        Log.d(tag, "initiating game")

        // Loads saved data from prefs
        points = MutableLiveData(prefs.getInt("points", 0))
        totalDistanceTravelled = prefs.getInt("totalDistanceTravelled", 0)

        val pointsString = prefs.getString("goalsWorthPoints", null)?.split(",")
        if (pointsString != null) {
            for (string in pointsString) {
                if (string.isNotEmpty()) {
                    goalsWorthPoints.add(string.toInt())
                }
            }
        }
    }

    fun spawnGoal(distanceAway: Double) {
        if (goalExists.value == false) {
            Log.d(tag, "Spawning goal!")
            val newGoal = goalGenerator.new(distanceAway) // Goalgenerator creates new marker + path
            goals.add(newGoal.position) // Add the goal to our position array, used for seeing if we're close enough
            //Log.d(tag, "Goal is worth: " + distanceAway.toInt())
            goalsWorthPoints.add(distanceAway.toInt()) // Add amount of points goal is worth to array we later access
            map.overlays.add(newGoal) // Add the entire overlay
            goalExists.value = goals.isNotEmpty() // Set goalExists to true
            Companion.kmlDocument.mKmlRoot.addOverlay(newGoal, Companion.kmlDocument) // Add overlay to KMLDocument aswell
            saveAll() // Save all data as a precaution
            map.invalidate() // Update map in view
        } else {
         Log.d(tag, "Goal already exists.")
        }
    }

    fun saveAll() { // Saves all data used by application
        try {
            val localFile: File = Companion.kmlDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
            Companion.kmlDocument.saveAsKML(localFile)
            prefs.edit().putInt("points", points.value!!).apply()
            prefs.edit().putInt("totalDistanceTravelled", totalDistanceTravelled).apply()
            prefs.edit().putString("goalsWorthPoints", goalsWorthPoints.joinToString(",")).apply()
        } catch (e: Exception) {
            throw e
        }
    }

    fun addDistanceTravelled(from: GeoPoint, to: GeoPoint) {
        val distance = from.distanceToAsDouble(to).toInt()

        if (distance < 100) {
            totalDistanceTravelled += distance
            Log.d(tag, "Distance travelled is now $totalDistanceTravelled")
        } else {
            Log.d(tag, "User moved more than 100m. Not adding to total.")
        }
    }

    fun checkIfGoalReached(location: GeoPoint) {
        // Checks if user has reached goal or not
        //Log.d(tag, "Goals: $goals")
        //Log.d(tag, "KML Overlays: " + Companion.kmlDocument.mKmlRoot.mItems.toString())
        //Log.d(tag, "Map Overlays: " + map.overlays.toString())

        for ((index, goal) in goals.withIndex()) { // For each goal (at the moment we only support one goal at a time)
            //Log.d(tag, "Loop: index: $index goal: $goal")
            if (location.distanceToAsDouble(goal) < 10.0) { // If within 10 meters of goal:
                Toast.makeText(map.context, "Goal reached! Points have been added.", Toast.LENGTH_LONG).show()
                addPoints(index) // Adds points
                removeGoalAndPathFromOverlays(index) // Removes overlays and goal
                goals.remove(goal) // Remove goal from goal list in Game
                goalExists.value = goals.isNotEmpty() // Goal does not exist anymore

                saveAll() // Save everything

                if (map.overlays.size >= 2) { // Handle the map overlays currently in view (dirty workaround)
                    map.overlays.removeAt(1)
                    Companion.kmlDocument = KmlDocument()
                    showSavedMapData()
                }
                map.invalidate() // Update map
            }
        }
    }

    fun showSavedMapData() {
        Log.d(tag, "Parsing KMLdocument")
        val localFile: File = Companion.kmlDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
        Companion.kmlDocument.parseKMLFile(localFile) // Parses the saved local file into current kmlDocument object
        val icon = ContextCompat.getDrawable(map.context, R.drawable.ic_flag_checkered) // Goal icon
        val defaultBitmap = icon?.toBitmap()
        val overlayStyle = Style(defaultBitmap, 0x00F, 3.0f, 0x000) // Styling for icons and path
        val kmlOverlay = Companion.kmlDocument.mKmlRoot.buildOverlay(map, overlayStyle, null,
            Companion.kmlDocument
        ) // Overlay for icons and path
        map.overlays.add(kmlOverlay)
        map.invalidate()
    }

    private fun removeGoalAndPathFromOverlays(index: Int) {
        var kmlIndex = index

        if (index != 0) {
            kmlIndex += 1
        }

        // Removing path and marker overlay from KML document
        Companion.kmlDocument.mKmlRoot.mItems.removeAt(kmlIndex) // Removes marker
        Companion.kmlDocument.mKmlRoot.mItems.removeAt(kmlIndex) // Removes pathoverlay

        if (map.overlays.size > 2) { // Removing path and marker from actual mapview if not loaded from KMLdocument
            map.overlays.removeAt(index + 3)
            map.overlays.removeAt(index + 2)
        }
    }

    private fun addPoints(index: Int) { // Used for adding to points total
        points.value = points.value!! + goalsWorthPoints[index]
        Log.d(tag, "Points are now: " + points.value)
        goalsWorthPoints.remove(index)
    }

    private fun clearData() { // This function is used for debug purposes, deleting all saved data of all sorts
        repeat(100) {
            Log.d("CRITICAL_$tag","CLEARING ALL DATA IS ON!!!!!")
        }
        val clearDocument = KmlDocument()
        val localFile: File = clearDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
        clearDocument.saveAsKML(localFile);
        prefs.edit().clear().apply() // Debug: To force delete prefs
    }
}