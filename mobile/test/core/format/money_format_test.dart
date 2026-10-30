import 'package:flutter_test/flutter_test.dart';

import 'package:driving_school_mobile/core/format/money_format.dart';

void main() {
  test('matches the Angular fr-TN DT currency output', () {
    expect(
      [56, 1012, 0, 250.5, -56].map(formatMoney).toList(),
      [
        '56,000 DT',
        '1\u202f012,000 DT',
        '0,000 DT',
        '250,500 DT',
        '\u002d56,000 DT',
      ],
    );
  });

  test('parses API decimals without floating-point arithmetic', () {
    expect(
      [
        MoneyAmount.fromJson('56.000').milliunits,
        MoneyAmount.fromJson('1012.000').milliunits,
        MoneyAmount.fromJson('0.000').milliunits,
        MoneyAmount.fromJson('250.500').milliunits,
        MoneyAmount.fromJson('-56.000').milliunits,
      ],
      [56000, 1012000, 0, 250500, -56000],
    );
    expect(formatMoney(MoneyAmount.fromJson('-56.000')), '\u002d56,000 DT');
  });

  test('renders missing and non-finite amounts as an em dash', () {
    expect(formatMoney(null), '\u2014');
    expect(formatMoney(double.nan), '\u2014');
    expect(formatMoney(double.infinity), '\u2014');
  });
}
