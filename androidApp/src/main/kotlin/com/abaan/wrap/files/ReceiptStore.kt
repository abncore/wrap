package com.abaan.wrap.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * Receipt photos, stored as JPEGs in the app's private files. The database keeps only the file name.
 * Photos are shrunk to [MAX_SIDE] pixels so a year of receipts stays small.
 */
class ReceiptStore(private val context: Context) {
    val directory: File get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    fun file(name: String): File = File(directory, name)

    /** A content Uri the camera app can write a new photo to; pass it to [import] afterwards. */
    fun newCameraUri(): Uri {
        val dir = File(context.cacheDir, CAMERA_DIRECTORY).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        return uriFor(File(dir, "capture.jpg"))
    }

    /** Copies the photo at [uri] into the receipts folder. Returns the new file name, or null if it can't be read. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight), MAX_SIDE)
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@runCatching null
            val rotation = context.contentResolver.openInputStream(uri)?.use { rotationOf(ExifInterface(it)) } ?: 0
            val bitmap = scaleAndRotate(decoded, rotation)
            val name = "${UUID.randomUUID()}.jpg"
            file(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            name
        }.getOrNull()
    }

    /** A downscaled copy for showing on screen. */
    suspend fun load(name: String, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
        val target = file(name)
        if (name.isBlank() || !target.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.path, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight), maxSide)
        }
        BitmapFactory.decodeFile(target.path, options)
    }

    fun delete(name: String) {
        if (name.isNotBlank()) file(name).delete()
    }

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    private fun sampleSize(longestSide: Int, target: Int): Int {
        var size = 1
        while (longestSide / (size * 2) >= target) size *= 2
        return size
    }

    private fun rotationOf(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun scaleAndRotate(bitmap: Bitmap, rotation: Int): Bitmap {
        val scale = minOf(1f, MAX_SIDE.toFloat() / max(bitmap.width, bitmap.height))
        if (scale == 1f && rotation == 0) return bitmap
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    companion object {
        const val DIRECTORY = "receipts"
        private const val CAMERA_DIRECTORY = "camera"
        private const val MAX_SIDE = 1600
        private const val JPEG_QUALITY = 85
    }
}
