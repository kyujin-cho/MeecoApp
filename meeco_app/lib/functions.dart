import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import 'package:cookie_jar/cookie_jar.dart';
import 'package:flutter/services.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:html/parser.dart' show parse;
import 'package:meeco_app/types.dart';
import 'package:path_provider/path_provider.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:yaml/yaml.dart';
import 'package:oauth2/oauth2.dart' as oauth2;

import 'exceptions.dart';

const PROXY_ENABLED = true;
const PROXY_IP = '172.17.142.251';
const PROXY_PORT = '9090';

Future<Uri> waitForUrl(BuildContext ctx, Uri authUrl) {
  var completer = new Completer();
  var webView = WebView(
    initialUrl: 'https://meeco.kr${authUrl.path}',
    navigationDelegate: (navigationRequest) {
      if (navigationRequest.url.startsWith('http://meeco.app/')) {
        completer.complete(Uri.parse(navigationRequest.url));
        return NavigationDecision.prevent;
      }
      return NavigationDecision.navigate;
    },
  );
  var alertDialog = new PlatformAlertDialog(
    title: Text('OAuth2 Authentication'),
    content: webView
  );
  showPlatformDialog(context: ctx, builder: (ctx) => alertDialog);
  
  return completer.future;
}

class Fetcher {
  oauth2.Client authClient;
  Dio dio;

  Fetcher() {
    dio = new Dio();
    dio.options.headers = {
      "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1",
      "Referer": "https://meeco.kr/"
    };
  }

  static Future<oauth2.Client> authenticate(BuildContext ctx) async { 
    var rawCredentials = await rootBundle.loadString('assets/credentials.yml');
    var appInfo = loadYaml(rawCredentials);
    
    final authEndpoint = Uri.parse('https://meeco.kr/?module=devcenter&act=dispDevcenterAuthorize&client_id=${appInfo['client_id']}');
    final tokenEndpoint = Uri.parse('https://meeco.kr/?module=devcenter&act=token');

    final redirectUrl = Uri.parse('http://meeco.app/oauth2-redirect');
    final appDocDir = await getApplicationDocumentsDirectory();
    final credFile = new File('${appDocDir.absolute.path}/oauth2/creds.json');

    if (await credFile.exists()) {
      var creds = new oauth2.Credentials.fromJson(await credFile.readAsString());
      return new oauth2.Client(creds, identifier: appInfo['client_id'], secret: appInfo['client_secret']);
    }

    var grant = new oauth2.AuthorizationCodeGrant(appInfo['client_id'], authEndpoint, tokenEndpoint);
    var authResultUrl = await waitForUrl(ctx, grant.getAuthorizationUrl(redirectUrl));
    return await grant.handleAuthorizationResponse(authResultUrl.queryParameters);
  }

  Dio prepareDio() {
    var dio = new Dio();
    dio.options.headers = {
      "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1",
      "Referer": "https://meeco.kr/"
    };
  }

