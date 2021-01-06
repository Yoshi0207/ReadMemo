package com.example.readmemo

import android.app.Application
import io.realm.Realm

class ReadMemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
    }
}