# Driving School

Application de gestion d'auto-école composée d'une API REST Spring Boot, d'une interface Web Angular et d'une application candidat Flutter. Le backend utilise PostgreSQL, Flyway, Spring Security et JWT.

## Fonctionnalités

- Gestion des candidats avec les statuts `ACTIVE`, `INACTIVE`, `COMPLETED` et `SUSPENDED`.
- Gestion des moniteurs, des véhicules et des circuits.
- Création, consultation et suivi des séances de conduite, avec détection des conflits de réservation.
- Gestion des paiements, relevés et soldes calculés à partir des séances facturables et des paiements enregistrés.
- Planning des séances pour l'administration, les moniteurs et les candidats.
- Tableaux de bord pour l'administration et les moniteurs.
- Suivi de la progression des candidats sur neuf compétences.
- Authentification par jetons et contrôle d'accès selon les rôles `ADMIN`, `INSTRUCTOR` et `CANDIDATE`.

L'inscription publique crée uniquement des comptes `CANDIDATE`. Les moniteurs sont créés par l'administration.

## Endpoints principaux

Les routes ci-dessous sont définies par les contrôleurs Spring. Les appels métier nécessitent un jeton Bearer, sauf les routes d'authentification.

| Rôle | Endpoints principaux |
|---|---|
| Public / authentification | `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/refresh`, `POST /api/auth/logout` |
| `ADMIN` | `GET /api/dashboard`; gestion des candidats via `/api/candidates` (création, modification, suppression et rôles inclus); gestion des moniteurs via `/api/instructors`; gestion des véhicules et circuits via `/api/vehicles` et `/api/circuits`; gestion des séances via `/api/driving-sessions` (suppression réservée à l'administration); gestion des paiements via `/api/payments` |
| `INSTRUCTOR` | `GET /api/dashboard/instructor`, `GET /api/instructors/me`; consultation des candidats, véhicules et circuits; gestion et consultation des séances via `/api/driving-sessions`; consultation du planning via `/api/schedule`; accès aux soldes, paiements, relevés et compétences des candidats auxquels le moniteur est autorisé |
| `CANDIDATE` | Profil `GET /api/me`; séances `GET /api/me/sessions`; solde `GET /api/me/balance`; paiements `GET /api/me/payments`; relevé `GET /api/me/statement`; planning `GET /api/me/schedule`; progression `GET /api/me/skills` |
| `ADMIN` et `INSTRUCTOR` | Consultation des candidats, véhicules, circuits et planning (`GET /api/candidates`, `/api/vehicles`, `/api/circuits`, `/api/schedule`); disponibilité `GET /api/schedule/availability`; consultation et mise à jour des compétences via `/api/candidates/{candidateId}/skills`; endpoints financiers par candidat sous `/api/candidates/{candidateId}` avec contrôle d'accès du service |

Les endpoints `/api/me/...` déterminent l'identité à partir du compte authentifié. Les routes de gestion et de consultation pour une autre personne utilisent un identifiant dans le chemin selon leur contrôleur.

## Règles métier principales

- Seules les séances au statut `DONE` ou `ABSENT` sont facturables.
- Le solde est calculé par le backend à partir des séances facturables et des paiements ; il n'est jamais saisi comme une valeur indépendante.
- Les conflits de réservation impliquant un moniteur, un véhicule ou un candidat sont refusés avec HTTP `409 Conflict`. La réponse de conflit de planning peut inclure la ressource et la séance en conflit.

## Lancement en développement

### Docker Compose

Depuis la racine, configurez localement les variables requises dans `.env` à partir de `.env.example`, puis lancez :

```powershell
docker compose up --build
```

Compose démarre PostgreSQL et l'API. L'API est accessible sur `http://localhost:8081` (port conteneur `8080`) et PostgreSQL sur `localhost:15432`. Les migrations Flyway sont appliquées au démarrage. Swagger est disponible sur `http://localhost:8081/swagger-ui.html`.

Pour créer le premier administrateur, configurez localement `BOOTSTRAP_ADMIN_ENABLED`, `BOOTSTRAP_ADMIN_EMAIL` et `BOOTSTRAP_ADMIN_PASSWORD` dans `.env`, puis redémarrez l'API. Désactivez ensuite l'initialisation (`BOOTSTRAP_ADMIN_ENABLED=false`). L'inscription publique reste réservée aux candidats.

### API avec Maven

Java 21, Maven et PostgreSQL doivent être disponibles. Configurez localement `DATABASE_PASSWORD` et `JWT_SECRET` ; les paramètres par défaut de la base pointent vers PostgreSQL sur `127.0.0.1:15432`. Puis :

```powershell
Set-Location backend
mvn spring-boot:run
```

L'API est accessible sur `http://localhost:8080`. Pour les tests : `mvn test` depuis `backend/`. Les tests d'intégration PostgreSQL nécessitent Docker pour Testcontainers.

### Frontend Angular

Après démarrage de l'API, depuis `frontend/` :

```powershell
npm ci
npm start
```

L'application est accessible sur `http://localhost:4200`. Le proxy de développement cible l'API locale sur le port `8080` ; adaptez `frontend/proxy.conf.json` si l'API tourne via Compose sur le port `8081`.

### Application mobile Flutter

Flutter installé, depuis `mobile/` :

```powershell
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8081
```

`10.0.2.2` permet à l'émulateur Android de joindre l'hôte. Pour iOS Simulator, utilisez généralement `http://localhost:8081`. Si l'API est lancée avec Maven, utilisez le port `8080`. L'URL est configurée par `API_BASE_URL` et n'est pas intégrée en dur à l'application.

## Limites connues

- Les notifications Firebase ne sont pas implémentées.
- La CI n'est pas configurée.
- Une configuration Docker destinée à la production n'est pas fournie.
- Le déploiement n'est pas mis en place.

## Documentation des composants

- [API backend](backend/README.md)
- [Frontend Angular](frontend/README.md)
- [Application mobile Flutter](mobile/README.md)
