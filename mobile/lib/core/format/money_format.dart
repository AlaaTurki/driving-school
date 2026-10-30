class MoneyAmount {
  const MoneyAmount.fromMilliunits(this.milliunits);

  factory MoneyAmount.fromJson(Object? value) {
    if (value is! num && value is! String) {
      throw FormatException('Montant invalide : $value');
    }
    if (value is num && !value.isFinite) {
      throw FormatException('Montant non fini : $value');
    }

    final match = RegExp(r'^([+-]?)(\d+)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$')
        .firstMatch(value.toString());
    if (match == null) {
      throw FormatException('Montant invalide : $value');
    }

    final fraction = match.group(3) ?? '';
    final exponent = int.parse(match.group(4) ?? '0');
    final decimalPlaces = fraction.length - exponent;
    var absoluteMilliunits = BigInt.parse('${match.group(2)}$fraction');
    if (decimalPlaces < 3) {
      absoluteMilliunits *= BigInt.from(10).pow(3 - decimalPlaces);
    } else if (decimalPlaces > 3) {
      final divisor = BigInt.from(10).pow(decimalPlaces - 3);
      if (absoluteMilliunits.remainder(divisor) != BigInt.zero) {
        throw FormatException('Le montant dépasse trois décimales : $value');
      }
      absoluteMilliunits ~/= divisor;
    }

    final milliunits = absoluteMilliunits.toInt();
    return MoneyAmount.fromMilliunits(
      match.group(1) == '-' ? -milliunits : milliunits,
    );
  }

  final int milliunits;

  bool get isNegative => milliunits < 0;
}

String formatMoney(Object? amount) {
  if (amount == null) {
    return '\u2014';
  }
  if (amount is num && !amount.isFinite) {
    return '\u2014';
  }
  final exactAmount =
      amount is MoneyAmount ? amount : MoneyAmount.fromJson(amount);
  final absolute = exactAmount.milliunits.abs();
  final integerPart = (absolute ~/ 1000).toString();
  final decimalPart = (absolute % 1000).toString().padLeft(3, '0');
  final groupedInteger = integerPart.replaceAllMapped(
    RegExp(r'\B(?=(\d{3})+(?!\d))'),
    (_) => '\u202f',
  );
  final sign = exactAmount.isNegative ? '\u002d' : '';

  return '$sign$groupedInteger,$decimalPart DT';
}
