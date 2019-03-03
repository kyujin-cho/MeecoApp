package com.kyujin.meeco

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpUpload
import com.github.kittinunf.fuel.json.responseJson
import com.google.gson.annotations.SerializedName
import com.helger.css.ECSSVersion
import com.helger.css.reader.CSSReaderDeclarationList
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.http.Headers
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

interface ImageUploadInterface {
    @retrofit2.http.Multipart
    @POST("/")
    @Headers(
        "Accept: application/json, text/javascript, */*; q=0.01",
        "Referer: https://meeco.kr/index.php?mid=free&act=dispBoardWrite",
        "User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1",
        "X-Requested-With: XMLHttpRequest",
        "Host: meeco.kr"
    )
    fun uploadImage(
        @Header("Cookie") cookies: String,
        @Header("X-CSRF-Token") csrfToken: String,
        @PartMap file: Map<String, @JvmSuppressWildcards RequestBody>
    ): Call<ImageResponse>
}

class ImageResponse {
    @SerializedName("chunk_status")
    var chunkStatus = 0
    @SerializedName("file_srl")
    var fileSrl = 0
    @SerializedName("file_size")
    var fileSize = 0
    @SerializedName("direct_download")
    var directDownload = ""
    @SerializedName("source_filename")
    var fileName = ""
    @SerializedName("upload_target_srl")
    var targetSrl = 0
    @SerializedName("download_url")
    var downloadUrl = ""
    @SerializedName("error")
    var error = 0
    @SerializedName("message")
    var message = ""
}

