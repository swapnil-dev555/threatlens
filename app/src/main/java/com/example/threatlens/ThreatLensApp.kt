package com.example.threatlens

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ThreatLensApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = true
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
            setCustomKey("use_fake_backend", BuildConfig.USE_FAKE_BACKEND)
        }
    }
}
