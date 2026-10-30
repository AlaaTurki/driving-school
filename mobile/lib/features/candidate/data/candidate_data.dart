import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_client.dart';
import '../../../core/format/money_format.dart';

class CandidateProfile {
  const CandidateProfile({required this.firstName});

  factory CandidateProfile.fromJson(Map<String, dynamic> json) =>
      CandidateProfile(firstName: _requiredString(json, 'firstName'));

  final String firstName;
}

class CandidateBalance {
  const CandidateBalance({
    required this.totalSessions,
    required this.totalPaid,
    required this.remaining,
    required this.credit,
  });

  factory CandidateBalance.fromJson(Map<String, dynamic> json) =>
      CandidateBalance(
        totalSessions: MoneyAmount.fromJson(json['totalSessions']),
        totalPaid: MoneyAmount.fromJson(json['totalPaid']),
        remaining: MoneyAmount.fromJson(json['remaining']),
        credit: _requiredBool(json, 'credit'),
      );

  final MoneyAmount totalSessions;
  final MoneyAmount totalPaid;
  final MoneyAmount remaining;
  final bool credit;
}

class CandidateSession {
  const CandidateSession({
    required this.id,
    required this.sessionDate,
    required this.startTime,
    required this.endTime,
    required this.durationHours,
    required this.instructor,
    required this.circuit,
    required this.price,
    required this.status,
    required this.notes,
  });

  factory CandidateSession.fromJson(Map<String, dynamic> json) {
    final instructor = _optionalMap(json['instructor']);
    final circuit = _optionalMap(json['circuit']);
    return CandidateSession(
      id: _requiredString(json, 'id'),
      sessionDate: _requiredDate(json, 'sessionDate'),
      startTime: _requiredString(json, 'startTime'),
      endTime: _requiredString(json, 'endTime'),
      durationHours: _requiredNum(json, 'durationHours'),
      instructor: instructor == null
          ? 'Non renseigné'
          : _requiredString(instructor, 'name'),
      circuit: circuit == null ? null : _requiredString(circuit, 'name'),
      price: MoneyAmount.fromJson(json['price']),
      status: _requiredString(json, 'status'),
      notes: _optionalString(json, 'notes'),
    );
  }

  final String id;
  final DateTime sessionDate;
  final String startTime;
  final String endTime;
  final num durationHours;
  final String instructor;
  final String? circuit;
  final MoneyAmount price;
  final String status;
  final String? notes;
}

class CandidatePayment {
  const CandidatePayment({
    required this.amount,
    required this.paymentDate,
    required this.paymentMethod,
    required this.notes,
  });

  factory CandidatePayment.fromJson(Map<String, dynamic> json) =>
      CandidatePayment(
        amount: MoneyAmount.fromJson(json['amount']),
        paymentDate: _requiredDate(json, 'paymentDate'),
        paymentMethod: _requiredString(json, 'paymentMethod'),
        notes: _optionalString(json, 'notes'),
      );

  final MoneyAmount amount;
  final DateTime paymentDate;
  final String paymentMethod;
  final String? notes;
}

class CandidateStatementLine {
  const CandidateStatementLine({
    required this.date,
    required this.startTime,
    required this.endTime,
    required this.hours,
    required this.price,
    required this.status,
    required this.paidAmount,
    required this.remaining,
  });

  factory CandidateStatementLine.fromJson(Map<String, dynamic> json) =>
      CandidateStatementLine(
        date: _requiredDate(json, 'date'),
        startTime: _requiredString(json, 'startTime'),
        endTime: _requiredString(json, 'endTime'),
        hours: _requiredNum(json, 'hours'),
        price: MoneyAmount.fromJson(json['price']),
        status: _requiredString(json, 'status'),
        paidAmount: MoneyAmount.fromJson(json['paidAmount']),
        remaining: MoneyAmount.fromJson(json['remaining']),
      );

  final DateTime date;
  final String startTime;
  final String endTime;
  final num hours;
  final MoneyAmount price;
  final String status;
  final MoneyAmount paidAmount;
  final MoneyAmount remaining;
}

class CandidateScheduleEntry {
  const CandidateScheduleEntry({
    required this.sessionId,
    required this.startTime,
    required this.endTime,
    required this.instructor,
    required this.circuit,
    required this.vehicle,
    required this.status,
  });

  factory CandidateScheduleEntry.fromJson(Map<String, dynamic> json) =>
      CandidateScheduleEntry(
        sessionId: _requiredString(json, 'sessionId'),
        startTime: _requiredString(json, 'startTime'),
        endTime: _requiredString(json, 'endTime'),
        instructor: _requiredString(json, 'instructorName'),
        circuit: _optionalString(json, 'circuit'),
        vehicle: _optionalString(json, 'vehicle'),
        status: _requiredString(json, 'status'),
      );

