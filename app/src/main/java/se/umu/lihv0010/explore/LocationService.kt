package se.umu.lihv0010.explore

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import org.osmdroid.util.GeoPoint
import se.umu.lihv0010.explore.MainActivity.Companion.game

class LocationService : Service() {
    private lateinit var locationManager: LocationManager
    private val notificationId = 1998 // Unique ID for the foreground notification

    override fun onCreate() {
        super.onCreate()
        // Initialize the location manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the service in the foreground
        startForeground(notificationId, createNotification())
        // Start listening for location updates
        startLocationUpdates()
        // Return START_STICKY to indicate that the service should be restarted if it gets killed
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        stopLocationUpdates()
        stopSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        // Create a notification for the foreground service
        // You can customize the notification as per your requirements
        val notificationChannelId = "location_channel"
        val notificationChannelName = "Location Updates"
        val notificationChannel = NotificationChannel(
            notificationChannelId,
            notificationChannelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Location Service")
            .setContentText("Listening to location updates")
            .setSmallIcon(R.drawable.ic_center_location)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        return notificationBuilder.build()
    }

    private fun startLocationUpdates() {
        try {
            // Request location updates from the location manager
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener
            )
            Log.d(TAG, "Location updates started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to request location updates: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        // Remove location updates from the location manager
        locationManager.removeUpdates(locationListener)
        Log.d(TAG, "Location updates stopped")
    }

    private val locationListener: LocationListener = LocationListener { location -> // Log the current location
        val newLocation = GeoPoint(location.latitude, location.longitude)
        if (newLocation != latestLocation) {
            game.addTravelledDistance(latestLocation, newLocation)
            //Log.d(tag, "New location is not last location. Checking for goal.")
            latestLocation = newLocation
            game.checkIfGoalReached(newLocation)
            game.myOverlays.fog.addHoleAt(newLocation)
        } else {
            //Log.d(tag, "New location was last location.")
        }
        Log.d(TAG, "Current location: ${location.latitude}, ${location.longitude}")
    }

    companion object {
        private const val TAG = "LocationService"
        private const val MIN_TIME_BETWEEN_UPDATES: Long = 3000 // Minimum time interval between location updates (in milliseconds)
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 10f // Minimum distance change for location updates (in meters)
        var latestLocation: GeoPoint = GeoPoint(0.0, 0.0) // Used by all of app to see current coordinates
    }
}