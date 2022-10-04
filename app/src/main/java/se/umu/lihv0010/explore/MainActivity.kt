package se.umu.lihv0010.explore

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.bonuspack.kml.Style
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

    // TODO: Setting home
    // TODO: Menu for creating goal, also for cancelling

    // TODO: Achievements
    // TODO: Background activity (check if user was on foot all the time)

    // Cosmetic:
    // TODO: Custom icons for player
    // TODO: App icon & change name

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
        //getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(binding.root)

        setupMapAndGameLogic()
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
        initActivityListener()
    }

    // TODO: Section into separate file
    // TODO: Deregister from updates: https://developer.android.com/guide/topics/location/transitions
    private lateinit var myPendingIntent : PendingIntent
    @SuppressLint("MissingPermission")
    private fun initActivityListener() {

        if (locationServices.isLocationPermissionGranted()) {
            myPendingIntent = PendingIntent.getActivity(applicationContext,
                0, intent, // TODO: idk what a request code is https://developer.android.com/guide/components/intents-filters#PendingIntent
                /* flags */ PendingIntent.FLAG_IMMUTABLE)

            val request = ActivityTransitionRequest(myTransitions())
            // myPendingIntent is the instance of PendingIntent where the app receives callbacks.

            ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, myPendingIntent)
            Log.d(tag, "Listening for activities!")
        }
    }

    private fun myTransitions(): MutableList<ActivityTransition> {
        val transitions = mutableListOf<ActivityTransition>()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        return transitions
    }

    private fun setupUI() {
        fabButtonHandler()
        binding.centerButton.setOnClickListener {
            locationServices.myLocationOverlay.enableFollowLocation()
            binding.centerButton.visibility = View.GONE
        }
        game.points.observe(this, Observer {
            binding.points.text = it.toString()
        })
    }

    private fun fabButtonHandler() {
        binding.fab.setOnClickListener {
            // TODO: Selector for distance
            game.spawnGoal(500.0)
            binding.fab.visibility = View.GONE
        }

        game.goalExists.observe(this, Observer {
            if (it == true) {
                //Log.d(tag, "Goal exists.")
                binding.fab.visibility = View.GONE
            } else {
                binding.fab.visibility = View.VISIBLE
            }
        })
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

    override fun onPause() {
        game.saveAll()
        super.onPause()
        map.onPause()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        game.showSavedMapData()
        populateGameGoals()
    }

    private fun populateGameGoals() {
        val myGoals = kmlDocument.mKmlRoot.mItems
        if (myGoals != null) {
            //Log.d(tag, "Populating goals in game class")
            for (goal in myGoals) {
                if (goal.javaClass.name == "org.osmdroid.bonuspack.kml.KmlPlacemark" && goal.mName != null) {
                    val goalPoint: GeoPoint = GeoPoint(goal.boundingBox.centerLatitude, goal.boundingBox.centerLongitude)
                    game.goals.add(goalPoint)
                    game.goalExists.value = game.goals.isNotEmpty()
                }
            }
        }
    }

    companion object {
        var kmlDocument = KmlDocument()
    }
}