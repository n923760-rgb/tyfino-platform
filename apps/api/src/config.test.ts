import assert from "node:assert/strict";
import test from "node:test";
import { ownerTotpSecretForEnvironment, rateLimitForEnvironment } from "./config.js";

test("production requires a valid OWNER TOTP secret", () => {
  assert.throws(
    () => ownerTotpSecretForEnvironment("production", undefined),
    /ADMIN_OWNER_TOTP_SECRET is required/
  );
  assert.throws(
    () => ownerTotpSecretForEnvironment("production", "CHANGE_ME_BASE32_TOTP_SECRET"),
    /RFC 4648 Base32/
  );
  assert.equal(
    ownerTotpSecretForEnvironment("production", "gezd gnbv gy3t qojq gezd gnbv gy3t qojq"),
    "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
  );
  assert.equal(ownerTotpSecretForEnvironment("test", undefined), undefined);
});

test("production requires explicit bounded rate limits", () => {
  assert.throws(
    () => rateLimitForEnvironment("production", "GLOBAL_RATE_LIMIT_PER_MINUTE", undefined, 120),
    /required in production/
  );
  assert.throws(
    () => rateLimitForEnvironment("production", "GLOBAL_RATE_LIMIT_PER_MINUTE", "CHANGE_ME", 120),
    /explicitly configured/
  );
  for (const invalid of ["0", "1.5", "10001", "many"]) {
    assert.throws(
      () => rateLimitForEnvironment("production", "GLOBAL_RATE_LIMIT_PER_MINUTE", invalid, 120),
      /integer between 1 and 10000/
    );
  }
  assert.equal(rateLimitForEnvironment("production", "GLOBAL_RATE_LIMIT_PER_MINUTE", "240", 120), 240);
  assert.equal(rateLimitForEnvironment("test", "GLOBAL_RATE_LIMIT_PER_MINUTE", undefined, 120), 120);
});
