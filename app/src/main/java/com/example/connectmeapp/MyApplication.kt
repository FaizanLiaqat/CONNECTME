package com.example.connectmeapp // Replace with your actual package name

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firebase disk persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}