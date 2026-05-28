package com.example.herbhopper_v1

import android.app.Application
import com.example.herbhopper_v1.data.SessionManager
import com.google.firebase.FirebaseApp

class HerbHopperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        SessionManager.init(this)
    }
}
