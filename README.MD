# Property Bill Automation System

A financial automation platform designed for property management portfolios to intake, parse, match, validate, approve, and execute payments for vendor invoices across multiple properties.

---

## Architecture & Technology Stack

* **Architecture Paradigm:** Monorepo, Contract-First (OpenAPI 3.0), Swagger for REST observability, Schema-First (Flyway) for DB Change management, PGAdmin for DB visualization.
* **Backend:** Java 21, Spring Boot 4.x, Spring Data JPA, Maven.
* **Frontend:** React + Vite (located under `/frontend`).
* **Database (Relational):** PostgreSQL 16 (financial ledger, state tracking, entity relations).
* **Storage (BLOB):** MinIO (S3-compatible storage for raw PDF invoices).
* **Payment Integration:** Stripe API (Test Mode).



---

## Repository Structure

```text
invoice-automation/
├── openapi.yaml                  # Root contract: single source of truth for REST APIs
├── docker-compose.yml            # Local infrastructure (PostgreSQL & MinIO)
├── .gitignore
├── .gitattributes
├── frontend/                     # React application
    ├── public/                   # Static assets (favicons, icons, etc.)
    ├── src/                      # Source code (Components, Pages, Assets)
    ├── package.json              # Frontend dependencies and npm scripts
└── backend/                      # Spring Boot application
    ├── pom.xml                   # Maven dependencies and code generators
    ├── mvnw / mvnw.cmd           # Maven wrapper
    └── src/main/
        ├── java/...              # Controllers, Entities, Repositories, Services
        └── resources/
            ├── application.yaml
            └── db/migration/     # Flyway SQL migrations (V1__..., V2__...)

```

---

## Local Environment Setup

