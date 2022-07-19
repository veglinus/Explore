package se.umu.lihv0010.explore

class Achievement(
    val title: String,
    val description: String,
    private val goalValue: Double,
    private val progressValue: Double
    ) {

    val progress = ((progressValue / goalValue) * 100).toInt()
}