package com.kyujin.meeco

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LevelListDrawable
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.Html
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.squareup.picasso.Picasso
import java.io.FileNotFoundException
import java.io.IOException
import java.net.MalformedURLException
import android.text.format.DateFormat
import android.text.format.Time
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class HTMLTextView(mContext: Context, val attrs: AttributeSet): TextView(mContext, attrs), Html.ImageGetter {
    val contextItems = arrayOf<CharSequence>("이미지 저장")
    var htmlText: String = ""
    set(htmlText) {
        val spanned = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT,this, null)!! as Spannable

        spanned.getSpans(0, spanned.length, ImageSpan::class.java).forEach {
            val flags = spanned.getSpanFlags(it)
            val spanStart = spanned.getSpanStart(it)
            val spanEnd = spanned.getSpanEnd(it)

            spanned.setSpan(object : URLSpan(it.source) {
                override fun onClick(widget: View) {
                    val builder = AlertDialog.Builder(context)

                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)


                    builder.setTitle("액션 선택")
                    builder.setItems(contextItems) { _, which ->
                        when (which) {
                            0 -> saveImage(drawableToBitmap(it.drawable), sdf.format(Date()))
                        }
                    }
                    builder.create().show()
                }
            }, spanStart, spanEnd, flags)
        }
        this.text = spanned
        this.movementMethod = LinkMovementMethod.getInstance()
    }
    var mActivity: Activity? = null

    override fun getDrawable(source: String?): Drawable {
        val d = LevelListDrawable()
        val empty = context.getDrawable(R.drawable.ic_baseline_cloud_download_24px)
        d.addLevel(0, 0, empty);
        d.setBounds(0, 0, empty.intrinsicWidth, empty.intrinsicHeight)

        LoadImage(source!!, d).execute()
        return d
    }

    fun saveImage(bitmap: Bitmap, date: String) {
        if (mActivity == null) return
        if (ContextCompat.checkSelfPermission(mActivity!!, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity!!, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        } else {
            MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "MeecoApp_$date", "Image from MeecoApp")
//            saveImageToExternal(context, this, "MeecoApp_$date", bitmap)
            Snackbar.make(this, "이미지가 저장되었습니다.", Snackbar.LENGTH_SHORT).show()
        }
    }

    inner class LoadImage(val source: String, val d: LevelListDrawable): AsyncTask<Void, Void, Bitmap>() {
        override fun doInBackground(vararg params: Void?): Bitmap? {
            try {
                return if (source.startsWith("sticker://"))
                    Picasso.get()
                        .load(source.substring(10))
                        .resize(200, 200)
                        .get()
                else
                    Picasso.get()
                        .load(source)
                        .get()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }
        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                val newd = BitmapDrawable(this@HTMLTextView.context.resources, result)
                d.addLevel(1, 1, newd)
                if (result.width > measuredWidth) {
                    val rate = measuredWidth.toDouble() / result.width
                    d.setBounds(0, 0, (result.width * rate).toInt(), (result.height * rate).toInt())
                } else {
                    d.setBounds(0, 0, result.width, result.height)
                }
                d.level = 1

                val t = this@HTMLTextView.text
                this@HTMLTextView.text = t
            }
            super.onPostExecute(result)
        }
    }
    fun drawableToBitmap (drawable: Drawable): Bitmap {
        var bitmap: Bitmap?

        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable as BitmapDrawable
            if(bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }

        if(drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap!!)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}