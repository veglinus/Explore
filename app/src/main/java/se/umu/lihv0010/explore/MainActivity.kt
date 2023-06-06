package se.umu.lihv0010.explore

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.common.util.MapUtils
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.bonuspack.overlays.GroundOverlay
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapListener
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Polygon
import se.umu.lihv0010.explore.databinding.ActivityMainBinding
import kotlin.random.Random
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tag = "DebugExplore"

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        binding.centerButton.visibility = View.VISIBLE // Shows a center button
        return super.dispatchTouchEvent(event) // x and y are accessible from event if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        val policy = ThreadPolicy.Builder().permitAll().build() // https://github.com/MKergall/osmbonuspack/wiki/Tutorial_0
        StrictMode.setThreadPolicy(policy) // https://stackoverflow.com/questions/21213224/roadmanager-for-osmdroid-error
        getInstance().userAgentValue = "ExploreApp/1.0"

        getResourcesForCompanionObject()
        setContentView(binding.root)
        setupMapAndGameLogic()
        setupUI()
        game.myOverlays.setup()
        populateGameGoals()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMapAndGameLogic() {
        Log.d(tag, "Setting up map!")
        map = binding.mapview
        map.setMultiTouchControls(true)
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.controller.setZoom(18.0) // 18 should be standard
        game = Game(map)

        val intent = Intent(this, LocationService::class.java)
        startService(intent)
    }

    private fun setupUI() {
        fabButtonHandler()
        binding.centerButton.setOnClickListener {
            game.myOverlays.myLocationOverlay.enableFollowLocation()
            binding.centerButton.visibility = View.GONE
        }
        binding.cancelButton.setOnClickListener {
            game.cancelGoal()
            binding.cancelButton.visibility = View.GONE
            binding.timer.text = ""
        }

        game.endTimeStamp.observe(this, Observer {
            handleTimer()
        })
    }

    private fun handleTimer() {
        Log.d(tag, "HandleTimer called")
        val currTime = System.currentTimeMillis()
        val timerLength = game.endTimeStamp.value!! - currTime

        if (currTime < game.endTimeStamp.value!!) { // If time isn't out yet
            //Log.d(tag, "Time isnt out yet")

            val timer = object: CountDownTimer(timerLength, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if (game.goals.isEmpty()) {
                        this.cancel()
                        binding.timer.text = ""
                    } else {
                        // From stackoverflow, because I just wanted something pretty that works. https://stackoverflow.com/questions/625433/how-to-convert-milliseconds-to-x-mins-x-seconds-in-java
                        val textOut = String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))
                        )
                        binding.timer.text = textOut
                    }
                }

                override fun onFinish() {
                    binding.timer.text = getString(R.string.times_out)
                    val lastGoalScore = game.goals.first().halfWorth()
                    Toast.makeText(map.context, getString(R.string.times_out_long), Toast.LENGTH_LONG).show()
                    game.saveAll()

                    binding.cancelButton.visibility = View.VISIBLE
                }
            }
            timer.start()
        } else { // If time is out

            if (game.goals.isNotEmpty()) {
                binding.timer.text = getString(R.string.times_out)
                binding.cancelButton.visibility = View.VISIBLE
            } else {
                binding.timer.text = ""
            }

        }
    }

    private fun fabButtonHandler() {
        binding.fab.setOnClickListener { // Onclick FAB
            val numberPicker = NumberPicker(this)
            val options = arrayOf("100m", "500m", "750m", "1km", "2km", "3km", "4km", "5km", "10km")
            numberPicker.displayedValues = options
            numberPicker.minValue = 0;
            numberPicker.maxValue = options.size - 1;

            val alertDialog: AlertDialog? = this.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setMessage(getString(R.string.set_goal_length))
                    builder.setView(numberPicker)
                    setPositiveButton(getString(R.string.go)) { _, _ -> // User clicked OK button

                        val userInput = options[numberPicker.value]
                        var filteredInput: Double =
                            userInput.filter { it -> it.isDigit() }.toDouble() // Filter out letters from string
                        if (filteredInput < 99.0) { // Filtering for KM in string, if less than 100 then it's km.
                            filteredInput *= 1000
                        }

                        Log.d(tag, "Input: $filteredInput")
                        game.spawnGoal(filteredInput)
                        binding.fab.visibility = View.GONE

                    }
                    setNegativeButton(getString(R.string.cancel)) { dialog, id -> }
                }
                builder.create()
            }
            alertDialog?.show()
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

    private fun populateGameGoals() { // Populates goals from KMLDocument into goals of game class, if starting application for example
        val myGoals = kmlDocument.mKmlRoot.mItems
        if (myGoals != null) {
            //Log.d(tag, "Populating goals in game class")
            for (goal in myGoals) {
                Log.d(tag, "Populating game goals with: $goal")
                if (goal.javaClass.name == "org.osmdroid.bonuspack.kml.KmlPlacemark" && goal.mName != null) {

                    Log.d(tag + "HERE", "Goal text: " + goal.mDescription)
                    val goalPoint: GeoPoint = GeoPoint(goal.boundingBox.centerLatitude, goal.boundingBox.centerLongitude)
                    val parsedGoal = Goal(map, 100.0, goalPoint)

                    game.goals.add(parsedGoal)
                    game.goalExists.value = game.goals.isNotEmpty()
                }
            }
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

    private fun getResourcesForCompanionObject() {
        res = resources
    }

    override fun onPause() {
        Log.d(tag, "onPause")
        game.saveAll()
        super.onPause()
        map.onPause()
    }

    override fun onResume() {
        Log.d(tag, "onResume")
        super.onResume()
        map.onResume()
    }

    override fun onDestroy() {
        game.myOverlays.fog.saveHoles()
        super.onDestroy()
    }

    companion object {
        var kmlDocument = KmlDocument()
        lateinit var res: Resources
        var latestGoalDirection = Random(System.currentTimeMillis()).nextDouble(0.0, 360.0) // Latest direction(0-360) of new goal

        lateinit var game: Game
        lateinit var map: MapView
    }
}