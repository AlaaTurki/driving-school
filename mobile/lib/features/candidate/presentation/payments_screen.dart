import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/format/date_format.dart';
import '../../../core/format/money_format.dart';
import '../../../core/widgets/candidate_page_scaffold.dart';
import '../data/candidate_data.dart';
import 'sessions_screen.dart' show sessionStatusLabel;

class CandidatePaymentsScreen extends ConsumerStatefulWidget {
  const CandidatePaymentsScreen({super.key});

  @override
  ConsumerState<CandidatePaymentsScreen> createState() =>
      _CandidatePaymentsScreenState();
}

class _CandidatePaymentsScreenState
    extends ConsumerState<CandidatePaymentsScreen> {
  final List<CandidatePayment> _additionalPayments = [];
  final List<CandidateStatementLine> _additionalStatement = [];
  int _paymentPage = 1;
  int _statementPage = 1;
  bool _loadingPayments = false;
  bool _loadingStatement = false;
  bool _paymentsLast = false;
  bool _statementLast = false;

  @override
  Widget build(BuildContext context) {
    final payments = ref.watch(paymentsScreenDataProvider);
    return CandidatePageScaffold(
      title: 'Mes paiements',
      body: payments.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => _ErrorState(
          onRetry: () => ref.invalidate(paymentsScreenDataProvider),
        ),
        data: (data) => RefreshIndicator(
          onRefresh: _refresh,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(16),
            children: [
              Text('Résumé', style: Theme.of(context).textTheme.titleLarge),
              _BalanceTile(
                title: 'Total séances',
                amount: data.balance.totalSessions,
              ),
              _BalanceTile(title: 'Total payé', amount: data.balance.totalPaid),
              _BalanceTile(
                title: data.balance.credit ? 'Crédit' : 'Reste à payer',
                amount: data.balance.remaining,
              ),
              const SizedBox(height: 16),
              Text('Paiements', style: Theme.of(context).textTheme.titleLarge),
              if (data.payments.items.isEmpty && _additionalPayments.isEmpty)
                const Text('Aucun paiement enregistré.')
              else
                ...[
                  ...data.payments.items,
                  ..._additionalPayments,
                ].map((payment) => _PaymentTile(payment: payment)),
              if (!(data.payments.last && _additionalPayments.isEmpty) &&
                  !(_additionalPayments.isNotEmpty && _paymentsLast))
                Align(
                  alignment: Alignment.center,
                  child: TextButton(
                    onPressed: _loadingPayments
                        ? null
                        : _loadMorePayments,
                    child: Text(
                      _loadingPayments ? 'Chargement…' : 'Charger plus de paiements',
                    ),
                  ),
                ),
              const Divider(height: 32),
              Text(
                'Relevé détaillé',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              if (data.statement.items.isEmpty && _additionalStatement.isEmpty)
                const Text('Aucune séance à afficher dans le relevé.')
              else
                ...[
                  ...data.statement.items,
                  ..._additionalStatement,
                ].map((line) => _StatementTile(line: line)),
              if (!(data.statement.last && _additionalStatement.isEmpty) &&
                  !(_additionalStatement.isNotEmpty && _statementLast))
                Align(
                  alignment: Alignment.center,
                  child: TextButton(
                    onPressed: _loadingStatement
                        ? null
                        : _loadMoreStatement,
                    child: Text(
                      _loadingStatement ? 'Chargement…' : 'Charger la suite du relevé',
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _refresh() async {
    setState(() {
      _additionalPayments.clear();
      _additionalStatement.clear();
      _paymentPage = 1;
      _statementPage = 1;
      _paymentsLast = false;
      _statementLast = false;
    });
    await ref.refresh(paymentsScreenDataProvider.future).then<void>((_) {});
  }

  Future<void> _loadMorePayments() async {
    setState(() => _loadingPayments = true);
    try {
      final page = await ref.read(candidateDataRepositoryProvider).getPayments(
            page: _paymentPage,
          );
      if (!mounted) return;
      setState(() {
        _additionalPayments.addAll(page.items);
        _paymentPage = page.page + 1;
        _paymentsLast = page.last;
      });
    } on Object catch (_) {
      _showLoadError();
    } finally {
      if (mounted) setState(() => _loadingPayments = false);
    }
  }

  Future<void> _loadMoreStatement() async {
    setState(() => _loadingStatement = true);
    try {
      final page =
          await ref.read(candidateDataRepositoryProvider).getStatement(
                page: _statementPage,
              );
      if (!mounted) return;
      setState(() {
        _additionalStatement.addAll(page.items);
        _statementPage = page.page + 1;
        _statementLast = page.last;
      });
    } on Object catch (_) {
      _showLoadError();
    } finally {
      if (mounted) setState(() => _loadingStatement = false);
    }
  }

  void _showLoadError() {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Chargement impossible. Réessayez.')),
    );
  }
}

class _BalanceTile extends StatelessWidget {
  const _BalanceTile({required this.title, required this.amount});

  final String title;
  final MoneyAmount amount;

  @override
  Widget build(BuildContext context) => ListTile(
        dense: true,
        contentPadding: EdgeInsets.zero,
        title: Text(title),
        trailing: Text(formatMoney(amount)),
      );
}

class _PaymentTile extends StatelessWidget {
  const _PaymentTile({required this.payment});

  final CandidatePayment payment;

  @override
  Widget build(BuildContext context) => Card(
        child: ListTile(
          title: Text(formatMoney(payment.amount)),
          subtitle: Text(
            '${formatDate(payment.paymentDate)} • '
            '${paymentMethodLabel(payment.paymentMethod)}'
            '${payment.notes?.isNotEmpty ?? false ? '\n${payment.notes}' : ''}',
          ),
          isThreeLine: payment.notes?.isNotEmpty ?? false,
        ),
      );
}

class _StatementTile extends StatelessWidget {
  const _StatementTile({required this.line});

  final CandidateStatementLine line;

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '${formatDate(line.date)} • ${formatTime(line.startTime)}',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              Text('Statut : ${sessionStatusLabel(line.status)}'),
              Text('Prix : ${formatMoney(line.price)}'),
              Text('Payé par séance : ${formatMoney(line.paidAmount)}'),
              Text('Reste : ${formatMoney(line.remaining)}'),
            ],
          ),
        ),
      );
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Impossible de charger les paiements.'),
            TextButton(onPressed: onRetry, child: const Text('Réessayer')),
          ],
        ),
      );
}

String paymentMethodLabel(String method) => switch (method) {
      'CASH' => 'Espèces',
      'CARD' => 'Carte',
      'BANK_TRANSFER' => 'Virement bancaire',
      'OTHER' => 'Autre',
      _ => method,
    };
