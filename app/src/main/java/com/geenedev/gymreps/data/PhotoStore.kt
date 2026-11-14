package com.geenedev.gymreps.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.geenedev.gymreps.model.Workout
import java.io.File

/** One photo per exercise name, kept in the app's private files folder. */
class PhotoStore(private val context: Context) {
    /** Bumped on every change, so every place showing a photo redraws. */
    var revision by mutableIntStateOf(0)
        private set

    private val dir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    private val captureFile: File
        get() = File(dir, "capture.tmp.jpg")

    fun existing(exercise: String): File? {
        if (exercise.isBlank()) return null
        return fileFor(exercise).takeIf { it.exists() && it.length() > 0 }
    }

    /** Where the camera app writes the new picture; [commitCapture] moves it into place. */
    fun captureUri(): Uri {
        captureFile.delete()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", captureFile)
    }

    fun commitCapture(exercise: String): Boolean {
        val capture = captureFile
        if (exercise.isBlank() || !capture.exists() || capture.length() == 0L) return false
        val target = fileFor(exercise)
        target.delete()
        return capture.renameTo(target).also { if (it) revision++ }
    }

    /** Copies a picture chosen from the gallery. */
    fun importFrom(uri: Uri, exercise: String): Boolean {
        if (exercise.isBlank()) return false
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                fileFor(exercise).outputStream().use { input.copyTo(it) }
            }.also { if (it != null) revision++ } != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * After a history entry is renamed, gives the new name the old name's photo (unless it
     * already has one). The old file stays: other entries may still use that name.
     */
    fun carryOver(from: String, to: String) {
        if (to.isBlank() || Workout.photoKey(from) == Workout.photoKey(to)) return
        val source = existing(from) ?: return
        if (existing(to) != null) return
        try {
            source.copyTo(fileFor(to), overwrite = true)
            revision++
        } catch (e: Exception) {
            // Leave things as they were; the entry just shows no photo.
        }
    }

    fun delete(exercise: String) {
        fileFor(exercise).delete()
        revision++
    }

    // History Edit card: a picture chosen there waits in the staged file until Save.
    private val editCaptureFile: File
        get() = File(dir, "edit-capture.tmp.jpg")

    private val stagedFile: File
        get() = File(dir, "edit-staged.tmp.jpg")

    /** Where the camera app writes a picture taken from the Edit card; see [stageCapture]. */
    fun editCaptureUri(): Uri {
        editCaptureFile.delete()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", editCaptureFile)
    }

    /** Moves a finished Edit-card capture to the staged file and returns it. */
    fun stageCapture(): File? {
        val capture = editCaptureFile
        if (!capture.exists() || capture.length() == 0L) return null
        val staged = stagedFile
        staged.delete()
        return staged.takeIf { capture.renameTo(it) }
    }

    /** Copies a gallery picture chosen in the Edit card to the staged file and returns it. */
    fun stageImport(uri: Uri): File? = try {
        val staged = stagedFile
        context.contentResolver.openInputStream(uri)?.use { input ->
            staged.outputStream().use { input.copyTo(it) }
        }?.let { staged.takeIf { it.length() > 0 } }
    } catch (e: Exception) {
        null
    }

    /** Applies the Edit card's choice on Save: [staged] becomes [exercise]'s photo, or [removed] deletes it. */
    fun applyEdit(exercise: String, staged: File?, removed: Boolean) {
        if (exercise.isBlank()) return
        when {
            staged != null && staged.exists() -> {
                val target = fileFor(exercise)
                target.delete()
                if (!staged.renameTo(target)) staged.copyTo(target, overwrite = true)
                revision++
            }
            removed -> delete(exercise)
        }
    }

    /** Drops anything the Edit card staged but did not save. */
    fun discardStaged() {
        stagedFile.delete()
        editCaptureFile.delete()
    }

    private fun fileFor(exercise: String) = File(dir, Workout.photoKey(exercise) + ".jpg")
}
