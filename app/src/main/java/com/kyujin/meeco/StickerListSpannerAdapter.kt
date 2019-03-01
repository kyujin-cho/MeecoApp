package com.kyujin.meeco

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class StickerListSpannerAdapter(context: Context, resourceId: Int, items: List<StickerRowInfo>): ArrayAdapter<StickerRowInfo>(context, resourceId, items) {
    class ViewHolder(itemView: View) {
        val textView = itemView.findViewById<TextView>(R.id.stickerSpinnerItemText)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.sticker_list_spinner_layout, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            view = convertView
        }

        if (getItem(position).stickerName == "") {
            holder.textView.setTextColor(R.attr.itemTextColor)
            holder.textView.text = "스티커를 선택하세요"
        } else {
            holder.textView.setTextColor(Color.BLACK)
            holder.textView.text = getItem(position).stickerName
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }


}