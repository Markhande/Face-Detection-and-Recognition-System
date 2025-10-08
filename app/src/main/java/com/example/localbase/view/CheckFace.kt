package com.example.localbase.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.localbase.R
import com.example.localbase.databinding.ActivityCheckFaceBinding
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.localbase.database.Student
import com.example.localbase.database.StudentViewModel
import com.example.localbase.databinding.ActivityMainBinding
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.health.connect.datatypes.units.Percentage
import android.media.FaceDetector
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Telephony.Mms.Part.FILENAME
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.lang.StrictMath.sqrt
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.toFloatArray
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import androidx.core.graphics.createBitmap
import com.bumptech.glide.Glide
import com.example.localbase.face.FaceDetectionProcessor
import com.example.localbase.face.FaceEmbeddingHelper
import com.example.localbase.helper.ToolBar
import com.example.localbase.overlayes.FaceOverlayView
import dagger.hilt.android.AndroidEntryPoint

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files.createFile
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.compareTo
import kotlin.text.toInt
import kotlin.toString

@AndroidEntryPoint
class CheckFace : AppCompatActivity() {

    private lateinit var binding: ActivityCheckFaceBinding

    @Inject
    lateinit var toolBar: ToolBar
    private val studentViewModel: StudentViewModel by viewModels()

    private lateinit var imageCapture: ImageCapture

    private lateinit var faceDetector: FaceDetection

    var tfLite: Interpreter? = null

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var faceOverlay: FaceOverlayView
    private var faceDetectedStartTime: Long = 0L
    private var hasCapturedImage = false
    private val facePresenceThreshold = 1000L

    private var capturedBitmap: Bitmap? = null

    private var faceEmbeddingHelper = FaceEmbeddingHelper

    @Inject
    lateinit var faceProcessor: FaceDetectionProcessor

    private lateinit var temp: List<Student>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //faceProcessor = FaceDetectionProcessor(this)

        // Inflate view binding and set content view
        binding = ActivityCheckFaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load the face embedding model
        val inter = faceEmbeddingHelper.loadModel(this)

        tfLite = inter

        // Initialize face overlay view and add it to the root view
        faceOverlay = FaceOverlayView(this)
        binding.root.addView(faceOverlay)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.reset.setOnClickListener {
            binding.floatArrayPoint.text = null
        }

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Image analysis use case with backpressure strategy
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(imageProxy)
            }

            // Image capture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to lifecycle
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera binding failed: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build()

            val detector = FaceDetection.getClient(options)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    faceOverlay.setFaces(
                        faces,
                        image.width,
                        image.height,
                        imageProxy.imageInfo.rotationDegrees,
                        true
                    )

                    val currentTime = System.currentTimeMillis()

                    if (faces.isNotEmpty()) {
                        if (faceDetectedStartTime == 0L) {
                            faceDetectedStartTime = currentTime
                        } else {
                            val elapsed = currentTime - faceDetectedStartTime
                            if (elapsed >= facePresenceThreshold && !hasCapturedImage) {
                                hasCapturedImage = true
                                captureImageInMemory()
                            }
                        }
                    } else {
                        faceDetectedStartTime = 0L
                        hasCapturedImage = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Face Detection", "Detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun captureImageInMemory() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = toolBar.imageProxyToBitmap(imageProxy) ?: return
                    capturedBitmap = bitmap

                    processImage(capturedBitmap!!)

                    imageProxy.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        applicationContext,
                        "Capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("CameraX", "Image capture error", exception)
                }
            }
        )
    }

    private fun processImage(bitmap: Bitmap) {
        // Detect and process faces
        val processedFaces = faceProcessor.detectAndProcessFaces(bitmap)

        processedFaces.forEachIndexed { index, face ->
            Log.d("FaceDetection", "Face $index detected with confidence: ${face.confidence}")

            // Save cropped face
            toolBar.saveBitmapToFile(face.croppedFace, "face_${index}_cropped.jpg")

            // Save normalized face
            toolBar.saveBitmapToFile(face.normalizedFace, "face_${index}_normalized.jpg")

            // You can now use the normalized face for:
            // - Face recognition
            // - Face analysis
            // - ML model input

            val embedding = faceEmbeddingHelper.getFaceEmbedding(face.normalizedFace)

            //studentViewModel.insert(data)
            val embeddingStr = embedding.joinToString(", ") { "%.4f".format(it) + "f" }
            binding.floatArrayPoint.setText(embeddingStr.toString())

            studentViewModel.allStudents.observe(this) {
                //Log.d("checking", temp.toString())

                for (i in it) {
                    val secondFace = i.grades
                    val matching = faceEmbeddingHelper.cosineSimilarityPercentage(embedding, secondFace)
                    Log.d("simularities", "${i.name} → ${matching.toInt()}%")

                    if (matching > 75) {
                        stopCamera()
                        showAlert(i, face, matching)
                        break
                    } else {
                        restartFaceDetection()
                        toolBar.showSnackbar(binding.root, "Not Matched")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingInflatedId", "CheckResult")
    private fun showAlert(student: Student, face: FaceDetectionProcessor.ProcessedFace, matching: Float) {
        val viewLayout = LayoutInflater.from(this).inflate(R.layout.alert_present, null)

        val builder = AlertDialog.Builder(this)
            .setView(viewLayout)
            .setCancelable(false)
            .create()

        builder.window?.setBackgroundDrawableResource(android.R.color.transparent)
        builder.show()

        //image preview
        val imagePreview = viewLayout.findViewById<ImageView>(R.id.imagePreview)
        Glide.with(this)
            .load(face.normalizedFace)
            .circleCrop()
            .into(imagePreview)
        Log.d("check", face.normalizedFace.toString())

        //image dataset
        val imageDataset = viewLayout.findViewById<ImageView>(R.id.showImageDataSet)
        student.imagePath?.let {
            Glide.with(this)
                .load(File(it))
                .circleCrop()
                .into(imageDataset)

            Log.d("check", it)
        }


        // Set student name
        val nameTextView = viewLayout.findViewById<TextView>(R.id.matchedName)
        nameTextView.text = "Name: ${student.name}"

        // Set student ID
        val idTextView = viewLayout.findViewById<TextView>(R.id.faceId)
        idTextView.text = "ID: ${student.id}"

        // Set match percentage
        val percentTextView = viewLayout.findViewById<TextView>(R.id.percent)
        percentTextView.text = matching.toInt().toString()+"%"

        // Retry button
        val checkAgainButton = viewLayout.findViewById<Button>(R.id.checkAgain)
        checkAgainButton.setOnClickListener {
            builder.dismiss()
            restartFaceDetection()
        }
    }


    private fun restartFaceDetection() {
        faceDetectedStartTime = 0L
        hasCapturedImage = false
        capturedBitmap = null

        resumeCamera()
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            try {
                // Unbind all use cases
                cameraProvider.unbindAll()
                Log.d("CameraX", "Camera stopped and all use cases unbound.")
            } catch (e: Exception) {
                Log.e("CameraX", "Failed to unbind camera use cases.", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun resumeCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Image analysis use case with backpressure strategy
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(imageProxy)
            }

            // Image capture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to lifecycle
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis, imageCapture
                )

                Log.d("CameraX", "Camera resumed and use cases bound.")
            } catch (e: Exception) {
                Log.e("CameraX", "Failed to bind camera use cases.", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}