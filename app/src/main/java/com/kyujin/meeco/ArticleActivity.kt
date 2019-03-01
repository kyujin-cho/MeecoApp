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
import android.content.*
import android.content.ClipData.newPlainText
import android.support.design.widget.Snackbar
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.CardView
import android.util.Log


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)
        Slidr.attach(this)

        boardId = intent.getStringExtra("boardId")
        articleId = intent.getStringExtra("articleId")

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

        scrollView = findViewById(R.id.articleScrollView)

        val writeReplyButton = findViewById<Button>(R.id.writeReplyButton)
        val selectStickerButton = findViewById<Button>(R.id.openStickerDialogButton)

        val writeReplyArea = findViewById<CardView>(R.id.writeReplyArea)

        val mRecyclerView = findViewById<RecyclerView>(R.id.reply_recycler_view)
        mRecyclerView.isNestedScrollingEnabled = false
        val mLayoutManager = LinearLayoutManager(this)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        mRecyclerView.layoutManager = mLayoutManager

        myAdapter = ReplyRecyclerAdapter(viewArticleHolder)

        mRecyclerView.adapter = myAdapter

        passwordPreferences = SecurePreferences(this)
        var cookies = HashMap<String, String>()

        isLoggedIn = passwordPreferences!!.getString("userName", "").isNotBlank()

        if (isLoggedIn) {
            sharedPreferences = getSharedPreferences("cookie_jar", Context.MODE_PRIVATE)
            sharedPreferences!!.all.forEach { t, k -> cookies.put(t, k as String) }
        }

        writeReplyButton.setOnClickListener { view ->
            val replyText = writeReplyEdit!!.text.toString()
            if (replyText.isBlank()) {
                Toast.makeText(this, "댓글 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                cookies.clear()
                sharedPreferences!!.all.forEach { t, k -> cookies.put(t, k as String) }
                boardFetcher.writeReply(boardId, articleId, "0", replyText, cookies) { success, error, newCookies ->
                    runOnUiThread {
                        val editor = sharedPreferences!!.edit()
                        newCookies.forEach { t, u -> editor.putString(t, u) }
                        editor.apply()
                        if (success) {
                            Snackbar.make(view, "댓글을 등록했습니다", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show()
                            loadReplys(cookies) {
                                nickNameText!!.text = it.nickname
                                timeText!!.text = it.time
                                viewCountText!!.text = "조회수 " + it.viewCount.toString()
                                replysText!!.text = "댓글 " + viewArticleHolder.size
                                likeCountText!!.text = "❤︎ " + it.likes.toString()
                                contentText!!.htmlText = it.rawHTML
                                myAdapter!!.notifyDataSetChanged()
                                writeReplyEdit!!.setText("")

                                scrollView!!.fullScroll(View.FOCUS_DOWN)
                            }
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
            nickNameText!!.text = it.nickname
            timeText!!.text = it.time
            viewCountText!!.text = "조회수 " + it.viewCount.toString()
            replysText!!.text = "댓글 " + viewArticleHolder.size
            likeCountText!!.text = "❤︎ " + it.likes.toString()
            contentText!!.htmlText = it.rawHTML
            if (it.profileImageUrl.isNotBlank())
                Picasso.get()
                    .load(it.profileImageUrl)
                    .noFade()
                    .into(articleProfileImgae)
            myAdapter!!.notifyDataSetChanged()

            if (!isLoggedIn) {
                writeReplyArea.visibility = View.GONE
            }
        }



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
            } else -> return false
        }
    }

    override fun sendStickerSelectResult(csrfToken: String, item: StickerInfo?) {
        if (item == null) return

        val cookies = getCookieFromSharedPrefs(sharedPreferences!!)
        boardFetcher.writeStickerReply(boardId, articleId, "0", csrfToken, item, cookies)  { success, error ->
            if (success) {
                Snackbar.make(writeReplyEdit!!, "댓글을 등록했습니다", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
                loadReplys(cookies) {
                    nickNameText!!.text = it.nickname
                    timeText!!.text = it.time
                    viewCountText!!.text = "조회수 " + it.viewCount.toString()
                    replysText!!.text = "댓글 " + viewArticleHolder.size
                    likeCountText!!.text = "❤︎ " + it.likes.toString()
                    contentText!!.htmlText = it.rawHTML
                    myAdapter!!.notifyDataSetChanged()
                    writeReplyEdit!!.setText("")

                    scrollView!!.fullScroll(View.FOCUS_DOWN)
                }
            } else {
                Snackbar.make(writeReplyEdit!!, "댓글 등록에 실패했습니다: $error", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
            }
        }
    }
}
