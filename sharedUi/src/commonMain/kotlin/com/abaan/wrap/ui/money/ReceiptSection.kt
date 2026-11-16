package com.abaan.wrap.ui.money

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.ui.platform.ReceiptPhotos
import com.abaan.wrap.ui.platform.rememberPickPhoto
import com.abaan.wrap.ui.platform.rememberTakePhoto
import org.koin.compose.koinInject

/** Receipt photo for a spend: take one, pick one, view it full size or remove it. */
@Composable
fun ReceiptSection(
    receipt: String,
    importing: Boolean,
    error: String?,
    cameraTarget: () -> String,
    onPicked: (uri: String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var viewing by rememberSaveable { mutableStateOf(false) }
    val takePhoto = rememberTakePhoto { taken ->
        pendingCapture?.takeIf { taken }?.let(onPicked)
        pendingCapture = null
    }
    val pickPhoto = rememberPickPhoto(onPicked)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            importing -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            receipt.isNotBlank() -> {
                val image = rememberReceipt(receipt, maxSide = 600)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { viewing = true },
                ) {
                    image?.let {
                        Image(
                            bitmap = it,
                            contentDescription = stringResource(Res.string.receipt_view),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Row {
                    TextButton(onClick = { viewing = true }) { Text(stringResource(Res.string.receipt_view)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onRemove) {
                        Text(stringResource(Res.string.receipt_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val target = cameraTarget()
                        pendingCapture = target
                        takePhoto(target)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(painterResource(Res.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.receipt_take))
                }
                OutlinedButton(
                    onClick = pickPhoto,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(painterResource(Res.drawable.ic_image), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.receipt_pick))
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }

    if (viewing && receipt.isNotBlank()) {
        ReceiptViewer(receipt, onDismiss = { viewing = false })
    }
}

@Composable
private fun ReceiptViewer(receipt: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            rememberReceipt(receipt, maxSide = 2000)?.let {
                Image(
                    bitmap = it,
                    contentDescription = stringResource(Res.string.receipt_view),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close), tint = Color.White)
            }
        }
    }
}

@Composable
private fun rememberReceipt(name: String, maxSide: Int): ImageBitmap? {
    val store = koinInject<ReceiptPhotos>()
    val image by produceState<ImageBitmap?>(null, name, maxSide) {
        value = store.load(name, maxSide)
    }
    return image
}
