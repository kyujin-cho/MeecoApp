import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bloc/bloc.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_flipperkit/flipper_client.dart';
import 'package:flutter_flipperkit/flutter_flipperkit.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';

import 'package:meeco_app/bloc/bloc.dart';
import 'package:meeco_app/bloc/event.dart';
import 'package:meeco_app/bloc/repository.dart';
import 'package:meeco_app/bloc/state.dart';
import './pages/mainpage.dart';
import './pages/todaypage.dart';
import './pages/settingpage.dart';

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
  // BlocSupervisor.delegate = SimpleBlocDelegate();
  final userRepository = UserRepository();
  final AuthenticationBloc authenticationBloc = AuthenticationBloc(userRepository: userRepository);
  
  runApp(
    BlocBuilder(
      bloc: authenticationBloc..dispatch(AppStarted()),
      builder: (context, tab) => MultiBlocProvider(
        providers: [
          BlocProvider<AuthenticationBloc>(builder: (context) => authenticationBloc),
          BlocProvider<LoginBloc>(builder: (context) => LoginBloc(userRepository: userRepository, authenticationBloc: authenticationBloc))
        ],
        child: MeecoApp(),
      ),
    )
  );
}

class MeecoApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AuthenticationBloc, AuthenticationState>(
      builder: (context, state) {
        if (state is AuthenticationUninitialized) {
          return SplashPage();
        }
        if (state is AuthenticationAuthenticated || state is AuthenticationUnauthenticated) {
          return MainApp();
        }
        if (state is AuthenticationLoading) {
          return LoadingIndicator();
        }
        return null;
      },
    );
  }
}

class MainApp extends StatefulWidget {
  @override
  _MainAppState createState() => _MainAppState();
}

class _MainAppState extends State<MainApp> { 
  int _selectedIndex = 0;

  static List<Widget> _widgetOptions = <Widget>[
    new MainPage(),
    new TodayPage(),
    new SettingPage()
  ];

  static List<Widget> _titleOptions = <Widget>[
    Text('Boards', style: TextStyle(color: Colors.black)),
    Text('Today', style: TextStyle(color: Colors.black)),
    Text('Settings', style: TextStyle(color: Colors.black))
  ];
  static List<Widget> _solidIconOptions = <Widget>[
    Icon(CupertinoIcons.location_solid),
    Icon(CupertinoIcons.heart_solid),
    Icon(CupertinoIcons.settings_solid)
  ];
  static List<Widget> _iconOptions = <Widget>[
    Icon(CupertinoIcons.location),
    Icon(CupertinoIcons.heart),
    Icon(CupertinoIcons.settings)
  ];

  void _onNavItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return PlatformApp(
      title: 'MeecoApp',
      android: (context) => MaterialAppData(
        theme: Theme.of(context).copyWith(
          appBarTheme: Theme.of(context).appBarTheme.copyWith(
            color: Colors.white,
            iconTheme: IconThemeData(color: Colors.black)
          ),
        )
      ),
      home: PlatformScaffold(
        appBar: PlatformAppBar(
          title: _titleOptions.elementAt(_selectedIndex),
        ),
        bottomNavBar: PlatformNavBar(
          currentIndex: _selectedIndex,
          itemChanged: _onNavItemTapped,
          items: Iterable<int>.generate(_titleOptions.length)
                  .map<BottomNavigationBarItem>((index) => BottomNavigationBarItem(
                      title: _titleOptions.elementAt(index), 
                      icon: _iconOptions.elementAt(index),
                      activeIcon: _solidIconOptions.elementAt(index)
                  )).toList(),
        ),
        iosContentPadding: true,
        body: _widgetOptions.elementAt(_selectedIndex),
      )
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
