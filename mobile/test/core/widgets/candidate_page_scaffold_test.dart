import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:driving_school_mobile/core/widgets/candidate_page_scaffold.dart';

void main() {
  testWidgets('back button returns to the fallback when opened directly',
      (tester) async {
    final router = GoRouter(
      initialLocation: '/sessions',
      routes: [
        GoRoute(
          path: '/home',
          builder: (context, state) => const Scaffold(body: Text('Accueil')),
        ),
        GoRoute(
          path: '/sessions',
          builder: (context, state) => const CandidatePageScaffold(
            title: 'Mes séances',
            body: Text('Séances'),
          ),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.tap(find.byTooltip('Retour'));
    await tester.pumpAndSettle();

    expect(find.text('Accueil'), findsOneWidget);
  });

  testWidgets('back button pops to the previous page when available',
      (tester) async {
    final router = GoRouter(
      initialLocation: '/home',
      routes: [
        GoRoute(
          path: '/home',
          builder: (context, state) => Scaffold(
            body: TextButton(
              onPressed: () => context.push('/sessions'),
              child: const Text('Ouvrir les séances'),
            ),
          ),
        ),
        GoRoute(
          path: '/sessions',
          builder: (context, state) => const CandidatePageScaffold(
            title: 'Mes séances',
            body: Text('Séances'),
          ),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.tap(find.text('Ouvrir les séances'));
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Retour'));
    await tester.pumpAndSettle();

    expect(find.text('Ouvrir les séances'), findsOneWidget);
  });
}
