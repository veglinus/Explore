package se.umu.lihv0010.explore

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.PorterDuff
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
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

    @SuppressLint("MissingPermission") // We check for permissions in isLocationPermissionGranted
    private fun initLocationListener() {
        if (isLocationPermissionGranted()) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult.lastLocation?.let {
                        val result = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (result != null) {
                            val newLocation = GeoPoint(result.latitude, result.longitude)
                            if (newLocation != latestLocation) {
                                latestLocation = newLocation
                                game.checkIfGoalReached(newLocation)
                                removeFog(newLocation)
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

    private fun removeFog(newLocation: GeoPoint) {
        Log.d(tag, "Add hole at new ")
        game.fog.addHoleAt(newLocation)
        map.invalidate()
    }

    private fun createMyLocationMarker() {
        val provider = GpsMyLocationProvider(map.context)
        myLocationOverlay = MyLocationNewOverlay(provider, map)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
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
        var latestLocation: GeoPoint = GeoPoint(0.0, 0.0) // Used by all of app to see current coordinates
    }
}