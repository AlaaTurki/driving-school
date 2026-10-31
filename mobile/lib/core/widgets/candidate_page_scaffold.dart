import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class CandidatePageScaffold extends StatelessWidget {
  const CandidatePageScaffold({
    required this.title,
    required this.body,
    this.actions,
    this.showBackButton = true,
    this.fallbackLocation = '/home',
    super.key,
  });

  final String title;
  final Widget body;
  final List<Widget>? actions;
  final bool showBackButton;
  final String fallbackLocation;

  void _goBack(BuildContext context) {
    final router = GoRouter.maybeOf(context);
    if (router == null) return;
    if (router.canPop()) {
      router.pop();
    } else {
      router.go(fallbackLocation);
    }
  }

  @override
  Widget build(BuildContext context) {
    final router = GoRouter.maybeOf(context);
    final canPop = router?.canPop() ?? true;
    return PopScope<void>(
      canPop: canPop,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop && router != null && !router.canPop()) {
          router.go(fallbackLocation);
        }
      },
      child: Scaffold(
        appBar: AppBar(
          automaticallyImplyLeading: false,
          leading: showBackButton
              ? IconButton(
                  tooltip: 'Retour',
                  onPressed: () => _goBack(context),
                  icon: const Icon(Icons.arrow_back),
                )
              : null,
          title: Text(title),
          actions: actions,
        ),
        body: body,
      ),
    );
  }
}
