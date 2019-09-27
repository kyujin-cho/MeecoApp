import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';

import 'articlelist.dart';
import '../types.dart';


final Map<String, List<Pair<String, String>>> categories = {
  '커뮤니티': [
    Pair('news', 'IT 소식'), Pair('mini', '미니기기 / 음향'), Pair('free', '자유 게시판'), 
    Pair('gallery', '갤러리'), Pair('market', '장터 게시판'), Pair('humor', '유머 게시판')
  ],
  '운영 참여': [
    Pair('contact', '신고 / 건의'), Pair('notice', '공지사항'), Pair('Dispute', '분쟁 조정')
  ]
};

class MainPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    List<Widget> widgets = [];
    for (var key in categories.keys) {
      widgets.addAll(categories[key].map((pair) => 
        ListTile(
          title: PlatformText(pair.right),
          onTap: () {
            Navigator.of(context).pushNamed(
              '/articleList',
              arguments: pair
            );
          },
        )
      ).toList());
    }
    return PlatformScaffold(
      body: ListView(
        children: widgets,
      )
    );
  }
}
