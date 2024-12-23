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

fun drawBoundingBoxes(bitmap: Bitmap, boxes: List<BoundingBox>): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)
    val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }

    for (box in boxes) {
        val rect = RectF(
            box.x1 * mutableBitmap.width,
            box.y1 * mutableBitmap.height,
            box.x2 * mutableBitmap.width,
            box.y2 * mutableBitmap.height
        )
        canvas.drawRect(rect, paint)
        canvas.drawText(box.clsName, rect.left, rect.bottom, textPaint)
    }

    return mutableBitmap
}