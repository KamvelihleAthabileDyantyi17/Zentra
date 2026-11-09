// Website settings.
//
// USE_FIREBASE
//   false = demo mode: data lives in this browser only (localStorage). Great for trying the site
//           and taking screenshots. Open the customer site and the staff page in two tabs and
//           orders still update live between them.
//   true  = live mode: same Firebase project as the Android app, so orders, menu and Google
//           accounts are shared between the app and the website.
export const USE_FIREBASE = false;

// Firebase project settings (Firebase console → Project settings → Your apps → Web app).
// These values are not secret: Firebase protects data with the rules in /firestore.rules.
// TODO(firebase): register a Web app in the console and paste its appId below.
export const firebaseConfig = {
  apiKey: "AIzaSyBSf0IIJXRiGtzi84o7I4vIOw_0sassGYg",
  authDomain: "shannos-food-truck.firebaseapp.com",
  projectId: "shannos-food-truck",
  storageBucket: "shannos-food-truck.firebasestorage.app",
  messagingSenderId: "358281923499",
  appId: "PASTE_WEB_APP_ID_HERE",
};

// Firebase JavaScript SDK version loaded from Google's CDN.
export const FIREBASE_SDK_VERSION = "10.14.1";
