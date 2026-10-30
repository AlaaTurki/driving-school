import 'package:flutter_riverpod/flutter_riverpod.dart';

final StateProvider<int> sessionExpiredProvider =
    StateProvider<int>((ref) => 0);
