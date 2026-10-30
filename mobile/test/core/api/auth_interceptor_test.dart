import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:mocktail/mocktail.dart';

import 'package:driving_school_mobile/core/api/auth_interceptor.dart';
import 'package:driving_school_mobile/core/storage/token_storage.dart';

class MockFlutterSecureStorage extends Mock implements FlutterSecureStorage {}

void main() {
  late Dio dio;
  late Dio refreshDio;
  late DioAdapter apiAdapter;
  late DioAdapter refreshAdapter;
  late MockFlutterSecureStorage secureStorage;
  late TokenStorage tokenStorage;

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'https://api.example.test'));
    refreshDio = Dio(BaseOptions(baseUrl: 'https://api.example.test'));
    apiAdapter = DioAdapter(dio: dio);
    refreshAdapter = DioAdapter(dio: refreshDio);
    dio.httpClientAdapter = apiAdapter;
    refreshDio.httpClientAdapter = refreshAdapter;
    secureStorage = MockFlutterSecureStorage();
    tokenStorage = TokenStorage(secureStorage);

    when(() => secureStorage.read(key: 'access_token'))
        .thenAnswer((_) async => 'expired-access');
    when(() => secureStorage.read(key: 'refresh_token'))
        .thenAnswer((_) async => 'refresh-1');
    when(() => secureStorage.write(
          key: any(named: 'key'),
          value: any(named: 'value'),
        )).thenAnswer((_) async {});
    when(() => secureStorage.deleteAll()).thenAnswer((_) async {});
  });

  tearDown(() {
    dio.close();
    refreshDio.close();
  });

  test('refreshes once after 401 and replays the original request', () async {
    final protectedRequests = <Map<String, Object?>>[];
    final refreshRequests = <Map<String, Object?>>[];
    const expiredAccessToken = 'expired-access';
    const freshAccessToken = 'access-2';

    Map<String, Object?> requestRecord(RequestOptions options) {
      return {
        'method': options.method,
        'path': options.path,
        'authorization': options.headers['Authorization'],
      };
    }

    apiAdapter.onGet(
      '/api/candidate/profile',
      (server) {
        server.replyCallback(401, (options) {
          protectedRequests.add(requestRecord(options));
          return {'message': 'expired'};
        });
      },
      headers: {'Authorization': 'Bearer $expiredAccessToken'},
    );
    apiAdapter.onGet(
      '/api/candidate/profile',
      (server) {
        server.replyCallback(200, (options) {
          protectedRequests.add(requestRecord(options));
          return {'name': 'Camille'};
        });
      },
      headers: {'Authorization': 'Bearer $freshAccessToken'},
    );
    refreshAdapter.onPost(
      '/api/auth/refresh',
      (server) {
        server.replyCallback(200, (options) {
          refreshRequests.add(requestRecord(options));
          return {
            'accessToken': freshAccessToken,
            'refreshToken': 'refresh-2',
          };
        });
      },
      data: {'refreshToken': 'refresh-1'},
    );
    dio.interceptors.add(AuthInterceptor(
      dio: dio,
      refreshDio: refreshDio,
      tokenStorage: tokenStorage,
      onSessionExpired: () {},
    ));

    final responses = await Future.wait([
      dio.get<Map<String, dynamic>>('/api/candidate/profile'),
      dio.get<Map<String, dynamic>>('/api/candidate/profile'),
    ]);

    expect(responses.map((response) => response.data?['name']),
        everyElement('Camille'));
    expect(protectedRequests, [
      {
        'method': 'GET',
        'path': '/api/candidate/profile',
        'authorization': 'Bearer $expiredAccessToken',
      },
      {
        'method': 'GET',
        'path': '/api/candidate/profile',
        'authorization': 'Bearer $expiredAccessToken',
      },
      {
        'method': 'GET',
        'path': '/api/candidate/profile',
        'authorization': 'Bearer $freshAccessToken',
      },
      {
        'method': 'GET',
        'path': '/api/candidate/profile',
        'authorization': 'Bearer $freshAccessToken',
      },
    ]);
    expect(refreshRequests, [
      {
        'method': 'POST',
        'path': '/api/auth/refresh',
        'authorization': null,
      },
    ]);
    verify(() => secureStorage.write(
          key: 'access_token',
          value: freshAccessToken,
        )).called(1);
    verify(() => secureStorage.write(
          key: 'refresh_token',
          value: 'refresh-2',
        )).called(1);
  });

  test('clears tokens and expires the session when refresh fails', () async {
    var sessionExpired = false;
    apiAdapter.onGet(
      '/api/candidate/profile',
      (server) => server.reply(401, {'message': 'expired'}),
    );
    refreshAdapter.onPost(
      '/api/auth/refresh',
      (server) => server.reply(401, {'message': 'invalid refresh token'}),
      data: {'refreshToken': 'refresh-1'},
    );
    dio.interceptors.add(AuthInterceptor(
      dio: dio,
      refreshDio: refreshDio,
      tokenStorage: tokenStorage,
      onSessionExpired: () => sessionExpired = true,
    ));

    await expectLater(
      dio.get<dynamic>('/api/candidate/profile'),
      throwsA(isA<DioException>()),
    );

    expect(sessionExpired, isTrue);
    verify(() => secureStorage.deleteAll()).called(1);
  });
}
