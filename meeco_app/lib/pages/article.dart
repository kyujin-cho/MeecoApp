import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:meeco_app/functions.dart';
import 'package:meeco_app/types.dart';
import 'package:overlay_container/overlay_container.dart';
import 'package:responsive_container/responsive_container.dart';
import 'package:webview_flutter/webview_flutter.dart';

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
  WebViewController _controller;

  _ArticleState(this.articleRow);
  @override
  Widget build(BuildContext context) {
    Orientation currentOrientation = MediaQuery.of(context).orientation;

    return PlatformScaffold(
      appBar: PlatformAppBar(title: PlatformText(articleRow.boardName)),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.max,
        children: <Widget>[
          OverlayContainer(
            asWideAsParent: true,
            show: true,
            child: Container(
              height: 40.0,
              child: PlatformText(articleRow.title),
            )
          ),

          Container(
            height: 40.0,
            child: PlatformText(articleRow.title),
          ),
          Expanded(
            child: ListView(
              shrinkWrap: false,
              scrollDirection: Axis.vertical,
              physics: BouncingScrollPhysics(),
              children: <Widget>[
                ResponsiveContainer(
                  widthPercent: 100,
                  heightPercent: currentOrientation == Orientation.portrait ? 30 : 60,
                  alignment: Alignment(0, 0),
                  margin: EdgeInsets.all(0),
                  padding: EdgeInsets.all(0),
                  child: WebView(
                    initialUrl: '',
                    onWebViewCreated: (controller) {
                      _controller = controller;
                      _loadHtml();
                    },
                  ),
                )
              ],
            ),
          )
        ],
      ),
    );
  }

  _loadHtml() async {
    print('Webview loaded');
    article = await Fetcher.fetchArticle(boardId: articleRow.boardId, articleId: articleRow.articleId);
    _controller.loadUrl(Uri.dataFromString(
      Fetcher.getStyledHTMLForArticle(rawHtml: article.rawHTML, color: '#ffffff'),
      mimeType: 'text/html',
      encoding: Encoding.getByName('UTF-8')
    ).toString());
    print('Article loaded');
  }
}