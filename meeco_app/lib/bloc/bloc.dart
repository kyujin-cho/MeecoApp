import 'dart:async';

import 'package:bloc/bloc.dart';
import 'package:meta/meta.dart';

import 'event.dart';
import 'state.dart';
import 'repository.dart';

import '../functions.dart';

class AuthenticationBloc
    extends Bloc<AuthenticationEvent, AuthenticationState> {
  final UserRepository userRepository;

  AuthenticationBloc({@required this.userRepository})
      : assert(userRepository != null);

  @override
  AuthenticationState get initialState => AuthenticationUninitialized();

  @override
  Stream<AuthenticationState> mapEventToState(
      AuthenticationEvent event,
      ) async* {
    if (event is AppStarted) {
      final bool hasUserInfo = await userRepository.hasUserInfo();

      if (hasUserInfo) {
        await userRepository.refreshCookie();
        yield AuthenticationAuthenticated();
      } else {
        yield AuthenticationUnauthenticated();
      }
    }

    if (event is LoggedIn) {
      yield AuthenticationLoading();
      await Fetcher.tryLogin(username: event.username, password: event.password);
      await userRepository.persistUserInfo(event.username, event.password);
      yield AuthenticationAuthenticated();
    }

    if (event is LoggedOut) {
      yield AuthenticationLoading();
      await userRepository.deleteUserInfo();
      yield AuthenticationUnauthenticated();
    }
  }
}

class LoginBloc extends Bloc<LoginEvent, LoginState> {
  final UserRepository userRepository;
  final AuthenticationBloc authenticationBloc;

  LoginBloc({
    @required this.userRepository,
    @required this.authenticationBloc,
  })  : assert(userRepository != null),
        assert(authenticationBloc != null);

  @override
  LoginState get initialState => LoginInitial();

  @override
  Stream<LoginState> mapEventToState(
      LoginEvent event,
      ) async* {
     print('Caught event $event');
      if (event is LoginButtonPressed) {
      yield LoginLoading();

      try {
        var uid = await userRepository.authenticate(
          username: event.username,
          password: event.password
        );

        print('Logged in with uid $uid');
        authenticationBloc.dispatch(LoggedIn(username: event.username, password: event.password));
        yield LoginInitial();
      } catch (error) {
        yield LoginFailure(error: error.toString());
      }
    }
  }
}

