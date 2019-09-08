import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:meeco_app/functions.dart';
import 'package:uuid/uuid.dart';
import 'package:webview_flutter/webview_flutter.dart';

class ArticleWriter extends StatelessWidget {
  final String boardId; 
  final String articleId; 

  final Completer<WebViewController> _controller = Completer<WebViewController>();
  final Completer<String> _editorSequence = Completer<String>();
  final Completer<String> _csrfToken = Completer<String>();

  String targetSrl;

  ArticleWriter(this.boardId, {this.articleId});

  void _onFileUploadRequest(JavascriptMessage message) async {
    var data = jsonDecode(message.message);
    var controller = await _controller.future;

    var editorSequence = await _editorSequence.future;
    var csrfToken = await _csrfToken.future;

    var upload = await Fetcher.uploadIamge(
      fileName: Uuid().v1().toUpperCase() + '.png',
      imageDataAsBase64: data['image'],
      boardId: boardId,
      targetSrl: targetSrl,
      editorSequence: editorSequence,
      csrfToken: csrfToken
    );
    targetSrl = upload['targetSrl'];

    controller.evaluateJavascript('window.onMessageReceive("${data['id']}", null, "${upload['url']}")');
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
        backgroundColor: Colors.white,
      ),
      body: Builder(
        builder: (context) {
          return WebView(
            initialUrl: 'ckeditor/editor.html',
            javascriptMode: JavascriptMode.unrestricted,
            javascriptChannels: [
              JavascriptChannel(name: 'postImage', onMessageReceived: _onFileUploadRequest)
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