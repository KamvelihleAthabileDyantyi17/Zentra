# Testing evidence

Task 2 feedback asked us to *"complete and test the end-to-end customer journey on a physical Android phone, add meaningful tests for the core logic, verify the Firebase/Firestore configuration, and provide clear evidence of successful testing."* This page is that evidence.

## 1. Automated tests (run by GitHub Actions on every push)

| Suite | What it proves | Where to see it passing |
| --- | --- | --- |
| App unit tests (33) | Cart maths, checkout validation, order stages, full customer → staff → customer journey, Google sign-in and sign-out, account deletion, delivery fees, pickup times, settings, sales report and purchase recommendation | GitHub → Actions → latest run → **build** job → *Unit tests* step |
| Website end-to-end test (21 checks) | A customer and a staff member in two browser tabs: order, live status updates, Sales & buying, sold-out, sign-in, settings, deletion, phone layout | GitHub → Actions → latest run → **website** job; screenshots in the **website-test-screenshots** artifact |

**Evidence to add:** a screenshot of a green Actions run, saved as `docs/screenshots/testing/actions-green.png`.

## 2. Physical phone test (do this once Firebase is set up)

Install the APK from the latest Actions run (artifact **shannos-food-truck-debug**) on a real Android phone. Use **two phones** if you can: one as the customer, one as staff (or the staff website on a laptop).

Fill in one row per check. Save screenshots in `docs/screenshots/app/`.

**Phone:** ____________________ **Android version:** ______ **Tester:** ____________________ **Date:** __________

| # | Check | Steps | Expected | Pass / Fail | Screenshot |
| --- | --- | --- | --- | --- | --- |
| 1 | App opens | Launch the app | Home screen with the hero, favourites and Truck 1, no crash | | |
| 2 | Trucks | Tap the truck button | Truck 1 at Sandown Rd & Discovery Dr; Trucks 2 and 3 *Coming soon* and can't be chosen | | |
| 3 | Menu | Order now → tap each category | Items filter; + adds to order; badge on Order tab counts | | |
| 4 | Order | Order tab → change quantities, add a note | Line totals and total update | | |
| 5 | Validation | Checkout with empty name and number | Error messages, order not placed | | |
| 6 | Delivery fee | Deliver to me → Outside Parklands | Total goes up by R50; In Parklands shows Free | | |
| 7 | Pickup time | Choose a time slot | Slot highlighted; shows on the tracking screen | | |
| 8 | Place order | Fill details → Place order | Spinner, then tracking shows *Order received* and an order number | | |
| 9 | Order saved first | Turn on aeroplane mode → place an order | Button keeps spinning (no fake confirmation); cart is **not** cleared; turn data back on → it completes | | |
| 10 | Staff sees it | Second phone/web: Staff login → Orders | The order appears without refreshing, with the pickup time | | |
| 11 | Live updates | Staff: Start cooking → Mark ready → Collected | Customer's tracking screen changes at each tap | | |
| 12 | Feedback | Customer: Rate your food → 5 stars → Send | Thank-you screen; feedback appears in Firestore `feedback` | | |
| 13 | Google sign-in (SSO) | Account → Continue with Google | Name and email shown, *My orders* lists the order | | |
| 14 | Same login on website | Sign in on the website with the same Google account | Same *My orders* list | | |
| 15 | Session | Close the app fully, reopen | Still signed in | | |
| 16 | Settings | Turn *Remember my details* off → checkout | Name and number empty | | |
| 17 | Sold out | Staff: Menu → mark Slap Chips sold out | Customer menu shows *Sold out*; it's removed from carts | | |
| 18 | Sales & buying | Staff → Sales & buying | Today's orders and sales match; *What to buy* lists ingredients with costs | | |
| 19 | Sign out / delete | Account → Sign out; sign in again → Delete my account | Signed out; after delete, the user disappears from Firebase Authentication | | |
| 20 | Logging | Android Studio → Logcat → `tag:Shannos` while doing 8 to 11 | Each step is logged (order placed, stage changes) | | |

## 3. Firebase configuration check

| Check | How | Pass / Fail |
| --- | --- | --- |
| Sign-in providers on | Authentication → Sign-in method: Google, Anonymous, Email/Password enabled | |
| SHA-1 fingerprints added | Project settings → Android app → each developer's SHA-1 listed | |
| Rules published | Firestore → Rules matches `firestore.rules` | |
| Staff list | Firestore → `staff` has a document per staff UID | |
| Starter data | Firestore has `menu` (11 items) and `trucks` (3) | |
| Rules block others | Signed in as a customer, the staff dashboard says *isn't set up as staff* | |
