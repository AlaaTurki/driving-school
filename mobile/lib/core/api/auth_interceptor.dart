import 'package:dio/dio.dart';

import '../storage/token_storage.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor({
    required Dio dio,
    required Dio refreshDio,
    required TokenStorage tokenStorage,
    required void Function() onSessionExpired,
  })  : _dio = dio,
        _refreshDio = refreshDio,
        _tokenStorage = tokenStorage,
        _onSessionExpired = onSessionExpired;

  final Dio _dio;
  final Dio _refreshDio;
  final TokenStorage _tokenStorage;
  final void Function() _onSessionExpired;
  Future<String>? _refreshing;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (options.extra['authRetry'] == true) {
      handler.next(options);
      return;
    }
    if (_isAuthEndpoint(options.path)) {
      options.headers.remove('Authorization');
      handler.next(options);
      return;
    }
    try {
      final accessToken = await _tokenStorage.readAccessToken();
      if (accessToken != null && accessToken.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $accessToken';
      }
      handler.next(options);
    } on Object catch (error) {
      handler.reject(DioException(
        requestOptions: options,
        error: error,
      ));
    }
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    final request = err.requestOptions;
    if (err.response?.statusCode != 401 ||
        request.extra['authRetry'] == true ||
        _isAuthEndpoint(request.path)) {
      handler.next(err);
      return;
    }

    late final String accessToken;
    try {
      accessToken = await _refreshAccessToken();
    } on Object {
      handler.next(err);
      return;
    }

    request
      ..headers['Authorization'] = 'Bearer $accessToken'
      ..extra['authRetry'] = true;
    try {
      handler.resolve(await _dio.fetch<dynamic>(request));
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
  }

  bool _isAuthEndpoint(String path) =>
      path.endsWith('/api/auth/login') ||
      path.endsWith('/api/auth/register') ||
      path.endsWith('/api/auth/refresh') ||
      path.endsWith('/api/auth/logout');

  Future<String> _refreshAccessToken() {
    final pendingRefresh = _refreshing;
    if (pendingRefresh != null) {
      return pendingRefresh;
    }
    final refresh = _performRefresh();
    _refreshing = refresh;
    return refresh.whenComplete(() {
      if (identical(_refreshing, refresh)) {
        _refreshing = null;
      }
    });
  }

  Future<String> _performRefresh() async {
    try {
      final refreshToken = await _tokenStorage.readRefreshToken();
      if (refreshToken == null || refreshToken.isEmpty) {
        throw StateError('Aucun refresh token disponible.');
      }
      final response = await _refreshDio.post<Map<String, dynamic>>(
        '/api/auth/refresh',
        data: {'refreshToken': refreshToken},
      );
      final body = response.data;
      if (body == null ||
          body['accessToken'] is! String ||
          body['refreshToken'] is! String) {
        throw const FormatException('Reponse de renouvellement invalide.');
      }
      final accessToken = body['accessToken'] as String;
      await _tokenStorage.saveTokens(
        accessToken: accessToken,
        refreshToken: body['refreshToken'] as String,
      );
      return accessToken;
    } on Object {
      try {
        await _tokenStorage.clear();
      } finally {
        _onSessionExpired();
      }
      rethrow;
    }
  }
}
