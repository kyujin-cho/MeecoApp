package com.kyujin.meeco

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CategorySpinnerAdapter(context: Context, resourceId: Int, items: List<Pair<String, String>>): ArrayAdapter<Pair<String, String>>(context, resourceId, items) {
    class ViewHolder(itemView: View) {
        val textView = itemView.findViewById<TextView>(R.id.spinnerItemText)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.category_spinner_layout, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            view = convertView
        }

        holder.textView.text = getItem(position).r
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }


}