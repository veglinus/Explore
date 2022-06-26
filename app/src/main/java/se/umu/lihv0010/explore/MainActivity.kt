package se.umu.lihv0010.explore

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.MotionEvent
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
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

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

    // TODO: Implement https://github.com/MKergall/osmbonuspack

    // TODO: Placing markers functionability
    // TODO: Fog of war
    // TODO: Get popular landmarks from OSM

    private lateinit var centerDot: Marker
    private var earthCover = Polygon()

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
        map.setMultiTouchControls(true)


        initialMapControls()
        initLocationListener()
        createLocationMarker()
        loadFogOfWar()
        setupListener()
    }

    private fun setupListener() {
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                println("SingleTapConfirmedHelper")

                val circle = Polygon(map)
                circle.points = Polygon.pointsAsCircle(p, 5000.0)
                circle.fillColor = (Color.TRANSPARENT)
                circle.setStrokeColor(Color.RED)
                map.overlays.add(circle)
                map.invalidate()


                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                println("longPressHelper")
                //placeMarker(p) // TODO
                return false
            }
        }
        val overlayEvents = MapEventsOverlay(mReceive)
        map.overlays.add(overlayEvents)
    }

    private fun loadFogOfWar() {
        val geoPoints: MutableList<GeoPoint> = arrayListOf(
            GeoPoint(53.225768435790194, -0.087890625),
            GeoPoint(53.225768435790194,36.9140625),
            GeoPoint(71.49703690095419,36.9140625),
            GeoPoint(71.49703690095419,-0.087890625),
            GeoPoint(53.225768435790194, -0.087890625)
        )
        earthCover.fillColor = (-0xFA0000) // Black color fog, TODO: Animate?
        earthCover.points = geoPoints

        /*
        val holes: MutableList<List<GeoPoint>> = ArrayList()
        holes.add(uncoveredFog)
        earthCover.holes = holes
        map.overlayManager.add(earthCover)
        map.invalidate()
         */
    }

    private var locationsVisited: MutableList<GeoPoint> = mutableListOf()

    private fun removeFog(location: GeoPoint) {
        println("Removing fog from new area")

        /*
        val oldBox = BoundingBox.fromGeoPoints(uncoveredFog)
        for (newPoint in polygon.points) {
            if (oldBox.contains(newPoint)) {

                val newBox = BoundingBox.fromGeoPoints(polygon.actualPoints)


                for ((index, oldPoint) in uncoveredFog.withIndex()) {
                    if (newBox.contains(oldPoint)) {
                        println("New box contains old point in it")

                        val newList = uncoveredFog.toMutableList()
                        newList.remove(oldPoint)
                        uncoveredFog = newList as ArrayList<GeoPoint>
                    }
                }


            } else {
                uncoveredFog.add(newPoint)
            }
        }*/

        /* POLYLINE
        locationsVisited.add(location)
        var line = Polyline()
        line.setPoints(locationsVisited)
        map.overlays.add(line)
        */

        /*
        val polygon = Polygon() //see note below
        var geoPoints = Polygon.pointsAsRect(location, 200.0, 200.0) as MutableList<GeoPoint>
        polygon.fillColor = Color.argb(0, 255, 0, 0)
        polygon.strokeColor = Color.argb(0, 0, 0, 0)
        geoPoints.add(geoPoints[0]) //forces the loop to close
        polygon.points = geoPoints


        val holes: MutableList<List<GeoPoint>> = ArrayList()
        holes.add(geoPoints)
        earthCover.holes = holes
         */
    }

    private fun placeMarker(p: GeoPoint) {
        var newMarker = Marker(map)
        newMarker.position = p
        //newMarker.icon =
        map.overlays.add(newMarker)
        map.invalidate()
    }

    private fun createLocationMarker() {
        centerDot = Marker(map)
        centerDot.position = latestLocation
        centerDot.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(centerDot)
        map.invalidate()
    }

    private fun initialMapControls() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        mapController = map.controller
        mapController.setZoom(18.0) // Should be 18
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

                                removeFog(newLocation)

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