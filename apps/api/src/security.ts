import {
  createCipheriv,
  createDecipheriv,
  createHmac,
  randomBytes,
  scrypt as scryptCallback,
  timingSafeEqual
} from "node:crypto";

const SCRYPT_KEY_LENGTH = 64;
const ACTIVATION_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

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
  if (algorithm !== "scrypt" || n !== "16384" || r !== "8" || p !== "1" || !saltValue || !hashValue) {
    return false;
  }
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

export function generateActivationCode(): string {
  let value = "";
  const bytes = randomBytes(8);
  for (const byte of bytes) value += ACTIVATION_ALPHABET[byte % ACTIVATION_ALPHABET.length];
  return `TYF-${value.slice(0, 4)}-${value.slice(4)}`;
}

export function normalizeActivationCode(code: string): string {
  const clean = code.toUpperCase().replace(/[^A-Z0-9]/g, "");
  if (!clean.startsWith("TYF") || clean.length !== 11) return "";
  return `TYF-${clean.slice(3, 7)}-${clean.slice(7, 11)}`;
}

export function encryptSecret(value: string | null | undefined, key: Buffer): string | null {
  if (!value) return null;
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", key, iv);
  const ciphertext = Buffer.concat([cipher.update(value, "utf8"), cipher.final()]);
  const tag = cipher.getAuthTag();
  return `v1.${iv.toString("base64url")}.${tag.toString("base64url")}.${ciphertext.toString("base64url")}`;
}

export function decryptSecret(value: string | null, key: Buffer): string | null {
  if (!value) return null;
  const [version, ivValue, tagValue, ciphertextValue] = value.split(".");
  if (version !== "v1" || !ivValue || !tagValue || !ciphertextValue) throw new Error("Invalid encrypted value");
  const decipher = createDecipheriv("aes-256-gcm", key, Buffer.from(ivValue, "base64url"));
  decipher.setAuthTag(Buffer.from(tagValue, "base64url"));
  return Buffer.concat([
    decipher.update(Buffer.from(ciphertextValue, "base64url")),
    decipher.final()
  ]).toString("utf8");
}
