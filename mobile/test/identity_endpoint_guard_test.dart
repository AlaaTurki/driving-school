import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test('lib contains no candidate identity in API calls or candidate routes', () {
    final libDirectory = Directory('lib');
    expect(libDirectory.existsSync(), isTrue, reason: 'Run tests from mobile/.');

    final violations = <String>[];
    for (final entity in libDirectory.listSync(recursive: true)) {
      if (entity is! File || !entity.path.endsWith('.dart')) {
        continue;
      }
      final source = entity.readAsStringSync();
      for (final forbidden in ['candidateId', 'candidate_id', '/candidates/']) {
        if (source.contains(forbidden)) {
          violations.add('${entity.path}: $forbidden');
        }
      }
    }

    expect(violations, isEmpty, reason: violations.join('\n'));
  });
}
