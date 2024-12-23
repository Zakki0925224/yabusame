package io.github.zakki0925224.yabusame

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.*
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.*
import org.tensorflow.lite.support.image.*
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.*

class YoloV8Model (context: Context) {
    private var interpreter: Interpreter
    private var labels: List<String> = mutableListOf()
    private var tensorWidth: Int
    private var tensorHeight: Int
    private var numChannel: Int
    private var numElements: Int

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STDDEV))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    companion object {
        private const val MODEL_PATH = "yolov8s_float32.tflite"
        private const val LABEL_PATH = "labels.txt"
        private const val INPUT_MEAN = 0.0f
        private const val INPUT_STDDEV = 255.0f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONF_THRESHOLD = 0.7f
        private const val IOU_THRESHOLD = 0.4f
        private const val MAX_DETECTION_COUNT = 10

        // https://qiita.com/napspans/items/e7390280b7f31675325c
        private val DETECTION_LIST = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)
    }

    init {
        val model = FileUtil.loadMappedFile(context, MODEL_PATH)
        val options = Interpreter.Options()
        options.numThreads = 4

        this.interpreter = Interpreter(model, options)

        val inputShape = this.interpreter.getInputTensor(0).shape()
        val outputShape = this.interpreter.getOutputTensor(0).shape()
        this.tensorWidth = inputShape[1]
        this.tensorHeight = inputShape[2]
        this.numChannel = outputShape[1]
        this.numElements = outputShape[2]

        // read labels
        try {
            val inputStream: InputStream = context.assets.open(LABEL_PATH)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.useLines { lines -> lines.forEach { labels += it } }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun calcIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)

        val intersectArea = maxOf(0.0f, x2 - x1) * maxOf(0.0f, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h

        return intersectArea / (box1Area + box2Area - intersectArea)
    }

    // Extract boxes whose confidence level is higher than the threshold
    private fun bestBox(array: FloatArray) : List<BoundingBox>? {
        val boxes = mutableListOf<BoundingBox>()

        for (i in 0 until this.numElements) {
            var maxConf = CONF_THRESHOLD
            var maxIdx = -1
            var j = 4
            var arrayIdx = i + this.numElements * j

            while (j < this.numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += this.numElements
            }

            if (maxConf <= CONF_THRESHOLD) {
                continue
            }

            // invalid data
            if (maxConf > 1.0f) {
                continue
            }

            if (maxIdx !in DETECTION_LIST) {
                continue
            }

            val clsName = this.labels[maxIdx]
            val cx = array[i]
            val cy = array[i + this.numElements]
            val w = array[i + this.numElements * 2]
            val h = array[i + this.numElements * 3]
            val x1 = cx - (w / 2.0f)
            val y1 = cy - (h / 2.0f)
            val x2 = cx + (w / 2.0f)
            val y2 = cy + (h / 2.0f)

            if (x1 < 0.0f || x1 > 1.0f) continue
            if (y1 < 0.0f || y1 > 1.0f) continue
            if (x2 < 0.0f || x2 > 1.0f) continue
            if (y2 < 0.0f || y2 > 1.0f) continue

            boxes.add(
                BoundingBox(
                x1 = x1,
                y1 = y1,
                x2 = x2,
                y2 = y2,
                cx = cx,
                cy = cy,
                w = w,
                h = h,
                cnf = maxConf,
                cls = maxIdx,
                clsName = clsName
            )
            )
        }

        if (boxes.isEmpty()) {
            return null
        }

        // sort
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty() && selectedBoxes.size < MAX_DETECTION_COUNT) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calcIoU(first, nextBox)

                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    fun detect(frame: Bitmap): List<BoundingBox>? {
        if (this.tensorWidth == 0) return null
        if (this.tensorHeight == 0) return null
        if (this.numChannel == 0) return null
        if (this.numElements == 0) return null

        val resizedBitmap = Bitmap.createScaledBitmap(frame, this.tensorWidth, this.tensorHeight, false)
        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(resizedBitmap)
        val processedImage = this.imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(
            intArrayOf(1, this.numChannel, this.numElements),
            OUTPUT_IMAGE_TYPE)
        this.interpreter.run(imageBuffer, output.buffer)

        val boundingBoxes = bestBox(output.floatArray)

//        if (boundingBoxes != null) {
//            Log.d("detector", "detected: ${boundingBoxes.size} (confidence threshold: $CONF_THRESHOLD)")
//            Log.d("detector", "confidence: max: ${boundingBoxes.maxOf { it.cnf }}, min: ${boundingBoxes.minOf { it.cnf }}")
//            if (boundingBoxes.size == MAX_DETECTION_COUNT) {
//                for (box in boundingBoxes) {
//                    Log.d("detector", "box: $box")
//                }
//            }
//        }

        return boundingBoxes
    }
}