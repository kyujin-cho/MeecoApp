package com.kyujin.meeco

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.r0adkll.slidr.Slidr
import com.securepreferences.SecurePreferences
import com.squareup.picasso.Picasso
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.R.attr.label
import android.app.Activity
import android.content.*
import android.content.ClipData.newPlainText
import android.content.res.Configuration
import android.support.design.widget.Snackbar
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.webkit.WebSettings
import android.webkit.WebView


class ArticleActivity : AppCompatActivity(), StickerSelectionFragment.InterfaceCommunicator {

    val boardFetcher = BoardFetcher()
    var passwordPreferences: SecurePreferences? = null
    var sharedPreferences: SharedPreferences? = null
    var writeReplyEdit: EditText? = null
    var boardId = ""
    var articleId = ""
    var isLoggedIn = false
    var viewArticleHolder = ArrayList<ReplyInfo>()
    var titleText: TextView? = null
    var signatureText: TextView? = null
    var likeCountText: TextView? = null
    var contentText: HTMLTextView? = null
    var nickNameText: TextView? = null
    var timeText: TextView? = null
    var viewCountText: TextView? = null
    var articleProfileImgae: ImageView? = null
    var replysText: TextView? = null
    var extraInformationText: TextView? = null
    var myAdapter: ReplyRecyclerAdapter? = null
    var scrollView: NestedScrollView? = null
    var likeButton: Button? = null
    var webView: WebView? = null
    var inReplyTo = -1
    var article: ArticleInfo? = null
    var currentUser = ""
    var currentUserId = ""
    var inEditMode = -1
    var categories = ArrayList<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)
        Slidr.attach(this)

        val typedValue = TypedValue()
        val textColorTypedValue = TypedValue()
        val theme = theme
        theme.resolveAttribute(R.attr.webViewBackground, typedValue, true)
        theme.resolveAttribute(R.attr.primaryTextColor, textColorTypedValue, true)


        boardId = intent.getStringExtra("boardId")
        articleId = intent.getStringExtra("articleId")
        categories = (intent.getSerializableExtra("categories")?:ArrayList<Pair<String, String>>()) as ArrayList<Pair<String, String>>

        writeReplyEdit = findViewById(R.id.writeReplyEdit)

        val signatureLayout = findViewById<LinearLayout>(R.id.signature_layout)
        titleText = findViewById<TextView>(R.id.titleText)
        signatureText = findViewById<TextView>(R.id.signatureText)
        likeCountText = findViewById<TextView>(R.id.likeCountText)
        contentText = findViewById<HTMLTextView>(R.id.contentText)
        nickNameText = findViewById<TextView>(R.id.articleNickNameText)
        timeText = findViewById<TextView>(R.id.articleTimeText)
        viewCountText = findViewById<TextView>(R.id.articleViewsText)
        articleProfileImgae = findViewById<ImageView>(R.id.articleProfileImage)
        replysText = findViewById<TextView>(R.id.articleReplyCountText)
        extraInformationText = findViewById<TextView>(R.id.citationText)
        likeButton = findViewById(R.id.likeButton)
        likeButton!!.isEnabled = false
        webView = findViewById(R.id.webView)

        likeButton!!.setOnClickListener {
            boardFetcher.likeArticle(boardId, articleId, getCookieFromSharedPrefs(sharedPreferences!!)) { success, message ->
                Snackbar.make(likeButton!!, message, Snackbar.LENGTH_SHORT).show()
                if (success) {
                    refreshArticle(false)
                }
            }
        }


        contentText!!.mActivity = this

        scrollView = findViewById(R.id.articleScrollView)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val writeReplyButton = findViewById<Button>(R.id.writeReplyButton)
        val selectStickerButton = findViewById<Button>(R.id.openStickerDialogButton)

        val writeReplyArea = findViewById<CardView>(R.id.writeReplyArea)

        val mRecyclerView = findViewById<RecyclerView>(R.id.reply_recycler_view)
        passwordPreferences = SecurePreferences(this)
        var cookies = HashMap<String, String>()

        currentUser = passwordPreferences!!.getString("userName", "") ?: ""
        isLoggedIn = currentUser.isNotBlank()

        if (isLoggedIn) {
            currentUserId = passwordPreferences!!.getString("uid", "") ?: ""
            sharedPreferences = getSharedPreferences("cookie_jar", Context.MODE_PRIVATE)
            sharedPreferences!!.all.forEach { t, k -> cookies.put(t, k as String) }
        }

        mRecyclerView.isNestedScrollingEnabled = false
        val mLayoutManager = LinearLayoutManager(this)

        mRecyclerView.layoutManager = mLayoutManager

        myAdapter = ReplyRecyclerAdapter(this, currentUserId, viewArticleHolder, ArticleAdapterCommunicator())

        mRecyclerView.adapter = myAdapter

        writeReplyButton.setOnClickListener { view ->
            val replyText = writeReplyEdit!!.text.toString()
            if (replyText.isBlank()) {
                Toast.makeText(this, "댓글 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                boardFetcher.writeReply(boardId, articleId, if (inReplyTo != -1) viewArticleHolder[inReplyTo].replyId else "", if (inEditMode >= 0) viewArticleHolder[inEditMode].replyId else "0", replyText, getCookieFromSharedPrefs(sharedPreferences!!)) { success, error, newCookies ->
                    runOnUiThread {
                        val editor = sharedPreferences!!.edit()
                        newCookies.forEach { t, u -> editor.putString(t, u) }
                        editor.apply()
                        if (success) {
                            Snackbar.make(view, "댓글을 등록했습니다", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show()
                            refreshArticle(true)
                        } else {
                            Snackbar.make(view, "댓글 등록에 실패했습니다: $error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show()
                        }
                    }
                }

            }
        }

        selectStickerButton.setOnClickListener {
            val dialogFragment = StickerSelectionFragment()
            dialogFragment.show(supportFragmentManager, "dialog")
        }

        loadReplys(cookies) {
            if (it.category.isNotBlank()){
                val span = SpannableStringBuilder(it.category + " | " + it.title)
                span.setSpan(ForegroundColorSpan(Color.parseColor("#" + it.categoryColor)), 0, it.category.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                titleText!!.text = span
            } else {
                titleText!!.text = it.title
            }
            if (it.signature.isNotBlank()) {
                signatureText!!.text = it.signature
            } else {
                signatureLayout.visibility = View.GONE
            }
            if (it.informationName.isNotBlank()) {
                val span = SpannableStringBuilder(it.informationName + " " + it.informationValue)
                span.setSpan(ForegroundColorSpan(Color.BLACK), it.informationName.length, span.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                extraInformationText!!.setOnClickListener { e ->
                    if (it.informationValue.startsWith("http://") || it.informationValue.startsWith("https://")) {
                        val browserIntent = Intent(ACTION_VIEW, Uri.parse(it.informationValue))
                        startActivity(browserIntent)
                    } else {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Meeco", it.informationValue)
                        clipboard.primaryClip = clip
                        Toast.makeText(this, "복사되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                extraInformationText!!.text = span
            } else {
                extraInformationText!!.visibility = View.GONE
            }
            article = it
            nickNameText!!.text = it.nickname
            timeText!!.text = it.time
            viewCountText!!.text = "조회수 " + it.viewCount.toString()
            replysText!!.text = "댓글 " + viewArticleHolder.size
            if (it.likes >= 0) {
                val builder = SpannableStringBuilder("❤︎ " + it.likes.toString())
                builder.setSpan(ForegroundColorSpan(Color.parseColor("#FA7470")), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                likeButton!!.text = builder
                if (isLoggedIn) likeButton!!.isEnabled = true
            }
//            contentText!!.htmlText = it.rawHTML
            val width = windowManager.defaultDisplay.width
            webView!!.setBackgroundColor(typedValue.data)
            webView!!.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            webView!!.settings.javaScriptEnabled = true
            val colorStr = String.format("#%06X", (0xFFFFFF.and(textColorTypedValue.data)))
            webView!!.loadData(getStyledHTMLForArticle(it.rawHTML, width, colorStr), "text/html; charset=UTF-8", "UTF-8")
            if (it.profileImageUrl.isNotBlank())
                Picasso.get()
                    .load(it.profileImageUrl)
                    .noFade()
                    .into(articleProfileImgae)
            myAdapter!!.notifyDataSetChanged()
            invalidateOptionsMenu()
            if (!isLoggedIn) {
                writeReplyArea.visibility = View.GONE
            }

            title = it.boardName
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (article != null && currentUserId == article!!.userId) {
            menuInflater.inflate(R.menu.article_menu, menu)
        }
        return true
    }

    fun loadReplys(cookies: HashMap<String, String>, f: (article: ArticleInfo) -> Unit) {
        boardFetcher.fetchArticle(boardId, articleId, cookies) { it, replys, newCookies ->
            viewArticleHolder.clear()
            if (isLoggedIn) {
                val editor = sharedPreferences!!.edit()
                newCookies.forEach { t, u -> editor.putString(t, u) }
                editor.apply()
            }
            replys.forEach {
                viewArticleHolder.add(it)
            }
            runOnUiThread {
                f(it)
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item == null) return false
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.article_delete -> {
                AlertDialog.Builder(this)
                .setTitle("글 삭제")
                .setMessage("정말 삭제하시겠습니까?")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes) { dialog, which ->
                    boardFetcher.deleteArticle(boardId, articleId, getCookieFromSharedPrefs(sharedPreferences!!)) { success, error ->
                        if (success) {
                            finish()
                        } else {
                            Snackbar.make(titleText!!, "삭제에 실패하였습니다 : $error", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                 }
                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
                return true
            }
            R.id.article_edit -> {
                val intent = Intent(this, ArticleEditorActivity::class.java)
                intent.putExtra("boardId", boardId)
                intent.putExtra("articleId", articleId)
                if (categories.size > 0) {
                    val newList = ArrayList<Pair<String, String>>()
                    categories.subList(1, categories.size).forEach { newList.add(it) }
                    intent.putExtra("categories", newList)
                }

                startActivityForResult(intent, 2)
                return true
            }

            else -> return false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        when (requestCode) {
            2 -> articleEditResult(resultCode, data)
        }
    }

    fun articleEditResult(resultCode: Int, data: Intent) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                refreshArticle(false)
                Snackbar.make(likeButton!!, "게시글이 수정되었습니다.", Snackbar.LENGTH_SHORT).show()
            }

        }
    }

    override fun sendStickerSelectResult(csrfToken: String, item: StickerInfo?) {
        if (item == null) return

        val cookies = getCookieFromSharedPrefs(sharedPreferences!!)
        boardFetcher.writeStickerReply(boardId, articleId, if (inReplyTo != -1) viewArticleHolder[inReplyTo].replyId else "", if (inEditMode >= 0) viewArticleHolder[inEditMode].replyId else "0", csrfToken, item, cookies)  { success, error ->
            if (success) {
                Snackbar.make(writeReplyEdit!!, "댓글을 등록했습니다", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
                refreshArticle(true)
            } else {
                Snackbar.make(writeReplyEdit!!, "댓글 등록에 실패했습니다: $error", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
            }
        }
    }

    override fun onBackPressed() {
        if (inReplyTo != -1) refreshReplyTarget(-1)
        else super.onBackPressed()
    }

    fun refreshReplyTarget(index: Int) {
        if (index == -1) {
            viewArticleHolder[inReplyTo].selected = false
            inReplyTo = -1
        } else {
            if (inReplyTo != -1) {
                viewArticleHolder[inReplyTo].selected = false
            }
            viewArticleHolder[index].selected = true
            inReplyTo = index
        }
        myAdapter!!.notifyDataSetChanged()
    }
    
    fun refreshArticle(isReplyRelated: Boolean) {
        loadReplys(getCookieFromSharedPrefs(sharedPreferences!!)) {
            if (it.category.isNotBlank()){
                val span = SpannableStringBuilder(it.category + " | " + it.title)
                span.setSpan(ForegroundColorSpan(Color.parseColor("#" + it.categoryColor)), 0, it.category.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                titleText!!.text = span
            } else {
                titleText!!.text = it.title
            }

            nickNameText!!.text = it.nickname
            timeText!!.text = it.time
            viewCountText!!.text = "조회수 " + it.viewCount.toString()
            replysText!!.text = "댓글 " + viewArticleHolder.size
            val builder = SpannableStringBuilder("❤︎ " + it.likes.toString())
            builder.setSpan(ForegroundColorSpan(Color.parseColor("#FA7470")), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            likeButton!!.text = builder
            contentText!!.htmlText = it.rawHTML
            myAdapter!!.notifyDataSetChanged()
            writeReplyEdit!!.setText("")

            val textColorTypedValue = TypedValue()
            val theme = theme
            theme.resolveAttribute(R.attr.primaryTextColor, textColorTypedValue, true)

            val width = windowManager.defaultDisplay.width
            val colorStr = String.format("#%06X", (0xFFFFFF.and(textColorTypedValue.data)))
            webView!!.loadData(getStyledHTMLForArticle(it.rawHTML, width, colorStr), "text/html; charset=UTF-8", "UTF-8")

            if (isReplyRelated) scrollView!!.fullScroll(View.FOCUS_DOWN)
        }
    }

    inner class ArticleAdapterCommunicator: ReplyAdapterCommunicator {
        override fun setReplyTarget(index: Int) {
            refreshReplyTarget(index)
        }

        override fun onDeleteReply(target: ReplyInfo) {
            boardFetcher.deleteReply(boardId, articleId, target.replyId, getCookieFromSharedPrefs(sharedPreferences!!)) { success, error ->
                if (success) {
                    refreshArticle(true)
                } else {
                    Snackbar.make(titleText!!, "삭제에 실패하였습니다 : $error", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        override fun onLikeReply(target: ReplyInfo) {
            boardFetcher.likeReply(boardId, articleId, target.replyId, getCookieFromSharedPrefs(sharedPreferences!!)) { success, message ->
                Snackbar.make(likeButton!!, message, Snackbar.LENGTH_SHORT).show()
                if (success) {
                    refreshArticle(false)
                }
            }
        }

        override fun onEditReply(target: Int) {
            writeReplyEdit!!.setText(viewArticleHolder[target].replyContent, TextView.BufferType.EDITABLE)
            inEditMode = target
        }
    }
}
