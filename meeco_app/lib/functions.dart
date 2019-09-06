import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import 'package:cookie_jar/cookie_jar.dart';
import 'package:html/parser.dart' show parse;
import 'package:meeco_app/types.dart';
import 'package:path_provider/path_provider.dart';

import 'exceptions.dart';

class Fetcher {
  static Future<Dio> prepareDio() async {
    var appDocDir = await getApplicationDocumentsDirectory();
    var dio = new Dio();
    dio.interceptors.add(CookieManager(PersistCookieJar(dir: appDocDir.absolute.path + '/dioCookie')));
    dio.options.headers = {
      "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1",
      "Referer": "https://meeco.kr/"
    };
    return dio;
  }

  static Future<List<NormalRowInfo>> fetchNormal(String boardId, String category, int pageNum) async {
    print("fetchNormal called");
    var dio = await prepareDio();
    var url = 'https://meeco.kr/index.php?mid=$boardId&page=$pageNum';
    if (category.length > 0) url += '&category=$category';

    var response = await dio.get(url);
    var document = parse(response.data.toString());

    var listDocument = document.getElementsByClassName('list_document');

    if (document.querySelector("div.list_document div.ldd") == null) return List<NormalRowInfo>();

    var hasCategory = document.getElementsByClassName('bt_ctg').length > 0;
    var articles = List<NormalRowInfo>();
    var categories = List<Pair<String, String>>();
    var boardName = document.querySelector('li.list_bt_board.active > a').text;

    if (hasCategory) {
      categories = document.querySelectorAll('div.list_category > ul > li')
                    .where((obj) => obj.getElementsByTagName('a')[0].attributes['href'].split('/').contains('category'))
                    .map((obj) => Pair<String, String>(obj.getElementsByTagName('a')[0].attributes['href'].split('/').last, obj.text))
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
        category = obj.querySelector("span.list_ctg").text;
        var styleStr = obj.querySelector("span.list_ctg").attributes['style'];
        if (styleStr.length > 0) categoryColor = styleStr.split("#")[1];
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
        infoDiv[0].text, infoDiv[1].text, int.parse(infoDiv[2].text), replyCount,
        obj.querySelector('span.list_title > span.list_icon2.image') != null,
        obj.querySelector('span.list_title > span.list_icon2.secret') != null && infoDiv[0].text == '******'
      ));
    }

    return articles;
  }

  static Future<String> tryLogin({
    @required String username,
    @required String password,
    bool persist = true
  }) async {
    var dio = persist ? await prepareDio() : new Dio();
    if (!persist) {
      dio.options.headers = {
        "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A402 Safari/604.1",
        "Referer": "https://meeco.kr/"
      };
    }
    var response = await dio.get('https://meeco.kr/index.php?mid=index&act=dispMemberLoginForm');

    var document = parse(response.data.toString());
    var csrfToken = document.querySelector('meta[name="csrf-token"]').attributes['content'];
    var body = {
      "error_return_url": "/index.php?mid=mini&act=dispMemberLoginForm",
      "mid": "mini",
      "vid": "",
      "ruleset": "@login",
      "success_return_url": "https://meeco.kr/mini",
      "act": "procMemberLogin",
      "xe_validator_id": document.querySelector('form.ff input[name="xe_validator_id"]').attributes['value'],
      "user_id": username,
      "password": password,
      "_rx_csrf_token": csrfToken,
      "keep_signed": "Y"
    };
    response = await dio.post('https://meeco.kr/index.php', data: body);
    document = parse(response.data.toString());
    if (document.querySelector('div.message.error') != null) {
      throw LoginException(document.querySelector('div.message.error').text);
    }
    var exp = new RegExp(r'member_srl=([0-9]+)');
    return exp.firstMatch(document.querySelector('div.mb_area.logged > a').attributes['href']).group(1);
  }
}