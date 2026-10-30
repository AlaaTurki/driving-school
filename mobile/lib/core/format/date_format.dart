String formatDate(DateTime? date) {
  if (date == null) {
    return '\u2014';
  }

  final day = date.day.toString().padLeft(2, '0');
  final month = date.month.toString().padLeft(2, '0');
  return '$day/$month/${date.year.toString().padLeft(4, '0')}';
}

String formatTime(String? time) {
  if (time == null ||
      !RegExp(r'^([01]\d|2[0-3]):[0-5]\d(?::[0-5]\d(?:\.\d+)?)?$')
          .hasMatch(time)) {
    return '\u2014';
  }

  return time.substring(0, 5);
}
