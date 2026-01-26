package com.example.localbase.face

import android.graphics.Bitmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class FaceDetectionProcessor

@Inject
constructor(@param:ActivityContext private val context: Context) {

    private var faceDetector: FaceDetector

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("blaze_face_short_range.tflite")
            .build()

        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinDetectionConfidence(0.5f)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        faceDetector = FaceDetector.createFromOptions(context, options)
    }

    data class ProcessedFace(
        val croppedFace: Bitmap,
        val normalizedFace: Bitmap,
        val boundingBox: RectF,
        val confidence: Float
    )

    fun detectAndProcessFaces(inputBitmap: Bitmap): List<ProcessedFace> {
        val mpImage = BitmapImageBuilder(inputBitmap).build()
        val detectionResult = faceDetector.detect(mpImage)

        return processDetectionResults(inputBitmap, detectionResult)
    }

    private fun processDetectionResults(
        bitmap: Bitmap,
        result: FaceDetectorResult
    ): List<ProcessedFace> {
        val processedFaces = mutableListOf<ProcessedFace>()

        result.detections().forEach { detection ->
            val boundingBox = detection.boundingBox()
            val confidence = detection.categories()[0].score()

            // Expand bounding box slightly for better face capture
            val expandedBox = expandBoundingBox(
                boundingBox,
                bitmap.width,
                bitmap.height,
                0.2f
            )

            // Crop face from original image
            val croppedFace = cropFace(bitmap, expandedBox)

            // Normalize the cropped face
            val normalizedFace = normalizeFace(croppedFace)

            processedFaces.add(
                ProcessedFace(
                    croppedFace = croppedFace,
                    normalizedFace = normalizedFace,
                    boundingBox = expandedBox,
                    confidence = confidence
                )
            )
        }

        return processedFaces
    }

    private fun expandBoundingBox(
        box: RectF,
        imgWidth: Int,
        imgHeight: Int,
        expandRatio: Float
    ): RectF {
        val width = box.width()
        val height = box.height()
        val expandW = width * expandRatio
        val expandH = height * expandRatio

        val left = max(0f, box.left - expandW / 2)
        val top = max(0f, box.top - expandH / 2)
        val right = min(imgWidth.toFloat(), box.right + expandW / 2)
        val bottom = min(imgHeight.toFloat(), box.bottom + expandH / 2)

        return RectF(left, top, right, bottom)
    }

    private fun cropFace(bitmap: Bitmap, boundingBox: RectF): Bitmap {
        val x = boundingBox.left.toInt()
        val y = boundingBox.top.toInt()
        val width = boundingBox.width().toInt()
        val height = boundingBox.height().toInt()

        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    private fun normalizeFace(face: Bitmap, targetSize: Int = 224): Bitmap {
        // Resize to standard size
        val resizedFace = Bitmap.createScaledBitmap(face, targetSize, targetSize, true)

        // Apply histogram equalization for better lighting normalization
        val normalizedFace = histogramEqualization(resizedFace)

        return normalizedFace
    }

    private fun histogramEqualization(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calculate histogram for each channel
        val histR = IntArray(256)
        val histG = IntArray(256)
        val histB = IntArray(256)

        pixels.forEach { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            histR[r]++
            histG[g]++
            histB[b]++
        }

        // Calculate cumulative distribution function
        val cdfR = calculateCDF(histR, pixels.size)
        val cdfG = calculateCDF(histG, pixels.size)
        val cdfB = calculateCDF(histB, pixels.size)

        // Apply equalization
        val normalizedPixels = pixels.map { pixel ->
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val newR = cdfR[r]
            val newG = cdfG[g]
            val newB = cdfB[b]

            (a shl 24) or (newR shl 16) or (newG shl 8) or newB
        }.toIntArray()

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(normalizedPixels, 0, width, 0, 0, width, height)

        return result
    }

    private fun calculateCDF(histogram: IntArray, totalPixels: Int): IntArray {
        val cdf = IntArray(256)
        var sum = 0

        histogram.forEachIndexed { i, count ->
            sum += count
            cdf[i] = (sum * 255 / totalPixels).coerceIn(0, 255)
        }
        return cdf
    }

    fun close() {
        faceDetector.close()
    }
}