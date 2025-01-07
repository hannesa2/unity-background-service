package com.kdg.toast.plugin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar
import java.util.Date

class PedometerService : Service(), SensorEventListener {
    private var sharedPreferences: SharedPreferences? = null
    private var TAG: String = "PEDOMETER"
    private var sensorManager: SensorManager? = null
    private var running: Boolean = false
    private var currentDate: Date? = null
    private var initialDate: Date? = null
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            "PedometerLib",
            "Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun startNotification() {
        val input = "Counting your steps..."
        val notificationIntent = Intent(this, Bridge.myActivity.javaClass)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, "PedometerLib")
            .setContentTitle("Background Walking Service")
            .setContentText(input)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(112, notification)
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate: CREATED" + Bridge.steps)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        loadData()
        saveSummarySteps(Bridge.summarySteps + Bridge.steps)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "onTaskRemoved: REMOVED" + Bridge.steps)
        initSensorManager()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: STARTED")
        createNotificationChannel()
        startNotification()
        super.onCreate()
        Bridge.initialSteps = 0
        initSensorManager()
        val editor = sharedPreferences!!.edit()
        initialDate = Calendar.getInstance().time
        editor.putString(Bridge.INIT_DATE, currentDate.toString())
        editor.apply()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy: DESTROYED")
        disposeSensorManager()
        loadData()
        saveSummarySteps(Bridge.summarySteps + Bridge.steps)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        Log.i(TAG, "onSensorChanged!!!!!!: " + sensorEvent.values[0])
        if (Bridge.initialSteps == 0) {
            Log.i(TAG, "onSensorChanged: AWAKE")
            Bridge.initialSteps = sensorEvent.values[0].toInt()
        }
        if (running) {
            Bridge.steps = sensorEvent.values[0].toInt() - Bridge.initialSteps
            Log.i(TAG, "onSensorChanged: current steps: " + Bridge.steps)
            saveData(Bridge.steps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}

    fun initSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        running = true
        val countSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (countSensor != null) {
            sensorManager!!.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            Toast.makeText(Bridge.myActivity, "Sensor Not Found (", Toast.LENGTH_LONG).show()
        }
    }

    fun disposeSensorManager() {
        running = false
        sensorManager!!.unregisterListener(this)
    }

    fun saveData(currentSteps: Int) {
        val editor = sharedPreferences!!.edit()
        currentDate = Calendar.getInstance().time
        editor.putString(Bridge.DATE, currentDate.toString())
        Log.i(TAG, "saveData: saved! $currentSteps")
        editor.putInt(Bridge.STEPS, currentSteps)
        editor.apply()
    }

    fun saveSummarySteps(stepsToSave: Int) {
        val editor = sharedPreferences!!.edit()
        currentDate = Calendar.getInstance().time
        editor.putString(Bridge.DATE, currentDate.toString())
        Log.i(TAG, "saveSummarySteps: saved! $stepsToSave")
        editor.putInt("summarySteps", stepsToSave)
        editor.apply()
    }

    fun loadData() {
        Bridge.steps = sharedPreferences!!.getInt(Bridge.STEPS, 0)
        Bridge.summarySteps = sharedPreferences!!.getInt("summarySteps", 0)
        Log.i(TAG, "loadData: steps" + Bridge.steps)
        Log.i(TAG, "loadData: summarySteps " + Bridge.summarySteps)
    }
}