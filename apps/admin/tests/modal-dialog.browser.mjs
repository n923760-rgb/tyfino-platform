import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { existsSync, realpathSync } from "node:fs";
import { createRequire } from "node:module";
import { delimiter, join } from "node:path";

// npm exec supplies the pinned test tool on PATH without adding a runtime dependency.
const cli = (process.env.PATH ?? "").split(delimiter)
  .map((directory) => join(directory, "playwright")).find(existsSync);
assert.ok(cli, "Run through npm exec --package=playwright@1.58.2");
const require = createRequire(realpathSync(cli));
const { chromium } = require("playwright");
const origin = "http://127.0.0.1:4173";
const server = spawn(process.execPath, [
  "node_modules/vite/bin/vite.js", "--host", "127.0.0.1", "--port", "4173", "--strictPort"
], { stdio: "ignore", env: { ...process.env, VITE_API_BASE_URL: "" } });
let browser;
const problems = [];
const mutations = [];
const record = {
  id: "fixture-active", codeSuffix: "000000", licenseKind: "lifetime", status: "active",
  preActivationExpiresAt: null, activatedAt: "2026-09-01T00:00:00.000Z",
  grantStartsAt: "2026-09-01T00:00:00.000Z", grantExpiresAt: null, deviceBound: true,
  customerName: "عميل الاختبار", phoneNumber: null, externalReference: null,
  adminLabel: null, internalNote: null, createdAt: "2026-09-01T00:00:00.000Z"
};

async function waitFor(check, label) {
  const end = Date.now() + 10000;
  while (Date.now() < end) {
    if (await check()) return;
    await new Promise((resolve) => setTimeout(resolve, 25));
  }
  throw new Error(label);
}

async function focused(locator) {
  await waitFor(() => locator.evaluate((element) => document.activeElement === element), "Expected control focus");
}

async function openDialog(page, opener, title) {
  await opener.focus();
  await opener.press("Enter");
  const dialog = page.getByRole("dialog", { name: title, exact: true });
  await dialog.waitFor();
  await focused(dialog.getByRole("button", { name: "إغلاق", exact: true }));
  return dialog;
}

async function closed(page, opener) {
  await waitFor(async () => await page.getByRole("dialog").count() === 0, "Dialog did not close");
  await focused(opener);
}

async function pageFor(viewport) {
  const context = await browser.newContext({ viewport, serviceWorkers: "block" });
  const page = await context.newPage();
  page.on("pageerror", () => problems.push("Unexpected browser exception"));
  await page.route("**/*", async (route) => {
    const url = new URL(route.request().url());
    if (url.origin !== origin) {
      problems.push("Unexpected external request");
      return route.abort();
    }
    const path = url.pathname;
    const method = route.request().method();
    if (!path.startsWith("/v1/") && path !== "/readyz") return route.continue();
    if (method !== "GET") mutations.push(path);
    let data;
    if (path === "/v1/admin/me" && method === "GET") {
      data = { admin: { id: "fixture-owner", email: "owner@example.invalid", role: "owner" } };
    } else if (path === "/v1/admin/activation-codes" && method === "GET") {
      data = { activationCodes: [record], summary: { totalCodes: 1, availableCodes: 0, activeLicenses: 1, boundDevices: 1 } };
    } else if (path === "/v1/admin/activation-codes" && method === "POST") {
      data = { id: "fixture-created", code: "TEST-ONLY-NOT-AN-ACTIVATION-CODE" };
    } else if (path === "/v1/admin/app-settings" && method === "GET") {
      data = { trialEnabled: true };
    } else if (path === "/v1/admin/audit-logs" && method === "GET") {
      data = { auditLogs: [], integrityVerified: true };
    } else if (path === "/readyz" && method === "GET") {
      data = { status: "ready", database: "connected", schema: "current" };
    } else {
      problems.push("Unexpected mocked API route");
      return route.abort();
    }
    return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(data) });
  });
  await page.goto(origin + "/#activations");
  await page.getByRole("button", { name: "+ كود جديد", exact: true }).waitFor();
  return { context, page };
}

