// End-to-end test of the website in demo mode: a customer orders in one tab, staff move the
// order along in another, and we check every step. Runs in GitHub Actions on every push.
//
// Local run:  cd tests/web && npm install && npx playwright install chromium
//             (in another terminal) cd web && python -m http.server 8765
//             npm test
// Env: BASE_URL (default http://localhost:8765/), CHROME_PATH (optional, use an installed Chrome),
//      SHOTS_DIR (optional, save screenshots there).

import { chromium } from "playwright";
import assert from "node:assert/strict";
import fs from "node:fs";

const BASE = process.env.BASE_URL || "http://localhost:8765/";
const SHOTS = process.env.SHOTS_DIR;
if (SHOTS) fs.mkdirSync(SHOTS, { recursive: true });

const errors = [];
const browser = await chromium.launch(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH, args: ["--no-sandbox"] } : {});
const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
// Google Fonts aren't needed for the test (and may be offline)
await ctx.route(/fonts\.(googleapis|gstatic)\.com/, (r) => r.abort());

const watch = (page, name) => {
  page.on("pageerror", (e) => errors.push(`${name}: ${e.message}`));
  page.on("console", (m) => { if (m.type() === "error" && !m.text().includes("net::ERR_FAILED")) errors.push(`${name}: ${m.text()}`); });
};
const shot = async (page, file) => {
  if (!SHOTS) return;
  await page.bringToFront(); // background tabs can stall screenshots
  await page.screenshot({ path: `${SHOTS}/${file}`, fullPage: true, timeout: 15000 });
};
let step = "start";
const check = (name) => { step = name; console.log("✓", name); };

