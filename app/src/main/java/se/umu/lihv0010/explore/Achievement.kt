package se.umu.lihv0010.explore

class Achievement(
    val title: String,
    val description: String,
    private val goalValue: Int,
    private val progressValue: Int
    ) {

    val progress = ((progressValue / goalValue) * 100)
}