# Project-specific licensing proxy upstream

Task: TYFINO-20261030-exclusive-licensing-upstream (IMPLEMENTATION).
Official base: main9d8578157e1cdbd9473f7ea978f78434fb4bd32c.
Authority: owner requests continued TYFINO readiness work; existing reviewed-merge authority applies. Production deployment is excluded.

Owner-provided VPS evidence confirms API readiness200 internally at172.20.0.2. Caddy, attached to tyfino_backend and telegram_miniapp_edge, resolved generic api to172.21.0.2 and received connection refused; public readiness was502. The unique container name tyfino-api-1 resolved to172.20.0.2 and returned200. Owner later confirmed the host-file fix was applied and login worked. These are attributed owner runtime observations, not executions by this agent.

The source Caddyfile still used api:3000. Add the project-specific tyfino-licensing-api alias to the API's backend network and use it in the source Caddy upstream. This avoids a dependency on Compose-generated container names and keeps the approved single-VPS architecture. Existing API public origin, ports, private networks, database and other project routes are unchanged.

The live host uses a shared bind-mounted Caddyfile; merging this change cannot update it. Deployment notes require preserving all live overrides/routes and provisioning the API alias before changing the host upstream. No host commands, restarts, deployment, credentials, or DNS changes are performed here.

Validation: source YAML and alias/upstream consistency, whitespace check, then exact-source hosted Compose/CI checks. Local Docker daemon is unavailable, so multi-network runtime verification of the new alias is NOT RUN. Existing incident evidence establishes the generic-name problem, not execution of this new alias. Results and protected merge decision belong to the PR.