  Future<List<NormalRowInfo>> fetchNormal(String boardId, String category, int pageNum) async {
    print("fetchNormal called");
    var dio = this.dio;
    var url = 'https://meeco.kr/index.php?mid=$boardId&page=$pageNum';
    if (category.length > 0) url += '&category=$category';

    var response = await dio.get(url);
    var document = parse(response.data.toString());

    if (document.querySelector("div.list_document div.ldd") == null) return List<NormalRowInfo>();

    var hasCategory = document.getElementsByClassName('bt_ctg').length > 0;
    var articles = List<NormalRowInfo>();
    var categories = List<Pair<String, String>>();
    var boardName = document.querySelector('li.list_bt_board.active > a').text;

    if (hasCategory) {
      categories = document.querySelectorAll('div.list_category > ul > li')
                    .where((obj) => obj.getElementsByTagName('a')[0].attributes['href'].split('/').contains('category'))
                    .map((obj) => Pair<String, String>(obj.getElementsByTagName('a')[0].attributes['href'].split('/').last, obj.text.trim()))
                    .toList();
      categories.insert(0, Pair("", "전체"));
    }

    for (var obj in document.querySelector('div.list_document div.ldd').getElementsByTagName('li')) {
      if (obj.querySelector('span.list_title') == null) continue;

      var category = '';
      var categoryColor = '';
      var titleAnchor = obj.querySelector("a.list_link");
      var infoDiv = obj.querySelector("div.list_info").children;
      var replyCount = 0;
      var articleId = '';

      if (hasCategory) {
        category = obj.querySelector("span.list_ctg").text.trim();
        var styleStr = obj.querySelector("span.list_ctg").attributes['style'];
        if (styleStr != null && styleStr.length > 0) categoryColor = styleStr.split("#")[1];
        else categoryColor = "616BAF";
      }

      if (obj.querySelector("a.list_cmt") != null) {
        replyCount = int.parse(obj.querySelector("a.list_cmt").text);
      }

      if (new RegExp('/$boardId/[0-9]+').hasMatch(titleAnchor.attributes['href'])) {
        articleId = titleAnchor.attributes['href'].split('/').last;
      } else {
        articleId = titleAnchor.attributes['href'].split('=').last;
      }

      articles.add(NormalRowInfo(
        boardId, boardName, articleId, category, categoryColor, categories, titleAnchor.attributes['title'],
        infoDiv[0].text.trim(), infoDiv[1].text.trim(), int.parse(infoDiv[2].text), replyCount,
        obj.querySelector('span.list_title > span.list_icon2.image') != null,
        obj.querySelector('span.list_title > span.list_icon2.secret') != null && infoDiv[0].text == '******'
      ));
    }

    return articles;
  }

  Future<ArticleInfo> fetchArticle({
    @required String boardId,
    @required String articleId
  }) async {
    var dio = this.dio;
    var response = await dio.get('https://meeco.kr/$boardId/$articleId');
    var document = parse(response.data.toString());

    var hasCategory = document.getElementsByClassName('bt_ctg').length > 0;
    var infoList = document.querySelectorAll('div.atc_info > ul > li');
    var innerHTML = document.querySelector('div.atc_body > div.xe_content');
    var replies = document.querySelectorAll('div.cmt_list > article.cmt_unit');

    var category = '';
    var categoryColor = '';
    var informationName = '';
    var informationValue = '';
    var articleWriterId = '';
    
    if (document.querySelector('div.atc_ex table tbody') != null) {
      var row = document.querySelector('div.atc_ex table tbody');
      informationName = row.children[0].text;

      if (row.querySelector('td a') != null) {
        informationValue = row.querySelector('td a').attributes['href'];
      } else {
        informationValue = row.children[1].text;
      }
    }

    if (document.querySelector('div.cmt_list') != null) {
      for (var img in document.querySelector('div.cmt_list').getElementsByTagName('img')) {
        if (img.attributes['src'].startsWith('//')) img.attributes['src'] = 'https:' + img.attributes['src'];
      }
    }

    for (var img in innerHTML.getElementsByTagName('img')) {
      if (img.attributes['src'].startsWith('//')) img.attributes['src'] = 'https:' + img.attributes['src'];
    }

    if (hasCategory) {
      category = document.querySelector("span.atc_ctg").text.trim();
      categoryColor = document.querySelector("span.atc_ctg > span").attributes['style'].split("#")[1];
    }

    if (document.querySelector('div.atc_info > span.nickname > a > span') != null) {
      articleWriterId = document.querySelector('div.atc_info > span.nickname > a > span').className.replaceAll('member_', '');
    }

    List<ReplyInfo> replyContainer = [];
    for (var reply in replies) {
      if (reply.querySelector('div.xe_content') == null) continue;
      for (var anchor in reply.querySelectorAll('div.xe_content a')) {
        if (!anchor.attributes['href'].startsWith('https://meeco.kr/index.php?mid=sticker&sticker_srl=')) continue;

        var cssString = anchor.attributes['style'];
        Map<String, String> ruleMap = {};
        for (var rule in cssString.split(';')) {
          for (var pos = 0; pos < rule.length; pos++) {
            if (rule[pos] == ':') {
              ruleMap[rule.substring(0, pos)] = rule.substring(pos, rule.length);
              break;
            }
          }
        }
        if (ruleMap.containsKey('background-image')) {
          var stickerUrl = ruleMap['background-image'];
          anchor.innerHtml = '<img src="${stickerUrl.substring(5, stickerUrl.length - 1)}" style="width: 300px; height: 300px;">';
        }
      }
      replyContainer.add(ReplyInfo(
        reply.id.replaceAll('comment_', ''), boardId, reply.querySelector('div.pf_wrap > span.writer') != null,
        articleId, reply.querySelector('img.pf_img') != null ? reply.querySelector('img.pf_img').attributes['src'] : '',
        reply.querySelector('span.nickname').text.trim(),
        document.getElementsByClassName('bt_logout').length > 0 ? reply.querySelector('span.nickname').className.replaceAll('nickname member_', '') : '',
        reply.querySelector('span.date').text, reply.querySelector('div.xe_content').text, 
        int.parse(reply.querySelector('span.cmt_vote_up').text), 
        reply.querySelector('div.cmt_to') != null ? reply.querySelector('div.cmt_to').text : '',
        reply.querySelector('div.xe_content').innerHtml
      ));
    }

    var profileImageUrl = '';
    if (document.querySelector("article > header.atc_hd > div.atc_info > span > img.pf_img") != null) {
      profileImageUrl = document.querySelector("article img.pf_img").attributes['src'];
    }

    return ArticleInfo(
      boardId,
      articleId,
      document.querySelector("li.list_bt_board").text,
      category,
      categoryColor,
      document.querySelector("header.atc_hd > h1 > a").text,
      document.getElementsByClassName('bt_logout').length > 0 ? document.querySelector("div.atc_info > span.nickname > a").text.trim() : document.querySelector("div.atc_info > span.nickname").text.trim(),
      articleWriterId,
      infoList[0].text,
      replyContainer,
      int.parse(infoList[1].text), profileImageUrl,
      document.querySelector("button.bt_atc_vote") != null ? int.parse(document.querySelector("button.bt_atc_vote").querySelector("span.num").text) : -1,
      document.querySelector("div.atc_sign_body") != null ? document.querySelector("div.atc_sign_body").text: '',
      document.querySelector("div.atc_body > div.xe_content").innerHtml,
      informationName, informationValue
    );
  }

