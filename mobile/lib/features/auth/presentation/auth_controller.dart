import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_client.dart';
import '../../../core/auth/auth_status.dart';
import '../../../core/auth/session_expired_provider.dart';
import '../domain/auth_repository.dart';
import '../domain/auth_session.dart';

class AuthState {
  const AuthState({
    this.status = AuthStatus.loading,
    this.message,
  });

  final AuthStatus status;
  final String? message;

  AuthState copyWith({
    AuthStatus? status,
    String? message,
    bool clearMessage = false,
  }) {
    return AuthState(
      status: status ?? this.status,
      message: clearMessage ? null : message ?? this.message,
    );
  }
}

final StateNotifierProvider<AuthController, AuthState> authControllerProvider =
    StateNotifierProvider<AuthController, AuthState>((ref) {
  final controller = AuthController(ref.watch(authRepositoryProvider));
  ref.listen<int>(sessionExpiredProvider, (previous, next) {
    if (previous != next) {
      controller.onSessionExpired();
    }
  });
  return controller;
});

class AuthController extends StateNotifier<AuthState> {
  AuthController(this._repository) : super(const AuthState()) {
    restoreSession();
  }

  final AuthRepository _repository;

  Future<void> restoreSession() async {
    try {
      final session = await _repository.restoreSession();
      if (session == null) {
        state = const AuthState(status: AuthStatus.unauthenticated);
      } else {
        await _acceptSession(session);
      }
    } on Object catch (error) {
      state = AuthState(
        status: AuthStatus.unauthenticated,
        message: _errorMessage(error),
      );
    }
  }

  Future<void> login({
    required String email,
    required String password,
  }) async {
    state = const AuthState(status: AuthStatus.loading);
    try {
      await _acceptSession(await _repository.login(
        email: email,
        password: password,
      ));
    } on Object catch (error) {
      state = AuthState(
        status: AuthStatus.unauthenticated,
        message: _errorMessage(error),
      );
    }
  }

  Future<void> register({
    required String fullName,
    required String email,
    required String phone,
    required String password,
  }) async {
    state = const AuthState(status: AuthStatus.loading);
    try {
      await _acceptSession(await _repository.register(
        fullName: fullName,
        email: email,
        phone: phone,
        password: password,
      ));
    } on Object catch (error) {
      state = AuthState(
        status: AuthStatus.unauthenticated,
        message: _errorMessage(error),
      );
    }
  }

  Future<void> logout() async {
    state = const AuthState(status: AuthStatus.loading);
    Object? logoutError;
    try {
      await _repository.logout();
    } on Object catch (error) {
      logoutError = error;
    }
    state = AuthState(
      status: AuthStatus.unauthenticated,
      message: logoutError == null
          ? null
          : 'La session locale est fermée, mais le serveur n’a pas confirmé '
              'la révocation. ${_errorMessage(logoutError)}',
    );
  }

  void onSessionExpired() {
    state = const AuthState(
      status: AuthStatus.unauthenticated,
      message: 'Votre session a expiré. Veuillez vous reconnecter.',
    );
  }

  Future<void> _acceptSession(AuthSession session) async {
    if (!session.isCandidate) {
      state = const AuthState(
        status: AuthStatus.unauthenticated,
        message: 'Cette application est réservée aux candidats.',
      );
      try {
        await _repository.logout();
      } on Object catch (error) {
        state = AuthState(
          status: AuthStatus.unauthenticated,
          message: 'Cette application est réservée aux candidats. '
              'La révocation côté serveur a échoué : ${_errorMessage(error)}',
        );
      }
      return;
    }
    state = const AuthState(status: AuthStatus.authenticated);
  }

  String _errorMessage(Object error) {
    if (error is DioException) {
      final statusCode = error.response?.statusCode;
      if (statusCode == 401) {
        return 'Identifiants incorrects.';
      }
      if (error.response == null) {
        return 'Connexion au serveur impossible. Vérifiez votre réseau puis réessayez.';
      }
      return 'Le serveur a rencontré une erreur. Veuillez réessayer.';
    }
    if (error is FormatException) {
      return error.message;
    }
    return 'Une erreur inattendue est survenue. Veuillez réessayer.';
  }
}
