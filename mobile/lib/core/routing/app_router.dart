import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../auth/auth_status.dart';
import '../../features/auth/presentation/auth_controller.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/auth/presentation/register_screen.dart';
import '../../features/home/presentation/candidate_home_screen.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final refreshListenable = ValueNotifier<AuthStatus>(
    ref.read(authControllerProvider).status,
  );
  ref.listen<AuthState>(authControllerProvider, (_, next) {
    refreshListenable.value = next.status;
  });

  final router = GoRouter(
    initialLocation: '/login',
    refreshListenable: refreshListenable,
    redirect: (context, state) {
      final authStatus = ref.read(authControllerProvider).status;
      if (authStatus == AuthStatus.loading) {
        return null;
      }
      final isAuthRoute =
          state.matchedLocation == '/login' ||
          state.matchedLocation == '/register';
      if (authStatus == AuthStatus.authenticated && isAuthRoute) {
        return '/home';
      }
      if (authStatus == AuthStatus.unauthenticated && !isAuthRoute) {
        return '/login';
      }
      return null;
    },
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: '/register',
        builder: (context, state) => const RegisterScreen(),
      ),
      GoRoute(
        path: '/home',
        builder: (context, state) => const CandidateHomeScreen(),
      ),
    ],
  );
  ref.onDispose(() {
    router.dispose();
    refreshListenable.dispose();
  });
  return router;
});
