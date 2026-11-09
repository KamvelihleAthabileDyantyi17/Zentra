package com.example.shannosfoodtruck

import android.app.Application
import android.content.Context
import com.example.shannosfoodtruck.data.ActiveOrderStore
import com.example.shannosfoodtruck.data.AppLog
import com.example.shannosfoodtruck.data.AuthService
import com.example.shannosfoodtruck.data.DemoAuthService
import com.example.shannosfoodtruck.data.DemoRepository
import com.example.shannosfoodtruck.data.FirebaseAuthService
import com.example.shannosfoodtruck.data.FirestoreRepository
import com.example.shannosfoodtruck.data.FoodRepository
import com.example.shannosfoodtruck.data.PrefsActiveOrderStore

class ShannosApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AppLog.i("App", "Started v${BuildConfig.VERSION_NAME} using ${if (BuildConfig.USE_FIREBASE) "Firebase (live data)" else "demo data"}")
    }
}

/** Creates the app's shared objects once. Flip USE_FIREBASE in app/build.gradle.kts for offline demo data. */
class AppContainer(context: Context) {
    val repository: FoodRepository =
        if (BuildConfig.USE_FIREBASE) FirestoreRepository() else DemoRepository()

    val auth: AuthService =
        if (BuildConfig.USE_FIREBASE) FirebaseAuthService() else DemoAuthService()

    val activeOrderStore: ActiveOrderStore = PrefsActiveOrderStore(context)
}
