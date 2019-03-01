package com.kyujin.meeco

import android.content.Intent
import android.graphics.Color
import android.media.Image
import android.opengl.Visibility
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val replyMarginText: TextView
    val nickNameText: TextView
    val profileImage: ImageView
    val timeText: TextView
    val likesText: TextView
    val replyToText: TextView
    val replyContentText: HTMLTextView

    init {
        replyMarginText = itemView.findViewById(R.id.replyMargin)
        replyToText = itemView.findViewById(R.id.inReplyToText)
        profileImage = itemView.findViewById(R.id.replyProfileImage)
        nickNameText = itemView.findViewById(R.id.replyNickNameText)
        timeText = itemView.findViewById(R.id.timeText)
        likesText = itemView.findViewById(R.id.likesText)
        replyContentText = itemView.findViewById(R.id.replyContentText)
    }
}

class ReplyRecyclerAdapter(val replys: ArrayList<ReplyInfo>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.recycler_view_row_reply, p0, false)
        return ReplyViewHolder(
            view
        )
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        val vh = p0 as ReplyViewHolder
        val targetItem = replys[p1]

        if (targetItem.replyTo.isBlank()) {
            vh.replyMarginText.visibility = View.GONE
            vh.replyToText.visibility = View.GONE
        } else {
            vh.replyToText.text = targetItem.replyTo + " 님에게"
        }

        if (targetItem.isWriter) {
            val span = SpannableStringBuilder(targetItem.nickname + " (글쓴이)")
            span.setSpan(ForegroundColorSpan(Color.parseColor("#3f51b5")), 0, targetItem.nickname.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            vh.nickNameText.text = span
        } else {
            vh.nickNameText.text = targetItem.nickname
        }
        vh.timeText.text = targetItem.time
        vh.likesText.text = "좋아요 " + targetItem.likes.toString()
        vh.replyContentText.htmlText = targetItem.rawHTML
        Log.i("ReplyRecycler", targetItem.profileImageUrl)
        if (targetItem.profileImageUrl.isNotBlank()) {
            Picasso.get()
                .load(targetItem.profileImageUrl)
                .resize(100, 100)
                .noFade()
                .into(vh.profileImage)
        }
    }

    override fun getItemCount(): Int {
        return replys.size
    }
}