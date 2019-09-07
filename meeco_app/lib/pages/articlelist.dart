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
      appBar: PlatformAppBar(title: Text(boardName)),
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

  Route _createRoute(Widget page) {
    return PageRouteBuilder(
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        return child;
      }
    );
  }

  Widget _determineTrailing(NormalRowInfo row) {
    if (row.replyCount > 0) { 
      var icon;
      if (Theme.of(context).platform == TargetPlatform.iOS) {
        icon = CupertinoIcons.conversation_bubble;
      } else {
        icon = Icons.chat_bubble;
      }

      return Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          Icon(icon, size: 16.0, color: Colors.black),
          Text(row.replyCount.toString())
        ],
      );
    } else {
      return Text('');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: <Widget>[
        p2r.SmartRefresher(
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
                body = Text('Pull up load');
                break;
                case p2r.LoadStatus.loading:
                body = CupertinoActivityIndicator();
                break;
                case p2r.LoadStatus.failed:
                body = Text('Failed to load! Click to retry');
                break;
                case p2r.LoadStatus.canLoading:
                body = Text('Release to load more');
                break;
                case p2r.LoadStatus.noMore:
                body = Text('End of board');
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
          child: ListView.separated(
            separatorBuilder: (context, index) => Divider(
              height: 0.0,
              color: Colors.grey,
            ),
            itemBuilder: (context, index) {
              var row = items[index];
              return ListTile(
                title: RichText(
                  text: TextSpan(
                    text: '',
                    children: [
                      TextSpan(
                        text: present > 0 ? '${row.category} ' : '',
                        style: present > 0 ? TextStyle(color: Fetcher.hexToColor('#${row.categoryColor}'), fontSize: 16.0) : null,
                      ),
                      TextSpan(
                        text: present > 0 ? row.title : '',
                        style: TextStyle(fontSize: 16.0, color: Colors.black),
                      ),
                    ]
                  )
                ),
                subtitle: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(row.nickname),
                    Text(' · '),
                    Text(row.time)
                  ],
                ),
                trailing: _determineTrailing(row),
                onTap: () {
                  Navigator.of(context).push(_createRoute(ArticlePage(articleRow: row)));
                },
              );
            },
            itemCount: present,
          ),
        ),
        Align(
          alignment: Alignment.bottomRight,
          child: Padding(
            padding: EdgeInsets.all(20),
            child: FloatingActionButton(
              child: Icon(Icons.create),
              backgroundColor: Colors.blue,
              onPressed: () {},
            ),
          ),
        )
      ],
    );
  }
}