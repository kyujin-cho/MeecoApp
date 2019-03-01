package com.kyujin.meeco

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import com.github.irshulx.Editor
import com.github.irshulx.EditorListener

class ArticleEditorActivity : AppCompatActivity() {
    var editor: Editor? = null
    val boardFetcher = BoardFetcher()
    var titleText: EditText? = null
    var currentBoard = ""
    var sharedPreferences: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_editor)

        currentBoard = intent!!.getStringExtra("boardId")
        titleText = findViewById(R.id.articleTitleText)
        editor = findViewById(R.id.articleEditor)
        editor!!.setLineSpacing(0.5F)
        editor!!.editorListener = MyEditorListener()
        editor!!.render()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences("cookie_jar", Context.MODE_PRIVATE)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.new_article_menu, menu)
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
                boardFetcher.writeArticle(currentBoard, title, article, cookies) { success, error, articleId, newCookies ->
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

    inner class MyEditorListener: EditorListener {
        override fun onUpload(image: Bitmap?, uuid: String?) {
            if (image == null || uuid == null) {
                createSnackbar("알 수 없는 오류가 발생하였습니다", titleText!!)
            }
            val cookies = getCookieFromSharedPrefs(sharedPreferences!!)
            boardFetcher.uploadImage(
                this@ArticleEditorActivity.applicationContext,
                currentBoard,
                image!!,
                uuid!!,
                cookies
            ) { success, error, url, newCookies ->
                if (success) {
                    editor!!.onImageUploadComplete(url, uuid)
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
}
