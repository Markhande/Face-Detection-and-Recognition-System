package com.example.localbase.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.localbase.database.Student
import com.example.localbase.database.StudentViewModel
import com.example.localbase.databinding.ActivityMainBinding
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.tensorflow.lite.Interpreter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.localbase.face.FaceDetectionProcessor
import com.example.localbase.face.FaceEmbeddingHelper
import com.example.localbase.helper.ToolBar
import com.example.localbase.overlayes.FaceOverlayView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.text.toInt
import com.example.localbase.R
import com.example.localbase.helper.RotateTransformation

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
    private val facePresenceThreshold = 10L

    private var capturedBitmap: Bitmap? = null

    private var faceEmbeddingHelper = FaceEmbeddingHelper

    @Inject
    lateinit var faceProcessor: FaceDetectionProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //faceProcessor = FaceDetectionProcessor(this)

        // Inflate view binding and set content view
        binding = ActivityMainBinding.inflate(layoutInflater)
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
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = toolBar.imageProxyToBitmap(imageProxy) ?: return
                    capturedBitmap = bitmap
                    val imagRotaed = toolBar.rotateBitmap(capturedBitmap!!, -90f)
                    processImage(imagRotaed)

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

    @RequiresApi(Build.VERSION_CODES.O)
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

            saveFaceValue(
                face = face.normalizedFace,
                faceDetails = embedding,
                face.confidence
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    private fun saveFaceValue(face: Bitmap, faceDetails: FloatArray, accuracy: Float) {
        stopCamera()
        val facevalue = toolBar.rotateBitmap(face, -90f)

        val layout = LayoutInflater.from(this).inflate(R.layout.alert_save_face_details, null)

        val alertSaveFace = AlertDialog
            .Builder(this)
            .setView(layout)
            .setCancelable(true)
            .create()

        alertSaveFace.window?.setBackgroundDrawableResource(android.R.color.transparent)

        alertSaveFace.show()

        val getName = layout.findViewById<EditText>(R.id.DetectedfaceName)
        val Image = layout.findViewById<ImageView>(R.id.showImage)
        val cancel = layout.findViewById<TextView>(R.id.cancel_button)
        val saveFace = layout.findViewById<Button>(R.id.saveFaceDetails)
        val percent = layout.findViewById<TextView>(R.id.percent)

        //val imageSave = toolBar.saveImageToStorage(toolBar.getBitmapFromImageView(Image)!!, "faceSample")


        Glide.with(this)
            .load(facevalue)
            .transform(RotateTransformation(90f))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(Image)

        cancel.setOnClickListener {
            restartFaceDetection()
            alertSaveFace.dismiss()
        }

        percent.text = toolBar.cropToPercentage(accuracy) + "%"

        if (toolBar.cropToPercentage(accuracy).toInt() >= 80) {
            percent.setBackgroundResource(R.drawable.background_green)
            percent.text = "Good " + toolBar.cropToPercentage(accuracy) + "%"
            toolBar.focusEditText(getName)
            cancel.text = "Cancel"
        } else {
            percent.setBackgroundResource(R.drawable.background_red)
            percent.text = "Bad Capture " + toolBar.cropToPercentage(accuracy) + "%"
            toolBar.focusEditText(getName)
            cancel.text = "Re-Take"
            saveFace.isEnabled = false
            saveFace.text = "Disable"
        }

        alertSaveFace.setOnCancelListener {
            restartFaceDetection()
        }

        saveFace.setOnClickListener {

            //val imageSave = toolBar.saveImageToStorage(toolBar.getBitmapFromImageView(Image)!!, getName.text.toString().trim().ifBlank { toolBar.getCurrentDate()+toolBar.getCurrentTime() })
            val imageSave = toolBar.saveImageToStorage(
                toolBar.getBitmapFromImageView(Image)!!,
                getName.text.toString().trim()
            )
            val faceData = Student(
                date = toolBar.getCurrentDate(),
                time = toolBar.getCurrentTime(),
                name = getName.text.toString().trim().ifBlank { "" },
                grades = faceDetails,
                imagePath = imageSave
            )

            studentViewModel.insert(faceData)

            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
            finishAffinity()
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