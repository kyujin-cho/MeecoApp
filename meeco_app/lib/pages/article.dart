import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:flutter_speed_dial/flutter_speed_dial.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';
import 'package:meeco_app/functions.dart';
import 'package:meeco_app/types.dart';

class ArticlePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    Pair<String, String> arguments = ModalRoute.of(context).settings.arguments;

    return ArticleWidget(boardId: arguments.left, articleId: arguments.right);
  }
}

class ArticleWidget extends StatefulWidget {
  final String boardId;
  final String articleId;

  ArticleWidget({
    Key key, 
    @required this.boardId,
    @required this.articleId
  }): super(key: key);
  
  @override
  _ArticleState createState() => new _ArticleState(boardId: boardId, articleId: articleId);
}

class _ArticleState extends State<ArticleWidget> {
  final String boardId;
  final String articleId;
  ArticleInfo article;

  @override
  void initState() {
    super.initState();

    _loadHtml();
    print('Html Loaded');
  }

  _ArticleState({
    @required this.boardId,
    @required this.articleId
  });

  @override
  Widget build(BuildContext context) {
    return PlatformScaffold(
      backgroundColor: Colors.white,
      appBar: PlatformAppBar(title: PlatformText(article != null ? article.title : '')),
      body: Stack(
        children: <Widget>[
          SingleChildScrollView(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.start,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                ListTile(
                  leading: CircleAvatar(
                    radius: 20,
                    backgroundImage: article != null ? NetworkImage(article.profileImageUrl) : AssetImage('assets/ic_meeco_icon.png')
                  ),
                  contentPadding: EdgeInsets.symmetric(horizontal: 10.0),
                  title: Text(
                    article != null ? article.title : '',
                    style: TextStyle(fontSize: 24.0, fontWeight: FontWeight.bold),
                  ),
                  subtitle: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(article != null ? article.nickname : ''),
                      Text(' · '),
                      Text(article != null ? article.time : '')
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
                    '댓글 ${article != null ? article.replies.length : ''} 개',
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
                ),
                Container(
                  height: 80,
                )
              ],
            ),
          ),
          Align(
            alignment: Alignment.bottomRight,
            child: Padding(
              padding: EdgeInsets.all(34),
              child: SpeedDial(
                marginRight: 0,
                marginBottom: 2,
                animatedIcon: AnimatedIcons.menu_close,
                animatedIconTheme: IconThemeData(size: 22.0),
                overlayOpacity: 0,
                tooltip: 'Menu',
                heroTag: 'speed-dial-hero-tag',
                children: [
                  SpeedDialChild(
                    child: Icon(Icons.reply_all),
                    backgroundColor: Colors.blue,
                    label: 'Write reply',
                    onTap: () => {}
                  ),
                  SpeedDialChild(
                    child: Icon(Icons.thumb_up),
                    backgroundColor: Colors.red,
                    label: 'Like',
                    onTap: () => {}
                  )
                ],
              ),
            )
          )
        ],
      ),
      iosContentPadding: true,
    );
  }

  _loadHtml() async {
    print('Webview loaded');
    article = await Fetcher.fetchArticle(boardId: boardId, articleId: articleId);
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