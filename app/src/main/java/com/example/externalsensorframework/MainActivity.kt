package com.example.externalsensorframework

import android.os.Bundle

class MainActivity:
    AppCompatActivitySensorFrameworkUtil()/*, SensorEventListener, SensorObserver */{

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    }
}