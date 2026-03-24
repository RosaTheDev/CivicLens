# CivicLens

CivicLens is a fullstack capstone application that helps users discover their local, state, and federal representatives, view donor summaries, and track stances on representatives and bills.

## Stack

- **Backend:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, OpenAPI/Swagger
- **Frontend:** React, TypeScript, Vite, React Router
- **Infrastructure:** Docker Compose (Postgres), Colima on macOS, Docker Desktop on Windows, GitHub Actions (CI, tests, Semgrep, optional image build)

## Repository structure

- `backend/` – Spring Boot API (REST auth + GraphQL domain)
- `frontend/` – React + TypeScript SPA
- `infra/` – Docker Compose for local Postgres (use with Colima)
- `.github/workflows/` – CI pipeline

## Prerequisites

- Git ≥2.40
- Node ≥18 LTS
- Java 17 + Maven
- Docker runtime for local Postgres:
  - **macOS:** Colima + Docker CLI/Compose
  - **Windows:** Docker Desktop (WSL2 backend recommended)
- `curl` or Postman for API testing

## Colima on macOS (what and why)

`Colima` (Container on Lima) is a lightweight Linux VM that runs Docker containers on macOS.  
This project uses it so Postgres can run locally via Docker Compose without requiring Docker Desktop.

The macOS setup script now handles runtime setup automatically:

- detects available runtime tools (`colima`, Docker-compatible engine including Rancher Desktop, or `podman`)
- uses one of those automatically and only stops containers that conflict on port `55432`
- if none are installed, prompts you to install one (`Colima`, `Docker Desktop`, `Rancher Desktop`, or `Podman`)

## Quick start scripts (recommended)

From the repo root, run:

### macOS

```bash
chmod +x scripts/setup-and-run-mac.sh
./scripts/setup-and-run-mac.sh
```

### Windows (PowerShell)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-and-run-windows.ps1
```

Both scripts:

- detect and initialize a supported container runtime automatically
- if no runtime is installed, prompt to install one and continue setup
- stop only conflicting resources used by CivicLens (containers on `55432`, host listeners on `8080`, `5173`, and `55432`)
- generate local DB credentials in repo-root `.env.local` (gitignored) if missing
- auto-reconcile DB password drift with existing local Postgres volumes (legacy fallback + safe reset when needed)
- install a Git pre-commit hook that runs `gitleaks` on staged changes to catch secrets before commit
- install project dependencies (`npm install`, Maven dependency prefetch)
- reset/start this project's Postgres with Docker Compose (`civiclens` compose project)
- start backend (`http://localhost:8080`)
- start frontend (`http://localhost:5173`)
- wait for health checks, print a clear "CivicLens is running" success banner, and auto-open the frontend URL in your default browser

## Manual quick start

### 1. Database

```bash
cd infra
docker compose up -d
```

Postgres is exposed on `localhost:55432` (user/password/db in `infra/docker-compose.yml`).

If you are running manually (without scripts), set database env vars first:

```bash
export CIVICLENS_DB_USER="civiclens"
export CIVICLENS_DB_PASSWORD="your-local-db-password"
export CIVICLENS_DB_NAME="civiclens"
```

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

- API base: `http://localhost:8080`
- App: `http://localhost:5173`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Windows notes

- This repo is fully runnable on Windows with Docker-compatible runtime tools.
- The Windows script detects available runtime tools (`docker`/Docker Desktop or Rancher, `podman`, optional `colima` in WSL scenarios).
- If no runtime is present, it prompts for an install choice and continues.
- It stops only containers already occupying port `55432`, then starts CivicLens cleanly.
- If you hit line-ending issues in scripts, use PowerShell and run from the repository root.
- If backend cannot connect to Postgres, verify port `55432` is available and container `civiclens-postgres` is healthy.

## Windows alternatives if Docker Desktop is unavailable

If Docker Desktop cannot be used in your environment (licensing, policy, admin restrictions), common alternatives are:

- **[Rancher Desktop](https://rancherdesktop.io/)** with `dockerd (moby)` enabled for Docker-compatible workflows.
- **[Podman Desktop](https://podman-desktop.io/)** with Podman Compose (or Docker-compat mode where available).
- **[Colima in WSL2 Linux](https://github.com/abiosoft/colima)** by running this project from a Linux distro inside WSL2.

For this repository, Docker-compatible `compose` support is required because the local database is managed via `infra/docker-compose.yml`.

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

CivicLens also resolves and caches each representative's **official photo URL** and displays it in the dashboard, watchlist, and detail cards.  
It first tries Congress.gov (when `CONGRESS_API_KEY` is set), then falls back to the public `congress-legislators` dataset so photos can still load in many cases without an API key.

## Running tests

- **Backend:** `cd backend && mvn test` (JaCoCo report in `target/site/jacoco/`)
- **Frontend:** `cd frontend && npm test -- --coverage` (report in `frontend/coverage/`)
- **Lint:** `cd frontend && npm run lint`
- **Semgrep:** `semgrep scan --config p/owasp-top-ten .` (from repo root)
- **Gitleaks (secrets):** `gitleaks git --staged --redact` (or `gitleaks detect --source . --redact`)

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
