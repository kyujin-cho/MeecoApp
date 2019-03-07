package com.kyujin.meeco

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatDelegate
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import com.github.irshulx.Editor
import com.github.irshulx.EditorListener
import com.github.irshulx.models.EditorTextStyle
import top.defaults.colorpicker.ColorPickerPopup
import java.io.IOException


class ArticleEditorActivity : AppCompatActivity() {
    var editor: Editor? = null
    val boardFetcher = BoardFetcher()
    var titleText: EditText? = null
    var currentBoard = ""
    var targetSrl: String? = null
    var sharedPreferences: SharedPreferences? = null
    var spinner: Spinner? = null
    var currentCategory = ""
    var categorySrl = ""
    var categories = ArrayList<Pair<String, String>>()
    var selectedCategory = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        setContentView(R.layout.activity_article_editor)

        sharedPreferences = getSharedPreferences("cookie_jar", Context.MODE_PRIVATE)

        targetSrl = intent.getStringExtra("articleId")

        currentBoard = intent.getStringExtra("boardId")
        titleText = findViewById(R.id.articleTitleText)

        val categories = (intent.getSerializableExtra("categories") ?: ArrayList<Pair<String, String>>()) as ArrayList<Pair<String, String>>
        if (categories.size > 0) {
            this.categories = categories
        }


        editor = findViewById(R.id.articleEditor)
        editor!!.editorListener = MyEditorListener()

        title = "새 글 작성"

        findViewById<View>(R.id.action_h1).setOnClickListener { editor!!.updateTextStyle(EditorTextStyle.H1) }

        findViewById<View>(R.id.action_h2).setOnClickListener { editor!!.updateTextStyle(EditorTextStyle.H2) }

        findViewById<View>(R.id.action_h3).setOnClickListener { editor!!.updateTextStyle(EditorTextStyle.H3) }

        findViewById<View>(R.id.action_bold).setOnClickListener { editor!!.updateTextStyle(EditorTextStyle.BOLD) }

        findViewById<View>(R.id.action_Italic).setOnClickListener { editor!!.updateTextStyle(EditorTextStyle.ITALIC) }

        findViewById<View>(R.id.action_indent).setOnClickListener { editor!!.updateTextStyle(EditorTextStyle.INDENT) }

        findViewById<View>(R.id.action_blockquote).setOnClickListener { editor!!.updateTextStyle(EditorTextStyle.BLOCKQUOTE) }

        findViewById<View>(R.id.action_outdent).setOnClickListener { editor!!.updateTextStyle(EditorTextStyle.OUTDENT) }

        findViewById<View>(R.id.action_bulleted).setOnClickListener { editor!!.insertList(false) }

        findViewById<View>(R.id.action_unordered_numbered).setOnClickListener { editor!!.insertList(true) }

        findViewById<View>(R.id.action_hr).setOnClickListener { editor!!.insertDivider() }


        findViewById<View>(R.id.action_color).setOnClickListener {
            ColorPickerPopup.Builder(this@ArticleEditorActivity)
                .initialColor(Color.RED) // Set initial color
                .enableAlpha(true) // Enable alpha slider or not
                .okTitle("Choose")
                .cancelTitle("Cancel")
                .showIndicator(true)
                .showValue(true)
                .build()
                .show(findViewById<View>(android.R.id.content), object : ColorPickerPopup.ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        createSnackbar("Picked ${colorHex(color)}", titleText!!)
                        editor!!.updateTextColor(colorHex(color))
                    }

                    fun onColor(color: Int, fromUser: Boolean) {

                    }
                })
        }

        findViewById<View>(R.id.action_insert_image).setOnClickListener { editor!!.openImagePicker() }

        findViewById<View>(R.id.action_insert_link).setOnClickListener { editor!!.insertLink() }

        findViewById<View>(R.id.action_erase).setOnClickListener { editor!!.clearAllContents() }

        if (targetSrl != null) {
            boardFetcher.loadEditArticle(currentBoard, targetSrl!!, getCookieFromSharedPrefs(sharedPreferences!!)) { selectedCategory, title, html, csrfToken, newCookies ->
                commitCookies(newCookies, sharedPreferences!!.edit())
                this.selectedCategory = selectedCategory
                if (selectedCategory >= 0 && spinner != null) spinner!!.setSelection(selectedCategory)
                titleText!!.text = SpannableStringBuilder(title)
                editor!!.render(html)
            }
        } else  {
            editor!!.render()
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val typedValue = TypedValue()
        val theme = theme
        theme.resolveAttribute(R.attr.primaryTextColor, typedValue, true)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (categories.size > 0) {
            menuInflater.inflate(R.menu.new_article_categories_menu, menu)
            spinner = menu!!.findItem(R.id.category_spinner).actionView as Spinner
            val adapter = CategorySpinnerAdapter(this, R.id.category_spinner, categories)
            adapter.setDropDownViewResource(R.layout.category_spinner_layout)
            spinner!!.adapter = adapter
            spinner!!.onItemSelectedListener = CategorySpinnerSelectListener()
            spinner!!.visibility = View.VISIBLE

            if (selectedCategory >= 0) spinner!!.setSelection(selectedCategory)
        } else {
            menuInflater.inflate(R.menu.new_article_menu, menu)
        }
        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item == null) return false
        when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent()
                setResult(Activity.RESULT_CANCELED, intent)
                finish()
                return true
            }
            R.id.confirmWriteButton -> {
                val cookies = getCookieFromSharedPrefs(sharedPreferences!!)
                val title = titleText!!.text.toString()
                val article = editor!!.contentAsHTML
                if (article.isEmpty() || title.isEmpty()) {
                    createSnackbar("제목과 내용을 모두 입력해 주세요", titleText!!)
                    return true
                }
                boardFetcher.writeArticle(currentBoard, title, targetSrl, categorySrl, article, cookies) { success, error, articleId, newCookies ->
                    if (success) {
                        val intent = Intent()
                        intent.putExtra("articleId", articleId)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    } else {
                        createSnackbar("글 작성에 실패하였습니다 : $error", titleText!!)
                    }
                }
                return true
            }
            else -> return false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            editor!!.PICK_IMAGE_REQUEST -> {
                if (data == null || data.data == null) return
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        try {
                            val url = data.data
                            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, url)
                            editor!!.insertImage(bitmap)
                        } catch (e: IOException) {

                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    inner class MyEditorListener: EditorListener {
        override fun onUpload(image: Bitmap?, uuid: String?) {
            if (image == null || uuid == null) {
                createSnackbar("알 수 없는 오류가 발생하였습니다", titleText!!)
                return
            }
            val cookies = getCookieFromSharedPrefs(sharedPreferences!!)
            boardFetcher.uploadImage(
                this@ArticleEditorActivity.applicationContext,
                currentBoard,
                image,
                targetSrl,
                uuid,
                cookies
            ) { success, error, targetSrl, url, newCookies ->
                if (success) {
                    editor!!.onImageUploadComplete(url, uuid)
                    this@ArticleEditorActivity.targetSrl = targetSrl
                } else {
                    createSnackbar("이미지 업로드에 실패하였습니다 : $error", titleText!!)
                    editor!!.onImageUploadFailed(uuid)
                }
            }
        }
        override fun onRenderMacro(name: String?, props: MutableMap<String, Any>?, index: Int): View {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onTextChanged(editText: EditText?, text: Editable?) {

        }

    }

    inner class CategorySpinnerSelectListener : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {

        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val currentItem = this@ArticleEditorActivity.spinner!!.getItemAtPosition(position) as Pair<String, String>
            currentCategory = currentItem.r
            categorySrl = currentItem.l
        }
    }
}
