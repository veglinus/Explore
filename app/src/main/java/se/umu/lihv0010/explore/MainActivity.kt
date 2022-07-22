package se.umu.lihv0010.explore

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import se.umu.lihv0010.explore.databinding.ActivityMainBinding
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var map : MapView
    private lateinit var binding: ActivityMainBinding
    private val tag = "DebugExplore"

    private lateinit var game: Game
    private lateinit var locationServices: LocationServices

    // TODO: Detect close to goal properly
    // TODO: Cancel button for goal (maybe allow user to finish x meters away for less points)
    // TODO: Custom icons for player and goal, set markers to have a global standard icon to surpass bug of kml markers having wrong marker

    // TODO: Achievements
    // TODO: Track time it takes to go to goal

    // TODO: App icon & change name
    // TODO: Background activity
    // TODO: Battery saver mode like in POGO

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
        showSavedMapData()
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

    private fun showSavedMapData() {
        val localFile: File = kmlDocument.getDefaultPathForAndroid(this, "my_data.kml")
        kmlDocument.parseKMLFile(localFile)
        val kmlOverlay = kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument) as FolderOverlay
        map.overlays.add(kmlOverlay)
    }

    fun onAchievementClick(mi: MenuItem) {
        val intent = Intent(this, AchievementsActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
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
        populateGameGoals()
    }

    private fun populateGameGoals() {
        val myGoals = kmlDocument.mKmlRoot.mItems
        if (myGoals != null) {
            for (goal in myGoals) {
                if (goal.javaClass.name == "org.osmdroid.bonuspack.kml.KmlPlacemark") {
                    val goalPoint: GeoPoint = GeoPoint(goal.boundingBox.centerLatitude, goal.boundingBox.centerLongitude)
                    game.goals.add(goalPoint)
                }
            }
        }
    }


    override fun onPause() {
        super.onPause()
        map.onPause()
        val localFile: File = kmlDocument.getDefaultPathForAndroid(this, "my_data.kml")
        kmlDocument.saveAsKML(localFile);
    }

    companion object {
        var kmlDocument = KmlDocument()
    }
}