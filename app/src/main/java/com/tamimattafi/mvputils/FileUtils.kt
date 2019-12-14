package com.tamimattafi.mvputils

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


object FileUtils {

    fun Activity.createFilesChooserIntent(action: String, onCreate: (intent: Intent) -> Unit) {
        Intent(action).apply {
            onCreate.invoke(this)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startChooser(this)
        }
    }

    fun Activity.createFilesResultIntent(action: String, onCreate: (intent: Intent) -> Unit) {
        Intent(action).apply {
            onCreate.invoke(this)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createFileImportingIntent(): Intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("android.content.extra.SHOW_ADVANCED", true)
    }

    fun Activity.startChooser(intent: Intent) {
        startActivity(Intent.createChooser(intent, resources.getString(R.string.choose_application)))
    }

    fun Activity.getFileUri(path: String): Uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", File(path))

    fun Context.getFolderPath(name: String): String {
        File(getExternalFilesDir(name)?.absolutePath.toString()).apply {
            if (!exists()) mkdirs()
            return absolutePath
        }
    }

    fun File.copy(destination: File) {
        FileInputStream(this).apply {
            FileOutputStream(destination).run {
                ByteArray(1024).let { buffer ->
                    var len: Int = read(buffer)
                    while (len > 0) {
                        write(buffer, 0, len)
                        len = read(buffer)
                    }
                }
                this@apply.close()
                this.close()
            }
        }
    }

    object Paths {

        val AUTHORITY = "ru.zennex.journal.provider"

        val DOCUMENTS_DIR = "documents"


        fun isLocal(url: String?): Boolean {
            return url != null && !url.startsWith("http://") && !url.startsWith("https://")
        }

        fun isLocalStorageDocument(uri: Uri): Boolean {
            return AUTHORITY == uri.authority
        }

        fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }

        fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                          selectionArgs: Array<String>?): String? {

            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)

            try {
                cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(columnIndex)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        @RequiresApi(19)
        fun Context.getPath(uri: Uri): String? {
            try {
                Log.e("FileUtils", "resolving path: $uri")
                if (DocumentsContract.isDocumentUri(this, uri)) {
                    if (isLocalStorageDocument(uri)) {
                        return DocumentsContract.getDocumentId(uri)
                    } else if (isExternalStorageDocument(uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]
                        if ("primary".equals(type, ignoreCase = true)) {
                            return getExternalFilesDir(null)?.absolutePath + "/" + split[1]
                        }
                    } else if (isDownloadsDocument(uri)) {
                        val id = DocumentsContract.getDocumentId(uri)
                        arrayOf("content://downloads/public_downloads",
                                "content://downloads/my_downloads",
                                "content://downloads/all_downloads").forEach {
                            try {
                                val contentUri = ContentUris.withAppendedId(Uri.parse(it), id.toLong())
                                val path = getDataColumn(this, contentUri, null, null)
                                if (path != null) {
                                    return path
                                }
                            } catch (e: Exception) {
                                Log.e("FileUtils", e.localizedMessage ?: e.message ?: e.toString())
                                Log.e("FileUtils", e.toString())
                            }
                        }
                        return getFilePathFromStream(uri)

                    } else if (isMediaDocument(uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]

                        var contentUri: Uri? = null
                        when (type) {
                            "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }
                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])

                        return getDataColumn(this, contentUri, selection, selectionArgs)
                    }
                } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {
                    return if (isGooglePhotosUri(uri)) uri.lastPathSegment
                    else getDataColumn(this, uri, null, null)
                } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
                    return uri.path
                } else return getFilePathFromStream(uri)
            } catch (e: Exception) {
                Log.e("FileUtils", e.localizedMessage ?: e.message ?: e.toString())
                Log.e("FileUtils", e.toString())
                e.printStackTrace()
            }