### 1. Prerequisites
Ensure you have the following installed on your local machine:
*   [Node.js](https://nodejs.org) (v18+ recommended)
*   [Java 17+](https://adoptium.net)
*   [Docker Desktop](https://docker.com)

### 2. Start Infrastructure (PostgreSQL & MinIO)

From the repository root, start the supporting containers:

```bash
docker compose up -d

```

Verify services are running:

* **PostgreSQL:** `localhost:5432` (`database: invoice_automation_postgres`, `user: `, `password: `)
* **pgAdmin web console:** `http://localhost:5050` (`user: `, `password: `)
* **MinIO API:** `http://localhost:9000`
* **MinIO Web Console:** `http://localhost:9001` (User: ``, Password: ``)
* **Swagger Web Console:** `http://localhost:8080/swagger-ui`


---

## Database Migrations: Flyway

This project uses a **Schema-First** methodology via Flyway. The database schema in SQL is the primary source of truth for all storage rules and constraints; JPA entities strictly mirror the Flyway tables.

### File Naming Convention & Location

* **Location:** `backend/src/main/resources/db/migration/`
* Flyway Requires a Specific Location Path:** `src/main/resources/db/migration/`
* By standard convention in Spring Boot, Flyway scans the application classpath on startup specifically at `classpath:db/migration` looking for versioned scripts (`V1__...sql`, `V2__...sql`).
* To change the default** In `application.yml` (`spring.flyway.locations=classpath:custom/path`), but in 99% of professional Java/Spring projects, developers leave it at `src/main/resources/db/migration/` because every Spring developer, documentation tutorial, and CI/CD tool expects it there out of the box.
  *(Note: While path locations can technically be customized in `application.yml` via `spring.flyway.locations`, this project sticks strictly to the standard classpath default).*
* **Naming Pattern:** `V<Version>__<Description>.sql` (e.g., `V1__initial_schema.sql`, `V2__seed_test_data.sql`).
* Must start with a capital `V`.
* Must be followed by the version number.
* Must use **two consecutive underscores** (`__`).
* Must end in `.sql`.



### Applying Migrations

Flyway runs automatically whenever the Spring Boot service starts. To manually compile resources and execute pending migrations:

```bash
cd backend
./mvnw clean compile spring-boot:run

```
---

PGADMIN: register your Postgres server inside pgAdmin:

Step 1: Add a New Server

1. On the pgAdmin dashboard home screen, click Add New Server (or right-click Servers in the left sidebar and choose Register ➔ Server...).

Step 2: Fill out the Connection Details

A popup modal will appear. Fill out these two tabs using the values from your .env file:
1. General Tab:
   • Name: Give it a friendly label (e.g., Invoice DB Local)
2. Connection Tab:
   • Host name/address: Type host.docker.internal
   (Note: Since pgAdmin is running inside its own Docker container, using localhost tells pgAdmin to look inside itself. Using host.docker.internal tells it to look at your actual computer's localhost port where your database is exposed).
   • Port: 5432
   • Maintenance database: Enter your ${POSTGRES_DB} value: invoice_automation_postgres
   • Username: Enter your ${POSTGRES_USER} value
   • Password: Enter your ${POSTGRES_PASSWORD} value
   • Check the box for Save password? so you don't have to retype it.

Step 3: Save and Verify

Click Save. The sidebar on the left will populate with your server tree.
Expand Servers ➔ Invoice DB Local ➔ Databases ➔ invoice_automation_postgres ➔ Schemas ➔ public ➔ Tables. You will see both your properties table and the flyway_schema_history logging table right there!


1. Drill down into Servers ➔ PostgreSQL ➔ Databases ➔ [Your DB Name] ➔ Schemas ➔ public ➔ Tables.
2. Look for a brand new metadata table named flyway_schema_history. Flyway automatically creates this table to log every migration script version, description, execution date, and success status.


---

## API Contract & Code Generation: OpenAPI & Swagger

This project follows an **API Contract-First** development workflow. All endpoint interfaces and DTOs are defined in the root `openapi.yaml` and compiled directly into Java types for the backend and TypeScript types for the frontend.

`openapi.yaml` Lives at the Root or Top Level

** OpenAPI does not have a strict required location, but it is a best practice to place it in the project root for both frontend and backend access.**

* **The Convention in Monorepos:**
* Place `openapi.yaml` either in the **root directory** of the repository (`/openapi.yaml`) or in a shared documentation/contract folder (`/contracts/openapi.yaml` or `/docs/openapi.yaml`).
* In `backend/pom.xml`, we point the openapi plugin to openapi path:
```xml
<inputSpec>${project.basedir}/../openapi.yaml</inputSpec>

```

* In `frontend/package.json`, the frontend script points to the exact same file:
```json
"generate-api": "openapi-typescript-codegen --input ../openapi.yaml --output ./src/api"

```

### Generating Java Interfaces and DTOs

1. Define endpoints and data schemas in the root `/openapi.yaml`.
2. Run the code generation plugin via Maven inside `backend/`:

```bash
cd backend
./mvnw compile

```

3. **Output Location:** Generated files are written to:
```text
backend/target/generated-sources/openapi/src/main/java/org/dsd/cohort2026fall/tdr/
├── api/          # Spring @RestController interfaces (e.g., PropertiesApi.java)
└── model/        # Request & Response DTOs (e.g., PropertyResponse.java)

```


4. **Implementation:** Hand-written controllers implement the generated interfaces (e.g., `public class PropertyController implements PropertiesApi`), ensuring compile-time safety without risking manual code overwrites during regeneration.

Swagger for OPEN API
added
<dependency>
<groupId>org.springdoc</groupId>
<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
<version>3.1.0</version>
</dependency>

https://www.youtube.com/watch?v=YJK03LdXwfQ

---

## Running the Backend

From the `backend/` directory:

```bash
./mvnw clean spring-boot:run

```
From the root directory, you can run/redeploy the backend only with:

```bash
docker compose up -d --build backend
```

---

## Running the Frontend

From the `frontend/` directory, install dependencies and launch the hot-reloading development server:
```bash
npm install
npm run dev
```

---

## Running the Full Application Stack

From the root directory, you can run both backend and frontend concurrently using:

```bash
docker compose up -d --build
````


## Health Checks & Diagnostic Endpoints

Once the application is running on port `8080`, verify health and explore contracts using the following URLs:

| Service / Tool | Endpoint URL | Description |
| --- | --- | --- |
| **Spring Actuator Health** | `http://localhost:8080/actuator/health` | Service and database connectivity health check |
| **pgAdmin web console** | `http://localhost:5050` | PostgreSQL and FlyWay Migration monitoring |
| **Interactive Swagger UI** | `http://localhost:8080/swagger-ui.html` | Interactive REST client and OpenAPI specification visualizer |
| **Raw OpenAPI JSON Spec** | `http://localhost:8080/v3/api-docs` | Machine-readable API schema document for tooling |
| **MinIO Object Console** | `http://localhost:9001` | Browser interface for raw invoice PDF BLOB storage

|

---

##  Development Checklist

1. [ ] Ensure Docker daemon is active and run `docker compose up -d`.
2. [ ] Run `cd backend && ./mvnw clean compile` to apply `V1__initial_schema.sql` to Postgres and generate OpenAPI models.
3. [ ] Open `http://localhost:8080/swagger-ui.html` and verify the `/api/v1/properties` endpoints execute successfully against local database seed data.