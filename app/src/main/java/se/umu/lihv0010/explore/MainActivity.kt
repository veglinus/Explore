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
import android.widget.Toast
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
    private val tag = "DebugExplore"

    private val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
        interval = 200
        fastestInterval = 100
        maxWaitTime = 200
        priority = PRIORITY_HIGH_ACCURACY
    }

    lateinit var locationManager: LocationManager
    private var homeLocation = GeoPoint(57.86973548791104, 11.974444448918751)
    private var latestLocation: GeoPoint = GeoPoint(57.86973548791104, 11.974444448918751)
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    private val random = Random(System.currentTimeMillis())
    private var latestGoalDirection = random.nextDouble(0.0, 360.0)

    private var goals: MutableList<GeoPoint> = mutableListOf()
    private var visitedGoals: MutableList<GeoPoint> = mutableListOf() // Delete each day
    private var points: Int = 0

    // TODO: Zoom to new goal, show the description and points on it
    // TODO: Cancel button for goal (maybe allow user to finish x meters away for less points)

    // TODO: Achievements

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

        val policy = ThreadPolicy.Builder().permitAll().build() // https://github.com/MKergall/osmbonuspack/wiki/Tutorial_0
        StrictMode.setThreadPolicy(policy) // https://stackoverflow.com/questions/21213224/roadmanager-for-osmdroid-error

        getInstance().userAgentValue = "ExploreApp/1.0"
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(binding.root)

        setupMap()
        setupTouchListeners()
        setupUI()
    }

    private fun setupMap() {
        Log.d(tag, "Setting up map!")
        map = binding.mapview
        map.setMultiTouchControls(true)
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.controller.setZoom(18.0) // Should be 18

        initLocationListener()
    }

    private fun initLocationListener() {
        // TODO: Duplicate looking for location atm, both this and my location overlay
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

                                checkIfGoalReached(newLocation);
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
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()
            map.overlays.add(myLocationOverlay)
            map.invalidate()
        }
    }

    private fun checkIfGoalReached(location: GeoPoint) {
        for (goal in goals) {
            if (location.distanceToAsDouble(goal) < 50.0) {

                goals.remove(goal)
                visitedGoals.add(goal)

                // TODO: Need to find exact goal and remove it, so we can have multiple
                map.overlays.removeLast()
                map.overlays.removeLast()

                Toast.makeText(this, "Goal reached! Points have been added.", Toast.LENGTH_LONG).show()
                addPoints()
            }
        }
    }

    private fun addPoints() {
        points += 10
    }

    private fun setupTouchListeners() {
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                Log.d(tag, "Single tap")
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                Log.d(tag, "Long press")
                //placeMarker(p) // TODO
                return false
            }
        }
        val overlayEvents = MapEventsOverlay(mReceive)
        map.overlays.add(overlayEvents)
    }

    private fun setupUI() {
        binding.testButton.setOnClickListener {
            spawnGoal(500.0)
        }
        binding.centerButton.setOnClickListener {
            myLocationOverlay.enableFollowLocation()
            binding.centerButton.visibility = View.GONE
        }
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

    private fun spawnGoal(distance: Double) {
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

    private fun getClosestRoadAndPath(newGoal: GeoPoint): GeoPoint {
        // Creates path from start to finish
        val roadManager: RoadManager = OSRMRoadManager(this, "ExploreApp/1.0")
        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        val waypoints: ArrayList<GeoPoint> = arrayListOf(latestLocation, newGoal)
        val path = roadManager.getRoad(waypoints)
        path.mLength = 500.0
        val pathOverlay: Polyline = RoadManager.buildRoadOverlay(path)



        map.overlays.add(pathOverlay)
        return pathOverlay.actualPoints.last() // Returns our new point, on a reachable road
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