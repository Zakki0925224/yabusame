package io.github.zakki0925224.yabusame

import android.graphics.*

data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val cnf: Float,
    val cls: Int,
    val clsName: String
)

fun convertClsToColor(cls: Int): Int {
    return when (cls) {
        // pedestrian_light_g
        0 -> Color.GREEN
        // pedestrian_light_r
        1 -> Color.RED
        // traffic_light_g
        2 -> Color.GREEN
        // traffic_light_r
        3 -> Color.RED
        // traffic_light_y
        4 -> Color.YELLOW
        else -> Color.RED
    }
}

fun drawBoundingBoxes(bitmap: Bitmap, boxes: List<BoundingBox>): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        typeface = Typeface.DEFAULT
    }

    for (box in boxes) {
        val paint = Paint().apply {
            color = convertClsToColor(box.cls)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val rect = RectF(
            box.x1 * mutableBitmap.width,
            box.y1 * mutableBitmap.height,
            box.x2 * mutableBitmap.width,
            box.y2 * mutableBitmap.height
        )
        canvas.drawRect(rect, paint)
        canvas.drawText("${box.clsName} - ${String.format("%.2f", box.cnf)}", rect.left, rect.top, textPaint)
    }

    return mutableBitmap
}