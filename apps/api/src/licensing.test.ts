import assert from "node:assert/strict";
import test from "node:test";
import { addHours, entitlementPayload } from "./licensing.js";

test("trial duration is exactly 168 hours", () => {
  const start = new Date("2026-09-08T12:00:00.000Z");
  assert.equal(addHours(start, 168).toISOString(), "2026-09-15T12:00:00.000Z");
});

test("offline authority is capped at 72 hours", () => {
  const now = new Date("2026-09-08T12:00:00.000Z");
  const payload = entitlementPayload("lifetime", now, null, now);
  assert.equal(payload.offlineValidUntil, "2026-09-11T12:00:00.000Z");
});

test("offline authority never exceeds finite entitlement expiry", () => {
  const now = new Date("2026-09-08T12:00:00.000Z");
  const expires = addHours(now, 10);
  const payload = entitlementPayload("one_year", now, expires, now);
  assert.equal(payload.offlineValidUntil, expires.toISOString());
});
