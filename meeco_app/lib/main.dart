import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bloc/bloc.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';

import 'package:meeco_app/bloc/bloc.dart';
import 'package:meeco_app/bloc/event.dart';
import 'package:meeco_app/bloc/repository.dart';
import 'package:meeco_app/bloc/state.dart';
import 'package:meeco_app/functions.dart';
import 'package:meeco_app/pages/articlelist.dart';
import 'package:meeco_app/types.dart';

final Map<String, List<Pair<String, String>>> categories = {
  '커뮤니티': [
    Pair('news', 'IT 소식'), Pair('mini', '미니기기 / 음향'), Pair('free', '자유 게시판'), 
    Pair('gallery', '갤러리'), Pair('market', '장터 게시판'), Pair('humor', '유머 게시판')
  ],
  '운영 참여': [
    Pair('contact', '신고 / 건의'), Pair('notice', '공지사항'), Pair('Dispute', '분쟁 조정')
  ]
};


class SimpleBlocDelegate extends BlocDelegate {
  @override
  void onEvent(Bloc bloc, Object event) {
    super.onEvent(bloc, event);
    print(event);
  }

  @override
  void onTransition(Bloc bloc, Transition transition) {
    super.onTransition(bloc, transition);
    print(transition);
  }

  @override
  void onError(Bloc bloc, Object error, StackTrace stacktrace) {
    super.onError(bloc, error, stacktrace);
    print(error);
  }
}


void main() {
  BlocSupervisor.delegate = SimpleBlocDelegate();
  final userRepository = UserRepository();
  runApp(
    BlocProvider<AuthenticationBloc>(
      builder: (context) {
        return AuthenticationBloc(userRepository: userRepository)
          ..dispatch(AppStarted());
      },
      child: MeecoApp(userRepository: userRepository),
    ),
  );
}

class MeecoApp extends StatelessWidget {
  final UserRepository userRepository;

  MeecoApp({Key key, @required this.userRepository}): super(key: key);
  // This widget is the root of your application.

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AuthenticationBloc, AuthenticationState>(
      builder: (context, state) {
        if (state is AuthenticationUninitialized) {
          return SplashPage();
        }
        if (state is AuthenticationAuthenticated || state is AuthenticationUnauthenticated) {
          return PlatformApp(
            title: 'MeecoApp',
            home: PlatformScaffold(
              appBar: PlatformAppBar(
                title: new PlatformText('MeecoApp'),
              ),
//              bottomNavBar: PlatformNavBar(
//                items: [
//                  BottomNavigationBarItem(),
//                  BottomNavigationBarItem(),
//                ],
//              ),
              iosContentPadding: true,
              body: MainPage(),
            )
          );
        }
        if (state is AuthenticationLoading) {
          return LoadingIndicator();
        }
        return null;
      },
    );
  }
}

class SplashPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return PlatformApp(
      home: PlatformScaffold(
        body: Center(
          child: PlatformText('Splash Screen'),
        ),
      ),
    );
  }
}

class LoadingIndicator extends StatelessWidget {
  @override
  Widget build(BuildContext context) => Center(
    child: CircularProgressIndicator(),
  );
}

class MainPage extends StatelessWidget {
  Route _createRoute(Pair<String, String> boardInfo) {
    return PageRouteBuilder(
      pageBuilder: (context, animation, secondaryAnimation) => ArticleListPage(boardInfo),
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        return child;
      }
    );
  }

  @override
  Widget build(BuildContext context) {
    List<Widget> widgets = [];
    for (var key in categories.keys) {
      // TODO: Create header for category
      widgets.addAll(categories[key].map((pair) => 
        ListTile(
          title: PlatformText(pair.right),
          onTap: () {
            Navigator.of(context).push(_createRoute(pair));
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
