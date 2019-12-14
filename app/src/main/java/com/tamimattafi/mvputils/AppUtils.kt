package com.tamimattafi.mvputils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar


object AppUtils {

    fun Context.convertDpToPixel(dp: Float): Float {
        return dp * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun getDrawable(context: Context, drawableId: Int): Drawable? {
        return ResourcesCompat.getDrawable(context.resources, drawableId, null)
    }

    @ColorInt
    fun getColor(context: Context, colorId: Int): Int {
        return ResourcesCompat.getColor(context.resources, colorId, null)
    }

    fun Activity.createTextBitmap(text: Any, drawableId: Int): Bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).also { bitmap ->
        Canvas(bitmap).apply {
            Paint().apply {
                textSize = 35F
                color = Color.BLACK
            }.also { paint ->
                drawColor(Color.TRANSPARENT)
                drawBitmap(BitmapFactory.decodeResource(resources, drawableId), 0F, 0F, paint)
                drawText(text.toString(), width / 2f, height / 2F - (paint.descent() + paint.ascent()) / 2F, paint)
            }
        }
    }

    fun Context.showToast(text: String, length: Int = Toast.LENGTH_LONG) {
        Toast.makeText(
                this,
                text,
                length
        ).show()
    }

    fun View.showSnackBar(text: String) {
        Snackbar.make(this, text, Snackbar.LENGTH_LONG)
                .show()
    }


    fun View.showActionSnackBar(text: String, actionText: String, action: (view: View) -> Unit) {
        Snackbar.make(this, text, Snackbar.LENGTH_LONG)
                .setAction(actionText, action)
                .show()
    }

    fun View.getBitmap(): Bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888).also {
        Canvas(it).apply {
            background?.draw(this) ?: drawColor(Color.WHITE)
            draw(this)
        }
    }


    fun Context.isPermissionGranted(permission: String): Boolean =
            ContextCompat.checkSelfPermission(
                    this,
                    permission
            ) == PackageManager.PERMISSION_GRANTED

    fun enableViews(enable: Boolean, vararg views: View) {
        for (view in views) {
            view.isEnabled = enable
        }
    }

    fun hideViewsGone(hide: Boolean, vararg views: View) {
        changeViewsVisibility(if (hide) View.GONE else View.VISIBLE, *views)
    }

    fun hideViewsInvisible(hide: Boolean, vararg views: View) {
        changeViewsVisibility(if (hide) View.INVISIBLE else View.VISIBLE, *views)
    }

    fun changeViewsVisibility(visibility: Int, vararg views: View) {
        views.forEach {
            it.visibility = visibility
        }
    }

    @RequiresApi(18)
    fun Activity.lockOrientation() {
        requestedOrientation = if (isPortrait()) ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        else ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
    }

    fun Activity.unlockOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    fun Activity.isPortrait() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

}