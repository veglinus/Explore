package se.umu.lihv0010.explore

import android.content.Context
import android.content.SharedPreferences

class Achievements(ctx: Context) {
    private var prefs: SharedPreferences = ctx.getSharedPreferences(
        "preferences", Context.MODE_PRIVATE
    )

    val achievementList = arrayListOf<Achievement>(
        Achievement("Walker", "Travel 1km by walking to goals.", 1000, prefs.getInt("points", 0)),
        Achievement("Runner", "Travel 10km by walking to goals.", 10000,  prefs.getInt("points", 0))
    )
}