package com.kyujin.meeco

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Environment
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.util.*

public fun getCookieFromSharedPrefs(sharedPreferences: SharedPreferences): HashMap<String, String> {
    val cookieJar = sharedPreferences.all
    val cookies = HashMap<String, String>()
    cookieJar.forEach { t, any -> cookies[t] = any as String }
    return cookies
}

public fun generateCookieString(cookies: HashMap<String, String>): String {
    return cookies.toList().map { it.first + "=" + it.second }.joinToString("; ")
}

public fun updateCookie(oldCookies: HashMap<String, String>, newCookies: Collection<String>) {
    newCookies.forEach {
        val (k, v) = it.split(";")[0].split("=")
        oldCookies[k] = v
    }
}

public fun commitCookies(newCookies: HashMap<String, String>, edit: SharedPreferences.Editor) {
    newCookies.forEach { t, u -> edit.putString(t, u) }
    edit.apply()
}

public fun createSnackbar(text: String, view: View) {
    Snackbar.make(view, text, Snackbar.LENGTH_SHORT).show()
}

public fun colorHex(color: Int): String {
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    return String.format(Locale.getDefault(), "#%02X%02X%02X", r, g, b)
}

public fun saveImageToExternal(context: Context, anyView: View, imgName: String, bm: Bitmap) {
//Create Path to save Image
    object : AsyncTask<Void, Void, Void>() {
        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            Snackbar.make(anyView, "이미지가 저장되었습니다.", Snackbar.LENGTH_SHORT).show()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/MeecoApp") //Creates app specific folder
            path.mkdirs();
            val imageFile = File(path, "$imgName.png") // Imagename.png
            val out = FileOutputStream(imageFile)
            try {
                bm.compress(Bitmap.CompressFormat.PNG, 100, out) // Compress Image
                out.flush()
                out.close()

                // Tell the media scanner about the new file so that it is
                // immediately available to the user.
                MediaScannerConnection.scanFile(context, arrayOf(imageFile.absolutePath), null) { path, uri ->
                    Log.i("ExternalStorage", "Scanned $path:")
                    Log.i("ExternalStorage", "-> uri = $uri")
                }
            } catch (e: Exception) {
                throw IOException()
            }
            return null
        }
    }.execute()
}
