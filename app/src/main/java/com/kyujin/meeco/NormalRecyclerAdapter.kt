package com.kyujin.meeco

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.support.v4.content.ContextCompat
import android.text.style.ImageSpan
import android.util.TypedValue


class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val titleText: TextView
    val nickNameText: TextView
    val timeText: TextView
    val viewCountText: TextView
    val replyCountText: TextView

    init {
        titleText = itemView.findViewById(R.id.titleText)
        nickNameText = itemView.findViewById(R.id.nickNameText)
        timeText = itemView.findViewById(R.id.timeText)
        viewCountText = itemView.findViewById(R.id.viewCountText)
        replyCountText = itemView.findViewById(R.id.replyText)
    }
}

class NormalRecyclerAdapter(val context: Context, val articles: ArrayList<NormalRowInfo>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.recycler_view_row_normal, p0, false)
        return MyViewHolder(
            view
        )
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        val vh = p0 as MyViewHolder
        val targetItem = articles[p1]

        val currentTheme = context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)
        val imageDrawable: Int
        val lockDrawable: Int
        val replyDrawable: Int
        when (currentTheme) {
            Configuration.UI_MODE_NIGHT_YES -> {
                imageDrawable = R.drawable.baseline_insert_photo_white_18
                lockDrawable = R.drawable.baseline_lock_white_18
                replyDrawable = R.drawable.baseline_comment_white_18
            }
            else -> {
                imageDrawable = R.drawable.baseline_insert_photo_black_18
                lockDrawable = R.drawable.baseline_lock_black_18
                replyDrawable = R.drawable.baseline_comment_black_18
            }
        }

        if (!targetItem.isSecret) {
            vh.itemView.setOnClickListener {
                val intent = Intent(it.context, ArticleActivity::class.java)
                intent.putExtra("boardId", articles[p1].boardId)
                intent.putExtra("articleId", articles[p1].articleId)
                intent.putExtra("categories", articles[p1].categories)
                it.context.startActivity(intent)
            }
        }

        val span = SpannableStringBuilder()
        if (targetItem.category.isNotEmpty()) {
            Log.i("NormalRecyclerAdapter", targetItem.category + " Not Empty")
            span.append(targetItem.category + " " + targetItem.title)
            span.setSpan(ForegroundColorSpan(Color.parseColor("#" + targetItem.categoryColor)), 0, targetItem.category.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        } else {
            span.append(targetItem.title)
            Log.i("NormalRecyclerAdapter", targetItem.category + " Empty")
        }

        if (targetItem.hasImage) {
            val d = ContextCompat.getDrawable(context, imageDrawable)
            d!!.setBounds(0, 0, d.intrinsicWidth - 3, d.intrinsicHeight - 3)
            span.append("  img")
            span.setSpan(ImageSpan(d, ImageSpan.ALIGN_BASELINE), span.length - 3, span.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (targetItem.isSecret) {
            val d = ContextCompat.getDrawable(context, lockDrawable)
            d!!.setBounds(0, 0, d.intrinsicWidth - 3, d.intrinsicHeight - 3)
            span.append(" img")
            span.setSpan(
                ImageSpan(d, ImageSpan.ALIGN_BASELINE),
                span.length - 3,
                span.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        vh.titleText.text = span

        val replySpan = SpannableStringBuilder("img ${targetItem.replyCount}")
        val d = ContextCompat.getDrawable(context, replyDrawable)
        d!!.setBounds(0, 0, d.intrinsicWidth - 5, d.intrinsicHeight - 5)
        replySpan.setSpan(ImageSpan(d, ImageSpan.ALIGN_BASELINE), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        vh.replyCountText.text = replySpan
        vh.nickNameText.text = targetItem.nickname
        vh.timeText.text = targetItem.time
        vh.viewCountText.text = "조회수 " + targetItem.viewCount.toString()
    }

    override fun getItemCount(): Int {
        return articles.size
    }
}