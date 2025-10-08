package com.example.localbase.face

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object FaceEmbeddingHelper {
    var tfLite: Interpreter? = null
    private const val TAG = "FaceEmbeddingHelper"

    /**
     * Loads the TFLite model from assets.
     * Default model name is "facenet.tflite", but you can pass any compatible model.
     */
    fun loadModel(context: Context, modelName: String = "facenet_512.tflite") : Interpreter {
        if (tfLite == null) {
            val model = loadModelFile(context, modelName)
            tfLite = Interpreter(model)

            // Log model input/output shapes
            val inputShape = tfLite!!.getInputTensor(0).shape().joinToString()
            val outputShape = tfLite!!.getOutputTensor(0).shape().joinToString()
            Log.d(TAG, "Model loaded: $modelName")
            Log.d(TAG, "Input shape: $inputShape")
            Log.d(TAG, "Output shape: $outputShape")
        }

        return tfLite!!
    }

    /**
     * Load TFLite model file from assets folder
     */
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    /**
     * Get face embedding from a given Bitmap image (should be a face-cropped image)
     * Returns a FloatArray of embedding (e.g., size 128, 192, or 384 depending on model)
     */
    fun getFaceEmbedding(bitmap: Bitmap): FloatArray {
        if (tfLite == null) throw IllegalStateException("Model not loaded. Call loadModel() first.")

        val inputSize = 112 // Most face embedding models use 112x112 input
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        // Prepare input tensor: shape (1, 112, 112, 3), normalized to [-1, 1]
        val input = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } }
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val px = resized.getPixel(x, y)
                input[0][y][x][0] = ((px shr 16 and 0xFF) - 127.5f) / 128f // R
                input[0][y][x][1] = ((px shr 8 and 0xFF) - 127.5f) / 128f  // G
                input[0][y][x][2] = ((px and 0xFF) - 127.5f) / 128f       // B
            }
        }

        // Dynamically get output size from the model
        val outputShape = tfLite!!.getOutputTensor(0).shape() // e.g., [1, 192] or [1, 384]
        val outputSize = outputShape[1]
        val output = Array(1) { FloatArray(outputSize) }

        // Run inference
        tfLite!!.run(input, output)

        // Normalize the embedding to unit length (L2 normalization)
        val embedding = output[0]
        val norm = kotlin.math.sqrt(embedding.map { it * it }.sum())
        val normalizedEmbedding = embedding.map { it / norm }.toFloatArray()

        Log.d(TAG, "Embedding size: ${normalizedEmbedding.size}")
        return normalizedEmbedding
    }

    fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        require(vec1.size == vec2.size) { "Vectors must be of same size" }

        val dotProduct = vec1.zip(vec2).sumOf { (a, b) -> (a * b).toDouble() }
        val magnitude1 = kotlin.math.sqrt(vec1.sumOf { (it * it).toDouble() })
        val magnitude2 = kotlin.math.sqrt(vec2.sumOf { (it * it).toDouble() })

        return (dotProduct / (magnitude1 * magnitude2)).toFloat()
    }

    fun cosineSimilarityPercentage(vec1: FloatArray, vec2: FloatArray): Float {
        require(vec1.size == vec2.size) { "Vectors must be of same size" }

        val dotProduct = vec1.zip(vec2).sumOf { (a, b) -> (a * b).toDouble() }
        val magnitude1 = kotlin.math.sqrt(vec1.sumOf { (it * it).toDouble() })
        val magnitude2 = kotlin.math.sqrt(vec2.sumOf { (it * it).toDouble() })

        val similarity = (dotProduct / (magnitude1 * magnitude2)).toFloat()

        // Convert cosine similarity to percentage
        return (similarity * 100)
    }
}