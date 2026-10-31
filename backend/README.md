# API backend

API REST de l'auto-école, construite avec Java 21 et Spring Boot 3.5. Elle utilise PostgreSQL, Spring Data JPA, Spring Security, des jetons JWT et Flyway pour les migrations de schéma.

## Fonctionnalités

- Authentification et inscription des candidats, renouvellement et révocation des sessions.
- Gestion des candidats, moniteurs, véhicules, circuits et compétences.
- Tableau de bord, planning et séances de conduite avec protection contre les conflits de réservation.
- Paiements, soldes et relevés financiers.
- Documentation OpenAPI/Swagger.

Les rôles principaux sont `ADMIN`, `INSTRUCTOR` et `CANDIDATE`. L'inscription publique crée uniquement des comptes `CANDIDATE`.

## Démarrage avec Docker Compose

Depuis la racine du dépôt, copiez `.env.example` vers `.env`, puis définissez des valeurs locales pour `DATABASE_PASSWORD` et `JWT_SECRET`. Le secret JWT doit être une valeur Base64 contenant au moins 32 octets ; vous pouvez en générer un avec `openssl rand -base64 32`.

```powershell
docker compose up --build
```

Compose démarre PostgreSQL et l'API. L'API est disponible sur `http://localhost:8081` et PostgreSQL sur le port hôte `15432`. Dans le réseau Compose, l'API se connecte au service `postgres` sur le port `5432`. Ne définissez pas `DATABASE_URL` sur `localhost` pour l'API conteneurisée.

Flyway applique les migrations au démarrage. Swagger est disponible sur `http://localhost:8081/swagger-ui.html`.

### Créer le premier compte administrateur

Avant le tout premier démarrage, définissez dans `.env` :

```dotenv
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=une-phrase-de-passe-locale-longue
```

Après l'initialisation, désactivez immédiatement `BOOTSTRAP_ADMIN_ENABLED`. La création échoue si un compte administrateur existe déjà ou si le mot de passe comporte moins de 12 caractères. N'enregistrez pas ces identifiants dans un dépôt ou dans les secrets d'un environnement partagé après l'initialisation.

## Démarrage local de l'API

Installez Java 21 et Maven, puis démarrez PostgreSQL en arrière-plan (par exemple avec `docker compose up -d postgres`). Depuis la racine du dépôt, configurez l'environnement PowerShell et lancez l'application :

```powershell
Set-Location backend
$env:DATABASE_URL = "jdbc:postgresql://127.0.0.1:15432/driving_school"
$env:DATABASE_USERNAME = "driving_school"
$env:DATABASE_PASSWORD = "votre-mot-de-passe-local"
$env:JWT_SECRET = (openssl rand -base64 32)
mvn spring-boot:run
```

L'API locale est disponible sur `http://localhost:8080`. L'interface Swagger est à `http://localhost:8080/swagger-ui.html`.

## Tests

```powershell
Set-Location backend
mvn test
```

Les tests d'intégration PostgreSQL utilisent Testcontainers ; Docker doit être disponible pour les exécuter.

## Documentation associée

- [README principal](../README.md)
- [Frontend Angular](../frontend/README.md)
- [Application mobile Flutter](../mobile/README.md)
