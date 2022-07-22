package se.umu.lihv0010.explore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.bonuspack.kml.KmlFeature
import org.osmdroid.bonuspack.kml.KmlGeometry
import org.osmdroid.bonuspack.kml.KmlPlacemark
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

    var goals: MutableList<GeoPoint> = mutableListOf() // List of current goals
    private var goalExists: Boolean
    private var visitedGoals: MutableList<GeoPoint> = mutableListOf() // Delete each day
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
        goalExists = prefs.getBoolean("goalExists", false)

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
        if (!goalExists) {
            println("Spawning goal!")
            val newGoal = goalGenerator.new(distanceAway)
            goals.add(newGoal.position)
            map.overlays.add(newGoal)
            goalExists = true
            MainActivity.kmlDocument.mKmlRoot.addOverlay(newGoal, MainActivity.kmlDocument)
            map.invalidate()
            saveAll()
        } else {
         Log.d(tag, "Goal already exists.")
        }
    }

    private fun saveAll() {
        try {
            val homeLocationJson = gson.toJson(homeLocation)
            prefs.edit().putString("homeLocation", homeLocationJson).apply()
            prefs.edit().putInt("points", points.value!!).apply()
            prefs.edit().putBoolean("goalExists", goalExists).apply()
        } catch (e: Exception) {
            throw e
        }
    }

    fun checkIfGoalReached(location: GeoPoint) {

        for ((index, goal) in goals.withIndex()) {

            if (location.distanceToAsDouble(goal) < 10.0) {
                val kmlIndex = index + 1

                // TODO: Test
                val pointsToAdd = MainActivity.kmlDocument.mKmlRoot.mItems[kmlIndex].mDescription.substringBefore(".").toInt()

                goalExists = false
                MainActivity.kmlDocument.mKmlRoot.mItems.removeAt(kmlIndex) // Removes marker
                MainActivity.kmlDocument.mKmlRoot.mItems.removeAt(kmlIndex - 1) // Removes pathoverlay
                Toast.makeText(map.context, "Goal reached! Points have been added.", Toast.LENGTH_LONG).show() // TODO: fix

                addPoints(pointsToAdd)
                saveAll()

                val localFile: File = Companion.kmlDocument.getDefaultPathForAndroid(map.context, "my_data.kml")
                Companion.kmlDocument.saveAsKML(localFile)

                // TODO: MAIN: Two goals in a row is not functional
                map.invalidate()
            }
        }
    }

    private fun addPoints(newPoints: Int) {
        points.value = points.value!! + newPoints // TODO: Magic number, should be waypoint score
        Log.d(tag, "Points are now: " + points.value)
    }

    private fun placeMarker(p: GeoPoint) {
        val newMarker = Marker(map)
        newMarker.position = p
        map.overlays.add(newMarker)
        map.invalidate()
    }

}