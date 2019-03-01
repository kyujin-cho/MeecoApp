package com.kyujin.meeco

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SharedMemory
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ScrollingView
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.*
import com.jakewharton.processphoenix.ProcessPhoenix
import com.securepreferences.SecurePreferences
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    val boardFetcher = BoardFetcher()
    val viewArticleHolder = ArrayList<NormalRowInfo>()
    val galleryArticleHolder = ArrayList<GalleryRowInfo>()
    var myAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null
    var recyclerView: RecyclerView? = null
    var currentPage = 1
    var currentBoard = "mini"
    var currentCategory = ""
    var spinner: Spinner? = null
    var passwordPreferences: SecurePreferences? = null
    var sharedPreferences: SharedPreferences? = null
    var loginCredentials: Pair<String, String>? = null
    var uid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            val intent = Intent(this, ArticleEditorActivity::class.java)
            intent.putExtra("boardId", currentBoard)
            startActivityForResult(intent, RequestCode.NEW_ARTICLE)
        }

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        val scrollView = findViewById<RecyclerView>(R.id.normal_recycler_view)
        scrollView.addOnScrollListener(ArticleScrollListener())

        nav_view.setNavigationItemSelectedListener(this)
        passwordPreferences = SecurePreferences(this)



        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val menu = navigationView.menu
        val headerView = navigationView.getHeaderView(0)
        val nav_login = menu.findItem(R.id.nav_login)

        val navBarUserNameText = headerView.findViewById<TextView>(R.id.navBarUserNameText)
        val navBarNickNameText = headerView.findViewById<TextView>(R.id.navBarNickNameText)
        val navBarProfileImg = headerView.findViewById<ImageView>(R.id.navBarProfileIcon)

        if (passwordPreferences!!.getString("userName", "").isNotBlank()) {
            val loginCredentials = Pair(
                passwordPreferences!!.getString("userName", "")!!,
                passwordPreferences!!.getString("password", "")!!
            )
            sharedPreferences = getSharedPreferences("cookie_jar", Context.MODE_PRIVATE)
            boardFetcher.tryLogin(loginCredentials.l, loginCredentials.r) { success, error, cookies, uid ->
                if (success && cookies != null) {
                    Log.i("MainActivity", "Successfully logged in as " + uid)
                    runOnUiThread {
                        val editor = sharedPreferences!!.edit()
                        cookies.forEach { t, u ->
                            editor.putString(t, u)
                        }
                        editor.apply()
                        this.loginCredentials = loginCredentials
                        this.uid = uid
                    }

                    boardFetcher.fetchUser(uid, sharedPreferences!!) { userData, newCookies ->
                        runOnUiThread {
                            val editor = sharedPreferences!!.edit()
                            newCookies.forEach { t, u ->
                                editor.putString(t, u)
                            }
                            editor.apply()
                            navBarUserNameText.text = userData.userName
                            navBarNickNameText.text = userData.nickName

                            nav_login.title = "로그아웃"
                            Picasso.get()
                                .load(userData.profileImageUrl)
                                .into(navBarProfileImg)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "알 수 없는 오류가 발생하여 로그인 정보가 초기화됩니다.", Toast.LENGTH_SHORT).show()
                        val editor = passwordPreferences!!.edit()
                        editor.putString("userName", "")
                        editor.putString("password", "")
                        editor.commit()
                    }
                }
            }
        }

        val mRecyclerView = findViewById<RecyclerView>(R.id.normal_recycler_view)
        mRecyclerView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        mRecyclerView.layoutManager = mLayoutManager

        myAdapter = NormalRecyclerAdapter(this, viewArticleHolder)

        mRecyclerView.adapter = myAdapter
        recyclerView = mRecyclerView

        swipeRefreshLayout = findViewById(R.id.articleSwipeRefreshLayout)
        swipeRefreshLayout!!.setOnRefreshListener {
            currentPage = 1
            if (currentBoard == "gallery") {
                fetchGalleryArticles {
                    galleryArticleHolder.clear()
                    it.forEach { x -> galleryArticleHolder.add(x) }
                    swipeRefreshLayout!!.isRefreshing = false
                    myAdapter!!.notifyDataSetChanged()
                    title = it[0].boardName
                }
            } else {
                fetchArticles {
                    viewArticleHolder.clear()
                    it.forEach { x -> viewArticleHolder.add(x) }
                    swipeRefreshLayout!!.isRefreshing = false
                    myAdapter!!.notifyDataSetChanged()
                    title = it[0].boardName
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        this.spinner = menu.findItem(R.id.category_spinner).actionView as Spinner
        Log.i("MainActivity", "Options Menu created")
        fetchArticles {
            title = it[0].boardName
            it.forEach { x -> viewArticleHolder.add(x) }
            myAdapter!!.notifyDataSetChanged()

            runOnUiThread {
                val adapter = CategorySpinnerAdapter(this, R.id.category_spinner, it[0].categories)
                adapter.setDropDownViewResource(R.layout.category_spinner_layout)
                spinner!!.onItemSelectedListener = CategorySpinnerSelectListener()
                spinner!!.adapter = adapter
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_mini -> changeBoard("mini")
            R.id.nav_news -> changeBoard("news")
            R.id.nav_free -> changeBoard("free")
            R.id.nav_market -> changeBoard("market")
            R.id.nav_contact -> changeBoard("contact")
            R.id.nav_notice -> changeBoard("notice")
            R.id.nav_gallery -> changeBoard("gallery")
            R.id.nav_login -> {
                if (this.loginCredentials != null) {
                    val editor = passwordPreferences!!.edit()
                    editor.putString("userName", "")
                    editor.putString("password", "")
                    editor.apply()
//                    ProcessPhoenix.triggerRebirth(this)
                } else {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        // Handle navigation view item clicks here.
//        when (item.itemId) {
//            R.id.nav_camera -> {
//                // Handle the camera action
//            }
//            R.id.nav_gallery -> {
//
//            }
//            R.id.nav_slideshow -> {
//
//            }
//            R.id.nav_manage -> {
//
//            }
//            R.id.nav_share -> {
//
//            }
//            R.id.nav_send -> {
//
//            }
//        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun fetchArticles(f: (ArrayList<NormalRowInfo>) -> Unit) {
        boardFetcher.fetchNormal(currentBoard, currentCategory, currentPage) {
            runOnUiThread {
                f(it)
            }
        }
    }

    fun fetchGalleryArticles(f: (ArrayList<GalleryRowInfo>) -> Unit) {
        boardFetcher.fetchGalleryList(currentBoard, currentCategory, currentPage) {
            runOnUiThread {
                f(it)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1 -> articleWriteResult(resultCode, data)
        }
    }

    fun articleWriteResult(resultCode: Int, data: Intent?) {
        when (resultCode) {

        }
    }

    fun changeBoard(boardId: String) {
        if (boardId == currentBoard) return
        if (boardId != "gallery") {
            if (currentBoard == "gallery") {
                myAdapter = NormalRecyclerAdapter(this, viewArticleHolder)
                recyclerView!!.adapter = myAdapter
            }

            currentBoard = boardId
            currentPage = 1
            currentCategory = ""
            viewArticleHolder.clear()
            fetchArticles {
                title = it[0].boardName
                it.forEach { item -> viewArticleHolder.add(item) }
                myAdapter!!.notifyDataSetChanged()

                if (it[0].categories.isNotEmpty()) {
                    val adapter = CategorySpinnerAdapter(this, R.id.category_spinner, it[0].categories)
                    adapter.setDropDownViewResource(R.layout.category_spinner_layout)
                    spinner!!.adapter = adapter
                    spinner!!.onItemSelectedListener = CategorySpinnerSelectListener()
                    spinner!!.visibility = View.VISIBLE
                } else {
                    spinner!!.adapter = null
                    spinner!!.onItemSelectedListener = null
                    spinner!!.visibility = View.GONE
                }
            }

            swipeRefreshLayout = findViewById(R.id.articleSwipeRefreshLayout)
            swipeRefreshLayout!!.setOnRefreshListener {
                currentPage = 1
                fetchArticles {
                    viewArticleHolder.clear()
                    it.forEach { x -> viewArticleHolder.add(x) }
                    swipeRefreshLayout!!.isRefreshing = false
                    myAdapter!!.notifyDataSetChanged()
                    title = it[0].boardName
                }
            }
        } else {
            myAdapter = GalleryRecyclerAdapter(this, galleryArticleHolder)

            recyclerView!!.adapter = myAdapter

            currentBoard = "gallery"
            currentPage = 1
            currentCategory = ""
            galleryArticleHolder.clear()

            fetchGalleryArticles {
                title = it[0].boardName
                it.forEach { item -> galleryArticleHolder.add(item) }
                myAdapter!!.notifyDataSetChanged()

                if (it[0].categories.isNotEmpty()) {
                    val adapter = CategorySpinnerAdapter(this, R.id.category_spinner, it[0].categories)
                    adapter.setDropDownViewResource(R.layout.category_spinner_layout)
                    spinner!!.adapter = adapter
                    spinner!!.onItemSelectedListener = CategorySpinnerSelectListener()
                    spinner!!.visibility = View.VISIBLE
                } else {
                    spinner!!.adapter = null
                    spinner!!.onItemSelectedListener = null
                    spinner!!.visibility = View.GONE
                }

            }

        }
    }

    inner class ArticleScrollListener: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (!recyclerView.canScrollVertically(1)) {
                swipeRefreshLayout!!.isRefreshing = true
                currentPage++
                if (currentBoard == "gallery") {
                    this@MainActivity.fetchGalleryArticles {
                        if (it.isNotEmpty()) {
                            it.forEach { x -> galleryArticleHolder.add(x) }
                            this@MainActivity.myAdapter!!.notifyDataSetChanged()
                            title = it[0].boardId
                        }
                    }
                } else {
                    this@MainActivity.fetchArticles {
                        if (it.isNotEmpty()) {
                            it.forEach { x -> viewArticleHolder.add(x) }
                            this@MainActivity.myAdapter!!.notifyDataSetChanged()
                            title = it[0].boardName
                        }
                    }
                }
                swipeRefreshLayout!!.isRefreshing = false
            }
        }
    }


    inner class CategorySpinnerSelectListener : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {

        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val currentItem = this@MainActivity.spinner!!.getItemAtPosition(position) as Pair<String, String>
            currentCategory = currentItem.l
            currentPage = 1
            swipeRefreshLayout!!.isRefreshing = true
            if (currentBoard == "gallery") {
                galleryArticleHolder.clear()
                fetchGalleryArticles {
                    it.forEach {
                        item -> galleryArticleHolder.add(item)
                    }
                    myAdapter!!.notifyDataSetChanged()
                    swipeRefreshLayout!!.isRefreshing = false
                }
            } else {
                viewArticleHolder.clear()
                fetchArticles {
                    it.forEach {
                        item -> viewArticleHolder.add(item)
                    }
                    myAdapter!!.notifyDataSetChanged()
                    swipeRefreshLayout!!.isRefreshing = false
                }
            }
        }
    }
}
