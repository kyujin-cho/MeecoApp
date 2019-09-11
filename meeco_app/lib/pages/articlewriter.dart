import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:meeco_app/functions.dart';
import 'package:uuid/uuid.dart';
import 'package:webview_flutter/webview_flutter.dart';

class ArticleWriterPage extends StatelessWidget {
  final String boardId; 
  final String articleId; 

  ArticleWriterPage({Key key, @required this.boardId, this.articleId}): super(key: key);

  @override
  Widget build(BuildContext context) {
    return ArticleWriter(boardId: boardId, articleId: articleId);
  }
}

class ArticleWriter extends StatefulWidget {
  final String boardId; 
  final String articleId; 

  ArticleWriter({Key key, @required this.boardId, this.articleId}): super(key: key);

  @override
  _ArticleWriterState createState() => _ArticleWriterState(boardId: boardId, articleId: articleId);
}

class _ArticleWriterState extends State<ArticleWriter> {
  final String boardId; 
  final String articleId; 

  final Completer<WebViewController> _controller = Completer<WebViewController>();
  final Completer<String> _editorSequence = Completer<String>();
  final Completer<String> _csrfToken = Completer<String>();

  String _targetSrl;
  String _title;
  String _articleHTML;

  _ArticleWriterState({Key key, @required this.boardId, this.articleId});

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

  @override
  Widget build(BuildContext context) {
    Fetcher.getImageUploadRequiredData(boardId).then((data) {
      _editorSequence.complete(data['editorSequence']);
      _csrfToken.complete(data['csrfToken']);
    });

    return PlatformScaffold(
      appBar: PlatformAppBar(
        title: Text(articleId == null ? '새 글 작성' : '글 편집', style: TextStyle(color: Colors.black)),
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
              JavascriptChannel(name: 'FTpostImage', onMessageReceived: _onFileUploadRequest)
            ].toSet(),
            onWebViewCreated: (controller) {
              _controller.complete(controller);
            },
          );
        }
      ),
    );
  }
}