try {
  const shop = await ctx.newPage(); watch(shop, "shop");
  await shop.goto(BASE); await shop.waitForSelector(".hero");
  await shot(shop, "01-home.png");
  check("home loads");

  // Trucks: 2 and 3 are coming soon and can't be chosen
  await shop.goto(BASE + "#/trucks"); await shop.waitForSelector('[data-action="truck"]');
  assert.equal(await shop.locator('[data-action="truck"][disabled]').count(), 2);
  assert.match(await shop.innerText(".narrow"), /Coming soon/);
  assert.match(await shop.innerText(".narrow"), /Sandown/);
  await shot(shop, "02-trucks.png");
  check("trucks 2 and 3 show as coming soon");

  // Add from menu
  await shop.goto(BASE + "#/menu"); await shop.waitForSelector(".menu-grid");
  await shop.click('button[aria-label="Add Signature Kota"]');
  await shop.click('button[aria-label="One more Signature Kota"]');
  await shop.click('button.chip:has-text("Drinks")');
  assert.equal(await shop.locator(".item").count(), 4);
  await shop.click('button[aria-label="Add Cold Drink (330ml)"]');
  assert.equal(await shop.innerText("#cart-count"), "3");
  await shot(shop, "03-menu.png");
  check("menu filter and add to order");

  await shop.click('a[data-route="cart"]'); await shop.waitForSelector(".total");
  assert.match(await shop.innerText(".total"), /R125\.00/);
  await shop.fill('textarea[data-field="note"]', "Extra sauce please");
  await shot(shop, "04-cart.png");
  await shop.click('a:has-text("Checkout")'); await shop.waitForSelector('[data-action="place"]');

  // Validation
  await shop.click('[data-action="place"]');
  assert.equal(await shop.locator(".field .error").count(), 2);
  check("checkout validation blocks missing details");

  // Delivery outside Parklands adds R50
  await shop.click('[data-action="fulfilment"][data-id="DELIVERY"]');
  await shop.click('[data-action="area"][data-id="OUTSIDE"]');
  assert.match(await shop.innerText(".narrow .total"), /R175\.00/);
  await shop.click('[data-action="area"][data-id="PARKLANDS"]');
  assert.match(await shop.innerText(".narrow .total"), /R125\.00/);
  await shop.click('[data-action="fulfilment"][data-id="PICKUP"]');
  check("delivery fee: free in Parklands, R50 outside");

  await shop.fill("#f-name", "Lindiwe");
  await shop.fill("#f-phone", "082 123 4567");
  await shop.click('[data-action="payment"][data-id="CASH"]');
  const slots = await shop.locator('[data-action="time"]').count();
  if (slots > 1) await shop.locator('[data-action="time"]').nth(1).click(); // a time slot, if the day isn't over
  assert.equal(await shop.inputValue("#f-name"), "Lindiwe");
  await shot(shop, "05-checkout.png");
  await shop.click('[data-action="place"]');
  await shop.waitForSelector(".status-card");
  assert.match(await shop.innerText(".status-card"), /order received/i);
  await shot(shop, "06-track-received.png");
  check("order placed and tracking shows Received");

  // Staff tab
  const staff = await ctx.newPage(); watch(staff, "staff");
  await staff.goto(BASE + "staff.html"); await staff.waitForSelector(".login-card");
  await shot(staff, "07-staff-login.png");
  await staff.fill('input[name="email"]', "owner@shannos.co.za");
  await staff.fill('input[name="password"]', "secret123");
  await staff.click('button[type="submit"]');
  await staff.waitForSelector(".ticket");
  assert.equal(await staff.locator(".ticket").count(), 1);
  assert.match(await staff.innerText(".ticket"), /Extra sauce please/);
  await shot(staff, "08-staff-orders.png");
  check("staff see the order live");

  await staff.click('.ticket [data-action="advance"]');
  await shop.waitForFunction(() => document.querySelector(".status-card").innerText.toUpperCase().includes("ON THE PAN"), null, { timeout: 5000 });
  await shot(shop, "09-track-cooking.png");
  check("customer sees On the pan live");
  await staff.click('.ticket [data-action="advance"]'); await staff.waitForTimeout(250);
  await staff.click('.ticket [data-action="advance"]'); await staff.waitForTimeout(250);
  assert.equal(await staff.locator(".ticket").count(), 0);
  await shop.waitForSelector('a:has-text("Rate your food")', { timeout: 5000 });
  check("order collected leaves the board");

  // Sales & buying
  await staff.click('[data-action="section"][data-id="report"]');
  await staff.waitForSelector("text=What to buy");
  const report = await staff.innerText(".content");
  assert.match(report, /R125\.00/);
  assert.match(report, /Bread/);
  assert.match(report, /Estimated shopping cost/);
  await shot(staff, "10-staff-sales-buying.png");
  check("sales report and purchase recommendation");

  await staff.click('[data-action="section"][data-id="menu"]');
  await staff.click('[data-action="toggle"][data-id="s1"]'); await staff.waitForTimeout(250);
  await shot(staff, "11-staff-menu.png");
  await staff.click('[data-action="section"][data-id="stock"]');
  assert.match(await staff.innerText(".content"), /loaf/);
  await shot(staff, "12-staff-stock.png");
  check("sold-out toggle and stock with units");

  // Feedback
  await shop.click('a:has-text("Rate your food")'); await shop.waitForSelector(".stars");
  await shop.click('[data-action="rate"][data-id="5"]');
  await shop.fill("#comment", "Lekker kota!");
  await shot(shop, "13-feedback.png");
  await shop.click('[data-action="send-feedback"]'); await shop.waitForSelector("text=Sharp, thank you!");
  check("feedback sent");

  await shop.goto(BASE + "#/menu"); await shop.waitForSelector(".menu-grid");
  await shop.click('button.chip:has-text("All")');
  assert.match(await shop.innerText(".menu-grid"), /Sold out/);
  check("sold-out item shows on the shop");

  // Account: Google SSO (demo), history, settings, session, delete
  await shop.click('a[data-route="account"]'); await shop.waitForSelector('[data-action="google"]');
  await shot(shop, "14-account-signed-out.png");
  await shop.click('[data-action="google"]'); await shop.waitForSelector("text=Signed in with Google");
  await shop.waitForSelector(".line");
  assert.match(await shop.innerText(".narrow"), /Collected/);
  await shot(shop, "15-account-signed-in.png");
  check("Google sign-in shows order history");

  await shop.reload(); await shop.waitForSelector("text=Signed in with Google");
  check("session survives reload");

  // Remember my details: on by default, saved after the order
  assert.match(await shop.innerText('[data-action="remember"]'), /On/);
  await shop.goto(BASE + "#/menu"); await shop.waitForSelector(".menu-grid");
  await shop.click('button[aria-label="Add Bottled Water"]');
  await shop.goto(BASE + "#/checkout"); await shop.waitForSelector("#f-phone");
  assert.equal(await shop.inputValue("#f-phone"), "082 123 4567");
  check("remembered details pre-fill checkout");
  await shop.goto(BASE + "#/account"); await shop.waitForSelector('[data-action="remember"]');
  await shop.click('[data-action="remember"]');
  assert.match(await shop.innerText('[data-action="remember"]'), /Off/);
  check("remember details can be switched off");

  shop.once("dialog", (d) => d.accept());
  await shop.click('[data-action="delete-account"]');
  await shop.waitForSelector('[data-action="google"]');
  check("delete account");

  await shop.goto(BASE + "privacy.html"); await shop.waitForSelector("#delete-account");
  check("privacy policy page");

  // Phone-size layout
  const phone = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true });
  await phone.route(/fonts\.(googleapis|gstatic)\.com/, (r) => r.abort());
  const m = await phone.newPage(); watch(m, "mobile");
  await m.goto(BASE); await m.waitForSelector(".hero");
  const navHeight = await m.evaluate(() => document.querySelector(".nav").getBoundingClientRect().height);
  assert.ok(navHeight < 60, `phone nav should fit on one line, was ${navHeight}px`);
  await shot(m, "16-mobile-home.png");
  check("phone layout");

  assert.deepEqual(errors, [], "no JavaScript errors");
  console.log("\nAll website checks passed.");
} catch (err) {
  console.error(`\n✗ Failed at: ${step}\n`, err);
  if (errors.length) console.error("Page errors:", errors);
  process.exitCode = 1;
} finally {
  await browser.close();
}