try {
  await waitFor(async () => {
    assert.equal(server.exitCode, null, "Local development server exited");
    try { return (await fetch(origin)).ok; } catch { return false; }
  }, "Local development server did not start");
  browser = await chromium.launch();
  console.log("Browser fixture: Chromium " + browser.version() + "; Arabic RTL; mocked local API");
  const { context, page } = await pageFor({ width: 1280, height: 900 });
  const opener = page.getByRole("button", { name: "+ كود جديد", exact: true });
  let dialog = await openDialog(page, opener, "إنشاء كود تفعيل");
  assert.equal(await page.locator("html").getAttribute("dir"), "rtl");
  await page.getByRole("searchbox", { name: "بحث في الأكواد" }).evaluate((element) => element.focus());
  await focused(dialog.getByRole("button", { name: "إغلاق", exact: true }));
  // Traverse past every form control in both directions, including radio groups.
  for (const key of ["Tab", "Shift+Tab"]) {
    for (let i = 0; i < 24; i++) {
      await page.keyboard.press(key);
      const focus = await dialog.evaluate((element) => ({
        inside: element.contains(document.activeElement), tag: document.activeElement?.tagName,
      }));
      assert.ok(focus.inside, "Focus escaped the dialog: " + key + " step " + i + " target " + focus.tag);
    }
  }
  console.log("PASS: native modal focus containment and background focus exclusion");
  await page.keyboard.press("Escape");
  await closed(page, opener);
  console.log("PASS: Escape restores the opener under React StrictMode");

  dialog = await openDialog(page, opener, "إنشاء كود تفعيل");
  await page.mouse.click(5, 5);
  await closed(page, opener);
  dialog = await openDialog(page, opener, "إنشاء كود تفعيل");
  await dialog.getByRole("button", { name: "إلغاء", exact: true }).click();
  await closed(page, opener);
  console.log("PASS: backdrop and form cancellation restore focus");

  const reset = page.getByRole("button", { name: "تغيير الجهاز", exact: true });
  dialog = await openDialog(page, reset, "تغيير الجهاز المرتبط");
  await dialog.getByRole("textbox", { name: "سبب التغيير" }).fill("اختبار");
  await page.keyboard.press("Escape");
  await closed(page, reset);
  const revoke = page.getByRole("button", { name: "إلغاء", exact: true });
  dialog = await openDialog(page, revoke, "إلغاء كود التفعيل");
  await dialog.getByRole("button", { name: "رجوع", exact: true }).click();
  await closed(page, revoke);
  assert.deepEqual(mutations, [], "Dismissal must not send a mutation");
  console.log("PASS: reset and revoke cancellation have no API side effects");

  dialog = await openDialog(page, opener, "إنشاء كود تفعيل");
  await dialog.getByRole("button", { name: "إنشاء الكود", exact: true }).click();
  const result = page.getByRole("dialog", { name: "تم إنشاء الكود", exact: true });
  await result.waitFor();
  await page.locator(".toast").waitFor();
  await focused(result.getByRole("button", { name: "إغلاق", exact: true }));
  await page.keyboard.press("Escape");
  await closed(page, opener);
  assert.deepEqual(mutations, ["/v1/admin/activation-codes"]);
  console.log("PASS: create-to-result dialog transition uses the existing mocked action");

  dialog = await openDialog(page, opener, "إنشاء كود تفعيل");
  const customer = dialog.getByRole("textbox", { name: "اسم العميل", exact: true });
  await customer.fill("اختبار");
  // The previous create action produces a toast. Its expiry rerenders the real parent,
  // changing the inline onClose callback without replacing this open form.
  await page.locator(".toast").waitFor({ state: "detached" });
  await focused(customer);
  await customer.press("End");
  await customer.type(" ناجح");
  assert.equal(await customer.inputValue(), "اختبار ناجح");
  await dialog.getByRole("heading").click();
  assert.equal(await page.getByRole("dialog").count(), 1, "Interior clicks dismissed the dialog");
  await dialog.getByRole("button", { name: "إغلاق", exact: true }).click();
  await closed(page, opener);
  console.log("PASS: typing survives parent rerender and close button restores focus");

  await page.goto(origin + "/#settings");
  const sessions = page.getByRole("button", { name: "إلغاء الجلسات الأخرى", exact: true });
  dialog = await openDialog(page, sessions, "إلغاء الجلسات الإدارية الأخرى");
  await dialog.getByRole("button", { name: "رجوع", exact: true }).click();
  await closed(page, sessions);
  assert.deepEqual(mutations, ["/v1/admin/activation-codes"]);
  console.log("PASS: administrative session dismissal restores focus without revocation");
  await context.close();

  const shell = await pageFor({ width: 1280, height: 400 });
  const shellPage = shell.page;
  const sidebar = shellPage.locator(".sidebar");
  const main = shellPage.locator(".content");
  const sidebarBox = await sidebar.boundingBox();
  const mainBox = await main.boundingBox();
  assert.ok(sidebarBox && mainBox &&
    Math.abs(sidebarBox.x + sidebarBox.width - 1280) <= 1 &&
    mainBox.x + mainBox.width <= sidebarBox.x + 1, "RTL sidebar overlaps the content column");
  const shellOpener = shellPage.getByRole("button", { name: "+ كود جديد", exact: true });
  await shellOpener.click();
  let shellDialog = shellPage.getByRole("dialog", { name: "إنشاء كود تفعيل", exact: true });
  await shellDialog.getByRole("button", { name: "إغلاق", exact: true }).click();
  await closed(shellPage, shellOpener);
  await shellPage.evaluate(() => window.scrollTo(0, 200));
  await waitFor(() => shellPage.evaluate(() => window.scrollY > 0), "Fixture must exercise document scrolling");
  const scrolledSidebar = await sidebar.boundingBox();
  assert.ok(scrolledSidebar && Math.abs(scrolledSidebar.y) <= 1, "Sidebar moved out of the viewport on document scroll");
  const logout = sidebar.getByRole("button", { name: "تسجيل الخروج", exact: true });
  await logout.scrollIntoViewIfNeeded();
  await logout.click({ trial: true });
  const logoutBox = await logout.boundingBox();
  assert.ok(logoutBox && logoutBox.y >= 0 && logoutBox.y + logoutBox.height <= 401, "Sidebar account action is clipped in a short viewport");
  const nav = sidebar.getByRole("navigation", { name: "التنقل الرئيسي" });
  await nav.getByRole("button", { name: "إعدادات التطبيق", exact: true }).click();
  await shellPage.getByRole("heading", { name: "إعدادات التطبيق", exact: true }).waitFor();
  const sessionOpener = shellPage.getByRole("button", { name: "إلغاء الجلسات الأخرى", exact: true });
  await sessionOpener.click();
  shellDialog = shellPage.getByRole("dialog", { name: "إلغاء الجلسات الإدارية الأخرى", exact: true });
  await shellDialog.getByRole("button", { name: "رجوع", exact: true }).click();
  await closed(shellPage, sessionOpener);
  await nav.getByRole("button", { name: "أكواد التفعيل", exact: true }).click();
  await shellPage.getByRole("heading", { name: "أكواد التفعيل", exact: true }).waitFor();
  const shellReset = shellPage.getByRole("button", { name: "تغيير الجهاز", exact: true });
  await shellReset.click();
  shellDialog = shellPage.getByRole("dialog", { name: "تغيير الجهاز المرتبط", exact: true });
  await shellDialog.getByRole("button", { name: "رجوع", exact: true }).click();
  await closed(shellPage, shellReset);
  const shellRevoke = shellPage.getByRole("button", { name: "إلغاء", exact: true });
  await shellRevoke.click();
  shellDialog = shellPage.getByRole("dialog", { name: "إلغاء كود التفعيل", exact: true });
  await shellDialog.getByRole("button", { name: "رجوع", exact: true }).click();
  await closed(shellPage, shellRevoke);
  assert.deepEqual(mutations, ["/v1/admin/activation-codes"], "Pointer navigation/cancellation sent a mutation");
  console.log("PASS: RTL sidebar separates content, stays visible on scroll, and exposes pointer actions in a short desktop viewport");
  await shell.context.close();

  const compact = await pageFor({ width: 375, height: 667 });
  const compactOpener = compact.page.getByRole("button", { name: "+ كود جديد", exact: true });
  dialog = await openDialog(compact.page, compactOpener, "إنشاء كود تفعيل");
  const box = await dialog.boundingBox();
  assert.ok(box && box.x >= 9 && box.x + box.width <= 366 && box.y >= 9 && box.y + box.height <= 658, "Compact dialog exceeded the viewport");
  await dialog.getByRole("textbox", { name: "ملاحظة داخلية", exact: true }).fill("اختبار");
  await dialog.getByRole("button", { name: "إلغاء", exact: true }).click();
  await closed(compact.page, compactOpener);
  console.log("PASS: compact RTL dialog fits and scrolls to form controls and cancellation");
  await compact.context.close();
  assert.deepEqual(problems, [], "Browser fixture encountered an unexpected error or request");
  console.log("PASS: 9 browser dialog/menu scenarios; no external API/provider requests");
} finally {
  try { await browser?.close(); } finally { server.kill("SIGTERM"); }
}
