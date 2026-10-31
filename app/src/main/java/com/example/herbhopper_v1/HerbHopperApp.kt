package com.example.herbhopper_v1

import android.app.Application
import com.example.herbhopper_v1.data.SessionManager

class HerbHopperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
