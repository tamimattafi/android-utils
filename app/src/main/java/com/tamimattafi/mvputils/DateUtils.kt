package com.tamimattafi.mvputils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {

    const val FILE_TIME_PATTERN: String = "dd-MM-yyyy_HH:mm:ss"
    const val UI_TIME_PATTERN: String = "dd MMMM, yyyy - HH:mm"
    const val EXPERIMENT_TIME_PATTERN = "dd-MM-yyyy HH:mm"

    fun Date?.unixToStringDate(pattern: String): String = this?.run {
        SimpleDateFormat(pattern, Locale.getDefault()).format(this)
    } ?: "～"

    fun Long.unixToStringDate(pattern: String): String = toDate().unixToStringDate(pattern)

    fun Long.longToStringDate(pattern: String): String = Date(this).unixToStringDate(pattern)

    fun String?.toDate(firstPattern: String, secondPattern: String) = fromDateString(firstPattern).unixToStringDate(secondPattern)

    fun String?.fromDateString(pattern: String): Date? = this?.run { SimpleDateFormat(pattern, Locale.getDefault()).parse(this) }


    fun Long.toDate(): Date? {
        return if (this > 1) {
            Date(this * 1000)
        } else null
    }

    fun Date?.toUnix(): Long? = this?.run { time / 1000 }


    fun getCurrentUnix(): Long = Calendar.getInstance().time.toUnix()!!

    fun String.getCurrentPattern(): String = Calendar.getInstance().time.unixToStringDate(this)

    fun Long.formatTime(): String = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(this),
            TimeUnit.MILLISECONDS.toMinutes(this) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(this) % TimeUnit.MINUTES.toSeconds(1))


    fun String.addFileDate(): String = "${this}_${FILE_TIME_PATTERN.getCurrentPattern()}"


}