import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../functions.dart';

class UserRepository {
  FlutterSecureStorage storage;
  UserRepository() {
    storage = new FlutterSecureStorage();
  }

  Future<String> authenticate({
    @required String username,
    @required String password
  }) async {
    return await Fetcher.tryLogin(username: username, password: password, persist: false);
  }

  Future<void> refreshCookie() async {
    var username = await storage.read(key: 'username');
    var password = await storage.read(key: 'password');
    await Fetcher.tryLogin(username: username, password: password);
  }

  Future<void> deleteUserInfo() async {
    /// delete from keystore/keychain
    await storage.delete(key: 'username');
    await storage.delete(key: 'password');
  }

  Future<void> persistUserInfo(String username, String password) async {
    /// write to keystore/keychain
    await storage.write(key: 'username', value: username);
    await storage.write(key: 'password', value: password);
  }

  Future<bool> hasUserInfo() async {
    /// read from keystore/keychain
    return await storage.read(key: 'username') != null;
  }
}
