# DairyManager Android

A milk collection manager for dairies and collection centers — the Android
port of
[DairyManager (2026-10-19)](https://github.com/entity-coder/2026-10-19),
the Python desktop app from the same sellable-apps lineup. It runs fully
offline on the dairy's phone: no account, no server, no internet needed.

## What it does

- **Farmer master** — name, village, phone, bank account and IFSC. Farmers
  are hidden, not deleted, once they have milk records, so the payment
  history is never destroyed by a cleanup.
- **Daily collection entry** — pick a farmer, date, shift (Morning/Evening),
  litres, fat and SNF. The rate is looked up live from the fat × SNF slab
  chart and shown before saving. One entry per farmer per date per shift:
  a second save for the same shift is refused, because a farmer delivers
  once per shift and a duplicate is almost always a double-tap.
- **Rate chart + settings** — an editable slab chart (fat range × SNF
  range → ₹/litre) plus a dairy name and a default rate. Readings outside
  every slab fall back to the default rate, and the rate is stored on each
  collection when it is saved, so later chart edits never rewrite history.
- **Payment sheets** — pick any period and see, per farmer: milk and gross
  for the period, opening balance carried from earlier periods, payments
  attributed to this period, and what is still pending. Farmers with an
  old balance but no milk in the period still appear, so old dues are never
  silently dropped.
- **Period-attributed payments** — record a payment from a sheet row,
  capped at that farmer's pending for the sheet. Payments are append-only
  and belong to a period, so a payment recorded late still clears the
  opening balance of the period it belongs to.
- **Shareable / printable sheets** — the sheet exports as a PDF (A4, with
  pagination for long farmer lists) that can be printed through the system
  print dialog or shared to WhatsApp, Gmail, and other apps.
- **Dashboard** — milk today split by shift, farmers who delivered today,
  total pending payments, and milk this month.

## How to run

This project was **not compiled or tested on a device** in the environment
where it was built (no Android SDK there). To run it:

1. Open Android Studio and choose **Open** → select this folder.
2. Let Gradle sync (JDK 17, Gradle 8.2, Android Gradle Plugin 8.2.2).
3. Run on an emulator or a physical phone (min SDK 24, target SDK 34).

## Direct APK download

Once the APK workflow is added to this repo (see below), every push to
`main` builds a debug APK and attaches it to a rolling `apk-latest`
release:

```
https://github.com/entity-coder/2026-10-28/releases/download/apk-latest/DairyManager-v1.0.apk
```

> ⚠️ That link only works after the workflow file exists in the repo and
> its first build has finished. Until then it will 404 — that is expected,
> not a broken link.

### One manual step: the APK workflow

The GitHub integration used to publish this repo cannot write files under
`.github/workflows/` (GitHub returns 403 for Apps without the workflows
permission), so the workflow file must be added once by hand:

1. Open the repo on GitHub → **Add file → Create new file**.
2. Name it exactly `.github/workflows/build-apk.yml` — the leading dot is
   mandatory. Without it, GitHub Actions silently ignores the file and no
   build will ever run.
3. Paste the workflow content (adapted for `DairyManager-v1.0.apk`) and commit.
4. Check the file's breadcrumb/path shows `.github/workflows/` — not just
   the commit message text.
5. In repo **Settings**, untick **Enable release immutability** — otherwise
   the rolling `apk-latest` release cannot be updated on later pushes.

## Tech

- Kotlin, single-activity + fragments with bottom navigation
- Room (SQLite) with `farmers`, `rate_slabs`, `collections`, `payments`
  and `settings` tables; `collections.farmerId` and `payments.farmerId`
  are foreign keys with `CASCADE` delete, so deleting a farmer removes
  their records (the UI refuses this while milk records exist, so in
  practice farmers are hidden instead)
- Kotlin Flows + `repeatOnLifecycle` for live UI updates
- View Binding, Material 3, `PdfDocument` for sheets (no third-party PDF
  library), `FileProvider` for sharing
- kapt version declared explicitly and exactly once (`1.9.22`, matching
  the Kotlin plugin) in `app/build.gradle.kts` — CI builds fail if kapt is
  applied without a version

## Project structure

```
app/src/main/java/com/dairymanager/app/
├── data/
│   ├── Farmer.kt / RateSlab.kt / CollectionEntry.kt / Payment.kt / Settings.kt
│   ├── DairyDao.kt          — all Room queries
│   ├── DairyDatabase.kt     — Room database (v1, destructive migration)
│   ├── Format.kt            — money, litres, ISO dates
│   ├── RateLookup.kt        — slab matching + starter chart
│   └── SheetMath.kt         — payment-sheet computation
└── ui/
    ├── MainActivity.kt      — bottom-nav shell + first-launch seeding
    ├── DashboardFragment.kt / FarmersFragment.kt / CollectFragment.kt
    ├── SheetsFragment.kt    — period sheets + capped payments
    ├── FarmerDetailActivity.kt / RateChartActivity.kt
    ├── FarmerAdapter.kt / CollectionAdapter.kt / SheetAdapter.kt / SlabAdapter.kt
    └── SheetPdf.kt          — paginated sheet PDF (print + share)
```

## Interview notes

**What is this project?**
DairyManager Android is an offline milk-collection app for small dairies:
farmers, daily milk entries with fat/SNF-based pricing, a configurable
rate chart, and per-period payment sheets that can be printed or shared.
It is a port of a desktop app from the same product lineup, rebuilt for a
phone at the collection counter.

**Why Room + a single DAO?**
All data is local and relational (farmers → collections → payments), and
the UI needs live updates when a collection is saved. Room gives typed
queries plus Flow observables with almost no boilerplate; one DAO keeps
the data layer in a single place for a v1 app this size. A repository
layer would be the next step if sync or multi-device support were added,
but for a single-phone offline app it would only add indirection.

**Why store the rate on each collection instead of computing it at read time?**
Because a rate chart changes over time, and a farmer must be paid the rate
that was in force when they delivered. Storing the rate at write time
makes history immutable: editing the chart only affects future
collections. The trade-off is that a wrongly entered collection keeps its
wrong rate — the fix is to delete and re-enter it, which is deliberate
for an auditable money trail.

**Why half-open slab ranges ([min, max))?**
So bands never overlap and a reading exactly on a boundary has exactly
one home: the higher band. The edge case is a reading above every band
(for example fat above 99 in the starter chart, which cannot happen in
practice) — it falls back to the default rate, and the collection screen
says so in the rate preview instead of silently applying a slab rate.

**Why are payments attributed to a period instead of just dated?**
A dairy often settles a period days after it ends. If payments were only
dated, a late payment would land in the wrong period's sheet. Attributing
each payment to the sheet it settles makes opening balances correct:
pending = opening + gross − paid, where "paid" means payments for exactly
this period. The assumption is that periods are entered consistently
(non-overlapping); overlapping periods would attribute a payment to the
period it names, which is still deterministic but could surprise.

**What are the main limitations?**
- Not compiled or device-tested in the build environment (no Android
  SDK); it must be opened in Android Studio, where Gradle sync is the
  first real check.
- Single device, no sync or backup: the database lives on the phone, so
  a lost phone loses the ledger. Exporting the database or adding a CSV
  backup is the most valuable next feature.
- No multi-user roles or authentication — anyone holding the phone can
  record collections and payments, which matches how a small dairy counter
  actually works but would not suit a larger operation.
- Dates are typed as yyyy-MM-dd text, not picked from a calendar widget;
  a date picker would be a small, clear UX improvement.
- Deleting a collection after its period has been paid can make a sheet
  pending go negative; the UI allows the delete because a wrong entry
  must be correctable, but the sheet shows the honest number rather than
  hiding it.

**How would I extend it?**
Add a CSV/database backup export first (data safety beats features), then
a date picker and a per-farmer statement screen (the desktop app already
has one, so the design exists). After that: SMS/WhatsApp sheet sharing
per farmer instead of one whole-sheet PDF, and an optional cloud sync for
dairies with more than one collection point.
