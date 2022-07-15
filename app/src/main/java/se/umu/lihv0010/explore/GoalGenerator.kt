package se.umu.lihv0010.explore

import android.R
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import se.umu.lihv0010.explore.LocationServices.Companion.latestLocation
import kotlin.random.Random

class GoalGenerator(mapInput: MapView) {
    private val tag = "DebugExploreGoalGenerator"
    val map = mapInput

    private val random = Random(System.currentTimeMillis())
    private var latestGoalDirection = random.nextDouble(0.0, 360.0) // Latest direction(0-360) of new goal

    fun new(distanceAway: Double): Marker {
        val newGoal = Marker(map)

        // TODO: Check if new marker is within 300m, if it is, generate a new one til we get there
        // TODO: Also check so that the latest point isnt the same as one that has been done before, within x meters

        // TODO: Fix icons and stylize
        val randomPoint = latestLocation.destinationPoint(distanceAway, randomDirection())
        newGoal.position = getClosestRoadAndPath(randomPoint, distanceAway) // TODO: Find the 500m line on this path and pick that instead of full path
        newGoal.image = ContextCompat.getDrawable(map.context, R.drawable.ic_dialog_info)
        newGoal.title = "Current goal"
        newGoal.snippet = "This goal is worth $distanceAway points!"
        newGoal.subDescription = "$distanceAway meters away"
        //newGoal.icon = ContextCompat.getDrawable(map.context, R.drawable.ic_menu_today)

        return newGoal
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

    private fun getClosestRoadAndPath(newGoal: GeoPoint, selectedDistance: Double): GeoPoint {
        // Creates path from start to finish
        val roadManager: RoadManager = OSRMRoadManager(map.context, "ExploreApp/1.0")
        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        val waypoints: ArrayList<GeoPoint> = arrayListOf(latestLocation, newGoal)
        val path = roadManager.getRoad(waypoints)
        val pathOverlay: Polyline = RoadManager.buildRoadOverlay(path)


        var distance: Double = 0.0
        val duplicateList = pathOverlay.actualPoints.toMutableList()
        for ((index, point) in duplicateList.withIndex()) { // Populates distance variable
            if (index < duplicateList.size - 1) {
                distance += point.distanceToAsDouble(duplicateList[index + 1])
            }
            if (distance > selectedDistance) { // Removes rest of path if we dont need path to be longer
                pathOverlay.actualPoints.remove(point)
            }
        }

        //Log.d(tag, "Distance: $distance")
        pathOverlay.usePath(true) // Uncomment to see first generated path which is as long as the distance variable

        map.overlays.add(pathOverlay)
        // TODO: Zoom to new goal
        map.zoomToBoundingBox(pathOverlay.bounds, true)
        return pathOverlay.actualPoints.last() // Returns our new point, on a reachable road
    }

}