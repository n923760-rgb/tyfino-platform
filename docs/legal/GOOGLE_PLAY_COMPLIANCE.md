# TYFINO Google Play Compliance Strategy

Status: ENGINEERING DRAFT — OWNER / LEGAL REVIEW REQUIRED  
Last reviewed: 2026-09-27  
Scope: Google Play listing, policy declarations, review readiness, and distribution posture

## 1. Product positioning

TYFINO is a **media player/client application**. It does not provide, bundle, sell, host, proxy, restream, or administer IPTV content or IPTV subscriptions.

The user supplies their own third-party Xtream Codes provider credentials directly in the Android application.

The TYFINO backend is limited to **application licensing** and does not receive the user's IPTV Host, Username, Password, catalogs, playlists, stream URLs, provider expiry, or viewing history.

Store copy, screenshots, support material, and review notes must remain consistent with this boundary.

## 2. Store-listing rules

Recommended wording:

- "TYFINO is a media player for users who already have access to a compatible IPTV service."
- "TYFINO does not provide channels, movies, series, or IPTV subscriptions."
- "Users are responsible for ensuring they have the rights required to access third-party content."

Avoid wording that:

- promises free or bundled premium channels;
- implies TYFINO owns or supplies third-party catalogs;
- advertises access to copyrighted works without authorization;
- uses third-party logos, marks, screenshots, posters, or channel branding without permission;
- suggests bypassing geographic, subscription, or access restrictions.

Google Play's Intellectual Property policy prohibits apps and listings that infringe or encourage infringement. TYFINO must therefore remain positioned as a neutral player and must not market unauthorized content access.

## 3. Privacy policy requirement

Before Play submission, publish a comprehensive privacy policy at an active, publicly accessible, non-geofenced URL.

The policy must also be accessible from within the application.

The policy must accurately describe:

- data TYFINO collects;
- data TYFINO does not collect;
- purposes and legal basis where applicable;
- sharing/disclosure;
- security practices;
- retention and deletion;
- user/data-subject rights;
- contact mechanism.

The repository draft is:

- `docs/legal/PRIVACY_POLICY.md`

A repository Markdown file is not by itself the final public policy URL required by Google Play.

## 4. Data safety declaration

Google Play requires developers to complete the Data safety form and keep it consistent with actual app behavior and the privacy policy.

Before submission:

1. inventory every outbound network flow from the app;
2. inventory third-party SDKs and libraries;
3. identify every user/device data type transmitted off-device;
4. verify whether each data type is collected, shared, optional, required, or processed ephemerally;
5. ensure the Data safety declaration matches the shipping binary.

Important TYFINO boundary:

- IPTV provider traffic is direct Android-to-provider traffic.
- Licensing traffic is Android-to-TYFINO.
- If any third-party SDK later collects user/device data, it must be included in the Data safety analysis.

Do not fill the Data safety form from documentation assumptions alone. Verify the exact release candidate.

## 5. Account deletion / data deletion

Google Play requires account-deletion controls when an app enables users to create an account.

Current TYFINO Android licensing uses an installation/license model rather than a consumer account system. If a future TYFINO user account is introduced, the account deletion requirement must be reassessed before release.

Independent of account deletion, TYFINO should maintain a clear data-rights/deletion request mechanism for personal data it controls.

See:

- `docs/legal/DATA_DELETION.md`

## 6. Review-access preparation

If Play reviewers must access a licensed area, prepare policy-compliant review instructions that do not expose production secrets.

Do not place real production Activation Codes, admin credentials, TOTP secrets, or IPTV credentials in public listing text or repository files.

Any review credential process must be temporary, bounded, auditable, and separately approved.

## 7. App content declarations

Before submission, review the current Play Console "App content" requirements, including as applicable:

- Privacy policy;
- Data safety;
- Ads declaration;
- Target audience/content;
- Content rating;
- App access/reviewer instructions;
- Sensitive permissions declarations.

The exact form fields can change over time and must be rechecked immediately before submission.

## 8. Intellectual-property gate

Before Store submission, verify:

- app icon and branding are owned/authorized;
- screenshots contain only content TYFINO is entitled to display;
- demo catalogs/media do not use unlicensed copyrighted material;
- listing text does not imply bundled third-party content;
- no third-party trademark is presented as TYFINO branding.

## 9. Payments boundary

This document does not approve a Play Billing design.

TYFINO currently uses an application-license Activation Code model. Any future in-app purchase, subscription, external purchase link, or region-specific payment flow requires a separate current Google Play Payments-policy review before implementation or publication.

## 10. Target API / technical compliance

The Android project must continue to meet the current Google Play target API requirement at the time of submission.

Do not rely on an old policy date. Verify the current requirement from Google immediately before release.

## 11. Pre-submission checklist

- [ ] Exact release candidate SHA identified.
- [ ] Release build and signing approved.
- [ ] Privacy policy published at public URL.
- [ ] Privacy policy linked in app.
- [ ] Data safety form verified against exact binary.
- [ ] Content rating completed.
- [ ] App-access instructions prepared if needed.
- [ ] Store copy describes TYFINO as a media player/client only.
- [ ] No bundled-content claims.
- [ ] No unauthorized third-party artwork/trademarks.
- [ ] Legal review completed for Terms and Privacy Policy.
- [ ] Device/runtime release qualification complete.
- [ ] Owner explicitly authorizes Store publication.

## 12. Rejection-response posture

If Play review raises a policy issue:

1. record the exact policy citation and reviewer message;
2. reproduce the issue against the exact submitted artifact/listing;
3. distinguish listing text, app behavior, data disclosure, IP, or access-review issues;
4. fix the narrow root cause;
5. update documentation/Data safety/listing when required;
6. resubmit only after exact-artifact verification.

Do not argue from product intent when observable behavior or listing text contradicts the policy.

## 13. Alternative direct APK distribution

Direct APK distribution is a separate distribution channel and does not remove the need for privacy, security, IP, or consumer-law compliance.

The existing TYFINO direct-APK/release process remains governed by the repository's approved Android release contracts and protected-action rules.

This document does not authorize a public APK release.

## 14. Official references reviewed

- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data safety guidance: https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play App content / review preparation: https://support.google.com/googleplay/android-developer/answer/9859455
- Google Play Intellectual Property policy: https://support.google.com/googleplay/android-developer/answer/9888072

These links are reference material. The live Google Play policy text controls at submission time.
