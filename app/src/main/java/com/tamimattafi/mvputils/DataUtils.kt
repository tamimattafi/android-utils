package com.tamimattafi.mvputils

object DataUtils {

    fun ByteArray.toHex(): String {
        val response = StringBuilder()
        for (b in this) {
            response.append(String.format("%02X", b)).append(" ")
        }
        return response.toString().trim { it <= ' ' }
    }

    fun Byte.toHex(): String {
        val response = StringBuilder()
        response.append(String.format("%02X", this)).append(" ")
        return response.toString().trim { it <= ' ' }
    }

    fun ByteArray.bytesToString(): String {
        val response = StringBuilder()
        for (b in this) {
            response.append(String.format("%c", b)).append("")
        }
        return response.toString().trim { it <= ' ' }
    }
}
