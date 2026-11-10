// Website settings.
//
// USE_FIREBASE
//   false = demo mode: data lives in this browser only (localStorage). Great for trying the site
//           and taking screenshots. Open the customer site and the staff page in two tabs and
//           orders still update live between them.
//   true  = live mode: same Firebase project as the Android app, so orders, menu and Google
//           accounts are shared between the app and the website.
export const USE_FIREBASE = true;

// Firebase project settings (Firebase console → Project settings → Your apps → Web app).
// These values are not secret: Firebase protects data with the rules in /firestore.rules.
export const firebaseConfig = {
  apiKey: "AIzaSyB9xj4HV3i0LQALt28dUQIXiaARjo1s5Ag",
  authDomain: "shannos-food-truck-3349e.firebaseapp.com",
  projectId: "shannos-food-truck-3349e",
  storageBucket: "shannos-food-truck-3349e.firebasestorage.app",
  messagingSenderId: "228911235953",
  appId: "1:228911235953:web:c5e332adead74a5ae92cd0",
};

// Firebase JavaScript SDK version loaded from Google's CDN.
export const FIREBASE_SDK_VERSION = "10.14.1";
