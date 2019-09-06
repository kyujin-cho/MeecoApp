import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class Pair<T1,T2> {
  T1 left;
  T2 right;

  Pair(this.left, this.right);
}

class NormalRowInfo {
  String boardId;
  String boardName;
  String articleId;
  String category;
  String categoryColor;
  List<Pair<String, String>> categories;
  String title;
  String nickname;
  String time;
  int viewCount;
  int replyCount;
  bool hasImage;
  bool isSecret;

  NormalRowInfo(this.boardId, this.boardName, this.articleId,
      this.category, this.categoryColor, this.categories, this.title,
      this.nickname, this.time, this.viewCount, this.replyCount, this.hasImage,
      this.isSecret);

  @override
  String toString() {
    return "$boardName #$articleId { title = $title }";
  }
}

class ArticleInfo {
  String boardId;
  String articleId;
  String boardName;
  String category;
  String categoryColor;
  String title;
  String nickname;
  String userId;
  String time;
  List<ReplyInfo> replies;
  int viewCount;
  String profileImageUrl;
  int likes;
  String signature;
  String rawHTML;
  String informationName;
  String informationValue;

  ArticleInfo(this.boardId, this.boardName, this.articleId,
    this.category, this.categoryColor, this.title, this.nickname, 
    this.userId, this.time, this.replies, this.viewCount, this.profileImageUrl,
    this.likes, this.signature, this.rawHTML, this.informationName, this.informationValue);
}

class ReplyInfo {
  String replyId;
  String boardId;
  bool isWriter;
  String articleId;
  String profileImageUrl;
  String nickname;
  String userId;
  String time;
  String replyContent;
  int likes;
  String replyTo;
  String rawHTML;

  bool selected = false;

  ReplyInfo(this.replyId, this.boardId, this.isWriter, this.articleId, 
            this.profileImageUrl, this.nickname, this.userId, this.time, 
            this.replyContent, this.likes, this.replyTo, this.rawHTML);
}