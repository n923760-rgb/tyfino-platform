import assert from "node:assert/strict";
import type { FastifyInstance } from "fastify";
import type { Pool } from "pg";

/** Runs serially within the existing disposable-PostgreSQL lifecycle test. */
export async function assertAdminAuditAtomicity(
  app: FastifyInstance, db: Pool, ownerDb: Pool, cookie: string
): Promise<void> {
  const counts = async () => {
    const result = await db.query<{ codes: number; audits: number }>(
      `SELECT (SELECT count(*)::int FROM activation_codes) AS codes,
              (SELECT count(*)::int FROM audit_logs) AS audits`
    );
    return result.rows[0]!;
  };
  const settings = async () => (await db.query<{ trial_enabled: boolean }>(
    "SELECT trial_enabled FROM app_settings WHERE singleton = true"
  )).rows[0]!.trial_enabled;
  const originalTrialEnabled = await settings();
  const beforeFailure = await counts();

  // Test-owner connection only: do not disable the existing audit-chain protections.
  const rejectAudit = async (sqlState: "P0001" | "23505") => ownerDb.query(
    `CREATE OR REPLACE FUNCTION tyfino_test_reject_admin_audit() RETURNS trigger
     LANGUAGE plpgsql AS $$
     BEGIN
       IF NEW.action IN ('activation.create', 'settings.update') THEN
         RAISE EXCEPTION 'injected audit failure' USING ERRCODE = '${sqlState}';
       END IF;
       RETURN NEW;
     END;
     $$`
  );
  await rejectAudit("P0001");
  try {
    await ownerDb.query(
      `CREATE TRIGGER tyfino_test_reject_admin_audit
       BEFORE INSERT ON audit_logs FOR EACH ROW
       EXECUTE FUNCTION tyfino_test_reject_admin_audit()`
    );
    const failedCreation = await app.inject({
      method: "POST", url: "/v1/admin/activation-codes",
      headers: { cookie }, payload: { licenseKind: "lifetime" }
    });
    assert.equal(failedCreation.statusCode, 500);
    assert.equal(failedCreation.json().error, "internal_error");
    assert.equal(failedCreation.json().code, undefined);
    assert.deepEqual(await counts(), beforeFailure);

    const failedSettings = await app.inject({
      method: "PATCH", url: "/v1/admin/app-settings",
      headers: { cookie }, payload: { trialEnabled: !originalTrialEnabled }
    });
    assert.equal(failedSettings.statusCode, 500);
    assert.equal(failedSettings.json().error, "internal_error");
    assert.equal(await settings(), originalTrialEnabled);
    assert.deepEqual(await counts(), beforeFailure);

    // Even the existing five-attempt uniqueness retry must leave no partial codes.
    await rejectAudit("23505");
    const exhaustedCreation = await app.inject({
      method: "POST", url: "/v1/admin/activation-codes",
      headers: { cookie }, payload: { licenseKind: "lifetime" }
    });
    assert.equal(exhaustedCreation.statusCode, 503);
    assert.equal(exhaustedCreation.json().error, "code_generation_failed");
    assert.equal(exhaustedCreation.json().code, undefined);
    assert.deepEqual(await counts(), beforeFailure);
  } finally {
    await ownerDb.query("DROP TRIGGER IF EXISTS tyfino_test_reject_admin_audit ON audit_logs");
    await ownerDb.query("DROP FUNCTION IF EXISTS tyfino_test_reject_admin_audit()");
  }

  const created = await app.inject({
    method: "POST", url: "/v1/admin/activation-codes",
    headers: { cookie }, payload: { licenseKind: "lifetime" }
  });
  assert.equal(created.statusCode, 201);
  const code = created.json() as { id: string; code: string };
  assert.match(code.code, /^TYF-[0-9A-HJKMNP-TV-Z-]+$/);
  const creationAudit = await db.query<{ entity_id: string; metadata: { codeSuffix: string } }>(
    "SELECT entity_id, metadata FROM audit_logs WHERE action = 'activation.create' AND entity_id = $1",
    [code.id]
  );
  assert.equal(creationAudit.rowCount, 1);
  assert.equal(creationAudit.rows[0]!.metadata.codeSuffix, code.code.slice(-6));
  assert.deepEqual(await counts(), { codes: beforeFailure.codes + 1, audits: beforeFailure.audits + 1 });

  const changed = await app.inject({
    method: "PATCH", url: "/v1/admin/app-settings",
    headers: { cookie }, payload: { trialEnabled: !originalTrialEnabled }
  });
  assert.equal(changed.statusCode, 200);
  assert.equal(changed.json().trialEnabled, !originalTrialEnabled);
  assert.equal(await settings(), !originalTrialEnabled);
  assert.deepEqual(await counts(), { codes: beforeFailure.codes + 1, audits: beforeFailure.audits + 2 });

  const restored = await app.inject({
    method: "PATCH", url: "/v1/admin/app-settings",
    headers: { cookie }, payload: { trialEnabled: originalTrialEnabled }
  });
  assert.equal(restored.statusCode, 200);
  assert.equal(await settings(), originalTrialEnabled);
  assert.deepEqual(await counts(), { codes: beforeFailure.codes + 1, audits: beforeFailure.audits + 3 });
  const integrity = await db.query<{ valid: boolean }>("SELECT verify_audit_log_chain() AS valid");
  assert.equal(integrity.rows[0]?.valid, true);
}
