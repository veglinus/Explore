package se.umu.lihv0010.explore

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class LocationServices(private val map: MapView, private val game: Game) {
    private val tag = "DebugExploreLocationManagerClass"

    private val locationRequest = LocationRequest.create().apply {
        interval = 60
        fastestInterval = 30
        maxWaitTime = 2
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    var locationManager: LocationManager = map.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(map.context)
    private lateinit var locationCallback: LocationCallback
    lateinit var myLocationOverlay: MyLocationNewOverlay

    init {
        initLocationListener()
        createMyLocationMarker()
    }

    @SuppressLint("MissingPermission")
    private fun initLocationListener() {
        if (isLocationPermissionGranted()) {

            // TODO: Listen for activity, if activity is recognized then start measuring/comparing


            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    //Log.d(tag, "on location result")
                    super.onLocationResult(locationResult)
                    locationResult.lastLocation?.let {
                        val result = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (result != null) {
                            val newLocation = GeoPoint(result.latitude, result.longitude)
                            if (newLocation != latestLocation) {

                                // TODO: Check if user is on walk/run aka not stationary
                                // using: https://blog.mindorks.com/activity-recognition-in-android-still-walking-running-driving-and-much-more

                                //game.addDistanceTravelled(latestLocation, newLocation)
                                latestLocation = newLocation
                                game.checkIfGoalReached(newLocation)
                                Log.d(tag, "New location: $latestLocation")
                            }
                        }
                    }
                }
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

        } else {
            Log.d(tag, "Location services not granted")
        }

    }

    private fun createMyLocationMarker() {
        val provider = GpsMyLocationProvider(map.context)
        myLocationOverlay = MyLocationNewOverlay(provider, map)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()

        //myLocationOverlay.setPersonIcon() // TODO: Set icon of player
        map.overlays.add(myLocationOverlay)
        map.invalidate()
    }

    private fun isLocationPermissionGranted(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                map.context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                map.context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                map.context as Activity,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
            false
        } else {
            true
        }
    }

    companion object {
        var latestLocation: GeoPoint = GeoPoint(57.86973548791104, 11.974444448918751)
    }
}