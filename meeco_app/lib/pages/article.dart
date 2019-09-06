import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';
import 'package:meeco_app/functions.dart';
import 'package:meeco_app/types.dart';

class ArticlePage extends StatelessWidget {
  final NormalRowInfo articleRow;

  ArticlePage({Key key, @required this.articleRow}): super(key: key);
  
  @override
  Widget build(BuildContext context) {
    return ArticleWidget(articleRow: articleRow);
  }
}

class ArticleWidget extends StatefulWidget {
  final NormalRowInfo articleRow;

  ArticleWidget({Key key, @required this.articleRow}): super(key: key);
  
  @override
  _ArticleState createState() => new _ArticleState(articleRow);
}

class _ArticleState extends State<ArticleWidget> {
  final NormalRowInfo articleRow;
  ArticleInfo article;

  @override
  void initState() {
    super.initState();

    _loadHtml();
  }

  _ArticleState(this.articleRow);
  @override
  Widget build(BuildContext context) {
    return PlatformScaffold(
      appBar: PlatformAppBar(title: PlatformText(articleRow.boardName)),
      body: SingleChildScrollView(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            ListTile(
              leading: CircleAvatar(
                radius: 20,
                backgroundImage: NetworkImage(article != null ? article.profileImageUrl : '')
              ),
              contentPadding: EdgeInsets.symmetric(horizontal: 10.0),
              title: Text(
                articleRow.title,
                style: TextStyle(fontSize: 24.0, fontWeight: FontWeight.bold),
              ),
              subtitle: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(articleRow.nickname),
                  Text(' · '),
                  Text(articleRow.time)
                ],
              ),
            ),
            Divider(
              color: Colors.grey,
            ),
            HtmlWidget(
              article != null ? article.rawHTML : ''
            ),
            Divider(
              color: Colors.grey,
            ),
            Container(
              padding: EdgeInsets.symmetric(horizontal: 10.0),
              child: Text(
                '댓글 ${articleRow.replyCount} 개',
                style: TextStyle(color: Colors.grey),
              ),
            ),
            Divider(
              color: Colors.grey,
            ),
            ListView.separated(
              separatorBuilder: (context, index) => Divider(
                color: Colors.grey
              ),
              primary: false,
              shrinkWrap: true,
              itemBuilder: (context, index) => ReplyWidget(article != null && article.replies.length > 0 ? article.replies[index] : null),
              itemCount: article != null ? article.replies.length : 0,
            )
          ],
        ),
      )
    );
  }

  _loadHtml() async {
    print('Webview loaded');
    article = await Fetcher.fetchArticle(boardId: articleRow.boardId, articleId: articleRow.articleId);
    setState(() {
      
    });
  }
}

class ReplyWidget extends StatefulWidget {
  final ReplyInfo reply;
  ReplyWidget(this.reply);
  
  @override
  _ReplyState createState() => _ReplyState(this.reply);
}

class _ReplyState extends State<ReplyWidget> {
  final ReplyInfo reply;
  _ReplyState(this.reply);

  Widget _determineReplyTo(String replyTo) {
    if (replyTo.trim().length == 0) return Container();
    return Text(replyTo.trim(), style: TextStyle(color: Colors.grey));
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 10.0 + (reply.replyTo.length > 0 ? 15.0 : 0.0)),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(reply.nickname),
                Text(' · '),
                Text(reply.time.trim())
              ],
            ),
          ),
          _determineReplyTo(reply.replyTo),
          HtmlWidget(
            Fetcher.getStyledHTMLForArticle(rawHtml: reply.rawHTML, color: '#000000'),
            bodyPadding: EdgeInsets.symmetric(vertical: 10.0),
          )
        ],
      ),
    );
  }
}