  Future<Map<String, String>> getImageUploadRequiredData(String boardId) async {
    var dio = this.dio;
    var response = await dio.get('https://meeco.kr/index.php?mid=$boardId&act=dispBoardWrite');
    var document = parse(response.data.toString());
    var regex = RegExp('xe_editor_sequence: ([0-9]+)');
    var csrfToken = document.querySelector('meta[name="csrf-token"]').attributes['content'];

    if (!regex.hasMatch(response.data.toString())) throw GenericException('editorSequence not found');
    var editorSequence = regex.firstMatch(response.data.toString()).group(1);

    return {
      'editorSequence': editorSequence,
      'csrfToken': csrfToken
    };
  }

  Future<String> postArticle({
    @required String boardId,
    @required String title,
    @required String html,
    @required String csrf,
    String targetSrl,
    String categorySrl
  }) async {
    var dio = this.dio;
    var body = {
      "_filter": "insert",
      "error_return_url": "/index.php?mid=$boardId&act=dispBoardWrite",
      "act": "procBoardInsertDocument",
      "mid": boardId,
      "content": html,
      "title": title,
      "comment_status": "ALLOW",
      "_rx_csrf_token": csrf,
      "use_editor": "Y",
      "use_html": "Y",
      "module": "board",
      "_rx_ajax_compat": "XMLRPC",
      "vid": "",
      "document_srl": targetSrl != null ? targetSrl : '0',
      "_saved_doc_message": "자동 저장된 글이 있습니다. 복구하시겠습니까? 글을 다 쓰신 후 저장하면 자동 저장 본은 사라집니다."
    };
    if (categorySrl != null && categorySrl.length > 0) {
      body["category_srl"] = categorySrl;
    }
    var headers = {
      'X-CSRF-Token': csrf,
      'X-Requested-With': 'XMLHttpRequest'
    };

    var response = await dio.post('https://meeco.kr/', data: body, options: Options(
      headers: headers,
      responseType: ResponseType.json
    ));

    if (response.data['error'] != 0) throw GenericException('Server returned error: ${response.data['message']}');
    return response.data['document_srl'];
  }

