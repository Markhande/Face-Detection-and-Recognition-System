package com.example.localbase.helper

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.localbase.R
import com.example.localbase.overlayes.FaceOverlayView
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.qualifiers.ActivityContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.exifinterface.media.ExifInterface


class ToolBar
@Inject
constructor(@ActivityContext private val context: Context) {

    fun showToast(name: String = "Hello"){
        Toast.makeText(context, name, Toast.LENGTH_SHORT).show()
    }

    private fun showAttendanceDialog(name: String) {
        // Create an AlertDialog Builder
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Mark Student Attendance")

        // Inflate custom layout
        val layout = LayoutInflater.from(context).inflate(R.layout.alert_present, null)

        builder.setView(layout) // Set the custom layout view

        // Set positive button for closing the dialog
        builder.setPositiveButton("Close") { dialog, _ ->
            dialog.dismiss()
        }

        // Create the dialog
        val dialog = builder.create()


        // Show the dialog
        dialog.show()
    }

    // Convert ImageProxy to Bitmap
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun saveBitmapToFile(bitmap: Bitmap, filename: String) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }


    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = Date()
        return sdf.format(currentDate)
    }


    fun rotateBitmap(original: Bitmap, degrees: Float): Bitmap {
        // Create a Matrix object to handle the rotation
        val matrix = Matrix()
        // Rotate the matrix by the given angle
        matrix.postRotate(degrees)

        // Create a new bitmap by applying the rotation to the original bitmap
        return Bitmap.createBitmap(
            original, // Original bitmap
            0, 0, original.width, original.height, // Define the area to be rotated
            matrix, // Apply the matrix transformation
            true // Whether to filter the pixels (e.g., for anti-aliasing)
        )
    }


    fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, imageName: String): String? {
        val directory = context.filesDir // This is internal storage
        val file = File(directory, imageName)

        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Return the image path
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun saveImageToExternalStorage(context: Context, bitmap: Bitmap, imageName: String): String? {
        val directory = File(context.getExternalFilesDir(null), "images") // external directory
        if (!directory.exists()) {
            directory.mkdirs() // Create the directory if it doesn't exist
        }
        val file = File(directory, imageName)

        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Return the image path
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getBitmapFromImageView(imageView: ImageView): Bitmap? {
        val drawable = imageView.drawable
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            null
        }
    }

    fun loadImageFromPath(imagePath: String, imageView: ImageView) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        imageView.setImageBitmap(bitmap)
    }

    fun saveImageToStorage(bitmap: Bitmap, imageName: String): String? {
        // Choose the directory to save the image (internal storage)
        val directory = context.filesDir // Internal storage
        val file = File(directory, imageName)

        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Return the absolute file path
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun cropToPercentage(value: Float): String {
        val percentage = value * 100
        val cropped = percentage.toInt() // Truncate decimal
        return "$cropped"
    }

    fun focusEditText(editText: EditText) {
        editText.requestFocus()
        editText.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun showSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(view, message, duration).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTime(): String {
        val currentTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return currentTime.format(formatter)
    }

    fun displayImage(model: Any, imageView: ImageView){
        Glide.with(context)
            .load(model)
            .circleCrop()
            .into(imageView)
    }

    fun vibrateDevice(millSec: Long = 500) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0 and above
            val vibrationEffect = VibrationEffect.createOneShot(millSec, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        } else {
            // For older Android versions
            vibrator.vibrate(millSec)
        }
    }

    fun logd(check:String= "check", value: String){
        Log.d(check, value)
    }

    fun getExifRotation(path: String): Int {
        val exif = ExifInterface(path)
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun openAppInfoSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun showCameraPermissionDeniedDialog() {
        AlertDialog.Builder(context).apply {
            setTitle("Permission Required")
            setMessage("You have denied the camera permission. Please go to settings and enable it to use this feature.")
            setCancelable(false)

            setPositiveButton("Go to Settings") { dialog, _ ->
                openAppInfoSettings()
                dialog.dismiss()
            }

            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            create().show()
        }
    }

    fun custAni():  ActivityOptions{
        val options = ActivityOptions.makeCustomAnimation(
            context,
            R.anim.fade_in,
            R.anim.fade_out
        )
        return options
    }

}