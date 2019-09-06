import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:meeco_app/pages/article.dart';
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

  Future<Null> _loadMore() async {
    if (isLoading) return;
    isLoading = true;
    pageNum++;
    await loadPage();
    _refreshController.loadComplete();
  }

  Future<Null> _onRefresh() async {
    items.clear();
    present = 0;
    pageNum = 1;
    await this.loadPage();
    _refreshController.refreshCompleted();
  }

  Route _createRoute(NormalRowInfo article) {
    return PageRouteBuilder(
      pageBuilder: (context, animation, secondaryAnimation) => ArticlePage(articleRow: article),
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        return child;
      }
    );
  }

  @override
  Widget build(BuildContext context) {
    return p2r.SmartRefresher(
      enablePullUp: true,
      enablePullDown: true,
      header: p2r.WaterDropHeader(
        complete: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const Icon(
                Icons.done,
                color: Colors.grey,
              ),
              Container(
                width: 15.0,
              ),
              Text(
                "Complete",
                style: TextStyle(color: Colors.grey),
              )
            ],
          ),
        failed: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const Icon(
                Icons.close,
                color: Colors.grey,
              ),
              Container(
                width: 15.0,
              ),
              Text("Failed", style: TextStyle(color: Colors.grey))
            ],
          )
      ),
      footer: p2r.CustomFooter(
        builder: (context, mode) {
          Widget body;

          switch (mode) {
            case p2r.LoadStatus.idle:
            body = PlatformText('Pull up load');
            break;
            case p2r.LoadStatus.loading:
            body = CupertinoActivityIndicator();
            break;
            case p2r.LoadStatus.failed:
            body = PlatformText('Failed to load! Click to retry');
            break;
            case p2r.LoadStatus.canLoading:
            body = PlatformText('Release to load more');
            break;
            case p2r.LoadStatus.noMore:
            body = PlatformText('End of board');
            break;
          }

          return Container(
            height: 55.0,
            child: Center(child: body),
          );
        },
      ),
      controller: _refreshController,
      onRefresh: _onRefresh,
      onLoading: _loadMore,
      child: ListView.builder(
        itemBuilder: (context, index) {
          return ListTile(
            title: PlatformText(present > 0 ? items[index].title : ''),
            onTap: () {
              Navigator.of(context).push(_createRoute(items[index]));
            },
          );
        },
        itemCount: present,
      ),
    );
  }
}