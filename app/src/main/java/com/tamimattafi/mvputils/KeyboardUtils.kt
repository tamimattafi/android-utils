package com.tamimattafi.mvputils

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager

object KeyboardUtils {

    fun Context.isKeyboardVisible(action: (isVisible: Boolean, manager: InputMethodManager?) -> Unit) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.apply {
            action.invoke(InputMethodManager::class.java.getMethod("getInputMethodWindowVisibleHeight").invoke(this) as Int > 0, this)
        } ?: action.invoke(false, null)
    }

    fun Context.hideKeyboard() {
        isKeyboardVisible { isVisible, manager ->
            if (isVisible) {
                (this as Activity).currentFocus?.let { view ->
                    manager?.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
    }

    fun Context.hideKeyboardImplicit() {
        isKeyboardVisible { isVisible, manager ->
            if (isVisible) manager?.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        }
    }

    fun Context.showKeyboard() {
        isKeyboardVisible { isVisible, manager ->
            if (!isVisible) {
                manager?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }
        }
    }
}