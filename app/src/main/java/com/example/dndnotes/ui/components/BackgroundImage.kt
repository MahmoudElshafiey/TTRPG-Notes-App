package com.example.dndnotes.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import java.io.File

@Composable
fun BackgroundImage(
    uriString: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (uriString != null) {
        var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
        val context = LocalContext.current

        LaunchedEffect(uriString) {
            bitmap = withContext(Dispatchers.IO) {
                try {
                    // Resolve to bytes ONCE (re-readable), so bounds + decode
                    // don't consume a single stream twice.
                    val bytes = when {
                        uriString.startsWith("content://") || uriString.startsWith("file://") -> {
                            val uri = Uri.parse(uriString)
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }
                        else -> {
                            File(uriString).readBytes()
                        }
                    }

                    if (bytes != null) {
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                            val metrics = context.resources.displayMetrics
                            val target = maxOf(metrics.widthPixels, metrics.heightPixels)
                            inSampleSize = (maxOf(outWidth, outHeight).toFloat() / target)
                                .coerceAtLeast(1f).toInt().coerceAtLeast(1)
                            inJustDecodeBounds = false
                        }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = modifier,
                contentScale = contentScale,
                alignment = Alignment.Center
            )
        }
    }
}
