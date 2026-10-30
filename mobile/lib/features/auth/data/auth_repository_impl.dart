import 'package:dio/dio.dart';

import '../../../core/storage/token_storage.dart';
import '../domain/auth_repository.dart';
import '../domain/auth_session.dart';

class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl({
    required Dio dio,
    required TokenStorage tokenStorage,
  })  : _dio = dio,
        _tokenStorage = tokenStorage;

  final Dio _dio;
  final TokenStorage _tokenStorage;

  @override
  Future<AuthSession> login({
    required String email,
    required String password,
  }) =>
      _authenticate('/api/auth/login', {
        'email': email.trim(),
        'password': password,
      });

  @override
  Future<AuthSession> register({
    required String fullName,
    required String email,
    required String phone,
    required String password,
  }) =>
      _authenticate('/api/auth/register', {
        'fullName': fullName.trim(),
        'email': email.trim(),
        'phone': phone.trim(),
        'password': password,
      });

  Future<AuthSession> _authenticate(
    String path,
    Map<String, dynamic> payload,
  ) async {
    final response = await _dio.post<Map<String, dynamic>>(
      path,
      data: payload,
    );
    final data = response.data;
    if (data == null) {
      throw const FormatException('Réponse d’authentification vide.');
    }
    final session = AuthSession.fromJson(data);
    await _tokenStorage.saveTokens(
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
    );
    return session;
  }

  @override
  Future<AuthSession?> restoreSession() async {
    final refreshToken = await _tokenStorage.readRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      return null;
    }
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/auth/refresh',
        data: {'refreshToken': refreshToken},
      );
      final data = response.data;
      if (data == null) {
        throw const FormatException('Réponse de renouvellement vide.');
      }
      final session = AuthSession.fromJson(data);
      await _tokenStorage.saveTokens(
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
      );
      return session;
    } on Object {
      await _tokenStorage.clear();
      rethrow;
    }
  }

  @override
  Future<void> logout() async {
    final refreshToken = await _tokenStorage.readRefreshToken();
    try {
      if (refreshToken != null && refreshToken.isNotEmpty) {
        await _dio.post<void>(
          '/api/auth/logout',
          data: {'refreshToken': refreshToken},
        );
      }
    } finally {
      await _tokenStorage.clear();
    }
  }
}
