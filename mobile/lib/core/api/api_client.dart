import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../features/auth/data/auth_repository_impl.dart';
import '../../features/auth/domain/auth_repository.dart';
import '../auth/session_expired_provider.dart';
import '../storage/token_storage.dart';
import 'auth_interceptor.dart';

final Provider<String> apiBaseUrlProvider = Provider<String>((ref) {
  const configuredUrl = String.fromEnvironment('API_BASE_URL');
  final uri = Uri.tryParse(configuredUrl);
  if (uri == null ||
      !uri.hasAuthority ||
      (uri.scheme != 'https' && uri.scheme != 'http')) {
    throw StateError(
      'API_BASE_URL must be an HTTP(S) URL, '
      'for example --dart-define=API_BASE_URL=http://10.0.2.2:8080.',
    );
  }
  return configuredUrl;
});

final Provider<TokenStorage> tokenStorageProvider = Provider<TokenStorage>(
  (ref) => TokenStorage(const FlutterSecureStorage()),
);

final Provider<Dio> authDioProvider = Provider<Dio>((ref) {
  final dio = Dio(_baseOptions(ref.watch(apiBaseUrlProvider)));
  ref.onDispose(() => dio.close());
  return dio;
});

final Provider<Dio> apiClientProvider = Provider<Dio>((ref) {
  final dio = Dio(_baseOptions(ref.watch(apiBaseUrlProvider)));
  dio.interceptors.add(AuthInterceptor(
    dio: dio,
    refreshDio: ref.watch(authDioProvider),
    tokenStorage: ref.watch(tokenStorageProvider),
    onSessionExpired: () {
      final expirationSignal = ref.read(sessionExpiredProvider.notifier);
      expirationSignal.state++;
    },
  ));
  ref.onDispose(() => dio.close());
  return dio;
});

final Provider<AuthRepository> authRepositoryProvider =
    Provider<AuthRepository>((ref) {
  return AuthRepositoryImpl(
    dio: ref.watch(authDioProvider),
    tokenStorage: ref.watch(tokenStorageProvider),
  );
});

BaseOptions _baseOptions(String baseUrl) => BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 15),
      headers: {'Accept': 'application/json'},
    );
