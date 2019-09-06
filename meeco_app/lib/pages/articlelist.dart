import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart' as p2r;

import '../types.dart';
import '../functions.dart';

class ArticleListPage extends StatelessWidget {
  String boardId;
  String boardName; 

  ArticleListPage(Pair<String, String> boardInfo) {
    boardId = boardInfo.left;
    boardName = boardInfo.right;
  }

  @override
  Widget build(BuildContext context) {
    return PlatformScaffold(
      appBar: PlatformAppBar(title: PlatformText(boardName)),
      body: ListWidget(boardId: boardId),
    );
  }
}

class ListWidget extends StatefulWidget {
  final String boardId;
  ListWidget({Key key, @required this.boardId}): super(key: key);

  @override
  _ListWidgetState createState() => new _ListWidgetState(this.boardId);
}

class _ListWidgetState extends State<ListWidget> {
  String boardId;

  var items = List<NormalRowInfo>();
  var present = 0;
  var pageNum = 1;
  var isLoading = false;
  p2r.RefreshController _refreshController = p2r.RefreshController(initialRefresh: true);

  _ListWidgetState(this.boardId);

  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
    new GlobalKey<RefreshIndicatorState>();

  @override
  void initState() {
    super.initState();
    loadPage();
  }

  Future<Null> loadPage() async {
    var newItems = await Fetcher.fetchNormal(this.boardId, '', this.pageNum);
    this.items.addAll(newItems);
    setState(() {
      print('New items: ${newItems.length}');
      present += newItems.length;
      isLoading = false;
    });
  }

  void loadMore() {
    if (isLoading) return;
    isLoading = true;
    pageNum++;
    loadPage();
  }

  Future<Null> _onRefresh() async {
    items.clear();
    present = 0;
    pageNum = 1;
    await this.loadPage();
  }

  Future<Null> _onLoading() async { 

  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return p2r.SmartRefresher(
      enablePullUp: true,
      enablePullDown: true,
      header: p2r.WaterDropHeader(),
      footer: p2r.CustomFooter(
        builder: (context, mode) {
          Widget body;

          switch (mode) {
            case p2r.LoadStatus.idle:
            body = Text('Pull up load');
            break;
            // case p2r.LoadStatus.loading:
            // body = 
          }
        },
      ),
    );
  }

  // @override
  // Widget build(BuildContext context) {
  //   return new NotificationListener<ScrollNotification>(
  //     onNotification: (ScrollNotification scrollInfo) {
  //       if (scrollInfo.metrics.pixels == scrollInfo.metrics.maxScrollExtent) loadMore();
  //     },
  //     child: RefreshIndicator(
  //       key: _refreshIndicatorKey,
  //       onRefresh: _refresh,
  //       child: new ListView.builder(
  //       itemCount: present,
  //       itemBuilder: (context, index) {
  //         return ListTile(title: PlatformText(present > 0 ? items[index].title : ''));
  //       },
  //     ),
  //     ),
  //   );
  // }
}