  final String sessionId;
  final String startTime;
  final String endTime;
  final String instructor;
  final String? circuit;
  final String? vehicle;
  final String status;
}

class CandidateScheduleDay {
  const CandidateScheduleDay({required this.date, required this.sessions});

  factory CandidateScheduleDay.fromJson(Map<String, dynamic> json) =>
      CandidateScheduleDay(
        date: _requiredDate(json, 'date'),
        sessions: (json['sessions'] as List<dynamic>)
            .map((item) => CandidateScheduleEntry.fromJson(_asMap(item)))
            .toList(growable: false),
      );

  final DateTime date;
  final List<CandidateScheduleEntry> sessions;
}

class CandidateSkill {
  const CandidateSkill({
    required this.name,
    required this.displayOrder,
    required this.level,
  });

  factory CandidateSkill.fromJson(Map<String, dynamic> json) => CandidateSkill(
        name: _requiredString(json, 'name'),
        displayOrder: _requiredInt(json, 'displayOrder'),
        level: _requiredInt(json, 'level'),
      );

  final String name;
  final int displayOrder;
  final int level;
}

class CandidateSkills {
  const CandidateSkills({
    required this.overallProgress,
    required this.skills,
  });

  factory CandidateSkills.fromJson(Map<String, dynamic> json) =>
      CandidateSkills(
        overallProgress: _requiredInt(json, 'overallProgress'),
        skills: (json['skills'] as List<dynamic>)
            .map((item) => CandidateSkill.fromJson(_asMap(item)))
            .toList(growable: false),
      );

  final int overallProgress;
  final List<CandidateSkill> skills;
}

class CandidatePage<T> {
  const CandidatePage({
    required this.items,
    required this.page,
    required this.totalPages,
    required this.last,
  });

  final List<T> items;
  final int page;
  final int totalPages;
  final bool last;
}

class PaymentsScreenData {
  const PaymentsScreenData({
    required this.balance,
    required this.payments,
    required this.statement,
  });

  final CandidateBalance balance;
  final CandidatePage<CandidatePayment> payments;
  final CandidatePage<CandidateStatementLine> statement;
}

class HomeScreenData {
  const HomeScreenData({
    required this.profile,
    required this.balance,
    required this.nextSession,
  });

  final CandidateProfile profile;
  final CandidateBalance balance;
  final CandidateSession? nextSession;
}

class CandidateDataRepository {
  const CandidateDataRepository(this._dio);

  final Dio _dio;

  Future<CandidateProfile> getProfile() async =>
      CandidateProfile.fromJson(_asMap((await _dio.get<Object?>('/api/me')).data));

  Future<CandidateBalance> getBalance() async => CandidateBalance.fromJson(
        _asMap((await _dio.get<Object?>('/api/me/balance')).data),
      );

  Future<CandidatePage<CandidateSession>> getSessions({
    required int page,
    required int size,
    DateTime? dateFrom,
    DateTime? dateTo,
  }) async {
    final response = await _dio.get<Object?>(
      '/api/me/sessions',
      queryParameters: {
        'page': page,
        'size': size,
        if (dateFrom != null) 'dateFrom': _dateQuery(dateFrom),
        if (dateTo != null) 'dateTo': _dateQuery(dateTo),
      },
    );
    return _page(response.data, CandidateSession.fromJson);
  }

  Future<CandidatePage<CandidatePayment>> getPayments({
    required int page,
    int size = 20,
  }) async {
    final response = await _dio.get<Object?>(
      '/api/me/payments',
      queryParameters: {'page': page, 'size': size},
    );
    return _page(response.data, CandidatePayment.fromJson);
  }

  Future<CandidatePage<CandidateStatementLine>> getStatement({
    required int page,
    int size = 20,
  }) async {
    final response = await _dio.get<Object?>(
      '/api/me/statement',
      queryParameters: {'page': page, 'size': size},
    );
    return _page(response.data, CandidateStatementLine.fromJson);
  }

  Future<List<CandidateScheduleDay>> getSchedule() async {
    final response = await _dio.get<Object?>('/api/me/schedule');
    return (response.data as List<dynamic>)
        .map((item) => CandidateScheduleDay.fromJson(_asMap(item)))
        .toList(growable: false);
  }

  Future<CandidateSkills> getSkills() async => CandidateSkills.fromJson(
        _asMap((await _dio.get<Object?>('/api/me/skills')).data),
      );

