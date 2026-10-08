# Driving School API

Socle REST Spring Boot 3 / Java 21 : PostgreSQL, Flyway, Spring Security, JWT et authentification. Les rôles sont `ADMIN`, `INSTRUCTOR` et `CANDIDATE`. L'inscription publique crée exclusivement des comptes `CANDIDATE`.

## Lancer avec Docker Compose

1. Copier `.env.example` en `.env`.
2. Renseigner des valeurs locales fortes pour `DATABASE_PASSWORD` et `JWT_SECRET`. Le secret JWT doit être en Base64 et contenir au moins 32 octets. Par exemple, le générer avec `openssl rand -base64 32`.
3. Lancer `docker compose up --build`.

Compose configure l'API pour joindre PostgreSQL via le nom de service `postgres` sur le réseau Docker. Le port `15432` de la machine hôte est publié vers le port PostgreSQL `5432` du conteneur afin d'éviter les conflits avec d'autres serveurs PostgreSQL locaux. Ne configurez pas `DATABASE_URL=localhost` pour l'API conteneurisée : `localhost` désignerait le conteneur API lui-même. Flyway crée les tables au démarrage de l'API. La documentation Swagger est disponible sur `http://localhost:8080/swagger-ui.html`.

Pour créer le tout premier administrateur, renseigner dans `.env` :

```dotenv
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=une-phrase-de-passe-locale-longue
```

Redémarrer l'API une seule fois. La création échoue si un rôle `ADMIN` existe déjà ou si le mot de passe contient moins de 12 caractères. Ensuite, désactiver immédiatement `BOOTSTRAP_ADMIN_ENABLED`. Ces identifiants ne doivent pas être conservés dans les secrets d'un environnement partagé après initialisation.

## Lancer sans Docker pour l'API

Avec PostgreSQL disponible localement et Java 21/Maven installés, depuis la racine du projet :

```powershell
Set-Location backend
$env:DATABASE_URL = "jdbc:postgresql://127.0.0.1:15432/driving_school"
$env:DATABASE_USERNAME = "driving_school"
$env:DATABASE_PASSWORD = "votre-mot-de-passe-local"
$env:JWT_SECRET = (openssl rand -base64 32)
mvn spring-boot:run
```

Tests unitaires et intégration :

```powershell
Set-Location backend
mvn test
```

Le scénario `AuthFlowIntegrationTest` crée une base PostgreSQL avec Testcontainers ; Docker doit être disponible pour l'exécuter.

## Application Web Angular

L'application Angular d'administration et des moniteurs se trouve dans `frontend`. Après avoir démarré l'API et PostgreSQL, lancer dans un autre terminal depuis la racine du projet :

```powershell
Set-Location frontend
npm install
npm start
```

Ouvrir `http://localhost:4200`. Le proxy Angular transmet `/api` à `http://localhost:8080`. Le frontend comprend la connexion JWT, l'inscription des candidats, la gestion des profils candidats par les administrateurs et moniteurs, ainsi qu'un tableau d'accueil. Le module Candidats permet de rechercher les inscrits, filtrer leur statut actif/inactif et modifier leur nom, téléphone ou statut.

## Tester la connexion

Dans Swagger, ouvrez `POST /api/auth/login`, choisissez **Try it out**, puis envoyez :

```json
{
  "email": "admin@example.com",
  "password": "votre-mot-de-passe-local"
}
```

Pour créer un compte candidat, utilisez `POST /api/auth/register` avec un nom complet, une adresse e-mail, un numéro de téléphone et un mot de passe d'au moins 8 caractères. Une inscription réussie crée le compte et renvoie directement une session avec un jeton d'accès de 15 minutes et un refresh token de 7 jours. `POST /api/auth/refresh` échange un refresh token contre une nouvelle paire de jetons et invalide l'ancien. `POST /api/auth/logout` révoque le refresh token transmis.

L'API `GET /api/candidates` liste les profils candidats et `PUT /api/candidates/{id}` met à jour le nom complet, le téléphone et le statut (`ACTIVE` ou `INACTIVE`). Désactiver un compte candidat empêche sa connexion. Ces routes sont réservées aux rôles `ADMIN` et `INSTRUCTOR`. Les nouvelles inscriptions demandent le nom complet, l'e-mail, le téléphone et un mot de passe ; la migration Flyway ajoute les profils et reprend les anciens comptes candidats.

Postman peut envoyer la même requête en `POST http://localhost:8080/api/auth/login`, avec `Content-Type: application/json`. Une connexion valide renvoie un `accessToken`, un `refreshToken`, le type `Bearer`, l'expiration, l'identifiant utilisateur et ses rôles. Stocker le refresh token de façon sécurisée côté client ; seul son hash est conservé en base.

Les erreurs de validation renvoient un JSON structuré avec le statut, le chemin et les champs invalides. Un email ou mot de passe incorrect renvoie `401`.

