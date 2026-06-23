# Changelog

## 0.1.0

- Initial native Android SDK for Decisa mobile attribution (Kotlin library / AAR).
- Public API: `Decisa.initialize`, `Decisa.identify`, `Decisa.track`,
  and `DecisaEvent` with named constructors (`purchase`, `lead`, `startTrial`,
  `subscribe`, `addToCart`, `custom`, and the rest of the canonical taxonomy).
- First-run deferred attribution via `POST /v1/resolve`, persisted with
  `SharedPreferences`; local fallback visitor id on no-match / 204.
- Client-side SHA-256 hashing of email/phone for `POST /v1/identify`.
- Native Play Install Referrer reader for deterministic `dcs_mclid` matching.
- Injectable transport/persistence/referrer-reader seams for unit tests.
