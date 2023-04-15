package se.umu.lihv0010.explore

import android.graphics.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class FogOverlay : Overlay() {

    private var holeGeoPoints: MutableList<GeoPoint> = mutableListOf()

    override fun draw(c: Canvas?, osmv: MapView?, shadow: Boolean) {
        super.draw(c, osmv, shadow)

        if (!shadow) {
            val fogPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                alpha = 125
            }

            // Draw the entire fog overlay
            c?.drawRect(0f, 0f, osmv?.width?.toFloat() ?: 0f, osmv?.height?.toFloat() ?: 0f, fogPaint)

            // Create a transparent hole in the fog overlay at each specified GeoPoint
            if (holeGeoPoints.isNotEmpty()) {
                // Create a bitmap to draw the holes on
                val bitmap = Bitmap.createBitmap(osmv?.width ?: 0, osmv?.height ?: 0, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                val holePaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    alpha = 255
                }

                // Draw each hole onto the bitmap
                holeGeoPoints.forEach { geoPoint ->
                    // Convert hole radius from meters to pixels
                    val holeRadiusPixels = osmv?.projection?.metersToEquatorPixels(100f) ?: 0.0

                    val holePoint = osmv?.projection?.toPixels(geoPoint, null)
                    canvas.drawCircle(holePoint?.x?.toFloat() ?: 0f, holePoint?.y?.toFloat() ?: 0f,
                        holeRadiusPixels.toFloat(), holePaint)
                }

                // Draw the bitmap onto the canvas using PorterDuff.Mode.SRC_OUT
                val overlayPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    alpha = 255
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                }
                c?.drawBitmap(bitmap, 0f, 0f, overlayPaint)

                // Recycle the bitmap
                bitmap.recycle()
            }
        }
    }

    fun addHoleAt(geoPoint: GeoPoint) {
        holeGeoPoints.add(geoPoint)
    }

    fun removeHoleAt(index: Int) {
        if (index in holeGeoPoints.indices) {
            holeGeoPoints.removeAt(index)
        }
    }

    fun clearHoles() {
        holeGeoPoints.clear()
    }
}
