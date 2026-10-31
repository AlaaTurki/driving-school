import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/format/date_format.dart';
import '../../../core/format/money_format.dart';
import '../../../core/widgets/candidate_page_scaffold.dart';
import '../data/candidate_data.dart';

enum SessionPeriod { upcoming, history, all }

class CandidateSessionsScreen extends ConsumerStatefulWidget {
  const CandidateSessionsScreen({super.key});

  @override
  ConsumerState<CandidateSessionsScreen> createState() =>
      _CandidateSessionsScreenState();
}

class _CandidateSessionsScreenState
    extends ConsumerState<CandidateSessionsScreen> {
  SessionPeriod _period = SessionPeriod.upcoming;
  late Future<CandidatePage<CandidateSession>> _firstPage;
  final List<CandidateSession> _extraItems = [];
  int _nextPage = 1;
  bool _isLast = false;
  bool _loadingMore = false;
  String? _loadMoreError;

  @override
  void initState() {
    super.initState();
    _firstPage = _fetchPage(0);
  }

  @override
  Widget build(BuildContext context) => CandidatePageScaffold(
        title: 'Mes séances',
        body: FutureBuilder<CandidatePage<CandidateSession>>(
          future: _firstPage,
          builder: (context, snapshot) {
            if (snapshot.hasError) {
              return _MessageState(
                message: 'Impossible de charger les séances.',
                onRetry: _refresh,
              );
            }
            if (!snapshot.hasData) {
              return const Center(child: CircularProgressIndicator());
            }
            final firstPage = snapshot.data!;
            final isLast = _nextPage == 1 ? firstPage.last : _isLast;
            final loadedSessions = [...firstPage.items, ..._extraItems];
            final sessions = switch (_period) {
              SessionPeriod.upcoming =>
                loadedSessions.where(_isUpcoming).toList(growable: false),
              SessionPeriod.history =>
                loadedSessions.where(_isHistory).toList(growable: false),
              SessionPeriod.all => loadedSessions,
            };
            return RefreshIndicator(
              onRefresh: _refresh,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                children: [
                  DropdownButtonFormField<SessionPeriod>(
                    initialValue: _period,
                    decoration: const InputDecoration(labelText: 'Période'),
                    items: const [
                      DropdownMenuItem(
                        value: SessionPeriod.upcoming,
                        child: Text('À venir'),
                      ),
                      DropdownMenuItem(
                        value: SessionPeriod.history,
                        child: Text('Historique'),
                      ),
                      DropdownMenuItem(
                        value: SessionPeriod.all,
                        child: Text('Toutes les séances'),
                      ),
                    ],
                    onChanged: (period) {
                      if (period != null) {
                        setState(() {
                          _period = period;
                          _extraItems.clear();
                          _nextPage = 1;
                          _isLast = false;
                          _loadMoreError = null;
                          _firstPage = _fetchPage(0);
                        });
                      }
                    },
                  ),
                  const SizedBox(height: 12),
                  if (sessions.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(24),
                      child: Center(child: Text('Aucune séance pour cette période.')),
                    )
                  else
                    ...sessions.map((session) => _SessionCard(session: session)),
                  if (_loadMoreError != null)
                    Text(
                      _loadMoreError!,
                      style: TextStyle(color: Theme.of(context).colorScheme.error),
                    ),
                  if (!isLast)
                    Center(
                      child: TextButton(
                        onPressed: _loadingMore ? null : _loadMore,
                        child: Text(
                          _loadingMore ? 'Chargement…' : 'Charger la suite',
                        ),
                      ),
                    ),
                ],
              ),
            );
          },
        ),
      );

  Future<CandidatePage<CandidateSession>> _fetchPage(int page) {
    final today = DateTime.now();
    final date = DateTime(today.year, today.month, today.day);
    return ref.read(candidateDataRepositoryProvider).getSessions(
          page: page,
          size: 20,
          dateFrom: _period == SessionPeriod.upcoming ? date : null,
          dateTo: _period == SessionPeriod.history ? date : null,
        );
  }

  Future<void> _refresh() async {
    setState(() {
      _extraItems.clear();
      _nextPage = 1;
      _isLast = false;
      _loadMoreError = null;
      _firstPage = _fetchPage(0);
    });
    await _firstPage;
  }

  Future<void> _loadMore() async {
    setState(() {
      _loadingMore = true;
      _loadMoreError = null;
    });
    try {
      final page = await _fetchPage(_nextPage);
      if (!mounted) return;
      setState(() {
        _extraItems.addAll(page.items);
        _nextPage = page.page + 1;
        _isLast = page.last;
      });
    } on Object catch (_) {
      if (mounted) {
        setState(() => _loadMoreError = 'Chargement impossible. Réessayez.');
      }
    } finally {
      if (mounted) setState(() => _loadingMore = false);
    }
  }
}

class _SessionCard extends StatelessWidget {
  const _SessionCard({required this.session});

  final CandidateSession session;

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '${formatDate(session.sessionDate)} • '
                '${formatTime(session.startTime)}',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              Text(
                'Durée : '
                '${session.durationHours.toString().replaceFirst('.', ',')} h',
              ),
              Text('Circuit : ${session.circuit ?? 'Non renseigné'}'),
              Text('Moniteur : ${session.instructor}'),
              Text('Prix : ${formatMoney(session.price)}'),
              Text('Statut : ${sessionStatusLabel(session.status)}'),
              if (session.notes?.isNotEmpty ?? false)
                Text('Remarques du moniteur : ${session.notes}'),
            ],
          ),
        ),
      );
}

class _MessageState extends StatelessWidget {
  const _MessageState({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(message),
            TextButton(onPressed: onRetry, child: const Text('Réessayer')),
          ],
        ),
      );
}

String sessionStatusLabel(String status) => switch (status) {
      'PLANNED' => 'À venir',
      'DONE' => 'Effectuée',
      'CANCELLED' => 'Annulée',
      'ABSENT' => 'Absent',
      _ => status,
    };

bool _isUpcoming(CandidateSession session) {
  return session.status == 'PLANNED' &&
      !_sessionStart(session).isBefore(DateTime.now());
}

bool _isHistory(CandidateSession session) =>
    _sessionStart(session).isBefore(DateTime.now());

DateTime _sessionStart(CandidateSession session) {
  final time = session.startTime.split(':');
  return DateTime(
    session.sessionDate.year,
    session.sessionDate.month,
    session.sessionDate.day,
    int.parse(time[0]),
    int.parse(time[1]),
  );
}
