import assert from "node:assert/strict";
import test from "node:test";
import { randomBytes } from "node:crypto";
import {
  decryptSecret,
  encryptSecret,
  generateActivationCode,
  hashPassword,
  normalizeActivationCode,
  tokenHash,
  verifyPassword
} from "./security.js";

test("password hashes verify only the correct password", async () => {
  const encoded = await hashPassword("a-long-test-password");
  assert.equal(await verifyPassword("a-long-test-password", encoded), true);
  assert.equal(await verifyPassword("wrong-password", encoded), false);
});

test("AES-GCM secrets round-trip and reject the wrong key", () => {
  const key = randomBytes(32);
  const encrypted = encryptSecret("provider-secret", key);
  assert.notEqual(encrypted, "provider-secret");
  assert.equal(decryptSecret(encrypted, key), "provider-secret");
  assert.throws(() => decryptSecret(encrypted, randomBytes(32)));
});

test("activation codes use the TYF format and normalize input", () => {
  const code = generateActivationCode();
  assert.match(code, /^TYF-[A-Z2-9]{4}-[A-Z2-9]{4}$/);
  assert.equal(normalizeActivationCode(code.toLowerCase().replaceAll("-", " ")), code);
});

test("token hashes are deterministic but do not expose tokens", () => {
  const value = tokenHash("token", "pepper");
  assert.equal(value, tokenHash("token", "pepper"));
  assert.notEqual(value, "token");
});
