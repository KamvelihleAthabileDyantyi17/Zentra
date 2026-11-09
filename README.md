# Shannos Food Truck

Order-ahead app for Shannos Food Truck: kasi food (kotas, amagwinya, rolls, cold drinks), ordered from your phone and collected hot from the truck.

Built by the **Zentra** team.

| Role | Name |
| --- | --- |
| Team Lead, DevOps & repo | Kamv'elihle Dyantyi |
| UI/UX & Design | Katleho Khutsoane |
| Requirements Analysis | Neziswa Bikitsha |
| Database & Development | Amandla Sekeleni |

- **Design prototype:** see the Shannos Food Truck prototype canvas (customer app + owner dashboard).
- **Demo video (v1):** https://youtu.be/KhhJptnaOf4

## What the app does

**Customers** (no account needed):
1. Pick a truck → browse the menu by category (Kotas, Sides, Rolls, Drinks)
2. Add items, leave a note for the kitchen, check out (collect or delivery, card or cash)
3. Track the order live: Received → On the pan → Ready to collect → Collected
4. Rate the food

**Staff** (Home → *Staff login*):
- Live order board per truck. One tap moves an order to the next stage, and the customer's phone updates instantly.
- Mark menu items sold out (they disappear from customers' carts straight away)
- Stock levels with Low / Out flags

## Tech

- Kotlin, Jetpack Compose (Material 3), Navigation Compose, ViewModel + StateFlow
- Firebase: Cloud Firestore (real-time data) and Authentication (anonymous for customers, email/password for staff)
- Gradle 8.11 · Android Gradle Plugin 8.7 · Kotlin 2.0 · JDK 17 · min SDK 24

```
app/src/main/java/com/example/shannosfoodtruck/
├── MainActivity.kt / ShannosApp.kt     app entry + shared objects
├── data/                               models, cart maths, repositories
│   ├── FoodRepository.kt               the one interface all screens use
│   ├── FirestoreRepository.kt          live Firebase implementation
│   └── DemoRepository.kt               offline/in-memory implementation (tests + demos)
└── ui/
    ├── theme/                          kasi colours + type
    ├── components/                     shared buttons, tiles, fields
    ├── customer/                       home, menu, cart, checkout, track, feedback
    └── staff/                          login + dashboard
```

## Run it

1. Open the project in Android Studio and let Gradle sync.
2. Run on a phone (USB debugging) or an emulator.

**No internet or Firebase not set up yet?** In `app/build.gradle.kts` set `USE_FIREBASE` to `"false"`. The app then runs on built-in demo data, and any email + 6-character password works for staff login.

## Firebase setup (once)

In the [Firebase console](https://console.firebase.google.com) for this project:

1. **Authentication → Sign-in method:** enable **Anonymous** and **Email/Password**.
2. **Authentication → Users:** add a staff user (e.g. the owner's email). Copy their **User UID**.
3. **Firestore → Data:** create a collection `staff` with a document whose ID is that UID (fields can be anything, e.g. `name: "Owner"`).
4. **Firestore → Rules:** paste the contents of [`firestore.rules`](firestore.rules) and publish.
5. In the app: Home → Staff login → sign in → **Menu → Load starter menu**. This creates the menu, the three trucks and stock lists. It never overwrites existing data.

Old collections from v1 (`menu_items`, sample `orders`/`feedback`) are no longer used and can be deleted.

## Tests

```
./gradlew testDebugUnitTest
```

Covers cart maths, sold-out handling, phone validation, order stages, and a full customer → staff → customer order journey on demo data. GitHub Actions runs the tests and builds the APK on every push. Download the APK from the workflow run's artifacts.

## Team workflow

- No direct commits to `main`. Work on `feature/<name>` branches and open a Pull Request.
- The Team Lead reviews PRs against the design prototype.
- CI must be green before merging.

## Still to do

- Real truck locations for Trucks 2 and 3 (`StarterData.kt`, marked `TODO(team)`)
- Delivery fee (currently shown as "[SET BY OWNER]")
- Real card payments (e.g. a payment gateway); today, card is paid at collection
- Stock that drops automatically as orders are placed
- Anton headline font (instructions in `ui/theme/Theme.kt`)

Project documents (plan, report, architecture, task list, old HTML prototype) are in [`docs/`](docs/).
