package com.example.localbase.overlayes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var faces: List<Face> = emptyList()
    private var imageWidth = 0
    private var imageHeight = 0
    private var rotationDegrees = 0
    private var isFrontCamera = true // <--- Add this flag to track mirroring

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    fun setFaces(
        faces: List<Face>,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean = true // <--- Pass this from your activity
    ) {
        this.faces = faces
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.rotationDegrees = rotationDegrees
        this.isFrontCamera = isFrontCamera
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (face in faces) {
            val rect = translateRect(face.boundingBox)
            canvas.drawRect(rect, paint)
        }
    }

    private fun translateRect(faceRect: Rect): RectF {
        val scaleX: Float
        val scaleY: Float
        val previewWidth: Int
        val previewHeight: Int

        // Adjust image dimensions for rotation
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            previewWidth = imageWidth
            previewHeight = imageHeight
        } else {
            previewWidth = imageHeight
            previewHeight = imageWidth
        }

        scaleX = width.toFloat() / previewWidth
        scaleY = height.toFloat() / previewHeight

        var left = faceRect.left.toFloat()
        var top = faceRect.top.toFloat()
        var right = faceRect.right.toFloat()
        var bottom = faceRect.bottom.toFloat()

        // Mirror horizontally if front camera
        if (isFrontCamera) {
            val mirroredLeft = previewWidth - right
            val mirroredRight = previewWidth - left
            left = mirroredLeft
            right = mirroredRight
        }

        return RectF(
            left * scaleX,
            top * scaleY,
            right * scaleX,
            bottom * scaleY
        )
    }
}