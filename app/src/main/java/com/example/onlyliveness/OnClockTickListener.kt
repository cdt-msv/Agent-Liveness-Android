package com.example.liveness

import java.util.*


interface OnClockTickListener {
    fun onSecondTick(currentTime: GregorianCalendar)
    fun onMinuteTick(currentTime: GregorianCalendar)
}