package com.kyujin.meeco

import android.support.v7.widget.RecyclerView
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso

class StickerViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val stickerImage: ImageView

    init {
        stickerImage = view.findViewById(R.id.stickerSelectionImage)
    }
}

class StickerRecylclerAdapter(val stickers: ArrayList<StickerInfo>, val listener: OnItemClickListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.recycler_view_row_sticker, p0, false)
        return StickerViewHolder(view)
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        val vh = p0 as StickerViewHolder
        val targetItem = stickers[p1]

        vh.stickerImage.setOnClickListener {
            this@StickerRecylclerAdapter.listener.onItemClick(targetItem)
        }

        if (targetItem.url.isNotBlank()) {
            Picasso.get()
                .load(targetItem.url)
                .into(vh.stickerImage)
        }
    }

    override fun getItemCount(): Int {
        return stickers.size
    }
}