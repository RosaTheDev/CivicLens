# Security

## Authentication and passwords

- Passwords are **never** stored in plain text. The backend uses **BCrypt** (via Spring Security's `PasswordEncoder`) to hash passwords before persisting.
- **JWT** is used for stateless authentication. The signing key is configured via `civiclens.jwt.secret`; in production, set `JWT_SECRET` to a long, random value (at least 32 characters for HS256).
- Use **HTTPS** in production and keep the JWT secret secure.

## Database credentials

- Do not commit database credentials in source files.
- Local setup scripts generate a repo-root `.env.local` (gitignored) containing `CIVICLENS_DB_USER`, `CIVICLENS_DB_PASSWORD`, and `CIVICLENS_DB_NAME`.
- For manual runs, export these environment variables before starting Postgres/backend.

## SAST (Semgrep)

CI runs Semgrep with rulesets such as `p/owasp-top-ten`, `p/java`, and `p/react`. To run locally:

```bash
# Install: pip install semgrep (or use the Semgrep GitHub Action)
semgrep scan --config p/owasp-top-ten --config p/java --config p/react .
```

Review and fix any findings; document exceptions if necessary.

## Secret scanning (Gitleaks)

- Secret scanning is enforced in two places:
  - **Pre-commit hook** (installed by setup scripts) runs `gitleaks` on staged changes.
  - **CI** runs `gitleaks` on pushes and pull requests.
- Manual run examples:

```bash
gitleaks git --staged --redact
# or
gitleaks detect --source . --redact
```

## DAST and network scans

For a running instance (local or deployed), you can run:

- **OWASP ZAP**: Point ZAP at `http://localhost:8080` (backend) or your deployed URL and run a baseline or full scan. Document findings and remediations.
- **Nmap**: e.g. `nmap -sV -p 8080,5173 localhost` to verify exposed ports. For a demo target like `http://scanme.nmap.org`, use the commands from your course materials and record results.

## Hooks (optional)

To run checks before commit:

1. Install [pre-commit](https://pre-commit.com/) or use npm/husky.
2. Example hooks:
   - On backend Java changes: `cd backend && mvn -q test`
   - On frontend changes: `cd frontend && npm run lint`

Document the exact commands in your team README.
