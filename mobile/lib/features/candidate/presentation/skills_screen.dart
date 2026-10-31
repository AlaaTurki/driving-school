import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/candidate_page_scaffold.dart';
import '../data/candidate_data.dart';

class CandidateSkillsScreen extends ConsumerWidget {
  const CandidateSkillsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final skills = ref.watch(candidateSkillsProvider);
    return CandidatePageScaffold(
      title: 'Ma progression',
      body: skills.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => _SkillsError(
          onRetry: () => ref.invalidate(candidateSkillsProvider),
        ),
        data: (data) => RefreshIndicator(
          onRefresh: () async {
            await ref.refresh(candidateSkillsProvider.future).then<void>((_) {});
          },
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                'Progression globale : ${data.overallProgress} %',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              LinearProgressIndicator(
                value: data.overallProgress.clamp(0, 100) / 100,
                minHeight: 8,
              ),
              const SizedBox(height: 16),
              if (data.skills.isEmpty)
                const Center(child: Text('Aucune compétence à afficher.'))
              else
                ...data.skills.map(
                  (skill) => Card(
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(child: Text(skill.name)),
                              Text('${skill.level} %'),
                            ],
                          ),
                          const SizedBox(height: 8),
                          LinearProgressIndicator(
                            value: skill.level.clamp(0, 100) / 100,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SkillsError extends StatelessWidget {
  const _SkillsError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Impossible de charger la progression.'),
            TextButton(onPressed: onRetry, child: const Text('Réessayer')),
          ],
        ),
      );
}
