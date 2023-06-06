package se.umu.lihv0010.explore

import android.content.Context
import android.content.SharedPreferences

class Achievements(ctx: Context) {
    private var prefs: SharedPreferences = ctx.getSharedPreferences(
        "preferences", Context.MODE_PRIVATE
    )

    val achievementList = arrayListOf(
        Achievement("Walker", "Travel 1km.", 1000, prefs.getInt("totalDistanceTravelled", 0)),
        Achievement("Runner", "Travel 10km.", 10000,  prefs.getInt("totalDistanceTravelled", 0))
    )
}