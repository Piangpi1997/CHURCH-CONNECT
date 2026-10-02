package org.cmf.churchconnect

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

internal object AppCheckInstaller {
    fun install(app: FirebaseApp) {
        val providerFactory = if (BuildConfig.DEBUG) {
            Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                .getMethod("getInstance").invoke(null) as AppCheckProviderFactory
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(providerFactory)
    }
}
