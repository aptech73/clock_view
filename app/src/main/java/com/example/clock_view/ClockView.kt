package com.example.clock_view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import android.text.format.DateUtils.SECOND_IN_MILLIS
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min


class ClockView (
    context : Context,
    attrs : AttributeSet
) : View(context, attrs) {

    private var mDial: Drawable? = null
    private var mHourHand: Drawable? = null
    private var mMinuteHand: Drawable? = null
    private var mSecondHand: Drawable? = null

    private var mTime: Calendar? = null
    private var mDescFormat: String? = null
    private var mTimeZone: TimeZone? = null
    private val mEnableSeconds = true

    init {
        mTime = Calendar.getInstance()
        mDescFormat = (DateFormat.getTimeFormat(context) as SimpleDateFormat).toLocalizedPattern()

        mDial = AppCompatResources.getDrawable(context, R.drawable.clock_dial)
        mHourHand = AppCompatResources.getDrawable(context, R.drawable.clock_hand_hour)
        mMinuteHand = AppCompatResources.getDrawable(context, R.drawable.clock_hand_minute)
        mSecondHand = AppCompatResources.getDrawable(context, R.drawable.clock_hand_second)

        initDrawable(mDial!!)
        initDrawable(mHourHand!!)
        initDrawable(mMinuteHand!!)
        initDrawable(mSecondHand!!)
    }

    private val mClockTick: Runnable = object : Runnable {
        override fun run() {
            onTimeChanged()
            if (mEnableSeconds) {
                val now = System.currentTimeMillis()
                val delay = SECOND_IN_MILLIS - now % SECOND_IN_MILLIS
                postDelayed(this, delay)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        context.registerReceiver(mIntentReceiver, filter)
        mTime = (if (mTimeZone != null) mTimeZone else TimeZone.getDefault())?.let {
            Calendar.getInstance(
                it
            )
        }
        onTimeChanged()
        if (mEnableSeconds) {
            mClockTick.run()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(mIntentReceiver)
        removeCallbacks(mClockTick)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth =
            max(mDial!!.intrinsicWidth.toDouble(), suggestedMinimumWidth.toDouble())
                .toInt()
        val minHeight =
            max(mDial!!.intrinsicHeight.toDouble(), suggestedMinimumHeight.toDouble())
                .toInt()
        setMeasuredDimension(
            getDefaultSize(minWidth, widthMeasureSpec),
            getDefaultSize(minHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        val saveCount = canvas.save()
        canvas.translate((w / 2).toFloat(), (h / 2).toFloat())
        val scale = min(
            (w.toFloat() / mDial!!.intrinsicWidth).toDouble(),
            (
                    h.toFloat() / mDial!!.intrinsicHeight).toDouble()
        ).toFloat()
        if (scale < 1f) {
            canvas.scale(scale, scale, 0f, 0f)
        }
        mDial!!.draw(canvas)
        val hourAngle = mTime!![Calendar.HOUR] * 30f
        canvas.rotate(hourAngle, 0f, 0f)
        mHourHand!!.draw(canvas)
        val minuteAngle = mTime!![Calendar.MINUTE] * 6f
        canvas.rotate(minuteAngle - hourAngle, 0f, 0f)
        mMinuteHand!!.draw(canvas)
        if (mEnableSeconds) {
            val secondAngle = mTime!![Calendar.SECOND] * 6f
            canvas.rotate(secondAngle - minuteAngle, 0f, 0f)
            mSecondHand!!.draw(canvas)
        }
        canvas.restoreToCount(saveCount)
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return mDial === who || mHourHand === who || mMinuteHand === who || mSecondHand === who || super.verifyDrawable(
            who
        )
    }

    private val mIntentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED == intent.action) {
                val tz = intent.getStringExtra("time-zone")
                mTime = Calendar.getInstance(TimeZone.getTimeZone(tz))
            }
            onTimeChanged()
        }
    }

    private fun onTimeChanged() {
        mTime!!.setTimeInMillis(System.currentTimeMillis())
        setContentDescription(DateFormat.format(mDescFormat, mTime))
        invalidate()
    }

    private fun initDrawable(drawable: Drawable) {
        val midX = drawable.intrinsicWidth / 2
        val midY = drawable.intrinsicHeight / 2
        drawable.setBounds(-midX, -midY, midX, midY)
    }

}