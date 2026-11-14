package com.geenedev.gymreps.ui

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.geenedev.gymreps.data.PhotoStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Camera button next to the exercise field. With no photo it offers camera or gallery;
 * with one it shows the photo as a thumbnail and opens it full size on tap.
 */
@Composable
fun ExercisePhotoButton(exercise: String, store: PhotoStore) {
    val context = LocalContext.current
    val name = exercise.trim()
    var version by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var viewerOpen by remember { mutableStateOf(false) }
    // The exercise a pending camera/gallery result belongs to, kept if the activity is recreated.
    var pendingFor by rememberSaveable { mutableStateOf("") }

    val file = remember(name, version) { store.existing(name) }
    val thumb by produceState<ImageBitmap?>(null, file, version) {
        value = file?.let { withContext(Dispatchers.IO) { decodeUpright(it, maxSide = 192) } }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved && store.commitCapture(pendingFor)) version++
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && store.importFrom(uri, pendingFor)) version++
    }

    fun takePhoto() {
        pendingFor = name
        try {
            takePicture.launch(store.captureUri())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun choosePhoto() {
        pendingFor = name
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Box {
        FilledTonalIconButton(
            onClick = {
                when {
                    name.isEmpty() ->
                        Toast.makeText(context, "Type an exercise name first", Toast.LENGTH_SHORT).show()
                    file != null -> viewerOpen = true
                    else -> menuOpen = true
                }
            },
            modifier = Modifier.size(56.dp),
        ) {
            val image = thumb
            if (file != null && image != null) {
                Image(
                    image,
                    contentDescription = "Machine photo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Add machine photo")
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Take photo") },
                leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    takePhoto()
                },
            )
            DropdownMenuItem(
                text = { Text("Choose from gallery") },
                leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    choosePhoto()
                },
            )
        }
    }

    if (viewerOpen && file != null) {
        val full by produceState<ImageBitmap?>(null, file, version) {
            value = withContext(Dispatchers.IO) { decodeUpright(file, maxSide = 1600) }
        }
        AlertDialog(
            onDismissRequest = { viewerOpen = false },
            title = { Text(name) },
            text = {
                full?.let {
                    Image(
                        it,
                        contentDescription = "Machine photo for $name",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewerOpen = false }) { Text("Close") } },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        store.delete(name)
                        version++
                        viewerOpen = false
                    }) { Text("Remove") }
                    TextButton(onClick = {
                        viewerOpen = false
                        takePhoto()
                    }) { Text("Retake") }
                }
            },
        )
    }
}

/** The exercise's machine photo for a history card; draws nothing when it has none. */
@Composable
fun ExercisePhotoThumb(exercise: String, store: PhotoStore) {
    val revision = store.revision
    val file = remember(exercise, revision) { store.existing(exercise) }
    val image by produceState<ImageBitmap?>(null, file, revision) {
        value = file?.let { withContext(Dispatchers.IO) { decodeUpright(it, maxSide = 144) } }
    }
    val loaded = image
    if (file != null && loaded != null) {
        Image(
            loaded,
            contentDescription = "Machine photo for $exercise",
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

/** The photo choice made in the history Edit card; nothing is written until Save. */
class PhotoEdit {
    /** New picture waiting to replace the photo, or null to keep the current one. */
    var staged by mutableStateOf<File?>(null)
    var removed by mutableStateOf(false)
    /** Bumped on each new staged picture (it always lands at the same path). */
    var version by mutableIntStateOf(0)
}

/**
 * Photo row of the history Edit card: the entry's current photo (or the newly chosen one)
 * with Change / Remove, or Add when there is none. The choice is applied by [PhotoStore.applyEdit].
 */
@Composable
fun EditPhotoRow(exercise: String, edit: PhotoEdit, store: PhotoStore) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val revision = store.revision
    val file = remember(exercise, revision, edit.staged, edit.removed, edit.version) {
        edit.staged ?: if (edit.removed) null else store.existing(exercise)
    }
    val thumb by produceState<ImageBitmap?>(null, file, edit.version, revision) {
        value = file?.let { withContext(Dispatchers.IO) { decodeUpright(it, maxSide = 192) } }
    }
    DisposableEffect(Unit) { onDispose { store.discardStaged() } }

    fun stage(staged: File?) {
        if (staged == null) return
        edit.staged = staged
        edit.removed = false
        edit.version++
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) stage(store.stageCapture())
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) stage(store.stageImport(uri))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        val image = thumb
        if (file != null && image != null) {
            Image(
                image,
                contentDescription = "Entry photo",
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(8.dp))
        }
        Column {
            Box {
                TextButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (file != null) "Change photo" else "Add photo")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Take photo") },
                        leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            try {
                                takePicture.launch(store.editCaptureUri())
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Choose from gallery") },
                        leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                }
            }
            if (file != null) {
                TextButton(onClick = {
                    edit.staged = null
                    edit.removed = true
                }) { Text("Remove photo") }
            }
        }
    }
}

/** Decodes [file] scaled down to about [maxSide] pixels and turned upright using its EXIF tag. */
private fun decodeUpright(file: File, maxSide: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null
    val degrees = try {
        when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (e: Exception) {
        0f
    }
    val upright = if (degrees == 0f) bitmap else Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true,
    )
    return upright.asImageBitmap()
}
