# Google Play: release guide

Everything needed to publish Shannos Food Truck, and the **evidence the Task 3 rubric asks for**:
a signed APK, store screenshots, and a screenshot of the app uploaded in Google Play Console.

| Ready in the repo | You do |
| --- | --- |
| App icon (adaptive, plus `play-icon-512.png`) | Create the upload keystore (step 2) |
| Feature graphic `feature-graphic-1024x500.png` | Take phone screenshots (step 4) |
| Store listing text (below) | Fill in the owner details in `web/privacy.html` |
| Privacy policy page `web/privacy.html` | Create the Play developer account and upload (steps 5–7) |
| In-app and web **Delete my account** (Play requirement) | |
| Release signing in Gradle + signed build in GitHub Actions | |

---

## 1. Change the application ID (do this with the Firebase person)

Google Play **rejects** IDs starting with `com.example`, so the app needs a real one, e.g. `za.co.shannos.foodtruck`.
The ID can never change after the first upload, so choose carefully.

1. Firebase console → ⚙ Project settings → **Add app → Android** with package `za.co.shannos.foodtruck`.
   Add the team's debug SHA-1 fingerprints and the upload key SHA-1 (step 2).
2. Download the new `google-services.json` and replace `app/google-services.json`.
3. In `app/build.gradle.kts` change `applicationId = "com.example.shannosfoodtruck"` to `applicationId = "za.co.shannos.foodtruck"`.
   Leave `namespace` as it is; it's only the Kotlin package name and doesn't need to change.
4. Commit, push, and check the GitHub Actions build is green.

## 2. Create the upload keystore (once, keep it forever)

The keystore proves future updates come from you. **If it's lost, you can't update the app**
(Play App Signing lets Google reset it, but that takes time). Store it and its passwords in a password manager,
and never commit it: `.gitignore` already blocks `*.jks` and `keystore.properties`.

**Easiest: Android Studio**
Build → Generate Signed Bundle / APK → Android App Bundle → **Create new…**
- Key store path: somewhere *outside* the project, e.g. `C:\Users\<you>\keys\shannos-upload.jks`
- Alias: `shannos-upload`, validity 25+ years, your name as first/last name, `ZA` as country code.

