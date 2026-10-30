String formatMoney(num? amount) {
  if (amount == null || !amount.isFinite) {
    return '\u2014';
  }

  final fixedAmount = amount.abs().toStringAsFixed(3);
  final decimalSeparator = fixedAmount.indexOf('.');
  final integerPart = decimalSeparator < 0
      ? fixedAmount
      : fixedAmount.substring(0, decimalSeparator);
  final decimalPart = decimalSeparator < 0
      ? '000'
      : fixedAmount.substring(decimalSeparator + 1);
  final groupedInteger = integerPart.replaceAllMapped(
    RegExp(r'\B(?=(\d{3})+(?!\d))'),
    (_) => '\u202f',
  );
  final sign = amount.isNegative ? '\u002d' : '';

  return '$sign$groupedInteger,$decimalPart DT';
}
