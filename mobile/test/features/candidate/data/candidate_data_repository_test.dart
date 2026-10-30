import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import 'package:driving_school_mobile/features/candidate/data/candidate_data.dart';

void main() {
  late Dio dio;
  late DioAdapter adapter;
  late CandidateDataRepository repository;

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'https://api.example.test'));
    adapter = DioAdapter(dio: dio);
    dio.httpClientAdapter = adapter;
    repository = CandidateDataRepository(dio);
  });

  tearDown(() => dio.close());

  test('parses sessions and preserves decimal prices exactly', () async {
    adapter.onGet(
      '/api/me/sessions',
      (server) => server.reply(200, {
        'content': [
          {
            'id': 'session-1',
            'sessionDate': '2025-03-04',
            'startTime': '09:00:00',
            'endTime': '10:30:00',
            'durationHours': 1.5,
            'instructor': {'id': 'instructor-1', 'name': 'Nadia Ben Ali'},
            'circuit': {'id': 'circuit-1', 'name': 'Centre-ville'},
            'price': 56.125,
            'status': 'DONE',
            'notes': 'Bien maîtrisé',
          },
        ],
        'number': 0,
        'totalPages': 1,
        'last': true,
      }),
      queryParameters: {'page': 0, 'size': 20},
    );

    final result = await repository.getSessions(page: 0, size: 20);

    expect(result.items.single.price.milliunits, 56125);
    expect(result.items.single.durationHours, 1.5);
    expect(result.items.single.circuit, 'Centre-ville');
    expect(result.items.single.notes, 'Bien maîtrisé');
  });

  test('parses balance and signed credit without recomputing it', () async {
    adapter.onGet(
      '/api/me/balance',
      (server) => server.reply(200, {
        'totalSessions': 100.000,
        'totalPaid': 156.000,
        'remaining': -56.000,
        'credit': true,
      }),
    );

    final balance = await repository.getBalance();

    expect(balance.totalSessions.milliunits, 100000);
    expect(balance.totalPaid.milliunits, 156000);
    expect(balance.remaining.milliunits, -56000);
    expect(balance.credit, isTrue);
  });

  test('parses payments and statement paid amount from API pages', () async {
    adapter.onGet(
      '/api/me/payments',
      (server) => server.reply(200, {
        'content': [
          {
            'amount': 250.500,
            'paymentDate': '2025-03-04',
            'paymentMethod': 'CASH',
            'notes': null,
          },
        ],
        'number': 0,
        'totalPages': 1,
        'last': true,
      }),
      queryParameters: {'page': 0, 'size': 20},
    );
    adapter.onGet(
      '/api/me/statement',
      (server) => server.reply(200, {
        'candidateId': 'response-only',
        'content': [
          {
            'sessionId': 'session-1',
            'day': 'TUESDAY',
            'date': '2025-03-04',
            'startTime': '09:00:00',
            'endTime': '10:00:00',
            'hours': 1,
            'price': 56.000,
            'status': 'DONE',
            'paidAmount': 20.125,
            'remaining': 35.875,
          },
        ],
        'number': 0,
        'totalPages': 1,
        'last': true,
      }),
      queryParameters: {'page': 0, 'size': 20},
    );

    final paymentPage = await repository.getPayments(page: 0);
    final statementPage = await repository.getStatement(page: 0);

    expect(paymentPage.items.single.amount.milliunits, 250500);
    expect(statementPage.items.single.hours, 1);
    expect(statementPage.items.single.paidAmount.milliunits, 20125);
    expect(statementPage.items.single.remaining.milliunits, 35875);
  });

  test('parses candidate profile, schedule, and all nine skill levels', () async {
    adapter.onGet(
      '/api/me',
      (server) => server.reply(200, {'firstName': 'Sami'}),
    );
    adapter.onGet(
      '/api/me/schedule',
      (server) => server.reply(200, [
        {
          'date': '2025-03-04',
          'sessions': [
            {
              'sessionId': 'session-1',
              'instructorId': 'instructor-1',
              'instructorName': 'Nadia Ben Ali',
              'startTime': '09:00:00',
              'endTime': '10:00:00',
              'candidateName': 'Sami',
              'circuit': 'Centre-ville',
              'vehicle': null,
              'status': 'PLANNED',
            },
          ],
        },
      ]),
    );
    adapter.onGet(
      '/api/me/skills',
      (server) => server.reply(200, {
        'overallProgress': 50,
        'skills': List.generate(
          9,
          (index) => {
            'name': 'Compétence ${index + 1}',
            'displayOrder': index + 1,
            'level': index * 10,
          },
        ),
      }),
    );

    final profile = await repository.getProfile();
    final schedule = await repository.getSchedule();
    final skills = await repository.getSkills();

    expect(profile.firstName, 'Sami');
    expect(schedule.single.sessions.single.instructor, 'Nadia Ben Ali');
    expect(skills.skills, hasLength(9));
    expect(skills.overallProgress, 50);
  });
}
