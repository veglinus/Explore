package se.umu.lihv0010.explore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import org.osmdroid.bonuspack.kml.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Polyline
import java.lang.Exception
import se.umu.lihv0010.explore.MainActivity.Companion
import java.io.File

class Game(inputMap: MapView) {
    private val tag = "DebugExploreGameClass"
    private val map = inputMap
    val myOverlays = OverlaysHandler(inputMap)

    private var prefs: SharedPreferences = map.context.getSharedPreferences(
        "preferences", Context.MODE_PRIVATE
    )

    var points: MutableLiveData<Int>
    var endTimeStamp: MutableLiveData<Long>
    var totalDistanceTravelled: Int
    var goals: MutableList<Goal> = mutableListOf() // List of current goals
    var goalExists: MutableLiveData<Boolean> = MutableLiveData(false) // Checking if goal exists or not, used by UI

    init {
        //clearData()
        Log.d(tag, "initiating game")

        // Loads saved data from prefs
        points = MutableLiveData(prefs.getInt("points", 0))
        totalDistanceTravelled = prefs.getInt("totalDistanceTravelled", 0)
        endTimeStamp = MutableLiveData(prefs.getLong("endTimeStamp", 0L))
    }

    fun spawnGoal(distanceAway: Double) {
        if (goalExists.value == false) {
            Log.d(tag, "Spawning goal!")

            val newGoal = Goal(map, distanceAway)
            goals.add(newGoal) // Add the goal to our position array, used for seeing if we're close enough
            map.overlays.add(newGoal) // Add the entire overlay

            goalExists.value = true // Set goalExists to true
            Companion.kmlDocument.mKmlRoot.addOverlay(newGoal, Companion.kmlDocument) // Add overlay to KMLDocument aswell
            timeStamp(distanceAway)

            saveAll() // Save all data as a precaution
            map.invalidate() // Update map in view
            Toast.makeText(map.context, MainActivity.res.getString(R.string.timer_started), Toast.LENGTH_LONG).show()

        } else {
         Log.d(tag, "Goal already exists.")
        }
    }

    private fun timeStamp(distance: Double) {
        // This formula assumes it takes 72 seconds to walk 100m, or 720s/12 minutes to walk 1km.
        val newDistance = ((distance * 0.72) * 1000).toLong() // Converted to milliseconds
        val timeStamp = System.currentTimeMillis()
        endTimeStamp.value = timeStamp + newDistance

        Log.d(tag, "Timestamps goal: ${endTimeStamp.value}")
    }

    fun saveAll() { // Saves all data used by application
        try {
            val localFile: File = Companion.kmlDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
            Companion.kmlDocument.saveAsKML(localFile)
            prefs.edit().putInt("points", points.value!!).apply()
            prefs.edit().putLong("endTimeStamp", endTimeStamp.value!!).apply()
            prefs.edit().putInt("totalDistanceTravelled", totalDistanceTravelled).apply()
        } catch (e: Exception) {
            throw e
        }
    }

    fun checkIfGoalReached(location: GeoPoint) {
        // Checks if user has reached goal or not
        //debugLog()

        for (goal in goals) { // For each goal (at the moment we only support one goal at a time)
            if (location.distanceToAsDouble(goal.position) < 15.0) { // If within 15 meters of goal:
                onGoalEnd(false)
            }
        }
    }

    fun cancelGoal() {
        onGoalEnd(true)
    }


    private fun onGoalEnd(cancelled: Boolean = false) {
        for (goal in goals) {

            if (!cancelled) { // If not cancelled, add points

                Log.d(tag, "Goal not cancelled, give points")

                addPoints(goal.worth) // Adds points
                Toast.makeText(
                    map.context,
                    MainActivity.res.getString(R.string.goal_reached),
                    Toast.LENGTH_LONG
                ).show()
            } else { // if cancelled:

                Log.d(tag, "Goal cancelled")

                Toast.makeText(
                    map.context,
                    MainActivity.res.getString(R.string.goal_cancelled),
                    Toast.LENGTH_LONG
                ).show()
            }

            myOverlays.removeGoalAndPathFromOverlays() // Removes overlays and goal
            goals.remove(goal) // Remove goal from goal list in Game
            goalExists.value = false // Goal does not exist anymore
            saveAll() // Save everything

            for (overlay in map.overlays) {
                if (overlay is FolderOverlay) {
                    map.overlays.remove(overlay)
                }
            }

            Companion.kmlDocument = KmlDocument()
            myOverlays.showSavedMapData()

        }
    }



    private fun addPoints(worth: Int) { // Used for adding to points total
        points.value = points.value!! + worth
        Log.d(tag, "Points are now: " + points.value)
    }

    private fun clearData() { // This function is used for debug purposes, deleting all saved data of all sorts
        repeat(100) {
            Log.d("CRITICAL_$tag","CLEARING ALL DATA IS ON!!!!!")
        }
        val clearDocument = KmlDocument()
        val localFile: File = clearDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
        clearDocument.saveAsKML(localFile)
        prefs.edit().clear().apply() // Debug: To force delete prefs
    }

    private fun debugLog() {
        Log.d(tag, "Goals: $goals")
        Log.d(tag, "KML Overlays: " + Companion.kmlDocument.mKmlRoot.mItems.toString())
        Log.d(tag, "Map Overlays: " + map.overlays.toString())
    }
}