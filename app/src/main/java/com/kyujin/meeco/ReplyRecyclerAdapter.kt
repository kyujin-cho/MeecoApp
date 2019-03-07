package com.kyujin.meeco

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.Image
import android.opengl.Visibility
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import com.thefinestartist.utils.content.ThemeUtil.resolveAttribute



class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val replyMarginText: TextView
    val nickNameText: TextView
    val profileImage: ImageView
    val timeText: TextView
    val likesText: TextView
    val replyToText: TextView
    val replyContentText: HTMLTextView
    val textViewOptions: TextView
    val layout: RelativeLayout
    init {
        replyMarginText = itemView.findViewById(R.id.replyMargin)
        replyToText = itemView.findViewById(R.id.inReplyToText)
        profileImage = itemView.findViewById(R.id.replyProfileImage)
        nickNameText = itemView.findViewById(R.id.replyNickNameText)
        timeText = itemView.findViewById(R.id.timeText)
        likesText = itemView.findViewById(R.id.likesText)
        replyContentText = itemView.findViewById(R.id.replyContentText)
        textViewOptions = itemView.findViewById(R.id.textViewOptions)
        layout = itemView.findViewById(R.id.replyView)
    }
}

interface ReplyAdapterCommunicator {
    fun setReplyTarget(index: Int)
    fun onLikeReply(target: ReplyInfo)
    fun onEditReply(target: Int)
    fun onDeleteReply(target: ReplyInfo)
}

class ReplyRecyclerAdapter(val context: Context, val userId: String, val replys: ArrayList<ReplyInfo>, val listener: ReplyAdapterCommunicator): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.recycler_view_row_reply, p0, false)
        return ReplyViewHolder(
            view
        )
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        val vh = p0 as ReplyViewHolder
        val targetItem = replys[p1]
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(R.attr.webViewBackground, typedValue, true)

        if (targetItem.selected) {
            vh.layout.setBackgroundColor(Color.parseColor("#AB829A"))
        } else {
            vh.layout.setBackgroundColor(typedValue.data)
        }

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


        vh.textViewOptions.setOnClickListener {
            val popup = PopupMenu(context, vh.textViewOptions)
            if (targetItem.userId == userId)
                popup.inflate(R.menu.reply_options_candelete_menu)
            else if (userId.isNotBlank())
                popup.inflate(R.menu.reply_options_login_menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.do_reply -> {
                        listener.setReplyTarget(p1)
                        true
                    }
                    R.id.delete_reply -> {
                        AlertDialog.Builder(context)
                            .setTitle("댓글 삭제")
                            .setMessage("정말 삭제하시겠습니까?")

                            // Specifying a listener allows you to take an action before dismissing the dialog.
                            // The dialog is automatically dismissed when a dialog button is clicked.
                            .setPositiveButton(android.R.string.yes) { dialog, which ->
                                listener.onDeleteReply(targetItem)
                            }
                            // A null listener allows the button to dismiss the dialog and take no further action.
                            .setNegativeButton(android.R.string.no, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                        true
                    }
                    R.id.edit_reply -> {
                        listener.onEditReply(p1)
                        true
                    }
                    R.id.do_like_reply -> {
                        listener.onLikeReply(targetItem)
                        true
                    }


                    else -> false
                }
            }

            popup.show()
        }
    }

    override fun getItemCount(): Int {
        return replys.size
    }
}