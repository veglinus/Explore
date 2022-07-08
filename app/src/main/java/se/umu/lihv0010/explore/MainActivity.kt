package se.umu.lihv0010.explore

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import se.umu.lihv0010.explore.databinding.ActivityMainBinding
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private lateinit var map : MapView
    private lateinit var binding: ActivityMainBinding
    private val tag = "DebugExplore"

    private lateinit var game: Game
    private lateinit var locationServices: LocationServices

    // TODO: Zoom to new goal, show the description and points on it
    // TODO: Cancel button for goal (maybe allow user to finish x meters away for less points)

    // TODO: Achievements
    // TODO: Custom icons for player and goal
    // TODO: Listen to WatchOS Heartrate sensor?

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        locationServices.myLocationOverlay.disableFollowLocation() // Stops following center marker
        binding.centerButton.visibility = View.VISIBLE // Shows a center button
        return super.dispatchTouchEvent(event) // x and y are accessible from event if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        val policy = ThreadPolicy.Builder().permitAll().build() // https://github.com/MKergall/osmbonuspack/wiki/Tutorial_0
        StrictMode.setThreadPolicy(policy) // https://stackoverflow.com/questions/21213224/roadmanager-for-osmdroid-error

        getInstance().userAgentValue = "ExploreApp/1.0"
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(binding.root)

        setupMapAndGameLogic()
        //setupMapTouchListeners()
        setupUI()
    }

    private fun setupMapAndGameLogic() {
        Log.d(tag, "Setting up map!")
        map = binding.mapview
        map.setMultiTouchControls(true)
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.controller.setZoom(18.0) // 18 should be standard

        game = Game(map)
        locationServices = LocationServices(map, game)
    }

    private fun setupMapTouchListeners() {
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
            // TODO: Selector for distance
            game.spawnGoal(500.0)
        }

        binding.centerButton.setOnClickListener {
            locationServices.myLocationOverlay.enableFollowLocation()
            binding.centerButton.visibility = View.GONE
        }

        game.points.observe(this, Observer {
            binding.points.text = it.toString()
        })
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