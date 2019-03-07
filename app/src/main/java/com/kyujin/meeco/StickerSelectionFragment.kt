package com.kyujin.meeco

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.AdapterView
import android.widget.Spinner
import java.lang.ClassCastException

interface OnItemClickListener {
    fun onItemClick(item: StickerInfo)
}

class StickerSelectionFragment : DialogFragment(), OnItemClickListener {

    val boardFetcher = BoardFetcher()
    var sharedPreferences: SharedPreferences? = null
    var spinner: Spinner? = null
    var stickerItemsHolder = ArrayList<StickerRowInfo>()
    var spinnerAdapter: StickerListSpannerAdapter? = null
    var csrfToken = ""
    var recyclerView: RecyclerView? = null
    var recyclerViewAdapter: StickerRecylclerAdapter? = null
    var stickerDetailsHolder = ArrayList<StickerInfo>()

    interface InterfaceCommunicator {
        fun sendStickerSelectResult(csrfToken: String, item: StickerInfo? = null)
    }
    val TAG = "example_dialog"

    private var toolbar: Toolbar? = null

    var interfaceCommunicator: InterfaceCommunicator? = null

    fun display(fragmentManager: FragmentManager): StickerSelectionFragment {
        val exampleDialog = StickerSelectionFragment()
        exampleDialog.show(fragmentManager, TAG)
        return exampleDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            dialog.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.window.setWindowAnimations(R.style.AppTheme_Slide)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            interfaceCommunicator = context as InterfaceCommunicator
        } catch (e: ClassCastException) {
            e.printStackTrace()
            dismiss()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_sticker_selection, container, false)

        toolbar = view.findViewById(R.id.stickerSelectionToolbar)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spinner = view.findViewById(R.id.stickerListSpinner)
        sharedPreferences = context!!.getSharedPreferences("cookie_jar", Context.MODE_PRIVATE)

        toolbar!!.title = "스티커 선택"
        toolbar!!.inflateMenu(R.menu.sticker_selection_fragment)

        view.post {
            Log.i(TAG, "Recycler View Width: " + view.measuredWidth)
            val stickerSize = (view.width / 5)
            recyclerView!!.layoutManager = GridLayoutManager(context, 5)

            recyclerViewAdapter = StickerRecylclerAdapter(stickerDetailsHolder,  stickerSize - 10,this)
            recyclerView!!.adapter = recyclerViewAdapter!!
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val cookies = getCookieFromSharedPrefs(sharedPreferences!!)

        recyclerView = view!!.findViewById(R.id.stickerImageGridView)

        boardFetcher.fetchStickerList(cookies) { stickers, csrfToken, newCookies ->
            commitCookies(newCookies, sharedPreferences!!.edit())
            stickerItemsHolder.clear()
            stickerItemsHolder.add(StickerRowInfo("", "", ""))
            stickers.forEach { stickerItemsHolder.add(it) }
            this@StickerSelectionFragment.csrfToken = csrfToken

            val adapter = StickerListSpannerAdapter(context!!, R.id.category_spinner, stickerItemsHolder)
            adapter.setDropDownViewResource(R.layout.category_spinner_layout)
            spinner!!.onItemSelectedListener = StickerListSpinnerSelectListener()
            spinner!!.adapter = adapter
            toolbar!!.setNavigationOnClickListener {
                interfaceCommunicator!!.sendStickerSelectResult(csrfToken) // the parameter is any int code you choose.
                dismiss()
            }
        }
    }

    override fun onItemClick(item: StickerInfo) {
        interfaceCommunicator!!.sendStickerSelectResult(csrfToken, item)
        dismiss()
    }

    inner class StickerListSpinnerSelectListener : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {

        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val currentItem = this@StickerSelectionFragment.spinner!!.getItemAtPosition(position) as StickerRowInfo
            if (currentItem.stickerName == "") return
            val cookies = getCookieFromSharedPrefs(sharedPreferences!!)
            boardFetcher.fetchSticker(currentItem.stickerId, csrfToken, cookies) { stickers, newCookies ->
                commitCookies(newCookies, sharedPreferences!!.edit())
                stickerDetailsHolder.clear()
                stickers.forEach { stickerDetailsHolder.add(it) }
                recyclerViewAdapter!!.notifyDataSetChanged()
            }
        }
    }
}
