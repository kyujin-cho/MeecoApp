class LoginException implements Exception {
  String error;
  LoginException(String error) {
    this.error = error;
  }
}
