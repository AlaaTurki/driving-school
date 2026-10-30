import 'auth_session.dart';

abstract interface class AuthRepository {
  Future<AuthSession> login({
    required String email,
    required String password,
  });

  Future<AuthSession> register({
    required String fullName,
    required String email,
    required String phone,
    required String password,
  });

  Future<void> logout();

  Future<AuthSession?> restoreSession();
}