**Or the command line** (keytool comes with Android Studio's JDK):
```
keytool -genkeypair -v -keystore shannos-upload.jks -alias shannos-upload -keyalg RSA -keysize 2048 -validity 10000
```

Then create `keystore.properties` in the project root (next to `settings.gradle.kts`):
```
storeFile=C:/Users/<you>/keys/shannos-upload.jks
storePassword=your-store-password
keyAlias=shannos-upload
keyPassword=your-key-password
```

Get the upload key's SHA-1 for Firebase (Google sign-in in release builds needs it):
```
keytool -list -v -keystore C:/Users/<you>/keys/shannos-upload.jks -alias shannos-upload
```

## 3. Build the signed APK and App Bundle

**On your computer:** `./gradlew assembleRelease bundleRelease`
- Signed APK (rubric evidence, installable on a phone): `app/build/outputs/apk/release/app-release.apk`
- App Bundle (what Play wants): `app/build/outputs/bundle/release/app-release.aab`

**On GitHub Actions (optional, nice evidence):** add these repository secrets
(GitHub → Settings → Secrets and variables → Actions → New repository secret):

| Secret | Value |
| --- | --- |
| `SHANNOS_KEYSTORE_BASE64` | the keystore as base64. PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\Users\<you>\keys\shannos-upload.jks")) \| Set-Clipboard` |
| `SHANNOS_KEYSTORE_PASSWORD` | store password |
| `SHANNOS_KEY_ALIAS` | `shannos-upload` |
| `SHANNOS_KEY_PASSWORD` | key password |

Every push then also produces a **shannos-food-truck-release-signed** artifact containing the signed APK and AAB.

## 4. Phone screenshots

Play needs at least 2 (up to 8) phone screenshots, PNG or JPEG, between 320 and 3840 px on each side,
with the long side no more than twice the short side. A normal phone screenshot works.

Run the release build on a phone with live data and capture (power + volume down):
1. Home (hero + favourites)
2. Menu with something in the order
3. Your order (cart)
4. Checkout
5. Live tracking ("On the pan")
6. Account signed in with Google + My orders
7. Staff dashboard with an order
8. Staff menu with a sold-out item

Save them in `docs/screenshots/app/`. They count toward the "Screenshots" rubric row too.

## 5. Google Play developer account

[play.google.com/console/signup](https://play.google.com/console/signup): one-off US$25 fee, plus ID verification
that can take a few days, so **start early**. Use a team or owner account, not a personal one you'll lose access to.

> New personal accounts must run a **closed test with at least 12 testers for 14 days** before they can publish to
> production. For the assignment, an upload to the **Internal testing** track is enough evidence, and it has no waiting period.

## 6. Create the app in Play Console

Play Console → **Create app**
- App name: `Shannos Food Truck`, Default language: English (South Africa) – en-ZA
- App, Free, accept the declarations.

**Set up your app** (dashboard checklist), with answers:

| Section | Answer |
| --- | --- |
| Privacy policy | `https://shannos-food-truck.web.app/privacy.html` (after the website is deployed) |
| App access | Some functionality is restricted. Add a staff test login (email + password) so reviewers can see the dashboard |
| Ads | No ads |
| Content rating | Questionnaire → category *Food & drink / utility*; answer No to violence, etc. Expect "Everyone / 3+" |
| Target audience | 13+ (not designed for children) |
| News app | No |
| Data safety | See the table below |
| Government app / financial features / health | No |
| Category | Food & Drink |
| Contact details | `[CONTACT EMAIL]` |

**Data safety answers** (matches what the app actually does):

| Question | Answer |
| --- | --- |
| Collects or shares user data? | Collects: **yes**. Shares: **no** (Firebase is a service provider, which is not "sharing") |
| Encrypted in transit? | Yes |
| Can users request deletion? | Yes: in-app *Delete my account*, plus the web link `https://shannos-food-truck.web.app/privacy.html#delete-account` |
| Personal info: Name | Collected · App functionality, Account management · optional for Google sign-in, required to order |
| Personal info: Email address | Collected · Account management · optional (only with Google sign-in) |
| Personal info: Phone number | Collected · App functionality (contact about the order) · required to order |
| Personal info: Address | Collected · App functionality (delivery) · optional |
| Personal info: User IDs | Collected · Account management |
| Financial info: Purchase history | Collected · App functionality (order history) |
| App activity: Other user-generated content | Collected · feedback rating and comment · optional |
| Location, contacts, photos, device IDs, etc. | Not collected |

## 7. Upload and take the evidence screenshot

1. Testing → **Internal testing** → Create new release.
2. Choose **Use Google-generated key** (Play App Signing) when asked.
3. Upload `app-release.aab`. Release name: `2.4`. Paste the release notes below.
4. Save → Review release → **Start rollout to Internal testing**.
5. Add testers (your team's Gmail addresses) and share the opt-in link.
6. **Screenshot** the release page showing version 2.3 uploaded. That's the rubric evidence.
   Save it as `docs/play-store/evidence-play-console.png`.
7. App integrity → App signing: copy the **App signing key SHA-1** into Firebase as well, or Google sign-in
   won't work for people who install from Play.

---

## Store listing text

**App name** (30 max): `Shannos Food Truck`

**Short description** (80 max):
```
Order kotas, amagwinya & cold drinks ahead. Track it live and collect it hot.
```

**Full description**:
```
Kasi flavour. No queue.

Shannos Food Truck brings kotas, amagwinya, rolls and cold drinks to Parklands, and now you can order ahead and skip the line.

ORDER IN SECONDS
• Pick the truck you want to collect from
• Browse the menu by Kotas, Sides, Rolls and Drinks
• Leave a note for the kitchen: extra sauce, no cheese, you know how you like it
• Collect at the truck or ask for delivery
• Pay by card or cash at the truck

TRACK IT LIVE
Watch your order move from Received to On the pan to Ready to collect, updated the moment the team moves it along. Show your order number at the window and it's yours.

NO ACCOUNT NEEDED
Order as a guest, or Continue with Google to save your details and see your order history on your phone and on our website.

FOR THE TRUCK TEAM
Staff sign in to a live order board for each truck, mark items sold out instantly, and keep an eye on stock.

Lekker food, no queue. See you at the truck.
```

**Release notes for 2.4** (500 max):
```
• New look: the full app rebuilt in the Shannos kasi style
• Live order tracking, from received to ready to collect
• Continue with Google, with your order history on app and website
• Staff dashboard: live orders per truck, sold-out items, stock
• Choose a pickup time, or free delivery in Parklands
• Delete your account any time from the Account tab
```

## Assets in this folder

| File | Use |
| --- | --- |
| `play-icon-512.png` | Store listing → App icon (512 × 512) |
| `feature-graphic-1024x500.png` | Store listing → Feature graphic |
