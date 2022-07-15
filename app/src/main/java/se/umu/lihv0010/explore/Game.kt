package se.umu.lihv0010.explore

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.random.Random

class Game(inputMap: MapView) {
    private val tag = "DebugExploreGameClass"
    private val map = inputMap

    // TODO: These three values need to be saved and restored
    var homeLocation = GeoPoint(57.86973548791104, 11.974444448918751)
    var points: MutableLiveData<Int> = MutableLiveData(0)
    private var goalExists: Boolean = false

    private var goals: MutableList<GeoPoint> = mutableListOf() // List of current goals
    private var visitedGoals: MutableList<GeoPoint> = mutableListOf() // Delete each day
    private val goalGenerator = GoalGenerator(map)

    fun spawnGoal(distanceAway: Double) {
        if (!goalExists) {
            println("Spawning goal!")
            val newGoal = goalGenerator.new(distanceAway)
            goals.add(newGoal.position)
            map.overlays.add(newGoal)
            map.invalidate()
            goalExists = true
        } else {
         Log.d(tag, "Goal already exists.")
        }
    }

    fun checkIfGoalReached(location: GeoPoint) {
        for (goal in goals) {
            if (location.distanceToAsDouble(goal) < 50.0) {

                goals.remove(goal)
                visitedGoals.add(goal)

                // If we want multiple goals we need to find exact goal
                map.overlays.removeLast()
                map.overlays.removeLast()

                Toast.makeText(map.context, "Goal reached! Points have been added.", Toast.LENGTH_LONG).show() // TODO: fix
                addPoints()
            }
        }
    }

    private fun addPoints() {
        points.value = points.value!! + 10 // TODO: Magic number, should be waypoint score
        Log.d(tag, "Points are now: " + points.value)
    }

    private fun placeMarker(p: GeoPoint) {
        val newMarker = Marker(map)
        newMarker.position = p
        map.overlays.add(newMarker)
        map.invalidate()
    }

    private fun goHome() {
        val newGoal = Marker(map)
        //newGoal.position = getClosestRoadAndPath(homeLocation) // TODO: Find the 500m line on this path and pick that instead of full path
        goals.add(newGoal.position)
        map.overlays.add(newGoal)
        map.invalidate()
    }
}