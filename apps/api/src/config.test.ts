import assert from "node:assert/strict";
import test from "node:test";
import { ownerTotpSecretForEnvironment, rateLimitForEnvironment, timeoutForEnvironment } from "./config.js";

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

test("production requires explicit bounded service timeouts", () => {
  assert.throws(
    () => timeoutForEnvironment("production", "HTTP_REQUEST_TIMEOUT_MS", undefined, 15_000),
    /required in production/
  );
  assert.throws(
    () => timeoutForEnvironment("production", "HTTP_REQUEST_TIMEOUT_MS", "CHANGE_ME", 15_000),
    /explicitly configured/
  );
  for (const invalid of ["999", "1000.5", "120001", "slow"]) {
    assert.throws(
      () => timeoutForEnvironment("production", "HTTP_REQUEST_TIMEOUT_MS", invalid, 15_000),
      /integer between 1000 and 120000 milliseconds/
    );
  }
  assert.equal(timeoutForEnvironment("production", "HTTP_REQUEST_TIMEOUT_MS", "20000", 15_000), 20_000);
  assert.equal(timeoutForEnvironment("test", "HTTP_REQUEST_TIMEOUT_MS", undefined, 15_000), 15_000);
});
