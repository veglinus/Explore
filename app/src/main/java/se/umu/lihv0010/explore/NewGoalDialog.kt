package se.umu.lihv0010.explore

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.NonCancellable.start

/*
class NewGoalDialog : AlertDialog {

    private val numberPicker = NumberPicker(context)
    private val options = arrayOf("100m", "500m", "750m", "1km", "2km", "3km", "4km", "5km", "10km")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        numberPicker.displayedValues = options
        numberPicker.minValue = 0
        numberPicker.maxValue = options.size - 1

        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            builder.apply {
                setMessage("Set goal length")
                builder.setView(numberPicker)
                setPositiveButton("Go!") { _, _ -> // User clicked OK button

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
                setNegativeButton("Cancel") { dialog, id -> }
            }


            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        fun newDialog(): DialogFragment {
            return NewGoalDialog()
        }
    }
}*/