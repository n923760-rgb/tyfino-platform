import assert from "node:assert/strict";
import { Writable } from "node:stream";
import test from "node:test";
import pino from "pino";
import { apiLoggerOptions, routeLabel, safeErrorSummary } from "./logging.js";

test("API logger redacts authentication and licensing secrets", () => {
  const chunks: string[] = [];
  const destination = new Writable({
    write(chunk, _encoding, callback) {
      chunks.push(chunk.toString());
      callback();
    }
  });
  const logger = pino(apiLoggerOptions("info"), destination);
  const secrets = {
    authorization: "Bearer session-secret",
    cookie: "tyfino_admin_session=cookie-secret",
    password: "password-secret",
    activationCode: "TYF-SECRET-CODE",
    challengeToken: "challenge-secret",
    token: "opaque-session-secret"
  };
  const secretBody = {
    password: secrets.password,
    activationCode: secrets.activationCode,
    challengeToken: secrets.challengeToken,
    token: secrets.token
  };

  logger.info({
    req: { headers: { authorization: secrets.authorization, cookie: secrets.cookie }, body: secretBody },
    res: { headers: { "set-cookie": secrets.cookie } },
    body: secretBody,
    nested: { token: secrets.token },
    config: { ownerTotpSecret: "totp-secret", databaseUrl: "postgresql://user:secret@db/tyfino" }
  }, "redaction test");

  const output = chunks.join("");
  for (const secret of Object.values(secrets)) assert.equal(output.includes(secret), false);
  assert.equal(output.includes("totp-secret"), false);
  assert.equal(output.includes("postgresql://user:secret@db/tyfino"), false);
  assert.match(output, /\[REDACTED\]/);
});

test("request logging uses route templates and errors expose no message", () => {
  assert.equal(routeLabel("/v1/admin/activation-codes/:id"), "/v1/admin/activation-codes/:id");
  assert.equal(routeLabel(undefined), "unmatched");
  const error = Object.assign(new Error("postgresql://user:secret@db/tyfino"), { code: "ECONNREFUSED" });
  assert.deepEqual(safeErrorSummary(error), { type: "Error", code: "ECONNREFUSED" });
  assert.equal(JSON.stringify(safeErrorSummary(error)).includes("secret"), false);
});
