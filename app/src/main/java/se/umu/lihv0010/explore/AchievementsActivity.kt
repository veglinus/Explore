package se.umu.lihv0010.explore

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import se.umu.lihv0010.explore.R.layout.achievement_list_entry
import se.umu.lihv0010.explore.databinding.ActivityAchievementsBinding

class AchievementsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAchievementsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //val entryAdapter = ArrayAdapter(this, R.layout.achievement_list_entry, array)
        binding.achievementsList.adapter = ListAdapter(this)
    }

    private class ListAdapter(context: Context): BaseAdapter() {
        private val mContext: Context
        private val achievementList = Achievements.achievementList

        init {
            mContext = context
        }

        override fun getCount(): Int {
            return achievementList.size
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getItem(p0: Int): Any {
            return p0.toLong()
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            val layoutinflater = LayoutInflater.from(mContext)

            val row = layoutinflater.inflate(achievement_list_entry, p2, false)

            row.findViewById<TextView>(R.id.rowTitle).text = achievementList[p0].title
            row.findViewById<TextView>(R.id.rowDesc).text = achievementList[p0].description
            row.findViewById<ProgressBar>(R.id.progress).progress = achievementList[p0].progress

            return row
        }
    }
}