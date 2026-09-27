# TYFINO Observability and Alerting Plan

Status: PROPOSED OPERATIONS DESIGN — production qualification required
Baseline: `main@41c046ad313d447d8d5213f3a431cadf7cbf4df5` (2026-09-27)

## Scope and current evidence

This plan covers TYFINO licensing API, Admin access, PostgreSQL, backup jobs, and the host. It does not collect IPTV provider credentials, catalog contents, stream URLs, or viewing activity. Android playback telemetry or analytics is not approved by this document.

The repository currently provides `/healthz` and `/readyz`, bounded/redacted structured API completion logs with a request ID, five independently configured rate limits, and an in-database append-only audit chain with verification through the authorized Admin endpoint. CI exercises readiness, audit protection, and backup/restore. A production metrics exporter, collector, dashboard, alert receiver, external audit anchor, and retention configuration are **not established by repository evidence**. Production host state is UNKNOWN. See `docs/api.md`, `docs/security.md`, and `docs/deployment.md`.

## Signal design

Instrument only after confirming a collector and cardinality/privacy budget. Proposed metric names and dimensions are an interface design, **not currently emitted**:

| Signal | Proposed dimensions | Use |
| --- | --- | --- |
| `tyfino_api_requests_total` | matched route template, method, status class | availability and errors |
| `tyfino_api_request_duration_seconds` | matched route template, method | latency distribution |
| `tyfino_licensing_decisions_total` | action, bounded outcome | trial/activation/refresh failures |
| `tyfino_rate_limit_hits_total` | limiter name | abuse/false-positive review |
| `tyfino_admin_login_total` | bounded outcome | authentication failures |
| `tyfino_db_statement_duration_seconds` | approved query class | database pressure |
| `tyfino_audit_integrity_check_total` | result | chain verification failure |
| `tyfino_backup_last_success_timestamp_seconds` | backup class | freshness |
| `tyfino_restore_drill_last_success_timestamp_seconds` | environment | restore proof |

Never use Installation ID, IP address, customer name, Activation Code, token, concrete URL/path, provider identity, SQL text, or exception message as a metric label. A route template is acceptable; a route with a concrete identifier is not. Preserve the existing log redaction boundary when adding tracing. Trace IDs may be generated for correlation, but distributed tracing, sampling percentages, and an OpenTelemetry backend remain OPEN.

## Checks and responses

| Condition to observe | First response | Status |
| --- | --- | --- |
| `/readyz` fails while `/healthz` succeeds | Check database reachability and required schema/triggers; stop sending traffic to that instance | Existing probes; alerting OPEN |
| Sustained 5xx or latency increase | Correlate route-template logs and deployment SHA; inspect database and downstream resources | Threshold OPEN |
| Elevated activation/trial failure or 429 | Separate user errors from abuse and configuration; avoid logging codes or identities | Threshold OPEN |
| Admin login failures spike | Verify rate limits, TOTP state, and owner access without publishing identities | Threshold OPEN |
| Audit-chain verification fails or audit insert fails | Escalate as security incident; preserve evidence and restrict affected administrative operations | External monitor/anchor OPEN |
| Backup fails or restore drill becomes stale | Escalate; do not declare recovery readiness from a dump alone | Schedule/threshold OPEN |
| Disk, memory, database connections, or TLS expiry approaches limit | Inspect capacity and planned maintenance | Host monitoring OPEN |

Alert thresholds must be set from representative traffic, false-positive review, on-call ownership, and a tested delivery path. The supplied 5%/15-minute failure, p95 500 ms, 50% 5xx, 30-second replication lag, and 85% disk suggestions are **candidate starting points only**. There is no approved replica, so replication-lag alerting is inapplicable until one exists. No alert is PASS until a synthetic fault reaches the responsible operator and a recovery action is recorded.

## Logs, retention, and evidence

Existing API logs contain method, matched route template, status, duration, and request ID, not raw headers, bodies, query strings, or concrete path identifiers. Add host/container/PostgreSQL log rotation and a protected destination before production. Access, retention, deletion, and any off-host export must follow an approved privacy/security schedule. The suggested 30-day hot and 12-month cold periods are OPEN, not a commitment.

Audit verification is available in the authorized Admin view. The database owner can still disable triggers and recompute the chain; production needs restricted owner access, periodic verification, and an external integrity anchor. An anchor must contain only the minimum hash/checkpoint metadata, not customer data. Record exact deployment SHA, alert rule revision, test time, operator, sanitized signal, and remediation outcome in `ENGINEERING/EVIDENCE/` or an approved protected system.

## Production gate

Before declaring observability qualified: implement the chosen collection/alert path, approve thresholds and retention, test redaction and bounded labels, exercise readiness/API/audit/backup alerts end to end, and record ownership and runbooks. This document authorizes no production deployment or infrastructure change.
