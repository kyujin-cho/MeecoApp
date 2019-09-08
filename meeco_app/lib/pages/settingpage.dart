import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_platform_widgets/flutter_platform_widgets.dart';
import 'package:meeco_app/bloc/bloc.dart';
import 'package:meeco_app/bloc/event.dart';
import 'package:meeco_app/bloc/repository.dart';
import 'package:meeco_app/bloc/state.dart';

import '../main.dart';

class SettingPage extends StatelessWidget {
  final UserRepository userRepository = UserRepository();

  Route _createRoute(Widget page) {
    return PageRouteBuilder(
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        return child;
      }
    );
  }

  void _onLoginTapped(BuildContext context) {
    final _usernameController = TextEditingController();
    final _passwordController = TextEditingController();

    final _authenticationBloc = BlocProvider.of<AuthenticationBloc>(context);
    final _loginBloc = BlocProvider.of<LoginBloc>(context);

    showPlatformDialog(
      context: context,
      builder: (_) => PlatformAlertDialog(
        title: Text('Log In'),
        content: LoginDialog(
          usernameController: _usernameController, 
          passwordController: _passwordController,
          authenticationBloc: _authenticationBloc
        ),
        actions: <Widget>[
          PlatformDialogAction(
            child: Text('Cancel'),
            onPressed: () => Navigator.of(context).pop(),
          ),
          PlatformDialogAction(
            child: Text('Login'),
            onPressed: () {
              _loginBloc.dispatch(LoginButtonPressed(
                username: _usernameController.text, 
                password: _passwordController.text
              ));
            },
          ),
        ],
      ),
    );
  }

  void _onLogoutTapped() {

  }

  @override
  Widget build(BuildContext context) {
    return BlocProvider<LoginBloc>(
      builder: (context) => LoginBloc(
        authenticationBloc: BlocProvider.of<AuthenticationBloc>(context),
        userRepository: userRepository
      ),
      child: BlocBuilder<AuthenticationBloc, AuthenticationState>(
        builder: (context, state) {
          if (state is AuthenticationUnauthenticated) {
            return Center(
              child: PlatformButton(
                child: Text('Log In'),
                onPressed: () => _onLoginTapped(context),
              )
            );
          }
          if (state is AuthenticationAuthenticated) {
            return Center(
              child: PlatformButton(
                child: Text('Log Out'),
                onPressed: _onLogoutTapped,
              ),
            );
          }
          if (state is AuthenticationLoading) {
            return LoadingIndicator();
          }
          return null;
        },
      ),
    );
  }
}

class LoginDialog extends StatefulWidget {
  final TextEditingController usernameController, passwordController;
  final AuthenticationBloc authenticationBloc;
  LoginDialog({Key key, @required this.usernameController, @required this.passwordController, @required this.authenticationBloc});
  @override
  _LoginState createState() => _LoginState(
    usernameController: this.usernameController, 
    passwordController: this.passwordController
  );
}

class _LoginState extends State<LoginDialog> {
  TextEditingController usernameController, passwordController;
  bool loginFailed = false;

  _LoginState({@required this.usernameController, this.passwordController});

  @override
  Widget build(BuildContext context) {
    return BlocListener(
      listener: (context, state) {
        if (state is LoginFailure) {
          loginFailed = true;
          Timer(Duration(seconds: 2), () { loginFailed = false; });
        } 
        if (state is LoginInitial) {
          Navigator.of(context).pop();
        }
      },
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          PlatformTextField(
            autocorrect: false,
            controller: usernameController,
            keyboardType: TextInputType.emailAddress,
            android: (context) => MaterialTextFieldData(
              decoration: InputDecoration(
                hintText: 'Username',
              )
            ),
            ios: (context) => CupertinoTextFieldData(
              placeholder: 'Username'
            ),
          ),
          PlatformTextField(
            autocorrect: false,
            controller: passwordController,
            obscureText: true,
            android: (context) => MaterialTextFieldData(
              decoration: InputDecoration(
                hintText: 'Password',
              )
            ),
            ios: (context) => CupertinoTextFieldData(
              placeholder: 'Password'
            ),
          ),
          Text((loginFailed ? 'Login Failed' : ''), style: TextStyle(color: Colors.red))
        ]
      )
    );
  }
}