# DecisaSDK — Android attribution SDK for Kotlin (Gradle / AAR)

**DecisaSDK** is a native **Android attribution SDK** distributed as a **Gradle
library module (AAR)**. It connects **paid ads → Play Store install → in-app
conversions** using Decisa's first-party attribution ingest — the same public
endpoints as `pixel.js` in the browser.

Ship a **native pixel** in your Kotlin or Jetpack Compose app: authenticate with
your public `pixel_key` (`dcs_px_`), resolve deferred attribution on first launch
via the **Play Install Referrer** (`dcs_mclid`), then send **conversion tracking**
events to `/v1/identify` and `/v1/track`. Built for **web2app** and **funnel2app**
flows where users click an ad or landing page, install from the Play Store, and
convert days later — without GAID or ad-id consent surfaces in v1.

| | **decisa-android** (this repo) | [decisa-flutter](https://github.com/decisa-ai/decisa-flutter) | [decisa-swift](https://github.com/decisa-ai/decisa-swift) |
| --- | --- | --- | --- |
| Platform | Native Android (Kotlin) | Flutter (iOS + Android) | Native iOS (Swift) |
| Install match | **Deterministic** via Play Install Referrer `dcs_mclid` | Android: same; iOS: probabilistic | Probabilistic + AdServices |
| Package manager | Gradle / AAR | pub.dev | Swift Package Manager |
| Best for | Kotlin/Compose apps, Android-only stacks | Cross-platform mobile apps | SwiftUI/UIKit apps |

---

## Use cases: web2app & funnel2app

**Web2app** — A user clicks a Meta, Google, or TikTok ad that lands on a Decisa
UTM link, gets redirected to the Play Store, installs, and opens the app. On first
launch, `Decisa.initialize` reads `dcs_mclid` from the Install Referrer and POSTs
`/v1/resolve` for a **deterministic** match to the original click.

**Funnel2app** — A multi-step funnel (quiz, lead form, checkout page) lives on
the web; the final step sends users to the store via `?app=1`. In-app events
(`Lead`, `CompleteRegistration`, `Purchase`) carry the install attribution
forward so you can measure **funnel-to-app** conversion, not just installs.

**Paid ads without GAID** — v1 does not read the Google Advertising ID (GAID).
The `dcs_mclid` token embedded in the Play Install Referrer provides deterministic
attribution for Android web2app campaigns.

**CAPI & server-side fanout** — Events ingested via `/v1/track` flow through
Decisa's existing conversion pipeline (same taxonomy as the web pixel), enabling
**CAPI**-style server-side delivery to ad platforms without embedding secret keys
in the app binary.

---

## The public `pixel_key`, never a secret

The SDK authenticates **only** with your workspace's public `pixel_key` — the
string that begins with `dcs_px_`. It is the same public credential `pixel.js`
embeds in a public web page. It is **not a secret** and is safe to ship inside an APK.

**Never put a secret key in a mobile app.** Server-side Decisa SDKs authenticate
with a secret `dcs_ak_` / `dcs_sk_` key. This SDK refuses anything that is not a
`dcs_px_` key (an `IllegalArgumentException` fires in debug builds).

---

## Install (Gradle)

### Monorepo / local path

In your app's `settings.gradle.kts`:

```kotlin
include(":decisa-sdk")
project(":decisa-sdk").projectDir = file("../decisa-android/decisa-sdk")
```

In your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":decisa-sdk"))
}
```

### GitHub (clone + includeBuild)

```kotlin
// settings.gradle.kts
includeBuild("../decisa-android") {
    dependencySubstitution {
        substitute(module("ai.decisa.sdk:decisa-sdk")).using(project(":decisa-sdk"))
    }
}
```

Maven Central / GitHub Packages publication is planned for a future release.

Minimum **minSdk 21**, **compileSdk 34**.

---

## Quickstart

```kotlin
import ai.decisa.sdk.Decisa
import ai.decisa.sdk.DecisaEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Use a scope tied to your app lifecycle
        kotlinx.coroutines.GlobalScope.launch {
            // First launch: reads Play Install Referrer dcs_mclid, POSTs /v1/resolve,
            // persists visitor id + UTM. Subsequent launches reuse persisted state.
            Decisa.initialize(this@MyApplication, pixelKey = "dcs_px_your_public_key")
        }
    }
}

