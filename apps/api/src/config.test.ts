import assert from "node:assert/strict";
import test from "node:test";
import { ownerTotpSecretForEnvironment } from "./config.js";

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
