package se.umu.lihv0010.explore

import org.osmdroid.util.GeoPoint

class Fog {
    /*
    private fun loadFogOfWar() {
        val geoPoints: MutableList<GeoPoint> = arrayListOf(
            GeoPoint(53.225768435790194, -0.087890625),
            GeoPoint(53.225768435790194,36.9140625),
            GeoPoint(71.49703690095419,36.9140625),
            GeoPoint(71.49703690095419,-0.087890625),
            GeoPoint(53.225768435790194, -0.087890625)
        )
        earthCover.fillColor = (-0xFA0000) // Black color fog, TODO: Animate?
        earthCover.points = geoPoints

        /*
        val holes: MutableList<List<GeoPoint>> = ArrayList()
        holes.add(uncoveredFog)
        earthCover.holes = holes
        map.overlayManager.add(earthCover)
        map.invalidate()
         */
    }

    private fun removeFog(location: GeoPoint) {
        println("Removing fog from new area")

        /*
        val oldBox = BoundingBox.fromGeoPoints(uncoveredFog)
        for (newPoint in polygon.points) {
            if (oldBox.contains(newPoint)) {

                val newBox = BoundingBox.fromGeoPoints(polygon.actualPoints)


                for ((index, oldPoint) in uncoveredFog.withIndex()) {
                    if (newBox.contains(oldPoint)) {
                        println("New box contains old point in it")

                        val newList = uncoveredFog.toMutableList()
                        newList.remove(oldPoint)
                        uncoveredFog = newList as ArrayList<GeoPoint>
                    }
                }


            } else {
                uncoveredFog.add(newPoint)
            }
        }*/

        /* POLYLINE
        locationsVisited.add(location)
        var line = Polyline()
        line.setPoints(locationsVisited)
        map.overlays.add(line)
        */

        /*
        val polygon = Polygon() //see note below
        var geoPoints = Polygon.pointsAsRect(location, 200.0, 200.0) as MutableList<GeoPoint>
        polygon.fillColor = Color.argb(0, 255, 0, 0)
        polygon.strokeColor = Color.argb(0, 0, 0, 0)
        geoPoints.add(geoPoints[0]) //forces the loop to close
        polygon.points = geoPoints


        val holes: MutableList<List<GeoPoint>> = ArrayList()
        holes.add(geoPoints)
        earthCover.holes = holes
         */
    }

     */
}