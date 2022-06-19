package se.umu.lihv0010.explore

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker


// TODO: Following tutorial https://www.geeksforgeeks.org/how-to-get-current-location-in-android/

class MainActivity : AppCompatActivity() {
    private lateinit var map : MapView

    private val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
        interval = 200
        fastestInterval = 100
        maxWaitTime = 200
        priority = PRIORITY_HIGH_ACCURACY
    }

    lateinit var locationManager: LocationManager
    private var latestLocation: GeoPoint = GeoPoint(57.86973548791104, 11.974444448918751)
    private lateinit var mapController: IMapController
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var userCentered: Boolean = true

    private var xCoordinate: Float = 0F
    private var yCoordinate: Float = 0F


    // TODO: Center map button to set userCentered = true
    // TODO: Placing markers functionability
    // TODO: Fog of war
    // TODO: Get popular landmarks from OSM

    private lateinit var centerDot: Marker

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        xCoordinate = ev.x
        yCoordinate = ev.y
        userCentered = false
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.mapview)
        initialMapControls()
        initLocationListener()
        createMarker()
        setupListener()
    }

    private fun setupListener() {
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                println("SingleTapConfirmedHelper")
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                println("longPressHelper")
                popupMenu()
                return false
            }
        }
        val overlayEvents = MapEventsOverlay(mReceive)
        map.overlays.add(overlayEvents)
    }

    private fun popupMenu() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.menu.popup_menu, null)
        // TODO: Does not take menu, wants layout instead
        // https://stackoverflow.com/questions/5944987/how-to-create-a-popup-window-popupwindow-in-android

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it
        val popupWindow = PopupWindow(popupView, width, height, focusable)

        popupWindow.showAtLocation(map, Gravity.NO_GRAVITY, xCoordinate.toInt(), yCoordinate.toInt())

        // TODO: Listen to click and setup calls for the menu buttons
    }

    private fun placeMarker() {
        TODO("Not yet implemented")
    }

    private fun createMarker() {
        centerDot = Marker(map)
        centerDot.position = latestLocation
        centerDot.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(centerDot)
        map.invalidate()
    }

    private fun initialMapControls() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        mapController = map.controller
        mapController.setZoom(18.0)
    }

    private fun initLocationListener() {
        @SuppressLint("MissingPermission")
        if (isLocationPermissionGranted()) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult.lastLocation?.let {
                        val result = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (result != null) {
                            val newLocation = GeoPoint(result.latitude, result.longitude)
                            if (newLocation != latestLocation) {
                                println("Setting new location")
                                setLocation(newLocation)

                                centerDot.position = latestLocation
                            } else {
                                //println("Location is same as last check")
                            }
                        }

                    }
                }
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }

    private fun setLocation(newLocation: GeoPoint) {
        latestLocation = newLocation
        //mapController.setCenter(newLocation)
        if (userCentered) {
            mapController.animateTo(newLocation)
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
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

    override fun onResume() {
        super.onResume()
        map.onResume()
    }
    override fun onPause() {
        super.onPause()
        map.onPause()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(), 1)
        }
    }
}