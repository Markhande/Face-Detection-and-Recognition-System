package com.example.localbase.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.example.localbase.R
import com.example.localbase.adapter.FaceAdapter
import com.example.localbase.clickEvent.faceDetails
import com.example.localbase.database.Student
import com.example.localbase.database.StudentViewModel
import com.example.localbase.databinding.ActivityDashboardBinding
import com.example.localbase.helper.RotateTransformation
import com.example.localbase.helper.ToolBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import java.io.File
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class Dashboard : AppCompatActivity(), faceDetails {
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val studentViewModel: StudentViewModel by viewModels()
    private lateinit var adapterType: FaceAdapter
    private lateinit var binding: ActivityDashboardBinding
    @Inject
    lateinit var toolBar: ToolBar
    private var validation = 0

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
        if(it != null){

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If not, request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, proceed with camera access
            // Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        }

        onclick()
        recycleView()
        openGallery()
    }

    private fun openGallery(){
        binding.galleryCard.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    fun test(it: List<Student>) {
        lifecycleScope.launch {
            validation = it.size
        }

    }

    private fun onclick() = with(binding) {
        studentViewModel.allStudents.observe(this@Dashboard) { user ->

            checkFaceButton.setOnClickListener {
                if (toolBar.isCameraPermissionGranted()) {
                    if (user.isNotEmpty()) {
                        startActivity(Intent(this@Dashboard, CheckFace::class.java))
                    } else {
                        toolBar.showSnackbar(binding.root, "Add face")
                    }
                } else {
                    toolBar.showCameraPermissionDeniedDialog()
                }
            }
            cameraCard.setOnClickListener {
                if (toolBar.isCameraPermissionGranted()) {
                    startActivity(Intent(this@Dashboard, MainActivity::class.java))
                }else {
                    toolBar.showCameraPermissionDeniedDialog()
                }
            }
            deleteRecord.setOnClickListener {
                if (user.isNotEmpty()) {
                    deleteAllRecord()
                }else{
                    toolBar.showSnackbar(binding.root, "Data already cleared")
                }
            }
        }
    }

    private fun recycleView() {
        studentViewModel.allStudents.observe(this) {
            adapterType = FaceAdapter(this, it.reversed(), toolBar)

            val layoutManager = GridLayoutManager(this, 2)
            binding.faceDetails.layoutManager = LinearLayoutManager(this)

            binding.faceDetails.adapter = adapterType

            Log.d("testing", it.map { it.imagePath }.toString())

            binding.userCountBadge.setText(it.size.toString())
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check the request code and the result
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with camera access
                //Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun delete(student: Student) {}

    @SuppressLint("MissingInflatedId")
    override fun edit(student: Student) {

        val viewLayout = LayoutInflater.from(this).inflate(R.layout.item_face_details, null)


        val alertEdit = AlertDialog
            .Builder(this)
            .setView(viewLayout)
            .setCancelable(true)
            .create()

        alertEdit.window?.setBackgroundDrawableResource(android.R.color.transparent)

        alertEdit.show()
        val newName = viewLayout.findViewById<EditText>(R.id.updateName)
        val cancelBtn = viewLayout.findViewById<TextView>(R.id.updatecancelBtn)
        val newImage = viewLayout.findViewById<ImageView>(R.id.updateImage)
        val delete = viewLayout.findViewById<TextView>(R.id.updatedeleteButton)
        val button = viewLayout.findViewById<Button>(R.id.updateFaceDetails)

        newName.setText(student.name)
        toolBar.focusEditText(newName)

        Glide.with(this)
            .load(File(student.imagePath))
            .circleCrop()
            .transform(RotateTransformation(0f))
            .into(newImage)

        delete.setOnClickListener {
            deleteFaceData(student, alertEdit)
        }

        cancelBtn.setOnClickListener {
            alertEdit.dismiss()
        }

        button.setOnClickListener {

            studentViewModel.update(
                Student(
                    id = student.id,
                    name = newName.text.toString().trim().ifBlank { "" },
                    grades = student.grades,
                    imagePath = student.imagePath,
                    date = student.date,
                    time = student.time
                )
            )

            toolBar.showSnackbar(binding.root, "Updated")
            alertEdit.dismiss()
            recycleView()
        }

    }

    private fun deleteFaceData(student: Student, alertEdit: AlertDialog) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Delete Data")
            .setMessage("Do you want delete?")
            .setPositiveButton("OK") { dialog, _ ->
                studentViewModel.deleteById(student.id)
                recycleView()
                dialog.dismiss()
                alertEdit.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Handle the Cancel button click
                dialog.dismiss()
            }
        // Show the alert dialog
        builder.create().show()
    }

    private fun deleteAllRecord() {
        AlertDialog.Builder(this)
            .setTitle("Deletion!")
            .setMessage("Do you want to delete all records?")
            .setCancelable(true)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Yes") { dialog, _ ->
                studentViewModel.deleteAll()
                dialog.dismiss()
            }
            .show()
    }

}