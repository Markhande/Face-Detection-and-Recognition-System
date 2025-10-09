package com.example.localbase.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class RotateTransformation(private val rotateRotationAngle: Float) : BitmapTransformation() {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(("rotate$rotateRotationAngle").toByteArray(Charsets.UTF_8))
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotateRotationAngle)

        // Create rotated bitmap
        return Bitmap.createBitmap(
            toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true
        )
    }
}
