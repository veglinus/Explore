package se.umu.lihv0010.explore

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity


class MyBroadcastReceiver : BroadcastReceiver() {
    private val tag = "DebugExploreMyBroadcastReceiver"

    init {
        Log.d(tag, "BROADCAST RECEIVER INIT")
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "onReceive")
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!


            for (event in result.transitionEvents) {
                Log.d(tag, "TransitionEvent: $event")
            }


            val latestEventType = result.transitionEvents.last().activityType

            if (latestEventType == DetectedActivity.ON_FOOT) { // This is a type, might not work
                Log.d(tag, "User is on foot! $latestEventType")
                // TODO: User is on foot, start listening for location changes
            } else if (latestEventType == DetectedActivity.STILL) {
                Log.d(tag, "User is still! $latestEventType")
                // TODO: Stop listening to location changes
            }
        }
    }

}