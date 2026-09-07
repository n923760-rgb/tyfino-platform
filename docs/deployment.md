# Deployment preparation

## VPS assumptions

- Ubuntu 24.04
- Temporary shared VPS: 2 GB RAM, 40 GB disk
- Existing Sayad service remains isolated and untouched
- DNS is not changed until the TYFINO stack passes local health checks

## Required DNS records

Create these only when the server is ready:

| Type | Host | Value |
| --- | --- | --- |
| A | `@` | VPS IPv4 address |
| A | `admin` | VPS IPv4 address |
| A | `api` | VPS IPv4 address |

## Safe deployment order

1. Create a dedicated `tyfino` directory and non-root deployment user.
2. Install Docker Engine and the Compose plugin from Docker's official repository.
3. Create at least 2 GB swap because this VPS has 2 GB RAM.
4. Clone the private repository using a read-only deploy key.
5. Create the server `.env` with generated secrets.
6. Start the database and API without changing public DNS.
7. Verify `/healthz` and `/readyz` locally.
8. Back up existing DNS records, then add the TYFINO records.
9. Start Caddy and verify automatic TLS.
10. Configure encrypted off-site database backups.

Automatic GitHub deployment will be added only after the isolated VPS user and deploy key exist.
