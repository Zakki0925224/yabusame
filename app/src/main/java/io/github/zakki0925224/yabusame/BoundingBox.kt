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
        0 -> Color.CYAN          // bike
        1 -> Color.rgb(255, 165, 0) // bus
        2 -> Color.MAGENTA       // car
        3 -> Color.GREEN         // pedestrian_light_g
        4 -> Color.RED           // pedestrian_light_r
        5 -> Color.rgb(255, 200, 0) // person
        6 -> Color.BLUE          // road_sign_crosswalk
        7 -> Color.LTGRAY        // road_sign_no_parking
        8 -> Color.BLUE          // road_sign_no_passing
        9 -> Color.DKGRAY        // road_sign_other_directional
        10 -> Color.rgb(255, 80, 80)  // road_sign_speed_limit_10
        11 -> Color.rgb(255, 100, 100) // road_sign_speed_limit_20
        12 -> Color.rgb(255, 120, 120) // road_sign_speed_limit_30
        13 -> Color.rgb(255, 140, 140) // road_sign_speed_limit_40
        14 -> Color.rgb(255, 160, 160) // road_sign_speed_limit_50
        15 -> Color.RED          // road_sign_stop
        16 -> Color.GREEN        // traffic_light_g
        17 -> Color.RED          // traffic_light_r
        18 -> Color.YELLOW       // traffic_light_y
        19 -> Color.rgb(0, 128, 255) // truck
        else -> Color.DKGRAY
    }
}

fun convertClsToVoiceGuideMessage(cls: Int): String {
    return when (cls) {
        0 -> ""
        1 -> ""
        2 -> ""
        3 -> ""
        4 -> ""
        5 -> ""
        6 -> "横断歩道があります"
        7 -> "駐車禁止区域です"
        8 -> "追い越し禁止区域です"
        9 -> ""
        10 -> "10キロ制限区域です"
        11 -> "20キロ制限区域です"
        12 -> "30キロ制限区域です"
        13 -> "40キロ制限区域です"
        14 -> "50キロ制限区域です"
        15 -> "一時停止"
        16 -> "青信号です"
        17 -> "赤信号です"
        18 -> ""
        19 -> ""
        else -> ""
    }
}

fun drawBoundingBoxes(bitmapWidth: Int, bitmapHeight: Int, boxes: List<BoundingBox>): Bitmap {
    val mutableBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(mutableBitmap)
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

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