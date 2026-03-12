# CivicLens infrastructure

## Local Postgres

From this directory:

```bash
docker compose up -d
```

Default connection (used by backend when running locally):

- Host: `localhost`
- Port: `5432`
- Database: `civiclens`
- User: `civiclens`
- Password: `civiclens`

To stop:

```bash
docker compose down
```

To reset data:

```bash
docker compose down -v
docker compose up -d
```