class BoardFetcher {
    fun fetchNormal(boardId: String, category: String, pageNum: Int, f: (articles: ArrayList<NormalRowInfo>) -> Unit) {
        operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)
        var url = "https://meeco.kr/index.php?mid=$boardId&page=$pageNum"
        if (category.isNotBlank()) url += "&category=$category"
        url
            .httpGet()
            .responseString { request, response, result ->
                val obj = Jsoup.parse(result.get())
                val hasCategory = obj.select(".bt_ctg").isNotEmpty()
                val articles = ArrayList<NormalRowInfo>()
                var categories = ArrayList<Pair<String, String>>()
                if (!obj.select("div.list_document div.ldd").isEmpty()) {
                    if (hasCategory) {
                        categories = ArrayList(obj.select("div.list_category > ul > li")
                            .filter { it.select("a").attr("href").split("/").contains("category") }
                            .map {
                            Pair<String, String>(
                                it.select("a").attr("href").split("/").last(),
                                it.text()
                            )
                        })
                        categories.add(0, Pair("", "전체"))
                    }
                    obj.selectFirst("div.list_document div.ldd").select("li").forEach {
                        if (it.select("span.list_title").isEmpty()) return@forEach
                        var category = ""
                        var categoryColor = ""
                        val titleAnchor = it.selectFirst("a.list_link")
                        val infoDiv = it.selectFirst("div.list_info").children()
                        var replyCount = 0
                        var articleId = ""

                        if (hasCategory) {
                            category = it.selectFirst("span.list_ctg").text()
                            val styleStr = it.selectFirst("span.list_ctg").attr("style")
                            if (styleStr.isNotBlank()) categoryColor = styleStr.split("#")[1]
                            else categoryColor = "616BAF"

                        }
                        if (it.select("a.list_cmt").isNotEmpty()) {
                            Log.i("FetchNormal", "\"${it.selectFirst("a.list_cmt").text()}\"")
                            replyCount = it.selectFirst("a.list_cmt").text().toInt()
                        }

                        when (titleAnchor.attr("href")) {
                            in Regex("/$boardId/[0-9]+") -> {
                                articleId = titleAnchor.attr("href").split("/").last()
                            }
                            else -> {
                                articleId = titleAnchor.attr("href").split("=").last()
                            }
                        }

                        articles.add(
                            NormalRowInfo(
                                boardId,
                                obj.selectFirst("li.list_bt_board.active > a").text(),
                                articleId,
                                category,
                                categoryColor,
                                categories,
                                titleAnchor.attr("title"),
                                infoDiv[0].text(),
                                infoDiv[1].text(),
                                infoDiv[2].text().toInt(),
                                replyCount,
                                it.select("span.list_title > span.list_icon2.image").isNotEmpty(),
                                it.select("span.list_title > span.list_icon2.secret").isNotEmpty()
                            )
                        )
                    }
                }
                f(articles)
            }
    }

    fun fetchGalleryList(boardId: String, category: String, pageNum: Int, f: (images: ArrayList<GalleryRowInfo>) -> Unit) {
        operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)
        val urlRegex = """background-image ?: ?url\((https?:\/\/meeco\.kr\/[^\)]+\));""".toRegex()
        var url = "https://meeco.kr/index.php?mid=$boardId&page=$pageNum"
        if (category.isNotBlank()) url += "&category=$category"

        url
            .httpGet()
            .responseString { request, response, result ->
                val obj = Jsoup.parse(result.get())
                val hasCategory = obj.select(".bt_ctg").isNotEmpty()
                val articles = ArrayList<GalleryRowInfo>()
                var categories = ArrayList<Pair<String, String>>()

                if (obj.select("div.list_d.ldb").isNotEmpty()) {
                    if (hasCategory) {
                        categories = ArrayList(obj.select("div.list_category > ul > li")
                            .filter { it.select("a").attr("href").split("/").contains("category") }
                            .map {
                                Pair<String, String>(
                                    it.select("a").attr("href").split("/").last(),
                                    it.text()
                                )
                            })
                        categories.add(0, Pair("", "전체"))
                    }

                    obj.select("div.list_d.ldb > ul > li").forEach {
                        Log.i("GalleryList", it.text())
                        if (it.select("div.list_title").isEmpty()) return@forEach
                        var category = ""
                        var categoryColor = ""
                        val titleAnchor = it.selectFirst("a.list_link")
                        val infoDiv = it.selectFirst("div.list_info").children()
                        var replyCount = 0
                        var articleId = ""
                        var tnUrl = ""
                        if (it.select("div.ldb_thumb").isNotEmpty()) {
                            val css = it.selectFirst("div.ldb_thumb").attr("style")
                            tnUrl = urlRegex.find(css)!!.groups[1]!!.value
                        }

                        if (hasCategory) {
                            category = it.selectFirst("div.list_ctg").text()
                            val styleStr = it.selectFirst("div.list_ctg").attr("style")
                            if (styleStr.isNotBlank()) categoryColor = styleStr.split("#")[1]
                            else categoryColor = "616BAF"

                        }
                        if (it.select("a.ldg_cmt").isNotEmpty()) {
                            Log.i("FetchNormal", "\"${it.selectFirst("a.ldg_cmt").text()}\"")
                            replyCount = it.selectFirst("a.ldg_cmt").text().toInt()
                        }

                        when (titleAnchor.attr("href")) {
                            in Regex("/$boardId/[0-9]+") -> {
                                articleId = titleAnchor.attr("href").split("/").last()
                            }
                            else -> {
                                articleId = titleAnchor.attr("href").split("=").last()
                            }
                        }
                        articles.add(
                            GalleryRowInfo(
                                boardId,
                                obj.selectFirst("li.list_bt_board.active > a").text(),
                                articleId,
                                tnUrl,
                                category,
                                categoryColor,
                                categories,
                                titleAnchor.attr("title"),
                                infoDiv[0].text(),
                                it.selectFirst("div.list_summary").text(),
                                infoDiv[1].text(),
                                infoDiv[2].text().toInt(),
                                replyCount,
                                if (infoDiv.size > 3) infoDiv[3].text().toInt() else 0
                            )
                        )
                    }
                }

                f(articles)
            }
    }

    fun fetchArticle(boardId: String, articleId: String, cookies: HashMap<String, String>, f: (article: ArticleInfo, replys: ArrayList<ReplyInfo>, newCookies: HashMap<String, String>) -> Unit) {
        var cookieString = ""
        cookieString = generateCookieString(cookies)

        "https://meeco.kr/$boardId/$articleId"
            .httpGet()
            .header("Cookie", cookieString)
            .responseString { request, response, result ->
                val newCookies = HashMap<String, String>()
                val obj = Jsoup.parse(result.get())
                val hasCategory = obj.select(".bt_ctg").isNotEmpty()
                val infoLis = obj.select("div.atc_info > ul > li")
                var category = ""
                var categoryColor = ""
                val innerHTML = obj.selectFirst("div.atc_body > div.xe_content")
                val replys = obj.select("div.cmt_list > article.cmt_unit")
                var informationName = ""
                var informationValue = ""

                cookies.forEach { t, u -> newCookies[t] = u }
                updateCookie(newCookies, response.header("set-cookie"))

                if (obj.select("div.atc_ex table tbody").isNotEmpty()) {
                    val row = obj.selectFirst("div.atc_ex table tbody tr")
                    informationName = row.selectFirst("th").text()
                    if (row.select("td a").isNotEmpty()) {
                        informationValue = row.selectFirst("td a").attr("href")
                    } else {
                        informationValue = row.selectFirst("td").text()
                    }
                }

                if (obj.select("div.cmt_list").isNotEmpty()) {
                    obj.selectFirst("div.cmt_list").select("img").forEach {
                        if (it.attr("src").startsWith("//")) it.attr("src", "https:" + it.attr("src"))
                    }
                }

                innerHTML.select("img").forEach {
                    if (it.attr("src").startsWith("//")) it.attr("src", "https:" + it.attr("src"))
                }

                if (hasCategory) {
                    category = obj.selectFirst("span.atc_ctg").text()
                    categoryColor = obj.selectFirst("span.atc_ctg > a").attr("style").split("#")[1]
                }
                val replyList = ArrayList<ReplyInfo>()
                replys.forEach {
                    it.select("div.xe_content a").forEach {
                        if (it.attr("href").startsWith("https://meeco.kr/index.php?mid=sticker&sticker_srl=")) {
                            val cssString = it.attr("style")
                            val aDeclList = CSSReaderDeclarationList.readFromString(cssString, ECSSVersion.CSS30)
                            var stickerUrl = ""
                            if (aDeclList != null) {
                                aDeclList.forEach {
                                    if (it.property == "background-image") stickerUrl = it.expressionAsCSSString
                                }
                                it.html("""<img src="sticker://${stickerUrl.substring(4, stickerUrl.length - 1)}" style="width: 300px; height: 300px;">""")
                            }
                        }
                    }
                    replyList.add(ReplyInfo(
                        it.id().replace("comment_", ""),
                        boardId,
                        it.select("div.pf_wrap > span.writer").isNotEmpty(),
                        articleId,
                        if (it.select("img.pf_img").isNotEmpty()) it.selectFirst("img.pf_img").attr("src") else "",
                        it.selectFirst("span.nickname").text(),
                        it.selectFirst("span.date").text(),
                        it.selectFirst("div.xe_content").text(),
                        it.selectFirst("span.cmt_vote_up").text().toInt(),
                        if (it.select("div.cmt_to").isNotEmpty()) it.selectFirst("div.cmt_to").text() else "",
                        it.select("div.xe_content").html()
                    ))
                }

                f(ArticleInfo(
                    boardId,
                    articleId,
                    category,
                    categoryColor,
                    obj.selectFirst("header.atc_hd > h1 > a").text(),
                    if (cookieString.isNotBlank()) obj.selectFirst("div.atc_info > span.nickname > a").text() else obj.selectFirst("div.atc_info > span.nickname").text(),
                    infoLis[0].text(),
                    infoLis[1].text().toInt(),
                    infoLis[2].text().toInt(),
                    if (obj.select("article div.atc_info img.pf_img").isNotEmpty()) obj.selectFirst("article div.atc_info img.pf_img").attr("src") else "",
                    obj.selectFirst("button.bt_atc_vote").selectFirst("span.num").text().toInt(),
                    if (obj.select("div.atc_sign_body").isNotEmpty()) obj.selectFirst("div.atc_sign_body").text() else "",
                    obj.selectFirst("div.atc_body > div.xe_content").html(),
                    informationName, informationValue
                ), replyList, newCookies)
            }
    }

    fun tryLogin(userName: String, password: String, f: (success: Boolean, error: String, cookies: HashMap<String, String>?, uid: String) -> Unit) {
        val cookies = HashMap<String, String>()

        "https://meeco.kr/index.php?mid=index&act=dispMemberLoginForm"
            .httpGet()
            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1")
            .header("Referer", "https://meeco.kr/")
            .responseString { request, response, result ->
                val obj = Jsoup.parse(result.get())
                val CSRF = obj.selectFirst("meta[name=\"csrf-token\"]").attr("content")

                val formData = hashMapOf<String, String>(
                    "error_return_url" to "/index.php?mid=mini&act=dispMemberLoginForm",
                    "mid" to "mini",
                    "vid" to "",
                    "ruleset" to "@login",
                    "success_return_url" to "https://meeco.kr/mini",
                    "act" to "procMemberLogin",
                    "xe_validator_id" to obj.selectFirst("form.ff input[name=\"xe_validator_id\"]").attr("value"),
                    "user_id" to userName,
                    "password" to password,
                    "_rx_csrf_token" to CSRF,
                    "keep_signed" to "Y"
                ).toList()
                updateCookie(cookies, response.header("set-cookie"))


                "https://meeco.kr/index.php"
                    .httpPost(formData)
                    .header("Cookie", generateCookieString(cookies))
                    .responseString { postRequest, postResponse, postResult ->
                        val obj = Jsoup.parse(postResult.get())
                        if (obj.select("div.recheck-pass > div.message.error").isNotEmpty()) {
                            f(false, obj.selectFirst("div.recheck-pass > div.message.error").text(), null, "")
                        } else {
                            val uid = """member_srl=([0-9]+)""".toRegex()
                                .find(obj.select("div.mb_area.logged > a")[0]
                                .attr("href"))!!.destructured.component1()
                            updateCookie(cookies, postResponse.header("set-cookie"))

                            f(true, "", cookies, uid)
                        }
                    }
            }
    }

    fun fetchUser(uid: String, cookieJar: SharedPreferences, f: (userData: UserInfo, cookies: HashMap<String, String>) -> Unit) {
        val cookies = getCookieFromSharedPrefs(cookieJar)
        try {
            "https://meeco.kr/index.php?mid=index&act=dispMemberInfo&member_srl=$uid"
                .httpGet()
                .header("Cookie", generateCookieString(cookies))
                .responseString { request, response, result ->
                    updateCookie(cookies, response.header("set-cookie"))

                    val obj = Jsoup.parse(result.get())
                    val table = obj.select("div.mb_table > table > tbody > tr")
                    Log.i("FetchUser", "FetchUser done")
                    f(UserInfo(
                        table[0].selectFirst("td").text(),
                        table[2].selectFirst("td").text(),
                        table[1].selectFirst("td").text(),
                        obj.selectFirst("div.mb_pf > span.pf > img").attr("src")
                    ), cookies)
                }
        } catch (e: FuelError) {
            Log.i("FetchUser", "FetchUser failed")
            e.printStackTrace()
        }
    }

    fun writeReply(boardId: String, articleId: String, replyId: String, replyContent: String, cookies: HashMap<String, String>, f: (success: Boolean, error: String?, newCookies: HashMap<String, String>) -> Unit) {
        "https://meeco.kr/$boardId/$articleId"
            .httpGet()
            .header("Cookie", generateCookieString(cookies))
            .responseString { request, response, result ->
                updateCookie(cookies, response.header("set-cookie"))
                val obj = Jsoup.parse(result.get())
                val CSRF = obj.selectFirst("meta[name=\"csrf-token\"]").attr("content")
                val replyContentAsHTML = replyContent.split("\n").map { "<p>$it</p>" }.joinToString("")
                val formDataMap = hashMapOf<String, String>(
                    "_filter" to "insert_comment",
                    "error_return_url" to "/$boardId/$articleId",
                    "mid" to boardId,
                    "document_srl" to articleId,
                    "comment_srl" to "0",
                    "_rx_csrf_token" to CSRF,
                    "use_editor" to "Y",
                    "use_html" to "Y",
                    "module" to "board",
                    "act" to "procBoardInsertComment",
                    "_rx_ajax_compat" to "XMLRPC",
                    "vid" to "",
                    "content" to replyContentAsHTML
                )

                if (replyId.isNotBlank()) {
                    formDataMap["parent_srl"] = replyId
                }

                "https://meeco.kr"
                    .httpPost(formDataMap.toList())
                    .header("Cookie", generateCookieString(cookies))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1")
                    .header("Referer", "https://meeco.kr/")
                    .header("X-CSRF-Token", CSRF)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .responseJson { request, response, result ->
                        val jsonObject = result.get().obj()
                        if (jsonObject.getInt("error") == 0) f(true, "", cookies)
                        else f(false, jsonObject.getString("error"), cookies

                        )
                    }
            }
    }

    fun writeStickerReply(boardId: String, articleId: String, replyId: String, csrfToken: String, sticker: StickerInfo, cookies: HashMap<String, String>, f: (success: Boolean, error: String?) -> Unit) {
        val data =
            """<?xml version="1.0" encoding="utf-8" ?>
                <methodCall>
                    <params>
                        <_filter><![CDATA[insert_comment]]></_filter>
                        <error_return_url><![CDATA[/$boardId/$articleId]]></error_return_url>
                        <mid><![CDATA[free]]></mid>
                        <document_srl><![CDATA[$articleId]]></document_srl>
                        <parent_srl><![CDATA[0]]></parent_srl>
                        <content><![CDATA[{@sticker:${sticker.stickerId}|${sticker.stickerFileId}}]]></content>
                        <use_html><![CDATA[Y]]></use_html>
                        <module><![CDATA[board]]></module>
                        <act><![CDATA[procBoardInsertComment]]></act>
                    </params>
                </methodCall>""".trimIndent()

        "https://meeco.kr"
            .httpPost()
            .body(data)
            .header("Cookie", generateCookieString(cookies))
            .header("Content-Type", "text/plain")
            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1")
            .header("Referer", "https://meeco.kr/")
            .header("X-CSRF-Token", csrfToken)
            .header("X-Requested-With", "XMLHttpRequest")
            .response { request, response, result ->
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(ByteArrayInputStream(result.get()))

                val root = doc.documentElement
                if (root.getElementsByTagName("error").item(0).textContent == "0") {
                    f(true, "")
                } else {
                    f(false, root.getElementsByTagName("message").item(0).textContent)
                }
            }

    }

    fun fetchStickerList(cookies: HashMap<String, String>, f: (stickers: ArrayList<StickerRowInfo>, csrfToken: String, newCookies: HashMap<String, String>) -> Unit) {
//        page	1
//        module	sticker
//        act	getCommentStickerList
//        _rx_ajax_compat	JSON
        "https://meeco.kr"
            .httpGet()
            .header("Cookie", generateCookieString(cookies))
            .responseString { request, response, result ->
                updateCookie(cookies, response.header("set-cookie"))
                val obj = Jsoup.parse(result.get())
                val CSRF = obj.selectFirst("meta[name=\"csrf-token\"]").attr("content")

                "https://meeco.kr"
                    .httpPost(hashMapOf(
                        "page" to "1",
                        "module" to "sticker",
                        "act" to "getCommentStickerList",
                        "_rx_ajax_compat" to "JSON",
                        "_rx_csrf_token" to CSRF,
                        "vid" to ""
                    ).toList())
                    .header("Cookie", generateCookieString(cookies))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1")
                    .header("Referer", "https://meeco.kr/")
                    .header("X-CSRF-Token", CSRF)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .responseJson { request, response, result ->
                        val stickerList = ArrayList<StickerRowInfo>()
                        val arr = result.get().obj().getJSONArray("sticker")
                        for (i in 0 until arr.length()) {
                            val jsonObject = arr[i] as JSONObject
                            stickerList.add(
                                StickerRowInfo(
                                    stickerName = jsonObject.getString("title"),
                                    stickerId = jsonObject.getString("sticker_srl"),
                                    mainImageUrl = jsonObject.getString("main_image")
                                )
                            )
                        }

                        f(stickerList, CSRF, cookies)
                    }
            }
    }

    fun fetchSticker(stickerId: String, csrfToken: String, cookies: HashMap<String, String>, f: (stickers: ArrayList<StickerInfo>, newCookies: HashMap<String, String>) -> Unit) {
        "https://meeco.kr"
            .httpPost(hashMapOf(
                "sticker_srl" to stickerId,
                "module" to "sticker",
                "act" to "getStickerElemList",
                "_rx_ajax_compat" to "JSON",
                "_rx_csrf_token" to csrfToken,
                "vid" to ""
            ).toList())
            .header("Cookie", generateCookieString(cookies))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1")
            .header("Referer", "https://meeco.kr/")
            .header("X-CSRF-Token", csrfToken)
            .header("X-Requested-With", "XMLHttpRequest")
            .responseJson { request, response, result ->
                updateCookie(cookies, response.header("set-cookie"))
                val stickerImageList = ArrayList<StickerInfo>()
                val arr = result.get().obj().getJSONArray("stickerImage")

                for (i in 0 until arr.length()) {
                    val jsonObject = arr[i] as JSONObject
                    stickerImageList.add(
                        StickerInfo(
                            stickerId = stickerId,
                            stickerFileId = jsonObject.getString("sticker_file_srl"),
                            name = jsonObject.getString("name"),
                            url = "https://meeco.kr" + jsonObject.getString("url").substring(1)
                        )
                    )
                }

                f (stickerImageList, cookies)
            }
    }

    fun toRequestBody(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }

    fun uploadImage(context: Context, boardId: String, image: Bitmap, targetSrl: String?, fileName: String, cookies: HashMap<String, String>, f: (success: Boolean, error: String, targetSrl: String, url: String, newCookies: HashMap<String, String>) -> Unit) {
        val file = File(context.cacheDir, fileName + ".png")
        file.createNewFile()
        val bos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 0, bos)
        val bitmapData = bos.toByteArray()

        val fos = FileOutputStream(file)
        fos.write(bitmapData)
        fos.flush()
        fos.close()

        val nonce = "T${System.currentTimeMillis()}.${Math.random()}"

        val editorSeqRegex = "xe_editor_sequence: ([0-9]+),".toRegex()

        "https://meeco.kr/index.php?mid=$boardId&act=dispBoardWrite"
            .httpGet()
            .header("Cookie", generateCookieString(cookies))
            .responseString { request, response, result ->
                updateCookie(cookies, response.header("set-cookie"))
                val obj = Jsoup.parse(result.get())
                val (editorSequence) = editorSeqRegex.find(result.get())!!.destructured
                val CSRF = obj.selectFirst("meta[name=\"csrf-token\"]").attr("content")

                val map: HashMap<String, RequestBody> = HashMap<String, RequestBody>()
                map["editor_sequence"] = toRequestBody(editorSequence)
                map["upload_target_srl"] = toRequestBody(targetSrl?:"")
                map["mid"] = toRequestBody(boardId)
                map["act"] = toRequestBody("procFileUpload")
                map["nonce"] = toRequestBody(nonce.substring(0..(nonce.length-1)))
                map["""Filedata"; filename="$fileName.png"""] = RequestBody.create(MediaType.parse("image/png"), file)

                updateCookie(cookies, response.header("set-cookie"))
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://meeco.kr/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val service = retrofit.create(ImageUploadInterface::class.java)
                val call = service.uploadImage(
                    cookies = generateCookieString(cookies),
                    csrfToken = CSRF,
                    file = map
                )

                call.enqueue(object: Callback<ImageResponse> {
                    override fun onResponse(call: Call<ImageResponse>, response: Response<ImageResponse>) {
                        val newCookies = response.headers().values("set-cookie")
                        updateCookie(cookies, newCookies)

                        if (response.errorBody() != null) {
                            val errorBody = response.errorBody()!!.string()
                            f(false, "Errorbody Null", "", "", cookies)
                            Log.i("ImageUpload", errorBody)
                            return
                        }
                        val body = response.body()
                        if (body == null) {
                            f(false, "NPE", "", "", cookies)
                            return
                        }
                        if (body.error == 0) f(true, "", body.targetSrl.toString(),"https://img.meeco.kr${body.downloadUrl}", cookies)
                        else f(false, body.message, "", "", cookies)
                    }

                    override fun onFailure(call: Call<ImageResponse>, t: Throwable) {
                        t.printStackTrace()
                        f(false, t.message ?: "",  "","", cookies)
                    }
                })
            }
    }

    fun writeArticle(boardId: String, title: String, targetSrl: String?, html: String, cookies: HashMap<String, String>, f: (success: Boolean, error: String, articleId: String, newCookies: HashMap<String, String>) -> Unit) {
        "https://meeco.kr/index.php?mid=$boardId&act=dispBoardWrite"
            .httpGet()
            .header("Cookie", generateCookieString(cookies))
            .responseString { request, response, result ->
                updateCookie(cookies, response.header("set-cookie"))
                val obj = Jsoup.parse(result.get())
                val CSRF = obj.selectFirst("meta[name=\"csrf-token\"]").attr("content")
                "https://meeco.kr"
                    .httpPost(hashMapOf(
                        "_filter" to "insert",
                        "error_return_url" to "/index.php?mid=$boardId&act=dispBoardWrite",
                        "act" to "procBoardInsertDocument",
                        "mid" to boardId,
                        "content" to html,
                        "title" to title,
                        "comment_status" to "ALLOW",
                        "_rx_csrf_token" to CSRF,
                        "use_editor" to "Y",
                        "use_html" to "Y",
                        "module" to "board",
                        "_rx_ajax_compat" to "XMLRPC",
                        "vid" to "",
                        "document_srl" to (targetSrl?:"0"),
                        "_saved_doc_message" to "자동 저장된 글이 있습니다. 복구하시겠습니까? 글을 다 쓰신 후 저장하면 자동 저장 본은 사라집니다."
                    ).toList())
                    .header("Cookie", generateCookieString(cookies))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1")
                    .header("Referer", "https://meeco.kr/")
                    .header("X-CSRF-Token", CSRF)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .responseJson { request, response, result ->
                        val obj = result.get().obj()
                        if (obj.getInt("error") == 0) {
                            f(true, "", obj.getString("document_srl"), cookies)
                        } else {
                            f(false, obj.getString("message"), "", cookies)
                        }
                    }
            }
    }
}