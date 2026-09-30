# Admin modal browser regression

The shared Modal uses native `HTMLDialogElement.showModal()`: the browser makes the background inert, while a dialog-scoped Tab handler wraps current enabled/visible controls at traversal boundaries. Opening focuses the close button. Unmount closes the native dialog and restores its connected opener; Escape, close, backdrop and existing form cancellation remain controlled by the original `onClose`. Title IDs are unique per instance. Callback changes do not reopen or refocus the dialog.

Run from `apps/admin` with Node 22 after `npm ci`:

```sh
npm run build
npm exec --yes --package=playwright@1.58.2 -- playwright install --with-deps chromium
npm exec --yes --package=playwright@1.58.2 -- node tests/modal-dialog.browser.mjs
```

Validate runs these commands inside the existing exact-source Admin job. Playwright 1.58.2 is an ephemeral, pinned engineering tool; no application dependency or lockfile change is required.

The test runs the real development-mode React application, including StrictMode, on a loopback Vite development server. All API requests are intercepted with synthetic local fixtures and external origins are rejected. It opens controls with focus and Enter for the keyboard workflow. It checks Arabic RTL desktop and compact layouts, Tab and Shift+Tab containment, excluded background focus, restoration on Escape/close/backdrop/cancel, reset/revoke/session cancellation without mutation, create-to-result transition, and typing focus across the actual toast-expiry parent rerender. The compact test checks viewport bounds and scroll access.

This is Chromium keyboard/layout evidence, not screen-reader, Safari/Firefox, physical touch, production API, or visual-design qualification. Browser exceptions and unexpected requests fail the run. Fixtures and full codes are not printed; no screenshots, storage state, traces or credentials are retained.

The first browser run found a separate desktop pointer obstruction by the existing RTL sidebar. That shell/menu defect is tracked as the immediate next scoped task; this keyboard suite does not claim pointer reachability of background openers.
