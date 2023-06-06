package se.umu.lihv0010.explore

import android.graphics.Color
import android.util.Log
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import se.umu.lihv0010.explore.LocationService.Companion.latestLocation
import kotlin.random.Random

class GoalGenerator(mapInput: MapView) {
    private val tag = "DebugExploreGoalGenerator"
    private val map = mapInput

    private val random = Random(System.currentTimeMillis())
    private var latestGoalDirection = random.nextDouble(0.0, 360.0) // Latest direction(0-360) of new goal

    fun randomDirection(): Double {
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

    fun getClosestRoadAndPath(newGoal: GeoPoint, selectedDistance: Double): GeoPoint {
        // Creates path from start to finish
        val pathOverlay = getPath(newGoal)

        var distance = 0.0
        val duplicateList = pathOverlay.actualPoints.toMutableList()
        for ((index, point) in duplicateList.withIndex()) { // Populates distance variable
            if (index < duplicateList.size - 1) {
                distance += point.distanceToAsDouble(duplicateList[index + 1])
            }
            if (distance > selectedDistance) { // Removes rest of path if we don't need path to be longer
                pathOverlay.actualPoints.remove(point)
            }
        }
        pathOverlay.outlinePaint.color = Color.RED
        pathOverlay.usePath(true) // Uncomment to see first generated path which is as long as the distance variable
        map.overlays.add(pathOverlay)  // TODO: Implement overlay as custom class


        MainActivity.kmlDocument.mKmlRoot.addOverlay(pathOverlay, MainActivity.kmlDocument)
        map.zoomToBoundingBox(pathOverlay.bounds, true)
        map.controller.zoomOut()

        return pathOverlay.actualPoints.last() // Returns our new point, on a reachable road
    }

    private fun getPath(newGoal: GeoPoint): Polyline {
        val roadManager: RoadManager = OSRMRoadManager(map.context, "ExploreApp/1.0")
        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        val waypoints: ArrayList<GeoPoint> = arrayListOf(latestLocation, newGoal)
        val path = roadManager.getRoad(waypoints)
        return RoadManager.buildRoadOverlay(path)
    }
}