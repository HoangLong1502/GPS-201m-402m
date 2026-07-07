package com.longvhse192032.gpsracer

import android.app.Application
import com.google.android.gms.ads.MobileAds

class GpsRacerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
    }
}
