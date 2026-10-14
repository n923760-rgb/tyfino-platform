# Local category-picker search

Owner: continue TYFINO development/improvements; standing reviewed merge authority is conditional on exact-head qualification. No publication, signing or deployment.
Verified source: official main529c9e3cdc782b339d570dda23da513df91f8cdf, default main, no open PRs. Separate postmerge Validate38286456392 completed SUCCESS. Isolated branch feat/category-search-20261014.

## Bounded change

Explicit search in the existing native category picker only. Search replaces its title, with a48dp visible-focus search/clear control beside Close. Trimmed case-insensitive name matching retains provider order and exact category objects/IDs. The editable query is bounded to80 Unicode code points and is not persisted. No provider request, new dependency, item search contract, account store, branding, licensing, package/version or signing change.

Filtering runs on Dispatchers.Default over the existing maximum10,000 published categories; lazy rendering remains. Cooperative cancellation is checked every128 records and before returning. Each query/snapshot owns a different remembered result cell; an older completion cannot write into a replacement's displayed state. Existing account/section shell disposal still owns the presentation. Empty/loading results never expose previous rows. Clear restores the current-category viewport; no query, focus or clear event selects a category.

## Qualification

Four JVM cases: matching/order/Arabic/exact identity, Unicode bound,10,000-row snapshot and cancellation. Two new native cases join all128 baseline cases (130 expected each managed device): actual short native Dialog search/clear/exact selection, and snapshot replacement/no-match/Close with production content. Existing Arabic/font2 and short D-pad cases remain unchanged. These are synthetic metadata/presentation tests, not real provider/account/network or physical-device evidence.

Local source/XML/locale/full-diff and governance checks are recorded on the task PR. Local native execution is BLOCKED by absent qualified Android SDK/Gradle; exact-source existing Actions must qualify builds, units, lint, all seven jobs and configured owner APK API33 install/launch before merge. No tests/gates skipped or weakened. Physical TV, actual OEM IME, TalkBack, performance, provider and production readiness remain NOT RUN in this task. Self-review is not independent review.
