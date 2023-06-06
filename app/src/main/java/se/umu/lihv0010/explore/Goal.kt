package se.umu.lihv0010.explore

import androidx.core.content.ContextCompat
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import se.umu.lihv0010.explore.LocationService.Companion.latestLocation

class Goal(mapView: MapView,
           distanceAway: Double,
           inputPosition: GeoPoint? = null,
        ) : Marker(mapView) {
    var worth = distanceAway.toInt()
    private val goalGenerator = GoalGenerator(mapView)

    init {
        if (inputPosition == null) {
            val randomPoint = latestLocation.destinationPoint(distanceAway, goalGenerator.randomDirection())
            this.position = goalGenerator.getClosestRoadAndPath(randomPoint, distanceAway)
        } else {
            this.position = inputPosition
        }

        this.image = ContextCompat.getDrawable(mapView.context, android.R.drawable.ic_dialog_info)
        this.title = "Current goal"
        this.snippet = "This goal is worth $worth points!"
        this.subDescription = "$distanceAway meters away"
        this.icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_flag_checkered)
    }

    fun halfWorth() {
        this.worth = this.worth / 2
        this.snippet = "This goal is worth $worth points!"
    }
}
