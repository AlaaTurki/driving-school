# Frontend Angular

Application web Angular 20 pour les administrateurs, les moniteurs et les candidats. L'interface utilise Angular Material et communique avec l'API Spring Boot au moyen d'un proxy de développement.

## Fonctionnalités

- Connexion, inscription des candidats, gestion de session JWT et contrôle d'accès selon le rôle.
- Tableaux de bord adaptés aux rôles.
- Gestion des candidats et de leur progression.
- Gestion des moniteurs, véhicules et circuits.
- Planning, séances de conduite et paiements.
- Espace web candidat.

Le jeton d'accès est conservé dans `sessionStorage` et supprimé à la fermeture de l'onglet.

## Prérequis

- Node.js et npm.
- API backend disponible. Le proxy de développement dans `proxy.conf.json` cible `http://localhost:8080`, l'adresse par défaut de l'API démarrée localement avec Maven.

Pour lancer l'API avec Docker Compose, elle est exposée sur le port `8081`. Adaptez la cible du proxy à `http://localhost:8081` si vous utilisez cette configuration.

## Installation et serveur de développement

Depuis `frontend/` :

```powershell
npm ci
npm start
```

Ouvrez `http://localhost:4200`. Le serveur Angular transmet les requêtes `/api` à l'API configurée dans `proxy.conf.json`.

## Build et tests

```powershell
npm run build
npm test
```

Le build de production est généré dans `dist/`. Les tests unitaires utilisent Karma et Jasmine.

## Documentation associée

- [README principal](../README.md)
- [API backend](../backend/README.md)
- [Application mobile Flutter](../mobile/README.md)
