import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:mocktail/mocktail.dart';

import 'package:driving_school_mobile/core/storage/token_storage.dart';
import 'package:driving_school_mobile/features/auth/data/auth_repository_impl.dart';

class MockFlutterSecureStorage extends Mock implements FlutterSecureStorage {}

void main() {
  late Dio dio;
  late DioAdapter adapter;
  late MockFlutterSecureStorage secureStorage;
  late TokenStorage tokenStorage;
  late AuthRepositoryImpl repository;

  const sessionBody = {
    'accessToken': 'access-1',
    'refreshToken': 'refresh-1',
    'tokenType': 'Bearer',
    'expiresAt': '2026-10-29T12:00:00Z',
    'userId': 'c74d5d21-c58e-4e15-8ad1-533f8fcd9804',
    'email': 'candidate@example.test',
    'roles': ['CANDIDATE'],
  };

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'https://api.example.test'));
    adapter = DioAdapter(dio: dio);
    dio.httpClientAdapter = adapter;
    secureStorage = MockFlutterSecureStorage();
    tokenStorage = TokenStorage(secureStorage);
    repository = AuthRepositoryImpl(dio: dio, tokenStorage: tokenStorage);
    when(() => secureStorage.write(
          key: any(named: 'key'),
          value: any(named: 'value'),
        )).thenAnswer((_) async {});
  });

  tearDown(() => dio.close());

  test('login posts credentials and stores both tokens securely', () async {
    adapter.onPost(
      '/api/auth/login',
      (server) => server.reply(200, sessionBody),
      data: {'email': 'candidate@example.test', 'password': 'password123'},
    );

    final session = await repository.login(
      email: ' candidate@example.test ',
      password: 'password123',
    );

    expect(session.email, 'candidate@example.test');
    expect(session.isCandidate, isTrue);
    verify(() => secureStorage.write(
          key: 'access_token',
          value: 'access-1',
        )).called(1);
    verify(() => secureStorage.write(
          key: 'refresh_token',
          value: 'refresh-1',
        )).called(1);
  });

  test('registration sends all required profile fields', () async {
    adapter.onPost(
      '/api/auth/register',
      (server) => server.reply(201, sessionBody),
      data: {
        'fullName': 'Camille Candidate',
        'email': 'candidate@example.test',
        'phone': '+33123456789',
        'password': 'password123',
      },
    );

    final session = await repository.register(
      fullName: ' Camille Candidate ',
      email: 'candidate@example.test',
      phone: '+33123456789',
      password: 'password123',
    );

    expect(session.roles, ['CANDIDATE']);
  });
}
