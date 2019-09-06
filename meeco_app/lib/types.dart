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
    // TODO: implement toString
    return "$boardName #$articleId { title = $title }";
  }
}