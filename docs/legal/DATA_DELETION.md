# TYFINO Data Deletion & Data Rights Procedure

Status: ENGINEERING DRAFT — LEGAL / OPERATIONS REVIEW REQUIRED  
Last reviewed: 2026-09-27  
Scope: personal data controlled by TYFINO

## 1. Purpose

This document defines the intended process for privacy/data-rights and deletion requests relating to data controlled by TYFINO.

It does not apply to IPTV-provider data that TYFINO does not control.

## 2. Local Android data

Uninstalling TYFINO normally removes ordinary app-private local data according to Android platform behavior.

TYFINO also uses backup-exclusion rules for sensitive local data under the approved Android security contracts.

A production qualification task must verify the exact release candidate's backup/data-extraction configuration.

The application should also provide explicit account/removal flows for locally stored IPTV account data where already defined by the Android contracts.

## 3. Server-side TYFINO data

Depending on the user's relationship with TYFINO, server-side data may include:

- opaque Installation ID;
- trial/licensing state;
- entitlement/session records;
- optional administrative metadata;
- redacted audit/security records.

TYFINO does not control the user's IPTV provider account and cannot delete provider-side records on the user's behalf.

## 4. Request channel

Production must provide a public privacy/data-rights request channel.

Current placeholder:

**[OWNER TO PROVIDE PRIVACY REQUEST CHANNEL]**

Do not publish an invented email address.

## 5. Identity verification

Before fulfilling a request, TYFINO may take proportionate steps to verify that the requester is entitled to act on the relevant data.

Verification must not collect unnecessary additional personal data.

## 6. Response timing

Saudi PDPL implementing guidance provides a 30-day period for acting on data-subject rights requests, with a possible additional 30-day extension in specified circumstances when the data subject is informed.

The final operational SLA must be reviewed against the exact request type, applicable law, and production support process.

The recommendations document proposed separate 30/90/180-day milestones. Those are **not adopted as binding promises** in this draft because they require legal and operational validation.

## 7. Deletion / destruction

Where a valid deletion/destruction request applies, TYFINO should:

1. identify TYFINO-controlled records in scope;
2. distinguish deletable business/support data from records that must be retained under an applicable legal/security basis;
3. remove or irreversibly anonymize data where appropriate;
4. revoke active licensing sessions where necessary to complete the request;
5. record sufficient non-secret evidence that the request was handled;
6. communicate completion or any lawful limitation to the requester.

A deletion request must not be implemented by merely hiding or freezing data when destruction is legally required.

## 8. Audit and security records

Some audit/security records may require retention for legal, fraud-prevention, integrity, or security purposes.

The final retention schedule is **OPEN**.

Any retained record must be minimized, protected, purpose-limited, and deleted/anonymized when the approved retention purpose ends.

## 9. Backups

Production backup architecture and retention are not yet approved.

Once approved, the deletion procedure must document how deleted data ages out of backups and how restored backups are reconciled so deleted data is not unintentionally reintroduced into active systems.

Do not promise a fixed backup-purge period before the backup/retention design is approved.

## 10. Google Play account-deletion note

Current TYFINO Android licensing is installation/license based and does not currently define a consumer TYFINO account-creation feature.

If an account system is introduced, Google Play's current account-deletion requirements must be reassessed and an in-app deletion path plus required web resource must be implemented before release.

## 11. Bulk / administrative deletion

Bulk deletion, destructive database changes, or production record purges are protected/destructive operations.

They require:

- exact scope;
- authorization;
- validated backup/rollback plan where applicable;
- audit/evidence;
- explicit current owner approval under TYFINO governance.

## 12. Data-rights references

Saudi PDPL provides rights including information, access, obtaining a readable copy, correction, and destruction subject to the law and its regulations.

References:

- https://dgp.sdaia.gov.sa/wps/portal/pdp/knowledgecenter/details/PDPL
- https://dgp.sdaia.gov.sa/wps/portal/pdp/knowledgecenter/details/PDPL2
- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play account deletion guidance: https://support.google.com/googleplay/android-developer/answer/13327111

The live law/policy and legal review control over this draft.
