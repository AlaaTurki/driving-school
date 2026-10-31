import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/date_format.dart';
import '../../../core/format/money_format.dart';
import '../../../core/widgets/candidate_page_scaffold.dart';
import '../../../features/auth/presentation/auth_controller.dart';
import '../../candidate/data/candidate_data.dart';

class CandidateHomeScreen extends ConsumerWidget {
  const CandidateHomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final home = ref.watch(homeScreenDataProvider);
    return CandidatePageScaffold(
      title: 'Espace candidat',
      showBackButton: false,
      actions: [
        IconButton(
          tooltip: 'Se déconnecter',
          onPressed: () => ref.read(authControllerProvider.notifier).logout(),
          icon: const Icon(Icons.logout),
        ),
      ],
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
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
              ),
              const SizedBox(height: 8),
              Text(
                'Prêt pour votre prochaine étape ?',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
              ),
              const SizedBox(height: 20),
              _NextSessionCard(session: data.nextSession),
              Card(
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor:
                        Theme.of(context).colorScheme.primaryContainer,
                    child: Icon(
                      Icons.account_balance_wallet_outlined,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                  title: Text(
                    data.balance.credit ? 'Crédit' : 'Reste à payer',
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                  subtitle: data.balance.credit
                      ? const Text('Crédit disponible')
                      : null,
                  trailing: Text(
                    formatMoney(data.balance.remaining),
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: Theme.of(context).colorScheme.primary,
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Text(
                'Mon espace',
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
              ),
              const SizedBox(height: 8),
              GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisSpacing: 10,
                mainAxisSpacing: 4,
                childAspectRatio: 1.25,
                children: const [
                  _DestinationTile(
                    title: 'Mes séances',
                    icon: Icons.directions_car_outlined,
                    route: '/sessions',
                  ),
                  _DestinationTile(
                    title: 'Mes paiements',
                    icon: Icons.payments_outlined,
                    route: '/payments',
                  ),
                  _DestinationTile(
                    title: 'Mon planning',
                    icon: Icons.calendar_month_outlined,
                    route: '/schedule',
                  ),
                  _DestinationTile(
                    title: 'Ma progression',
                    icon: Icons.trending_up,
                    route: '/skills',
                  ),
                ],
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
        color: Theme.of(context).colorScheme.primary,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Prochaine séance',
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      color: Theme.of(context).colorScheme.onPrimary,
                      fontWeight: FontWeight.w700,
                    ),
              ),
              const SizedBox(height: 8),
              if (session == null)
                Text(
                  'Aucune séance à venir pour le moment.',
                  style: TextStyle(
                    color: Theme.of(context)
                        .colorScheme
                        .onPrimary
                        .withValues(alpha: 0.82),
                  ),
                )
              else ...[
                Text(
                  formatDate(session!.sessionDate),
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.onPrimary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                Text(
                  '${formatTime(session!.startTime)} • '
                  '${_duration(session!.durationHours)}',
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.onPrimary,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Circuit : ${session!.circuit ?? 'Non renseigné'}',
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.onPrimary,
                  ),
                ),
                Text(
                  'Moniteur : ${session!.instructor}',
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.onPrimary,
                  ),
                ),
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
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () => context.push(route),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                CircleAvatar(
                  radius: 21,
                  backgroundColor:
                      Theme.of(context).colorScheme.primaryContainer,
                  child: Icon(icon, color: Theme.of(context).colorScheme.primary),
                ),
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        title,
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                    ),
                    Icon(
                      Icons.arrow_forward,
                      size: 17,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  ],
                ),
              ],
            ),
          ),
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