            return null
        }

        @RequiresApi(19)
        private fun Context.getFilePathFromStream(uri: Uri): String? {
            val fileName = getFileName(uri)
            val cacheDir = getDocumentCacheDir()
            val file = generateFileName(fileName, cacheDir)
            var destinationPath: String? = null
            if (file != null) {
                destinationPath = file.absolutePath
                saveFileFromUri(this, uri, destinationPath)
            }

            return destinationPath
        }

        @RequiresApi(19)
        fun Context.getFile(uri: Uri): File? {
            val path = getPath(uri)
            if (path != null && isLocal(path)) {
                return File(path)
            }
            return null
        }

        @RequiresApi(19)
        private fun Context.getFileName(uri: Uri): String? {
            val mimeType = contentResolver.getType(uri)
            var filename: String? = null

            if (mimeType == null) {
                val path = getPath(uri)
                filename = if (path == null) {
                    getName(uri.toString())
                } else {
                    val file = File(path)
                    file.name
                }
            } else {
                val returnCursor = contentResolver.query(uri, null, null, null, null)
                if (returnCursor != null) {
                    val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    returnCursor.moveToFirst()
                    filename = returnCursor.getString(nameIndex)
                    returnCursor.close()
                }
            }

            return filename
        }

        fun getName(filename: String?): String? {
            if (filename == null) {
                return null
            }
            val index = filename.lastIndexOf('/')
            return filename.substring(index + 1)
        }

        fun Context.getDocumentCacheDir(): File {
            val dir = File(cacheDir, DOCUMENTS_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

        fun generateFileName(name: String?, directory: File): File? {
            var newName: String = name ?: return null

            var file = File(directory, newName)

            if (file.exists()) {
                var fileName: String = newName
                var extension = ""
                val dotIndex = newName.lastIndexOf('.')
                if (dotIndex > 0) {
                    fileName = newName.substring(0, dotIndex)
                    extension = newName.substring(dotIndex)
                }

                var index = 0

                while (file.exists()) {
                    index++
                    newName = "$fileName($index)$extension"
                    file = File(directory, newName)
                }
            }

            try {
                if (!file.createNewFile()) {
                    return null
                }
            } catch (e: IOException) {
                return null
            }
            return file
        }

        private fun saveFileFromUri(context: Context, uri: Uri, destinationPath: String) {
            var inputStream: InputStream? = null
            var bos: BufferedOutputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                bos = BufferedOutputStream(FileOutputStream(destinationPath, false))
                val buf = ByteArray(1024)
                inputStream!!.read(buf)
                do {
                    bos.write(buf)
                } while (inputStream.read(buf) != -1)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    inputStream?.close()
                    bos?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }

    }


    object Export {

        fun Activity.viewFile(path: String) {
            createFilesChooserIntent(Intent.ACTION_VIEW) {
                it.data = getFileUri(path)
            }
        }

        fun Activity.sendFile(path: String) {
            createFilesChooserIntent(Intent.ACTION_SEND) {
                it.putExtra(Intent.EXTRA_STREAM, getFileUri(path))
                it.type = "application/${path.substringAfterLast(".")}"
            }
        }

    }

    object Zip {

        fun File.saveAsZip(destination: File) {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).apply {
                if (isDirectory) zipSubFolder(this@saveAsZip, parent!!.length)
                else zipFile(this@saveAsZip)
                flush()
                close()
            }
        }

        private fun ZipOutputStream.zipFile(file: File, entryPath: String? = null) {
            val data = ByteArray(DEFAULT_BUFFER_SIZE)
            BufferedInputStream(FileInputStream(file), DEFAULT_BUFFER_SIZE).apply {
                putNextEntry(ZipEntry(entryPath
                        ?: file.getLastPathComponent()).also { it.time = file.lastModified() })
                var count = read(data, 0, DEFAULT_BUFFER_SIZE)
                while (read(data, 0, DEFAULT_BUFFER_SIZE) != -1) {
                    write(data, 0, count)
                    count = read(data, 0, DEFAULT_BUFFER_SIZE)
                }
            }
        }

        private fun ZipOutputStream.zipSubFolder(folder: File, basePathLength: Int) {
            folder.listFiles()?.forEach {
                if (it.isDirectory) zipSubFolder(it, basePathLength)
                else zipFile(it, it.path.substring(basePathLength))
            }
        }

        private fun File.getLastPathComponent(): String = absolutePath.substringAfterLast("/")

    }


}


