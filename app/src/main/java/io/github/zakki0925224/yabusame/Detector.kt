package io.github.zakki0925224.yabusame

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.*
import org.tensorflow.lite.gpu.*
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.*
import org.tensorflow.lite.support.image.*
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.*

class Detector (context: Context) {
    private var interpreter: Interpreter
    private var labels: List<String> = mutableListOf()
    private var tensorWidth: Int = 0
    private var tensorHeight: Int = 0
    private var numChannel: Int = 0
    private var numElements: Int = 0
    var cnfThreshold: Float = DEFAULT_CNF_THRESHOLD
    var ioUThreshold: Float = DEFAULT_IOU_THRESHOLD
    var isDetectorEnabled: Boolean = true
    var isGpuMode: Boolean = false

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STDDEV))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    interface DetectorListener {
        fun onDetected(boxes: List<BoundingBox>, inferenceTime: Long)
        fun onEmptyDetected()
        fun onDetectingButDetectorDisabled()
    }

    var detectorListener: DetectorListener? = null

    companion object {
        private const val MODEL_PATH = "yolov8n_float32.tflite"
        private const val LABEL_PATH = "labels.txt"
        private const val INPUT_MEAN = 0.0f
        private const val INPUT_STDDEV = 255.0f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        const val DEFAULT_CNF_THRESHOLD = 0.8f
        const val DEFAULT_IOU_THRESHOLD = 0.7f
    }

    init {
        val model = FileUtil.loadMappedFile(context, MODEL_PATH)
        val compatList = CompatibilityList()
        val options = Interpreter.Options().apply {
            if (compatList.isDelegateSupportedOnThisDevice) {
                this.addDelegate(GpuDelegate(compatList.bestOptionsForThisDevice))
                isGpuMode = true
            } else {
                this.numThreads = 4
                Log.d("detector", "GPU delegate is not supported.")
            }
        }

        this.interpreter = Interpreter(model, options)

        val inputShape = this.interpreter.getInputTensor(0)?.shape()
        val outputShape = this.interpreter.getOutputTensor(0)?.shape()

        if (inputShape != null) {
            this.tensorWidth = inputShape[1]
            this.tensorHeight = inputShape[2]

            if (inputShape[1] == 3) {
                this.tensorWidth = inputShape[2]
                this.tensorHeight = inputShape[3]
            }
        }

        if (outputShape != null) {
            this.numChannel = outputShape[1]
            this.numElements = outputShape[2]
        }

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
            var maxCnf = this.cnfThreshold
            var maxIdx = -1
            var j = 4
            var arrayIdx = i + this.numElements * j

            while (j < this.numChannel) {
                if (array[arrayIdx] > maxCnf) {
                    maxCnf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += this.numElements
            }

            if (maxCnf > this.cnfThreshold) {
                val clsName = labels[maxIdx]
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

                boxes.add(BoundingBox(
                    x1, y1, x2, y2, cx, cy, w, h, maxCnf, maxIdx, clsName
                ))
            }
        }

        if (boxes.isEmpty()) return null

        // sort
        val sortedBoxes = boxes.sortedByDescending { b -> b.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calcIoU(first, nextBox)

                if (iou >= this.ioUThreshold) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    fun close() {
        this.interpreter.close()
    }

    fun detect(frame: Bitmap) {
        if (!this.isDetectorEnabled) {
            this.detectorListener?.onDetectingButDetectorDisabled()
            return
        }
        if (this.tensorWidth == 0) return
        if (this.tensorHeight == 0) return
        if (this.numChannel == 0) return
        if (this.numElements == 0) return

        var inferenceTime = SystemClock.uptimeMillis()
        val resizedBitmap = Bitmap.createScaledBitmap(frame, this.tensorWidth, this.tensorHeight, false)
        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(resizedBitmap)
        val processedImage = this.imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(
            intArrayOf(1, this.numChannel, this.numElements),
            OUTPUT_IMAGE_TYPE)

        try {
            this.interpreter.run(imageBuffer, output.buffer)
        } catch (e: Exception) {
            Log.e("detector", "Failed to run the model", e)
            return
        }

        val bestBoxes = bestBox(output.floatArray)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        if (bestBoxes == null) {
            this.detectorListener?.onEmptyDetected()
            return
        }

        this.detectorListener?.onDetected(bestBoxes, inferenceTime)
    }
}