package com.kyujin.meeco

import android.text.Spanned

class NormalRowInfo(
    var boardId: String,
    var boardName: String,
    var articleId: String,
    var category: String,
    var categoryColor: String,
    var categories: ArrayList<Pair<String, String>>,
    var title: String,
    var nickname: String,
    var time: String,
    var viewCount: Int,
    var replyCount: Int,
    var hasImage: Boolean,
    var isSecret: Boolean
)

class GalleryRowInfo(
    var boardId: String,
    var boardName: String,
    var articleId: String,
    var thumbNailUrl: String,
    var category: String,
    var categoryColor: String,
    var categories: ArrayList<Pair<String, String>>,
    var title: String,
    var nickname: String,
    var preview: String,
    var time: String,
    var viewCount: Int,
    var replyCount: Int,
    var likeCount: Int
)

class ReplyInfo(
    var replyId: String,
    var boardId: String,
    var isWriter: Boolean,
    var articleId: String,
    var profileImageUrl: String,
    var nickname: String,
    var time: String,
    var replyContent: String,
    var likes: Int,
    var replyTo: String,
    var rawHTML: String
) {
    var selected = false
}

class ArticleInfo(
    var boardId: String,
    var articleId: String,
    var category: String,
    var categoryColor: String,
    var title: String,
    var nickname: String,
    var time: String,
    var viewCount: Int,
    var replyCount: Int,
    var profileImageUrl: String,
    var likes: Int,
    var signature: String,
    var rawHTML: String,
    var informationName: String,
    var informationValue: String
)

class UserInfo(
    var userName: String,
    var nickName: String,
    var email: String,
    var profileImageUrl: String
)

class StickerRowInfo(
    var stickerId: String,
    var stickerName: String,
    var mainImageUrl: String
)

class StickerInfo(
    var stickerId: String,
    var stickerFileId: String,
    var name: String,
    var url: String
)