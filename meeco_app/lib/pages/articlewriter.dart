import 'dart:async';
import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:meeco_app/functions.dart';
import 'package:uuid/uuid.dart';
import 'package:webview_flutter/webview_flutter.dart';

import '../types.dart';

class ArticleWriterArguments {
  final String boardId; 
  final String articleId; 
  final List<Pair<String, String>> categories;

  ArticleWriterArguments({@required this.boardId, @required this.categories, this.articleId});

}

class ArticleWriterPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    ArticleWriterArguments arguments = ModalRoute.of(context).settings.arguments;

    return ArticleWriter(boardId: arguments.boardId, articleId: arguments.articleId, categories: arguments.categories);
  }
}

class ArticleWriter extends StatefulWidget {
  final String boardId; 
  final String articleId; 
  final List<Pair<String, String>> categories;

  ArticleWriter({Key key, @required this.boardId, @required this.categories, this.articleId}): super(key: key);

  @override
  _ArticleWriterState createState() => _ArticleWriterState(boardId: boardId, articleId: articleId, categories: categories);
}

class _ArticleWriterState extends State<ArticleWriter> {
  final String boardId; 
  final String articleId; 
  final List<Pair<String, String>> categories;

  final Completer<WebViewController> _controller = Completer<WebViewController>();
  final Completer<String> _editorSequence = Completer<String>();
  final Completer<String> _csrfToken = Completer<String>();

  String _targetSrl;
  String _title = '';
  String _articleHTML = '';
  int _selectedCategoryIndex = 0;

  _ArticleWriterState({Key key, @required this.boardId, @required this.categories, this.articleId});

  @override
  void initState() {
    super.initState();
    print("HELLO");
    Fetcher.getImageUploadRequiredData(boardId).then((data) {
      _editorSequence.complete(data['editorSequence']);
      _csrfToken.complete(data['csrfToken']);
    });
  }

  void _onFileUploadRequest(JavascriptMessage message) async {
    var data = jsonDecode(message.message);
    var controller = await _controller.future;

    var editorSequence = await _editorSequence.future;
    var csrfToken = await _csrfToken.future;

    var upload = await Fetcher.uploadIamge(
      fileName: Uuid().v1().toUpperCase() + '.png',
      imageDataAsBase64: data['file'],
      boardId: boardId,
      targetSrl: _targetSrl,
      editorSequence: editorSequence,
      csrfToken: csrfToken
    );
    _targetSrl = upload['targetSrl'];

    controller.evaluateJavascript('window.onMessageReceive("${data['id']}", null, "${upload['url']}")');
  }

  void _onTitleChange(JavascriptMessage message) {
    _title = message.message;
  }

  void _onArticleChange(JavascriptMessage message) {
    _articleHTML = message.message;
  }

  void _onCategoryChange(JavascriptMessage message) {  
    _selectedCategoryIndex = int.parse(message.message);
  }

  Future<void> _writeArticle() async {
    if (_title.length == 0 || _articleHTML.length == 0) {
      return;
    }

    var categorySrl = (categories != null && categories.length > 0) ? categories[_selectedCategoryIndex+1].left : null;
    try {
      var documentSrl = await Fetcher.postArticle(
        boardId: boardId, title: _title, html: _articleHTML, 
        csrf: await _csrfToken.future, targetSrl: _targetSrl, categorySrl: categorySrl
      );
      Navigator.of(context).popAndPushNamed(
        '/article',
        arguments: Pair<String, String>(boardId, documentSrl) 
      );
    } catch (e) {
      throw e; // TODO: Implement Exception handling
    }
  }

  @override
  Widget build(BuildContext context) {
    return PlatformScaffold(
      appBar: PlatformAppBar(
        title: Text(articleId == null ? '새 글 작성' : '글 편집', style: TextStyle(color: Colors.black)),
        trailingActions: <Widget>[
          PlatformIconButton(
            androidIcon: Icon(Icons.send),
            iosIcon: Icon(CupertinoIcons.pencil),
            onPressed: _writeArticle,
          )
        ],
      ),
      body: Builder(
        builder: (context) {
          return WebView(
            initialUrl: 'ckeditor/editor.html',
            javascriptMode: JavascriptMode.unrestricted,
            debuggingEnabled: true,
            javascriptChannels: [
              JavascriptChannel(name: 'FTarticleChanged', onMessageReceived: _onArticleChange),
              JavascriptChannel(name: 'FTtitleChanged', onMessageReceived: _onTitleChange),
              JavascriptChannel(name: 'FTcategoryChanged', onMessageReceived: _onCategoryChange),
              JavascriptChannel(name: 'FTpostImage', onMessageReceived: _onFileUploadRequest)
            ].toSet(),
            onWebViewCreated: (controller) {
              _controller.complete(controller);
              if (categories != null && categories.length > 0) {
                var categoryList = categories.skip(1).map((item) => '"${item.right.toString()}"').toList();
                controller.evaluateJavascript('setTimeout(()=>{console.log("Category set");window.onSetCategories([${categoryList.join(', ')}]);},500)');
              }
            },
          );
        }
      ),
    );
  }
}