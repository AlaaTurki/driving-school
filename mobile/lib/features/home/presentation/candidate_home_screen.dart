import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/date_format.dart';
import '../../../core/format/money_format.dart';
import '../../../features/auth/presentation/auth_controller.dart';
import '../../candidate/data/candidate_data.dart';

class CandidateHomeScreen extends ConsumerWidget {
  const CandidateHomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final home = ref.watch(homeScreenDataProvider);
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
      body: home.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => _LoadError(
          onRetry: () => ref.invalidate(homeScreenDataProvider),
        ),
        data: (data) => RefreshIndicator(
          onRefresh: () async {
            await ref.refresh(homeScreenDataProvider.future).then<void>((_) {});
          },
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                'Bonjour ${data.profile.firstName}',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 16),
              _NextSessionCard(session: data.nextSession),
              const SizedBox(height: 12),
              Card(
                child: ListTile(
                  leading: const Icon(Icons.account_balance_wallet_outlined),
                  title: Text(
                    data.balance.credit ? 'Crédit' : 'Reste à payer',
                  ),
                  subtitle: data.balance.credit
                      ? const Text('Crédit disponible')
                      : null,
                  trailing: Text(
                    formatMoney(data.balance.remaining),
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              const _DestinationTile(
                title: 'Mes séances',
                icon: Icons.directions_car_outlined,
                route: '/sessions',
              ),
              const _DestinationTile(
                title: 'Mes paiements',
                icon: Icons.payments_outlined,
                route: '/payments',
              ),
              const _DestinationTile(
                title: 'Mon planning',
                icon: Icons.calendar_month_outlined,
                route: '/schedule',
              ),
              const _DestinationTile(
                title: 'Ma progression',
                icon: Icons.trending_up,
                route: '/skills',
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NextSessionCard extends StatelessWidget {
  const _NextSessionCard({required this.session});

  final CandidateSession? session;

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Prochaine séance',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              if (session == null)
                const Text('Aucune séance à venir pour le moment.')
              else ...[
                Text(formatDate(session!.sessionDate)),
                Text(
                  '${formatTime(session!.startTime)} • '
                  '${_duration(session!.durationHours)}',
                ),
                Text('Circuit : ${session!.circuit ?? 'Non renseigné'}'),
                Text('Moniteur : ${session!.instructor}'),
              ],
            ],
          ),
        ),
      );
}

class _DestinationTile extends StatelessWidget {
  const _DestinationTile({
    required this.title,
    required this.icon,
    required this.route,
  });

  final String title;
  final IconData icon;
  final String route;

  @override
  Widget build(BuildContext context) => Card(
        child: ListTile(
          leading: Icon(icon),
          title: Text(title),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => context.go(route),
        ),
      );
}

class _LoadError extends StatelessWidget {
  const _LoadError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Impossible de charger les données.'),
              const SizedBox(height: 8),
              FilledButton(onPressed: onRetry, child: const Text('Réessayer')),
            ],
          ),
        ),
      );
}

String _duration(num hours) {
  final normalized = hours.toString().replaceFirst('.', ',');
  return '$normalized h';
}
