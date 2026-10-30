import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/format/date_format.dart';
import '../data/candidate_data.dart';
import 'sessions_screen.dart' show sessionStatusLabel;

class CandidateScheduleScreen extends ConsumerWidget {
  const CandidateScheduleScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final schedule = ref.watch(candidateScheduleProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Mon planning')),
      body: schedule.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => _ScheduleError(
          onRetry: () => ref.invalidate(candidateScheduleProvider),
        ),
        data: (days) => RefreshIndicator(
          onRefresh: () async {
            await ref
                .refresh(candidateScheduleProvider.future)
                .then<void>((_) {});
          },
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(16),
            children: [
              if (days.isEmpty)
                const Padding(
                  padding: EdgeInsets.all(24),
                  child: Center(child: Text('Aucune séance à venir au planning.')),
                )
              else
                ...days.map(
                  (day) => Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        child: Text(
                          formatDate(day.date),
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                      ),
                      ...day.sessions.map(
                        (session) => Card(
                          child: ListTile(
                            title: Text(
                              '${formatTime(session.startTime)} – '
                              '${formatTime(session.endTime)}',
                            ),
                            subtitle: Text(
                              'Moniteur : ${session.instructor}\n'
                              'Circuit : ${session.circuit ?? 'Non renseigné'}\n'
                              'Statut : ${sessionStatusLabel(session.status)}',
                            ),
                            isThreeLine: true,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ScheduleError extends StatelessWidget {
  const _ScheduleError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Impossible de charger le planning.'),
            TextButton(onPressed: onRetry, child: const Text('Réessayer')),
          ],
        ),
      );
}
