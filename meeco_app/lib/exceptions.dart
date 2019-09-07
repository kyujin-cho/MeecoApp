class LoginException implements Exception {
  String error;
  LoginException(this.error);
}

class GenericException implements Exception {
  String error;
  GenericException(this.error);
}