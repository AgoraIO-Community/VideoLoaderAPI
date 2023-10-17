package io.agora.videoloaderapi

import android.app.Application

class AgoraApplication : Application() {

    companion object {
        private var sInstance: AgoraApplication? = null
        fun the(): AgoraApplication? {
            return sInstance
        }
    }

    override fun onCreate() {
        super.onCreate()
        sInstance = this
    }
}