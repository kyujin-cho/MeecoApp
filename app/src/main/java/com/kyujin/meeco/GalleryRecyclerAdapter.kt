package com.kyujin.meeco

import android.content.Context
import android.content.Intent
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
import android.widget.ImageView
import com.squareup.picasso.Picasso


class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val thumbnail: ImageView
    val categoryText: TextView
    val titleText: TextView
    val nickNameText: TextView
    val timeText: TextView
    val previewText: TextView
    val viewCountText: TextView
    val replyCountText: TextView
    val likeCountText: TextView

    init {
        thumbnail = itemView.findViewById(R.id.galleryThumbnailImage)
        categoryText = itemView.findViewById(R.id.galleryCategoryText)
        titleText = itemView.findViewById(R.id.galleryTitleText)
        nickNameText = itemView.findViewById(R.id.galleryNickNameText)
        previewText = itemView.findViewById(R.id.galleryPreviewText)
        timeText = itemView.findViewById(R.id.galleryTimeText)
        viewCountText = itemView.findViewById(R.id.galleryViewCountText)
        replyCountText = itemView.findViewById(R.id.galleryReplyCountText)
        likeCountText = itemView.findViewById(R.id.galleryLikeText)
    }
}

class GalleryRecyclerAdapter(val context: Context, val articles: ArrayList<GalleryRowInfo>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.recycler_view_row_gallery, p0, false)
        return GalleryViewHolder(
            view
        )
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        val vh = p0 as GalleryViewHolder
        val targetItem = articles[p1]

        vh.itemView.setOnClickListener {
            val intent = Intent(it.context, ArticleActivity::class.java)
            intent.putExtra("boardId", articles.get(p1).boardId)
            intent.putExtra("articleId", articles.get(p1).articleId)
            it.context.startActivity(intent)
        }


        if (targetItem.category.isNotEmpty()) {
            val span = SpannableStringBuilder()
            Log.i("NoramlRecyclerAdapter", targetItem.category + " Not Empty")
            span.append(targetItem.category)
            span.setSpan(ForegroundColorSpan(Color.parseColor("#" + targetItem.categoryColor)), 0, targetItem.category.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            vh.categoryText.text = span
        } else {
            vh.categoryText.visibility = View.GONE
        }

        if (targetItem.thumbNailUrl.isNotEmpty()) {
            Picasso.get()
                .load(targetItem.thumbNailUrl)
                .resize(vh.thumbnail.measuredWidth, 150)
                .centerCrop()
                .into(vh.thumbnail)
        }

        vh.titleText.text = targetItem.title
        vh.nickNameText.text = targetItem.nickname
        vh.previewText.text = targetItem.preview
        vh.timeText.text = targetItem.time
        vh.viewCountText.text = "조회수 " + targetItem.viewCount.toString()
        vh.replyCountText.text = "댓글 " + targetItem.replyCount.toString() + " 개"
        vh.likeCountText.text = "❤︎ " + targetItem.likeCount.toString() + " 개"
    }

    override fun getItemCount(): Int {
        return articles.size
    }
}