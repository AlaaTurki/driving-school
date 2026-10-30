class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.refreshToken,
    required this.email,
    required this.roles,
  });

  final String accessToken;
  final String refreshToken;
  final String email;
  final List<String> roles;

  bool get isCandidate => roles.contains('CANDIDATE');

  factory AuthSession.fromJson(Map<String, dynamic> json) {
    final roles = json['roles'];
    if (json['accessToken'] is! String ||
        json['refreshToken'] is! String ||
        json['email'] is! String ||
        roles is! List ||
        roles.any((role) => role is! String)) {
      throw const FormatException('Réponse d’authentification invalide.');
    }

    return AuthSession(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      email: json['email'] as String,
      roles: List<String>.from(roles),
    );
  }
}