// When a user signs in, associate them. Email/phone are SHA-256 hashed on-device.
lifecycleScope.launch {
    Decisa.identify(userId = "user_123", email = "jane@example.com")
}

// Record a conversion. Install utm_* rides along in event metadata.
lifecycleScope.launch {
    Decisa.track(DecisaEvent.purchase(value = 49.90, currency = "USD"))
}
```

### Events

```kotlin
DecisaEvent.purchase(value = 49.90, currency = "USD")
DecisaEvent.lead()
DecisaEvent.completeRegistration()
DecisaEvent.startTrial()
DecisaEvent.subscribe()
DecisaEvent.addToCart(value = 19.99, currency = "USD")
DecisaEvent.initiateCheckout()
DecisaEvent.addPaymentInfo()
DecisaEvent.viewContent()
DecisaEvent.pageView()
DecisaEvent.search()
DecisaEvent.appInstall()
DecisaEvent.custom("viewed_pricing", metadata = mapOf("plan" to "pro"))
```

---

## Deferred deep linking & install attribution

1. **Mint the click.** Point your ad at `https://api.decisa.ai/k/<slug>?app=1`.
2. **Configure store URLs.** Set `android_store_url` on the UTM link metadata so
   the redirect embeds `dcs_mclid=<token>` in the Play Install Referrer.
3. **Resolve on first launch.** `Decisa.initialize` reads the referrer and POSTs
   `/v1/resolve` for a **deterministic** match.
4. **Bind and track.** Persisted `visitor_id` is used for all later `identify` / `track`.

If resolve finds no match (or returns silent `204`), the SDK mints a local fallback
`visitor_id` (`v_…`) so tracking still works.

> v1 does **not** read GAID. `madid` is always absent unless you add it yourself later.

---

## Decisa ecosystem & MCP

Decisa exposes a **[Model Context Protocol (MCP)](https://github.com/decisa-ai)**
server for campaign and attribution operations — launch UTM links, inspect match
rates, manage conversion events — while this SDK handles the **in-app** side.

Typical stack:

- **Web / funnel** — `pixel.js` on landing pages
- **Mobile** — **decisa-android** (Kotlin) or [decisa-flutter](https://github.com/decisa-ai/decisa-flutter) for cross-platform
- **iOS native** — [decisa-swift](https://github.com/decisa-ai/decisa-swift)
- **Ops & agents** — Decisa MCP tools

---

## Backend contract

| Endpoint | Purpose | Returns |
| --- | --- | --- |
| `POST /v1/resolve` | First-run deferred-attribution lookup | `200 { data: { visitor_id, matched, match_type, utm_* } }` or `204` |
| `POST /v1/identify` | Associate hashed identity with the visitor | `202` |
| `POST /v1/track` | Record a pixel event | `202` |

---

## Architecture

```
decisa-sdk/src/main/kotlin/ai/decisa/sdk/
  Decisa.kt                  # initialize / identify / track facade
  DecisaClient.kt            # orchestration
  DecisaEvent.kt             # event factories + toTrackBody
  DecisaEventName.kt         # canonical event names
  DecisaAttribution.kt       # resolved attribution model
  DecisaTransport.kt         # OkHttp POST + envelope decode
  DecisaPersistence.kt       # SharedPreferences
  DecisaHashing.kt           # client-side SHA-256
  InstallReferrerReader.kt   # Play Install Referrer → dcs_mclid
decisa-sdk/src/test/kotlin/ai/decisa/sdk/
  DecisaSDKTest.kt           # unit tests with injectable mocks
```

---

## FAQ

**How is this different from an MMP (Adjust, AppsFlyer, Branch)?**
DecisaSDK is a lightweight first-party **native pixel** tied to Decisa's ingest.
Android installs get **deterministic** `dcs_mclid` matching — ideal for **web2app**
and **funnel2app** flows with Decisa UTM links.

**Do I need GAID?**
No in v1. Attribution uses the Play Install Referrer token minted by Decisa's
`?app=1` store redirect.

**Kotlin or Java?**
Kotlin-first API with coroutines. Call from Java via `runBlocking` or your own
coroutine scope.

**Cross-platform app?**
Use [decisa-flutter](https://github.com/decisa-ai/decisa-flutter). Use
**decisa-android** for native Kotlin/Compose without a Flutter bridge.

---

## License

MIT. See [LICENSE](LICENSE).
