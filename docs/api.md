# API v0.2

All production requests use HTTPS. Admin endpoints accept the secure `tyfino_admin_session` cookie. Player endpoints use an opaque bearer token stored in the device secure keystore.

## Administration

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/v1/admin/auth/login` | Start a 12-hour admin session |
| POST | `/v1/admin/auth/logout` | Revoke the current session |
| GET | `/v1/admin/me` | Return the current administrator |
| GET/POST | `/v1/admin/customers` | List or create customers |
| PATCH | `/v1/admin/customers/:id` | Update a customer or status |
| GET/POST | `/v1/admin/hosts` | List or create provider hosts |
| PATCH | `/v1/admin/hosts/:id` | Update or disable a provider host |
| GET/POST | `/v1/admin/provider-accounts` | List or create encrypted provider mappings |
| PATCH | `/v1/admin/provider-accounts/:id` | Update credentials, expiry, or status |
| GET/POST | `/v1/admin/activation-codes` | List or generate activation codes |
| POST | `/v1/admin/activation-codes/:id/revoke` | Revoke a code and its sessions |
| GET | `/v1/admin/devices` | List registered devices |
| PATCH | `/v1/admin/devices/:id` | Block or restore a device |
| GET | `/v1/admin/audit-logs` | Owner/admin activity history |

The plain activation code is returned exactly once when it is created. Only its HMAC hash and final four characters are stored.

## Player

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/v1/player/activate` | Validate code, enforce device limit, return a 30-day session |
| GET | `/v1/player/config` | Refresh the assigned encrypted provider connection |

The API returns provider connection data only to an active, registered device. Video streams are requested directly from the provider, never through the TYFINO VPS.
