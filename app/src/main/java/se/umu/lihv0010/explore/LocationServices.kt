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

class LocationServices(inputMap: MapView, gameInput: Game) {
    private val tag = "DebugExploreLocationManagerClass"
    private val map = inputMap
    private val game = gameInput

    private val locationRequest = LocationRequest.create().apply {
        interval = 200
        fastestInterval = 100
        maxWaitTime = 200
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    lateinit var locationManager: LocationManager
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    lateinit var myLocationOverlay: MyLocationNewOverlay

    init {
        initLocationListener()
    }

    private fun initLocationListener() {
        // TODO: Duplicate looking for location atm, both this and my location overlay
        @SuppressLint("MissingPermission")
        if (isLocationPermissionGranted()) {
            locationManager = map.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(map.context)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult.lastLocation?.let {
                        val result = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (result != null) {
                            val newLocation = GeoPoint(result.latitude, result.longitude)
                            if (newLocation != game.latestLocation) {
                                game.latestLocation = newLocation
                                game.checkIfGoalReached(newLocation);
                            }
                        }

                    }
                }
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        } else {
            Log.d(tag, "Location services not granted")
        }

        if (isLocationPermissionGranted()) {
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(map.context), map)
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()
            map.overlays.add(myLocationOverlay)
            map.invalidate()
        }
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
}