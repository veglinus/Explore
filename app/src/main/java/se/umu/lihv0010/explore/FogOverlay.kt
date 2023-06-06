package se.umu.lihv0010.explore

import android.content.Context
import android.graphics.*
import android.util.Log
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class FogOverlay(val context: Context) : Overlay() {
    private val saveFile = "hole_points.txt"
    private var tag = "DebugExploreFogOverlay"
    private val holeGeoPoints: HashSet<GeoPoint> = hashSetOf(GeoPoint(0.0, 0.0))

    private var bitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private var canvas: Canvas = Canvas(bitmap)

    private val overlayPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    init {
        loadHoles()
    }

    override fun draw(c: Canvas?, osmv: MapView?, shadow: Boolean) {
        super.draw(c, osmv, shadow)
        if (!shadow) {
            // Create a transparent hole in the fog overlay at each specified GeoPoint

            // Make bitmap & canvas blank before doing anything else
            bitmap = Bitmap.createBitmap(osmv?.width ?: 0, osmv?.height ?: 0, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap)

            // Draw existing holes into bitmap
            val holeRadiusPixels = osmv?.projection?.metersToEquatorPixels(100f) ?: 0.0

            for (i in 0 until holeGeoPoints.size) {
                val geoPoint = holeGeoPoints.elementAt(i)
                val holePoint = osmv?.projection?.toPixels(geoPoint, null)

                canvas.drawCircle(
                    holePoint?.x?.toFloat() ?: 0f, holePoint?.y?.toFloat() ?: 0f,
                    holeRadiusPixels.toFloat(), Paint()
                )
            }

            // Draw the bitmap onto the canvas using PorterDuff.Mode.DST_IN
            c?.drawBitmap(bitmap, 0f, 0f, overlayPaint)
            //colorFog(c, osmv)


            bitmap.recycle() // Recycle the bitmap
        }
    }

    fun addHoleAt(geoPoint: GeoPoint) {
        holeGeoPoints.add(geoPoint)
    }

    override fun onPause() {
        saveHoles()
        super.onPause()
    }

    fun saveHoles() {
        try {
            val fileOutputStream = context.openFileOutput(saveFile, Context.MODE_PRIVATE)
            val outputStreamWriter = OutputStreamWriter(fileOutputStream)
            for (geoPoint in holeGeoPoints) {
                outputStreamWriter.write("${geoPoint.latitude},${geoPoint.longitude}\n")
            }
            outputStreamWriter.close()
            Log.d(tag, "Holes saved successfully! Amount of holes: " + holeGeoPoints.size)
        } catch (e: IOException) {
            Log.e(tag, "Error saving holes: ${e.message}")
        }
    }

    private fun loadHoles() {
        try {
            val fileInputStream = context.openFileInput(saveFile)
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String? = bufferedReader.readLine()
            while (line != null) {
                val parts = line.split(",")
                if (parts.size == 2) {
                    val latitude = parts[0].toDouble()
                    val longitude = parts[1].toDouble()
                    val geoPoint = GeoPoint(latitude, longitude)
                    holeGeoPoints.add(geoPoint)
                }
                line = bufferedReader.readLine()
            }
            bufferedReader.close()
            Log.d(tag, "Holes loaded successfully! Amount of holes: " + holeGeoPoints.size)
        } catch (e: IOException) {
            Log.e(tag, "Error loading holes: ${e.message}")
        }
    }

    fun colorFog(c: Canvas?, osmv: MapView?) {
        val fogPaint = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP)
        }
        c?.drawRect(
            0f,
            0f,
            osmv?.width?.toFloat() ?: 0f,
            osmv?.height?.toFloat() ?: 0f,
            fogPaint
        )
    }
}