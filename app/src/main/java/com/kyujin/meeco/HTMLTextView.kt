package com.kyujin.meeco

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LevelListDrawable
import android.os.AsyncTask
import android.text.Html
import android.util.AttributeSet
import android.widget.TextView
import com.squareup.picasso.Picasso
import java.io.FileNotFoundException
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

class HTMLTextView(mContext: Context, val attrs: AttributeSet): TextView(mContext, attrs), Html.ImageGetter {
    var htmlText: String = ""
    set(htmlText) {
        val spanned = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT,this, null)
        this.text = spanned
    }
    override fun getDrawable(source: String?): Drawable {
        val d = LevelListDrawable()
        val empty = context.getDrawable(R.mipmap.ic_launcher)
        d.addLevel(0, 0, empty);
        d.setBounds(0, 0, empty.intrinsicWidth, empty.intrinsicHeight)

        LoadImage(source!!, d).execute()
        return d
    }

    inner class LoadImage(val source: String, val d: LevelListDrawable): AsyncTask<Void, Void, Bitmap>() {
        override fun doInBackground(vararg params: Void?): Bitmap? {
            try {
                if (source.startsWith("sticker://"))
                    return Picasso.get()
                        .load(source.substring(10))
                        .resize(200, 200)
                        .get()
                else
                    return Picasso.get()
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
}