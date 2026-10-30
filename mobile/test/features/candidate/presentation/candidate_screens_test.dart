import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:driving_school_mobile/core/format/money_format.dart';
import 'package:driving_school_mobile/features/candidate/data/candidate_data.dart';
import 'package:driving_school_mobile/features/candidate/presentation/payments_screen.dart';
import 'package:driving_school_mobile/features/candidate/presentation/sessions_screen.dart';
import 'package:driving_school_mobile/features/home/presentation/candidate_home_screen.dart';

void main() {
  testWidgets('home greets the candidate and shows next session details',
      (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          homeScreenDataProvider.overrideWith(
            (ref) async => HomeScreenData(
              profile: const CandidateProfile(firstName: 'Sami'),
              balance: _balance(20.000, credit: false),
              nextSession: _session(
                id: 'next',
                status: 'PLANNED',
                price: 56.000,
              ),
            ),
          ),
        ],
        child: const MaterialApp(home: CandidateHomeScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Bonjour Sami'), findsOneWidget);
    expect(find.text('Prochaine séance'), findsOneWidget);
    expect(find.text('Circuit : Centre-ville'), findsOneWidget);
    expect(find.text('Moniteur : Nadia Ben Ali'), findsOneWidget);
    expect(find.text('20,000 DT'), findsOneWidget);
  });

  testWidgets('home adapts when there is no upcoming session', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          homeScreenDataProvider.overrideWith(
            (ref) async => HomeScreenData(
              profile: const CandidateProfile(firstName: 'Maya'),
              balance: _balance(0, credit: false),
              nextSession: null,
            ),
          ),
        ],
        child: const MaterialApp(home: CandidateHomeScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Bonjour Maya'), findsOneWidget);
    expect(find.text('Aucune séance à venir pour le moment.'), findsOneWidget);
  });

  for (final scenario in [
    (
      name: 'positive balance',
      remaining: 12.345,
      credit: false,
      label: 'Reste à payer',
      amount: '12,345 DT'
    ),
    (
      name: 'zero balance',
      remaining: 0.000,
      credit: false,
      label: 'Reste à payer',
      amount: '0,000 DT'
    ),
    (
      name: 'credit balance',
      remaining: -12.345,
      credit: true,
      label: 'Crédit',
      amount: '\u002d12,345 DT'
    ),
  ]) {
    testWidgets('payments displays ${scenario.name}', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            paymentsScreenDataProvider.overrideWith(
              (ref) async => PaymentsScreenData(
                balance: _balance(
                  scenario.remaining,
                  credit: scenario.credit,
                ),
                payments: _emptyPage<CandidatePayment>(),
                statement: _emptyPage<CandidateStatementLine>(),
              ),
            ),
          ],
          child: const MaterialApp(home: CandidatePaymentsScreen()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text(scenario.label), findsOneWidget);
      expect(
        find.text(scenario.amount),
        findsOneWidget,
      );
    });
  }

  testWidgets('sessions show cancelled and absent status from the API',
      (tester) async {
    final repository = _SessionRepository([
      _session(id: 'cancelled', status: 'CANCELLED', price: 1.000),
      _session(id: 'absent', status: 'ABSENT', price: 2.000),
    ]);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          candidateDataRepositoryProvider.overrideWithValue(repository),
        ],
        child: const MaterialApp(home: CandidateSessionsScreen()),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(
      find.byType(DropdownButtonFormField<SessionPeriod>),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Toutes les séances').last);
    await tester.pumpAndSettle();

    expect(find.text('Statut : Annulée'), findsOneWidget);
    expect(find.text('Statut : Absent'), findsOneWidget);
  });
}

class _SessionRepository extends CandidateDataRepository {
  _SessionRepository(this.sessions) : super(Dio());

  final List<CandidateSession> sessions;

  @override
  Future<CandidatePage<CandidateSession>> getSessions({
    required int page,
    required int size,
    DateTime? dateFrom,
    DateTime? dateTo,
  }) async =>
      CandidatePage(
        items: sessions,
        page: page,
        totalPages: 1,
        last: true,
      );
}

CandidateBalance _balance(num remaining, {required bool credit}) =>
    CandidateBalance(
      totalSessions: MoneyAmount.fromJson(100),
      totalPaid: MoneyAmount.fromJson(100),
      remaining: MoneyAmount.fromJson(remaining),
      credit: credit,
    );

CandidateSession _session({
  required String id,
  required String status,
  required num price,
}) =>
    CandidateSession(
      id: id,
      sessionDate: DateTime(2025, 3, 4),
      startTime: '09:00:00',
      endTime: '10:30:00',
      durationHours: 1.5,
      instructor: 'Nadia Ben Ali',
      circuit: 'Centre-ville',
      price: MoneyAmount.fromJson(price),
      status: status,
      notes: null,
    );

CandidatePage<T> _emptyPage<T>() => const CandidatePage(
      items: [],
      page: 0,
      totalPages: 0,
      last: true,
    );
