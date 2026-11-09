// Picks the live (Firebase) or demo backend. Both expose the same functions, so the pages
// never need to know which one they're using: the same idea as FoodRepository in the app.

import { USE_FIREBASE } from "./config.js";
import { log } from "./util.js";

export async function getBackend() {
  if (USE_FIREBASE) {
    const { createFirebaseBackend } = await import("./firebase-backend.js");
    return { ...(await createFirebaseBackend()), demo: false };
  }
  log.i("Backend", "Demo mode: data is stored in this browser only");
  const { data, auth } = await import("./demo-backend.js");
  return { data, auth, demo: true };
}
