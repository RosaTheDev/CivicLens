# CivicLens infrastructure

## Local Postgres

From this directory:

```bash
docker compose up -d
```

Default connection (used by backend when running locally):

- Host: `localhost`
- Port: `55432`
- Database: from `CIVICLENS_DB_NAME` (default: `civiclens`)
- User: from `CIVICLENS_DB_USER` (default: `civiclens`)
- Password: from `CIVICLENS_DB_PASSWORD` (required)

To stop:

```bash
docker compose down
```

To reset data:

```bash
docker compose down -v
docker compose up -d
```
