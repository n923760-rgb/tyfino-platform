import assert from "node:assert/strict";
import test from "node:test";
import {
  generateActivationCode,
  hashPassword,
  isValidTotpSecret,
  normalizeActivationCode,
  tokenHash,
  totpCodeAt,
  verifyPassword,
  verifyTotp
} from "./security.js";

test("password hashes verify only the correct password", async () => {
  const encoded = await hashPassword("a-long-test-password");
  assert.equal(await verifyPassword("a-long-test-password", encoded), true);
  assert.equal(await verifyPassword("wrong-password", encoded), false);
});

test("TOTP follows the RFC 6238 SHA-1 vector and rejects replayed counters", () => {
  const secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
  assert.equal(isValidTotpSecret(secret), true);
  assert.equal(totpCodeAt(secret, 59_000, 8), "94287082");
  const code = totpCodeAt(secret, 59_000);
  assert.equal(code, "287082");
  const counter = verifyTotp(secret, code!, 59_000);
  assert.equal(counter, 1n);
  assert.equal(verifyTotp(secret, code!, 59_000, counter), null);
  assert.equal(verifyTotp(secret, "not-six-digits", 59_000), null);
});

test("activation codes carry 128 bits and normalize separators and case only", () => {
  const code = generateActivationCode();
  assert.match(code, /^TYF-[0-9A-HJKMNP-TV-Z]{5}(?:-[0-9A-HJKMNP-TV-Z]{5}){3}-[0-9A-HJKMNP-TV-Z]{6}$/);
  assert.equal(normalizeActivationCode(code.toLowerCase().replaceAll("-", " ")), code);
  assert.equal(normalizeActivationCode(`${code}!`), "");
});

test("activation code generation does not repeat in a representative sample", () => {
  const codes = new Set(Array.from({ length: 2000 }, generateActivationCode));
  assert.equal(codes.size, 2000);
});

test("token hashes are deterministic and do not expose the token", () => {
  const value = tokenHash("token", "pepper");
  assert.equal(value, tokenHash("token", "pepper"));
  assert.notEqual(value, "token");
});
