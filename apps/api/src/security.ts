import { createHmac, randomBytes, scrypt as scryptCallback, timingSafeEqual } from "node:crypto";

const SCRYPT_KEY_LENGTH = 64;
const CROCKFORD_BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
const RFC4648_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
const TOTP_STEP_SECONDS = 30n;

function scrypt(password: string, salt: Buffer): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    scryptCallback(password, salt, SCRYPT_KEY_LENGTH, { N: 16_384, r: 8, p: 1 }, (error, key) => {
      if (error) reject(error);
      else resolve(key);
    });
  });
}

export async function hashPassword(password: string): Promise<string> {
  const salt = randomBytes(16);
  const hash = await scrypt(password, salt);
  return `scrypt$16384$8$1$${salt.toString("base64url")}$${hash.toString("base64url")}`;
}

export async function verifyPassword(password: string, encoded: string): Promise<boolean> {
  const [algorithm, n, r, p, saltValue, hashValue] = encoded.split("$");
  if (algorithm !== "scrypt" || n !== "16384" || r !== "8" || p !== "1" || !saltValue || !hashValue) return false;
  const expected = Buffer.from(hashValue, "base64url");
  const actual = await scrypt(password, Buffer.from(saltValue, "base64url"));
  return actual.length === expected.length && timingSafeEqual(actual, expected);
}

export function randomToken(): string {
  return randomBytes(32).toString("base64url");
}

export function tokenHash(token: string, pepper: string): string {
  return createHmac("sha256", pepper).update(token).digest("hex");
}

function decodeRfc4648Base32(value: string): Buffer | null {
  const normalized = value.toUpperCase().replace(/=+$/, "").replace(/\s/g, "");
  if (!normalized || !/^[A-Z2-7]+$/.test(normalized)) return null;
  let bits = 0;
  let buffer = 0;
  const output: number[] = [];
  for (const character of normalized) {
    buffer = (buffer << 5) | RFC4648_BASE32.indexOf(character);
    bits += 5;
    if (bits >= 8) {
      output.push((buffer >>> (bits - 8)) & 0xff);
      bits -= 8;
      buffer &= (1 << bits) - 1;
    }
  }
  return Buffer.from(output);
}

function hotp(secret: Buffer, counter: bigint, digits: number): string {
  const movingFactor = Buffer.alloc(8);
  movingFactor.writeBigUInt64BE(counter);
  const digest = createHmac("sha1", secret).update(movingFactor).digest();
  const offset = digest[digest.length - 1]! & 0x0f;
  const binary = digest.readUInt32BE(offset) & 0x7fffffff;
  return (binary % (10 ** digits)).toString().padStart(digits, "0");
}

export function isValidTotpSecret(secret: string): boolean {
  return (decodeRfc4648Base32(secret)?.length ?? 0) >= 20;
}

export function totpCodeAt(secret: string, timeMs: number, digits = 6): string | null {
  const decoded = decodeRfc4648Base32(secret);
  if (!decoded || !Number.isInteger(digits) || digits < 6 || digits > 8) return null;
  const counter = BigInt(Math.floor(timeMs / 1000)) / TOTP_STEP_SECONDS;
  return hotp(decoded, counter, digits);
}

export function verifyTotp(
  secret: string,
  code: string,
  timeMs = Date.now(),
  lastAcceptedCounter: bigint | null = null
): bigint | null {
  if (!/^\d{6}$/.test(code)) return null;
  const decoded = decodeRfc4648Base32(secret);
  if (!decoded) return null;
  const current = BigInt(Math.floor(timeMs / 1000)) / TOTP_STEP_SECONDS;
  for (const offset of [-1n, 0n, 1n]) {
    const counter = current + offset;
    if (counter < 0n || (lastAcceptedCounter !== null && counter <= lastAcceptedCounter)) continue;
    const expected = Buffer.from(hotp(decoded, counter, 6));
    const actual = Buffer.from(code);
    if (expected.length === actual.length && timingSafeEqual(expected, actual)) return counter;
  }
  return null;
}

function encodeBase32(bytes: Buffer): string {
  let bits = 0;
  let value = 0;
  let output = "";
  for (const byte of bytes) {
    value = (value << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      output += CROCKFORD_BASE32[(value >>> (bits - 5)) & 31];
      bits -= 5;
      value &= (1 << bits) - 1;
    }
  }
  if (bits > 0) output += CROCKFORD_BASE32[(value << (5 - bits)) & 31];
  return output;
}

export function generateActivationCode(): string {
  const raw = encodeBase32(randomBytes(16));
  return `TYF-${raw.slice(0, 5)}-${raw.slice(5, 10)}-${raw.slice(10, 15)}-${raw.slice(15, 20)}-${raw.slice(20, 26)}`;
}

export function normalizeActivationCode(code: string): string {
  const clean = code.toUpperCase().replace(/[\s-]/g, "");
  if (!/^TYF[0-9A-HJKMNP-TV-Z]{26}$/.test(clean)) return "";
  const raw = clean.slice(3);
  return `TYF-${raw.slice(0, 5)}-${raw.slice(5, 10)}-${raw.slice(10, 15)}-${raw.slice(15, 20)}-${raw.slice(20)}`;
}