  Future<Map<String, String>> uploadIamge({
    String fileName, 
    String imageDataAsBase64,
    String boardId,
    String targetSrl,
    String editorSequence,
    String csrfToken
  }) async {
    var dio = this.dio;
    var nonce = 'T${new DateTime.now().millisecondsSinceEpoch}.${Random().nextDouble()}';
    var image = base64.decode(imageDataAsBase64);

    var headers = {
      'X-CSRF-Token': csrfToken,
      'X-Requested-With': 'XMLHttpRequest'
    };
    var formData = FormData.from({
      'nonce': nonce,
      'act': 'procFileUpload',
      'mid': boardId,
      'editor_sequence': editorSequence,
      'Filedata': UploadFileInfo.fromBytes(image, fileName)
    });
    if (targetSrl != null) {
      formData['targetSrl'] = targetSrl;
    }

    var response = await dio.post('https://meeco.kr/', data: formData, options: Options(
      headers: headers,
      responseType: ResponseType.json
    ));

    var responseJson = jsonDecode(response.data.toString());

    print(responseJson['error']);
    if (responseJson['error'] != 0) throw GenericException('Got error while uploading');
    return {
      'url': 'https://img.meeco.kr${responseJson['download_url']}',
      'targetSrl': responseJson['upload_target_srl']
    };
  }

  Future<String> tryLogin({
    @required String username,
    @required String password,
    bool persist = true
  }) async {
    var dio = this.dio;
    var response = await dio.get('https://meeco.kr/index.php?mid=index&act=dispMemberLoginForm');

    var document = parse(response.data.toString());
    
    var ffForm = document.querySelector('form.ff');
    if (ffForm == null) {
      response = await dio.get('https://meeco.kr');
      document = parse(response.data.toString());
      var exp = new RegExp(r'member_srl=([0-9]+)');
      return exp.firstMatch(document.querySelector('div.mb_area.logged > a').attributes['href']).group(1);
    }
    var csrfToken = document.querySelector('meta[name="csrf-token"]').attributes['content'];
    var validator = ffForm.querySelector('input[name="xe_validator_id"]');

    var body = {
      "error_return_url": "/index.php?mid=mini&act=dispMemberLoginForm",
      "mid": "mini",
      "vid": "",
      "ruleset": "@login",
      "success_return_url": "https://meeco.kr/mini",
      "act": "procMemberLogin",
      "xe_validator_id": validator.attributes['value'],
      "user_id": username,
      "password": password,
      "_rx_csrf_token": csrfToken,
      "keep_signed": "Y"
    };
    response = await dio.post('https://meeco.kr/index.php', data: body, options: Options(
      headers: {
        'X-CSRF-Token': csrfToken
      },
      contentType: ContentType.parse("application/x-www-form-urlencoded"),
      followRedirects: false,
      validateStatus: (status) { return status < 500; }
    ));

    if (response.statusCode != 302) {
      document = parse(response.data.toString());
      throw LoginException(document.querySelector('div.message.error').text);
    }
    response = await dio.get('https://meeco.kr');
    document = parse(response.data.toString());
    var exp = new RegExp(r'member_srl=([0-9]+)');
    return exp.firstMatch(document.querySelector('div.mb_area.logged > a').attributes['href']).group(1);
  }

  String getStyledHTMLForArticle({String rawHtml, String color}) {
    return '''<!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body {
                      color: $color !important;
                      width: 100%;
                    }
                    img, iframe {
                      max-width: 100%;
                      height: auto;
                    }
                  </style>
                </head>
                <body>
                  $rawHtml
                </body>
            </html>
    ''';
  }

  Color hexToColor(String code) {
    return new Color(int.parse(code.substring(1, 7), radix: 16) + 0xFF000000);
  }
}