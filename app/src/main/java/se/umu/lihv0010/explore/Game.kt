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
    private val gson = Gson()

    private var homeLocation: GeoPoint
    var points: MutableLiveData<Int>
    var totalDistanceTravelled: Int

    var goals: MutableList<GeoPoint> = mutableListOf() // List of current goals
    var goalsWorthPoints: MutableList<Int> = mutableListOf()
    var goalExists: MutableLiveData<Boolean> = MutableLiveData(goals.isNotEmpty())
    //private var visitedGoals: MutableList<GeoPoint> = mutableListOf() // Delete each day
    private val goalGenerator = GoalGenerator(map)

    init {
        //clearData()
        Log.d(tag, "initiating game")

        val homeLocationSaved = prefs.getString("homeLocation", null)
        homeLocation = if (homeLocationSaved != null) {
            gson.fromJson(homeLocationSaved, GeoPoint::class.java)
        } else {
            GeoPoint(57.86973548791104, 11.974444448918751) // TODO: Hardcoded atm
        }

        points = MutableLiveData(prefs.getInt("points", 0))
        //goalExists = prefs.getBoolean("goalExists", false)
        totalDistanceTravelled = prefs.getInt("totalDistanceTravelled", 0)

        val pointsString = prefs.getString("goalsWorthPoints", null)?.split(",")
        if (pointsString != null) {
            for (string in pointsString) {
                goalsWorthPoints.add(string.toInt())
            }
        }


    }

    private fun clearData() {
        repeat(100) {
            println("CLEARING ALL DATA IS ON!!!!!")
        }
        val clearDocument = KmlDocument()
        val localFile: File = clearDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
        clearDocument.saveAsKML(localFile);
        prefs.edit().clear().commit() // Debug: To force delete prefs
    }

    fun spawnGoal(distanceAway: Double) {
        if (goalExists.value == false) {
            Log.d(tag, "Spawning goal!")
            val newGoal = goalGenerator.new(distanceAway)
            goals.add(newGoal.position)
            goalsWorthPoints.add(distanceAway.toInt())
            map.overlays.add(newGoal)
            goalExists.value = goals.isNotEmpty()
            Companion.kmlDocument.mKmlRoot.addOverlay(newGoal, Companion.kmlDocument)
            saveAll()
            map.invalidate()
        } else {
         Log.d(tag, "Goal already exists.")
        }
    }

    fun saveAll() {
        try {
            val localFile: File = Companion.kmlDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
            Companion.kmlDocument.saveAsKML(localFile)

            val homeLocationJson = gson.toJson(homeLocation)
            prefs.edit().putString("homeLocation", homeLocationJson).apply()
            prefs.edit().putInt("points", points.value!!).apply()
            //prefs.edit().putBoolean("goalExists", goalExists).apply()
            prefs.edit().putInt("totalDistanceTravelled", totalDistanceTravelled).apply()
            prefs.edit().putString("goalsWorthPoints", goalsWorthPoints.joinToString(",")).apply()
        } catch (e: Exception) {
            throw e
        }
    }

    fun addDistanceTravelled(from: GeoPoint, to: GeoPoint) {
        val distance = from.distanceToAsDouble(to).toInt()

        if (distance < 1000) {
            totalDistanceTravelled += distance
            Log.d(tag, "Distance travelled is now $totalDistanceTravelled")
        } else {
            Log.d(tag, "User moved more than 1 km. Not adding to total.")
        }
    }

    fun checkIfGoalReached(location: GeoPoint) {
        Log.d(tag, "Goals: $goals")
        Log.d(tag, "KML Overlays: " + Companion.kmlDocument.mKmlRoot.mItems.toString())
        Log.d(tag, "Map Overlays: " + map.overlays.toString())

        for ((index, goal) in goals.withIndex()) {
            //Log.d(tag, "Loop: index: $index goal: $goal")
            if (location.distanceToAsDouble(goal) < 100.0) {
                Toast.makeText(map.context, "Goal reached! Points have been added.", Toast.LENGTH_LONG).show()

                addPoints(index) // TODO: Test if points are added correctly
                // TODO: Add distance travelled here instead
                removeGoalAndPathFromOverlays(index)
                goals.remove(goal) // Remove goal from goal list in Game
                goalExists.value = goals.isNotEmpty()

                saveAll()

                if (map.overlays.size >= 2) {
                    map.overlays.removeAt(1)
                    Companion.kmlDocument = KmlDocument()
                    showSavedMapData()
                }
                map.invalidate()
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

        // TODO: Only situation that doesn't work is reloading after spawning a goal.
        // Reaching goal doesnt remove it, even though layers are blank.

        // Removing path and marker overlay from KML document
        Companion.kmlDocument.mKmlRoot.mItems.removeAt(kmlIndex) // Removes marker
        Companion.kmlDocument.mKmlRoot.mItems.removeAt(kmlIndex) // Removes pathoverlay

        if (map.overlays.size > 2) { // Removing path and marker from actual mapview if not loaded from KMLdocument
            map.overlays.removeAt(index + 3)
            map.overlays.removeAt(index + 2)
        }
    }

    private fun addPoints(index: Int) {
        points.value = points.value!! + goalsWorthPoints[index]
        Log.d(tag, "Points are now: " + points.value)

        goalsWorthPoints.remove(index)
    }

    private fun placeMarker(p: GeoPoint) {
        val newMarker = Marker(map)
        newMarker.position = p
        map.overlays.add(newMarker)
        map.invalidate()
    }

}