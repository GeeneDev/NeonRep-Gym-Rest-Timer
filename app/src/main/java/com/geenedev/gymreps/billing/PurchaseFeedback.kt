package com.geenedev.gymreps.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import com.geenedev.gymreps.R

/** Opens Play's purchase sheet, or tells the user in a toast why it can't right now. */
fun Premium.buyWithFeedback(activity: Activity) {
    val message = when (buy(activity)) {
        Premium.BuyStart.STARTED -> return
        Premium.BuyStart.PRODUCT_UNAVAILABLE -> R.string.product_unavailable
        Premium.BuyStart.STORE_UNREACHABLE -> R.string.play_unreachable
    }
    Toast.makeText(activity, activity.getString(message), Toast.LENGTH_LONG).show()
}

/** Restores a previous purchase and says how it went in a toast. */
fun Premium.restoreWithFeedback(context: Context) {
    restore { result ->
        val message = when (result) {
            Premium.RestoreResult.RESTORED -> R.string.restore_done
            Premium.RestoreResult.NOTHING_FOUND -> R.string.restore_none
            Premium.RestoreResult.STORE_UNREACHABLE -> R.string.play_unreachable
        }
        Toast.makeText(context, context.getString(message), Toast.LENGTH_LONG).show()
    }
}

/** The Activity behind a Compose context (which may be wrapped, e.g. for the app language). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
