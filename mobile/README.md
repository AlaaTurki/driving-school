# Application mobile Flutter

Application Flutter en Dart destinée aux candidats. Elle utilise Riverpod pour l'état d'authentification, `go_router` pour la navigation protégée et Dio pour les appels à l'API.

## Prérequis

- Flutter installé.
- API backend disponible.

`API_BASE_URL` est obligatoire et aucune URL de production n'est intégrée à l'application. Depuis `mobile/`, installez les dépendances puis lancez l'application en définissant l'adresse de l'API :

```powershell
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8081
```

Cette URL convient à un émulateur Android lorsque l'API est lancée avec Docker Compose, qui publie l'API sur le port hôte `8081`. L'émulateur Android utilise `10.0.2.2` pour joindre l'hôte. Pour iOS Simulator, utilisez généralement `http://localhost:8081`. Si l'API est lancée directement avec Maven, son port par défaut est `8080` ; adaptez l'URL en conséquence.

## Authentification et stockage

Les jetons d'accès et de renouvellement sont conservés avec `flutter_secure_storage`. Au démarrage, l'application renouvelle la session ; les renouvellements simultanés sont partagés et les requêtes refusées avec un statut `401` sont rejouées après renouvellement. Si le renouvellement échoue, la session locale est effacée. Seuls les comptes ayant le rôle `CANDIDATE` peuvent accéder à l'espace authentifié.

## Espace candidat

Les écrans en lecture seule utilisent exclusivement les routes `/api/me/...` : profil,
solde, séances, paiements, relevé, planning et compétences. Les montants sont conservés
en millièmes entiers pour leur affichage, sans calcul financier sur le mobile. Les
formateurs partagés centralisent l'affichage des montants DT et des dates/horaires.

## Tests

```powershell
flutter test
```

## Plateformes natives

Les projets natifs `android/` et `ios/` sont présents. L'identifiant de l'application est `com.drivingschool.mobile` sur Android et iOS. Android autorise le trafic HTTP en clair uniquement dans le manifeste de débogage ; les builds de production doivent utiliser HTTPS. iOS conserve les paramètres App Transport Security par défaut : ne les assouplissez pas.

Les builds Android release utilisent encore la signature debug (`signingConfig = signingConfigs.getByName("debug")`). Configurez une signature de release avant toute publication.

## Documentation associée

- [README principal](../README.md)
- [API backend](../backend/README.md)
- [Frontend Angular](../frontend/README.md)
