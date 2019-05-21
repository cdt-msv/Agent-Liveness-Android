package com.example.liveness

import java.util.*

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.SystemClock

class Clock @JvmOverloads constructor(private var Context: Context, tickMethod: Int = Clock.TICKPERSECOND) {

    private val gregorian: GregorianCalendar
    private var mHandler: Handler? = null
    private val mOnClockTickListenerList = ArrayList<OnClockTickListener>()

    private var mTicker: Runnable? = null

    private var mIntentReceiver: BroadcastReceiver? = null
    private var mIntentFilter: IntentFilter? = null

    private var mTickMethod = 0

    init {
        this.mTickMethod = tickMethod
        this.gregorian = GregorianCalendar()

        when (mTickMethod) {
            0 -> this.startTickPerSecond()
            1 -> this.startTickPerMinute()

            else -> {
            }
        }
    }

    private fun tick(tickInMillis: Long) {
        this@Clock.gregorian.timeInMillis = this@Clock.gregorian.timeInMillis + tickInMillis
        this.notifyOnTickListeners()
    }

    private fun notifyOnTickListeners() {
        when (mTickMethod) {
            0 -> for (listener in mOnClockTickListenerList) {
                listener.onSecondTick(gregorian)
            }
            1 -> for (listener in mOnClockTickListenerList) {
                listener.onMinuteTick(gregorian)
            }
        }

    }

    private fun startTickPerSecond() {
        this.mHandler = Handler()
        this.mTicker = Runnable {
            tick(1000)
            val now = SystemClock.uptimeMillis()
            val next = now + (1000 - now % 1000)
            mHandler!!.postAtTime(mTicker, next)
        }
        this.mTicker!!.run()

    }

    private fun startTickPerMinute() {
        this.mIntentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                tick(60000)

            }
        }
        this.mIntentFilter = IntentFilter()
        this.mIntentFilter!!.addAction(Intent.ACTION_TIME_TICK)
        this.Context.registerReceiver(this.mIntentReceiver, this.mIntentFilter, null, this.mHandler)

    }

    fun stopTick() {
        if (this.mIntentReceiver != null) {
            this.Context.unregisterReceiver(this.mIntentReceiver)
        }
        if (this.mHandler != null) {
            this.mHandler!!.removeCallbacks(this.mTicker)
        }
    }

    fun getCurrentTime(): GregorianCalendar {
        return this.gregorian
    }

    fun addClockTickListener(listener: OnClockTickListener) {
        this.mOnClockTickListenerList.add(listener)

    }

    companion object {
        const val TICKPERSECOND = 0
        const val TICKPERMINUTE = 1
    }

}