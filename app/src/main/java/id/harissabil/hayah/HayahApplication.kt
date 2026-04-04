package id.harissabil.hayah

import android.app.Application
import id.harissabil.hayah.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class HayahApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@HayahApplication)
            modules(appModule)
        }
    }
}
