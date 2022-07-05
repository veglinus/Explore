package se.umu.lihv0010.explore

import android.util.Log
import android.widget.Toast
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

    // TODO: These two values need to be saved and restored
    var homeLocation = GeoPoint(57.86973548791104, 11.974444448918751)
    var points: Int = 0

    var latestLocation: GeoPoint = GeoPoint(57.86973548791104, 11.974444448918751)
    private val random = Random(System.currentTimeMillis())
    private var latestGoalDirection = random.nextDouble(0.0, 360.0) // Latest direction(0-360) of new goal
    private var goals: MutableList<GeoPoint> = mutableListOf() // List of current goals
    private var visitedGoals: MutableList<GeoPoint> = mutableListOf() // Delete each day

    fun spawnGoal(distance: Double) {
        println("Spawning goal!")
        val newGoal = Marker(map)
        val randomPoint = latestLocation.destinationPoint(distance, randomDirection())

        // TODO: Check if new marker is within 300m, if it is, generate a new one til we get there
        // TODO: Also check so that the latest point isnt the same as one that has been done before, within x meters

        newGoal.position = getClosestRoadAndPath(randomPoint) // TODO: Find the 500m line on this path and pick that instead of full path

        /*
        while (latestLocation.distanceToAsDouble(newGoal.position) < distance * 0.9) {

        }*/

        goals.add(newGoal.position)
        map.overlays.add(newGoal)
        map.invalidate()
    }

    private fun randomDirection(): Double {
        val rnd2 = random.nextBoolean()
        var newDirection = latestGoalDirection

        val newTurn = random.nextInt(45, 90)

        if (rnd2) {
            newDirection += newTurn
        } else {
            newDirection -= newTurn
        }

        if (newDirection > 360.0) {
            newDirection -= 360
        } else if (newDirection < 0) {
            newDirection += 360
        }

        Log.d(tag, "Random direction generated! $newDirection")
        latestGoalDirection = newDirection // For next generation to be 90 degrees off last one
        return newDirection
    }

    private fun getClosestRoadAndPath(newGoal: GeoPoint): GeoPoint {
        // Creates path from start to finish
        val roadManager: RoadManager = OSRMRoadManager(map.context, "ExploreApp/1.0")
        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        val waypoints: ArrayList<GeoPoint> = arrayListOf(latestLocation, newGoal)
        val path = roadManager.getRoad(waypoints)
        val pathOverlay: Polyline = RoadManager.buildRoadOverlay(path)

        map.overlays.add(pathOverlay)

        map.zoomToBoundingBox(pathOverlay.bounds, true)
        return pathOverlay.actualPoints.last() // Returns our new point, on a reachable road
    }

    fun checkIfGoalReached(location: GeoPoint) {
        for (goal in goals) {
            if (location.distanceToAsDouble(goal) < 50.0) {

                goals.remove(goal)
                visitedGoals.add(goal)

                // TODO: Need to find exact goal and remove it, so we can have multiple
                map.overlays.removeLast()
                map.overlays.removeLast()

                Toast.makeText(map.context, "Goal reached! Points have been added.", Toast.LENGTH_LONG).show() // TODO: fix
                addPoints()
            }
        }
    }

    private fun addPoints() {
        points += 10
    }

    private fun placeMarker(p: GeoPoint) {
        val newMarker = Marker(map)
        newMarker.position = p
        map.overlays.add(newMarker)
        map.invalidate()
    }

    private fun goHome() {
        val newGoal = Marker(map)
        newGoal.position = getClosestRoadAndPath(homeLocation) // TODO: Find the 500m line on this path and pick that instead of full path
        goals.add(newGoal.position)
        map.overlays.add(newGoal)
        map.invalidate()
    }
}