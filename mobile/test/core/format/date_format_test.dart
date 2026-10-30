import 'package:flutter_test/flutter_test.dart';

import 'package:driving_school_mobile/core/format/date_format.dart';

void main() {
  test('formats dates as jj/mm/aaaa', () {
    expect(formatDate(DateTime(2026, 10, 30)), '30/10/2026');
    expect(formatDate(DateTime(2026, 1, 5)), '05/01/2026');
  });

  test('formats API times as hh:mm', () {
    expect(formatTime('09:05:00'), '09:05');
    expect(formatTime('23:59'), '23:59');
  });

  test('renders missing or invalid dates and times as an em dash', () {
    expect(formatDate(null), '\u2014');
    expect(formatTime(null), '\u2014');
    expect(formatTime('25:61:00'), '\u2014');
  });
}