  Future<HomeScreenData> getHomeData() async {
    final values = await Future.wait<Object>([
      getProfile(),
      getBalance(),
      getSchedule(),
    ]);
    final profile = values[0] as CandidateProfile;
    final balance = values[1] as CandidateBalance;
    final days = values[2] as List<CandidateScheduleDay>;
    if (days.isEmpty || days.first.sessions.isEmpty) {
      return HomeScreenData(
        profile: profile,
        balance: balance,
        nextSession: null,
      );
    }

    final nextDay = days.first;
    final nextEntry = nextDay.sessions.first;
    CandidateSession? nextSession;
    var sessionPage = 0;
    while (nextSession == null) {
      final page = await getSessions(
        page: sessionPage,
        size: 100,
        dateFrom: nextDay.date,
        dateTo: nextDay.date,
      );
      for (final session in page.items) {
        if (session.id == nextEntry.sessionId) {
          nextSession = session;
          break;
        }
      }
      if (nextSession != null || page.last) break;
      sessionPage++;
    }
    if (nextSession == null) {
      throw StateError('La séance prévue est absente de la liste des séances.');
    }
    return HomeScreenData(
      profile: profile,
      balance: balance,
      nextSession: nextSession,
    );
  }
}

final Provider<CandidateDataRepository> candidateDataRepositoryProvider =
    Provider<CandidateDataRepository>(
  (ref) => CandidateDataRepository(ref.watch(apiClientProvider)),
);

final FutureProvider<HomeScreenData> homeScreenDataProvider =
    FutureProvider<HomeScreenData>(
  (ref) => ref.watch(candidateDataRepositoryProvider).getHomeData(),
);

final FutureProvider<PaymentsScreenData> paymentsScreenDataProvider =
    FutureProvider<PaymentsScreenData>((ref) async {
  final repository = ref.watch(candidateDataRepositoryProvider);
  final values = await Future.wait<Object>([
    repository.getBalance(),
    repository.getPayments(page: 0),
    repository.getStatement(page: 0),
  ]);
  return PaymentsScreenData(
    balance: values[0] as CandidateBalance,
    payments: values[1] as CandidatePage<CandidatePayment>,
    statement: values[2] as CandidatePage<CandidateStatementLine>,
  );
});

final FutureProvider<List<CandidateScheduleDay>> candidateScheduleProvider =
    FutureProvider<List<CandidateScheduleDay>>(
  (ref) => ref.watch(candidateDataRepositoryProvider).getSchedule(),
);

final FutureProvider<CandidateSkills> candidateSkillsProvider =
    FutureProvider<CandidateSkills>(
  (ref) => ref.watch(candidateDataRepositoryProvider).getSkills(),
);

CandidatePage<T> _page<T>(
  Object? data,
  T Function(Map<String, dynamic>) parse,
) {
  final json = _asMap(data);
  return CandidatePage<T>(
    items: (json['content'] as List<dynamic>)
        .map((item) => parse(_asMap(item)))
        .toList(growable: false),
    page: _requiredInt(json, 'number'),
    totalPages: _requiredInt(json, 'totalPages'),
    last: _requiredBool(json, 'last'),
  );
}

Map<String, dynamic> _asMap(Object? value) {
  if (value is Map<String, dynamic>) {
    return value;
  }
  if (value is Map) {
    return Map<String, dynamic>.from(value);
  }
  throw FormatException('Objet JSON invalide : $value');
}

Map<String, dynamic>? _optionalMap(Object? value) =>
    value == null ? null : _asMap(value);

String _requiredString(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! String) {
    throw FormatException('Champ texte invalide : $key');
  }
  return value;
}

String? _optionalString(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value == null) {
    return null;
  }
  if (value is! String) {
    throw FormatException('Champ texte invalide : $key');
  }
  return value;
}

num _requiredNum(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is num && value.isFinite) {
    return value;
  }
  if (value is String) {
    final parsed = num.tryParse(value);
    if (parsed != null && parsed.isFinite) {
      return parsed;
    }
  }
  throw FormatException('Champ numérique invalide : $key');
}

int _requiredInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! int) {
    throw FormatException('Champ entier invalide : $key');
  }
  return value;
}

bool _requiredBool(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! bool) {
    throw FormatException('Champ booléen invalide : $key');
  }
  return value;
}

DateTime _requiredDate(Map<String, dynamic> json, String key) {
  final value = _requiredString(json, key);
  return DateTime.parse(value);
}

String _dateQuery(DateTime date) =>
    '${date.year.toString().padLeft(4, '0')}-'
    '${date.month.toString().padLeft(2, '0')}-'
    '${date.day.toString().padLeft(2, '0')}';
