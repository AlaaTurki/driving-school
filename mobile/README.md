# Application mobile candidat

Application Flutter en Dart avec null safety. L'état d'authentification est géré par Riverpod (`StateNotifier`) ; go_router protège les routes et réagit aux changements de session. Dio centralise les appels API et le renouvellement des tokens.

## Configuration et lancement

Depuis `mobile/`, définir une URL locale puis lancer Flutter :

```powershell
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

`API_BASE_URL` est obligatoire et n'a aucune valeur de production intégrée. Sur l'émulateur Android, `localhost` pointe vers l'émulateur lui-même : utiliser `10.0.2.2` pour joindre le serveur local de l'ordinateur. Sur iOS Simulator, une API locale peut généralement être jointe avec `http://localhost:8080`.

Les tokens d'accès et de renouvellement sont stockés avec `flutter_secure_storage`. L'application renouvelle le couple de tokens au démarrage, partage un seul renouvellement lorsque plusieurs requêtes reçoivent simultanément un 401, puis rejoue ces requêtes. En cas d'échec, la session locale est effacée. Seuls les comptes ayant le rôle `CANDIDATE` peuvent accéder à l'espace authentifié.

## Tests

```powershell
flutter test
```

## Configuration native à générer/valider

Le SDK Flutter n'était pas disponible lors de la création de ce socle : les dossiers natifs `android/` et `ios/` n'ont pas encore été générés. Après validation, exécuter `flutter create --platforms=android,ios --org com.drivingschool .` depuis ce dossier, puis vérifier :

- Android : `applicationId` et `namespace` `com.drivingschool.mobile`, package de `MainActivity`, nom/identifiant de l'application, permission réseau et configuration `flutter_secure_storage` pour les versions Android ciblées.
- iOS : Bundle Identifier `com.drivingschool.mobile`, cible de déploiement, Keychain pour `flutter_secure_storage` et règles ATS adaptées à l'API locale HTTP (ne pas assouplir ATS en production).

La génération native et la configuration des plateformes restent en attente de validation.
