package se.umu.lihv0010.explore

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import se.umu.lihv0010.explore.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var map : MapView
    private lateinit var binding: ActivityMainBinding
    private val TAG = "DebugExplore"

    private val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
        interval = 200
        fastestInterval = 100
        maxWaitTime = 200
        priority = PRIORITY_HIGH_ACCURACY
    }
    lateinit var locationManager: LocationManager
    private var latestLocation: GeoPoint = GeoPoint(57.86973548791104, 11.974444448918751)
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    //private var xCoordinate: Float = 0F
    //private var yCoordinate: Float = 0F

    // TODO: Center map button to set userCentered = true
    // TODO: Implement https://github.com/MKergall/osmbonuspack
    // TODO: Placing markers functionability
    // TODO: Fog of war
    // TODO: Get popular landmarks from OSM

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        //xCoordinate = ev.x
        //yCoordinate = ev.y
        myLocationOverlay.disableFollowLocation() // Stops following center marker
        binding.centerButton.visibility = View.VISIBLE // Shows a center button
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        // https://github.com/MKergall/osmbonuspack/wiki/Tutorial_0
        // https://stackoverflow.com/questions/21213224/roadmanager-for-osmdroid-error
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        getInstance().userAgentValue = "ExploreApp/1.0"
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(binding.root)

        setupMap()
        setupTouchListeners()
        setupUI()
    }

    private fun setupMap() {
        Log.d(TAG, "Setting up map!")
        map = binding.mapview
        map.setMultiTouchControls(true)
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.controller.setZoom(18.0) // Should be 18

        initLocationListener()
    }

    private fun initLocationListener() {
        // TODO: Duplicate looking for location atm, both this and mylocationoverlay
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
                                latestLocation = newLocation
                            }
                        }

                    }
                }
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        } else {
            Log.d(TAG, "Location services not granted")
        }

        if (isLocationPermissionGranted()) {
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()
            map.overlays.add(myLocationOverlay)
            map.invalidate()
        }
    }

    private fun setupTouchListeners() {
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                Log.d(TAG, "Single tap")
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                Log.d(TAG, "Long press")
                //placeMarker(p) // TODO
                return false
            }
        }
        val overlayEvents = MapEventsOverlay(mReceive)
        map.overlays.add(overlayEvents)
    }

    private fun setupUI() {
        binding.testButton.setOnClickListener {
            spawnGoal()
        }
        binding.centerButton.setOnClickListener {
            myLocationOverlay.enableFollowLocation()
            binding.centerButton.visibility = View.GONE
        }
    }

    private fun placeMarker(p: GeoPoint) {
        var newMarker = Marker(map)
        newMarker.position = p
        map.overlays.add(newMarker)
        map.invalidate()
    }

    private fun spawnGoal() {
        println("Spawning goal!")
        val random = Random(System.currentTimeMillis())
        var oldLocation: GeoPoint = latestLocation
        val newGoal = Marker(map)
        newGoal.position = oldLocation.destinationPoint(500.0, random.nextDouble(0.0, 360.0))
        map.overlays.add(newGoal)
        map.invalidate()

        pathToGoal(newGoal.position)
    }

    private fun pathToGoal(newGoal: GeoPoint) {
        val roadManager: RoadManager = OSRMRoadManager(this, "ExploreApp/1.0")
        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        val waypoints: ArrayList<GeoPoint> = arrayListOf(latestLocation, newGoal)
        val path = roadManager.getRoad(waypoints)
        val pathOverlay: Polyline = RoadManager.buildRoadOverlay(path)
        map.overlays.add(pathOverlay)
        map.invalidate()
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

    override fun onResume() {
        super.onResume()
        map.onResume()
    }
    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}