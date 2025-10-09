package com.example.localbase.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import com.example.localbase.R
import com.example.localbase.databinding.ActivityCheckFaceBinding
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.localbase.database.Student
import com.example.localbase.database.StudentViewModel
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.tensorflow.lite.Interpreter
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.localbase.databinding.AlertImageNotFoundBinding
import com.example.localbase.face.FaceDetectionProcessor
import com.example.localbase.face.FaceEmbeddingHelper
import com.example.localbase.helper.RotateTransformation
import com.example.localbase.helper.ToolBar
import com.example.localbase.overlayes.FaceOverlayView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.text.toInt

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
    private val facePresenceThreshold = 0L

    private var capturedBitmap: Bitmap? = null

    private var faceEmbeddingHelper = FaceEmbeddingHelper

    @Inject
    lateinit var faceProcessor: FaceDetectionProcessor

    private lateinit var temp: List<Student>

    private var job : Job? = null

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
                    val imagRotaed = toolBar.rotateBitmap(capturedBitmap!!, -90f)
                    processImage(imagRotaed)
                    //processImage(capturedBitmap!!)

                    imageProxy.close()
                }

                override fun onError(exception: ImageCaptureException) {
//                    Toast.makeText(
//                        applicationContext,
//                        "Capture failed: ${exception.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
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
            val confidance =  toolBar.cropToPercentage(face.confidence).toInt()
            Log.d("FaceDetection", toolBar.cropToPercentage(face.confidence))

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

                val faceitemSize = it.size
                var facevaleu = faceitemSize
                Log.d("imageSize", faceitemSize.toString())

                for (i in it) {
                    val secondFace = i.grades
                    val matching =
                        faceEmbeddingHelper.cosineSimilarityPercentage(embedding, secondFace)
                    Log.d("simularities", "${i.name} → ${matching.toInt()}%")

                    if (matching > 75 && confidance > 80 ) {
                        stopCamera()
                        showAlert(i, face, matching)
                        break
                    } else {
                        if (facevaleu == 1) {
                            showFaceNotFoundDialog(i, face)
                        }
                    }

                    facevaleu--
                }
            }
        }
    }

    @SuppressLint("MissingInflatedId", "CheckResult")
    private fun showAlert(
        student: Student,
        face: FaceDetectionProcessor.ProcessedFace,
        accuracy: Float
    ) {
        val viewLayout = LayoutInflater.from(this).inflate(R.layout.alert_present, null)

        val builder = AlertDialog.Builder(this)
            .setView(viewLayout)
            .setCancelable(true)
            .create()

        builder.window?.setBackgroundDrawableResource(android.R.color.transparent)
        builder.show()

        //image preview
        val imagePreview = viewLayout.findViewById<ImageView>(R.id.imagePreview)
        Glide.with(this)
            .load(face.normalizedFace)
           // .transform(RotateTransformation(180f))
            .into(imagePreview)
        Log.d("check", face.normalizedFace.toString())

        //image dataset
        val imageDataset = viewLayout.findViewById<ImageView>(R.id.showImageDataSet)
        student.imagePath?.let {
            Glide.with(this)
                .load(File(it))
                .transform(RotateTransformation(0f))
                .into(imageDataset)

            Log.d("check", it)
        }

        builder.setOnCancelListener {
            builder.dismiss()
            restartFaceDetection()
        }


        // Set student name
        val nameTextView = viewLayout.findViewById<TextView>(R.id.matchedName)
        nameTextView.text = "Name: ${student.name}"

        // Set student ID
        val idTextView = viewLayout.findViewById<TextView>(R.id.faceId)
        idTextView.text = "ID: ${student.id}"

        // Set match percentage
        val percentTextView = viewLayout.findViewById<TextView>(R.id.percent)
        percentTextView.text = accuracy.toInt().toString() + "%"

        // Retry button
        val checkAgainButton = viewLayout.findViewById<Button>(R.id.checkAgain)
        checkAgainButton.setOnClickListener {
            builder.dismiss()
            restartFaceDetection()
        }
    }

    fun showFaceNotFoundDialog(i: Student, face: FaceDetectionProcessor.ProcessedFace) {
        stopCamera()
        toolBar.vibrateDevice(70)
        val binding = AlertImageNotFoundBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        binding.detectionAccuracy.text = "Detection: "+ toolBar.cropToPercentage(face.confidence)+"%"

        binding.tryAgain.setOnClickListener {
            dialog.dismiss()
            restartFaceDetection()
            job?.cancel()
        }
        dialog.setOnCancelListener {
            restartFaceDetection()
        }

//        job = lifecycleScope.launch {
//            delay(5000)
//            dialog.dismiss()
//            restartFaceDetection()
//        }

        dialog.setOnCancelListener {
            job?.cancel()
        }

        dialog.show()
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