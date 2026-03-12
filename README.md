# CivicLens

CivicLens is a fullstack capstone application that helps users discover their local, state, and federal representatives, view donor summaries, and track stances on representatives and bills.

## Stack

- **Backend:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, OpenAPI/Swagger
- **Frontend:** React, TypeScript, Vite, React Router
- **Infrastructure:** Colima (Postgres via Docker Compose), GitHub Actions (CI, tests, Semgrep, optional image build)

## Repository structure

- `backend/` – Spring Boot API (REST auth + GraphQL domain)
- `frontend/` – React + TypeScript SPA
- `infra/` – Docker Compose for local Postgres (use with Colima)
- `.github/workflows/` – CI pipeline

## Prerequisites

- Git ≥2.40, Node ≥18 LTS, Java 17, Colima + docker-compose (optional, for Postgres)
- `curl` or Postman for API testing

## Quick start

### 1. Database (optional: Colima)

With [Colima](https://github.com/abiosoft/colima) running, start Postgres using Docker Compose:

```bash
cd infra
docker-compose up -d
```

Postgres will be available at `localhost:5432` (default user/password/db as in `docker-compose.yml`).

### 2. Backend

```bash
cd backend
# Set DB URL if not using Colima, e.g. in application.yml or env:
# SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/civiclens
mvn spring-boot:run
```

- API base: `http://localhost:8080`
- REST endpoints (examples):
  - `GET /api/representatives?zip=94110` – reps by ZIP
  - `GET /api/watchlist` – current user's watchlist
  - `GET /api/donor-summaries?representativeId=1` – donor summary
  - `POST /api/stances` – save stance (form-encoded)
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

- App: `http://localhost:5173`

## Representative data (ZIP → reps)

The app resolves **federal representatives (House + Senate) by ZIP code** using:

- **[Who Is My Representative](https://whoismyrepresentative.com/api)** – Used by default. No API key. When you query `representatives(zip: "…")` and the database has no entries for that ZIP, the backend calls this API, maps the response into our domain, and persists it for future requests.
- **Config (optional):** `civiclens.whoismyrep.base-url` in `application.yml` (default: `http://whoismyrepresentative.com`).

**Other options** you could integrate later:

- **[Congress.gov API](https://api.congress.gov/)** – Official, rich member and legislation data. This project uses it (optionally) to show **recent bills** for each representative on the detail page. It requires a free API key.
- **Google Civic Information API** – Representative lookup by address; [scheduled to shut down April 30, 2025](https://developers.google.com/civic-information/docs/v2), so not recommended for new work.

### Congress.gov API key (optional but recommended)

If you want the **Recent bills on Congress.gov** section on the representative detail page to use real data:

1. **Request a key**
   - Visit [`https://api.congress.gov/`](https://api.congress.gov/).
   - Click “Sign Up” / “Request an API Key”.
   - Fill out the form (you can describe this app as an educational civic transparency project).
   - You’ll receive an API key by email.

2. **Expose it to the backend**
   - Set an environment variable in the shell where you run the backend:

     ```bash
     export CONGRESS_API_KEY="your-real-key-here"
     ```

   - Or add it to your shell profile (e.g. `~/.zshrc`) so it’s available in every new terminal.

3. **(Already wired) application.yml binding**
   - `backend/src/main/resources/application.yml` contains:

     ```yaml
     civiclens:
       congress:
         api-base-url: https://api.congress.gov/v3
         api-key: ${CONGRESS_API_KEY:}
     ```

   - You don’t need to change this; it just tells Spring Boot to read the key from `CONGRESS_API_KEY`.

4. **Verify it’s visible**
   - In the same shell where you run the backend:

     ```bash
     echo "$CONGRESS_API_KEY"
     ```

   - You should see your key (or at least a non-empty value).
   - Then start the backend:

     ```bash
     cd backend
     mvn spring-boot:run
     ```

With the key set, the backend will use the Congress.gov API to show the **three most recent sponsored/cosponsored bills** per representative, each with a real title, short description, and a link directly to that bill on `congress.gov`.

## Running tests

- **Backend:** `cd backend && mvn test` (JaCoCo report in `target/site/jacoco/`)
- **Frontend:** `cd frontend && npm test -- --coverage` (report in `frontend/coverage/`)
- **Lint:** `cd frontend && npm run lint`
- **Semgrep:** `semgrep scan --config p/owasp-top-ten .` (from repo root)

## Security considerations

- Passwords are hashed with **BCrypt**; never stored in plain text.
- **JWT** is used for stateless authentication; set `JWT_SECRET` in production and use HTTPS.
- **SAST:** Semgrep runs in CI (see `.github/workflows/ci.yml`). Locally: `semgrep scan --config p/owasp-top-ten .`
- **DAST:** Run OWASP ZAP or Nmap against the running app and document results. See `SECURITY.md` for details.
- **Hooks:** Optional pre-commit checks (e.g. `mvn test`, `npm run lint`) are described in `SECURITY.md`.

## Future enhancements (phase 2)

- **Civic Assistant:** REST endpoint (e.g. `POST /api/assistant/ask`) with a rule-based or LLM-backed answer service; frontend chat-style widget.
- **Elections:** Seed upcoming federal/state election dates; REST query (e.g. `GET /api/elections?state=CA`); frontend countdown and short explanations (primaries vs general).
- **Voting locations:** Integrate a polling-place API or link to state election sites; REST query (e.g. `GET /api/voting-locations?address=...`) and display on an "Elections" tab.

## License

MIT (or as required by your program).
