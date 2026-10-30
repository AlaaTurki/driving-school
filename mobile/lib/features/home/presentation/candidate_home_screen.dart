import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/presentation/auth_controller.dart';

class CandidateHomeScreen extends ConsumerWidget {
  const CandidateHomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Espace candidat'),
        actions: [
          IconButton(
            tooltip: 'Se déconnecter',
            onPressed: () => ref.read(authControllerProvider.notifier).logout(),
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: const Center(
        child: Text('Votre compte candidat est connecté.'),
      ),
    );
  }
}
