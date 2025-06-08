package com.hemant.pianobase

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class PianoKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Piano key data classes
    data class PianoKey(
        val rect: RectF,
        val note: String,
        val octave: Int,
        val isBlack: Boolean,
        var isPressed: Boolean = false
    )

    // Collections to store keys
    private val whiteKeys = mutableListOf<PianoKey>()
    private val blackKeys = mutableListOf<PianoKey>()
    private val allKeys = mutableListOf<PianoKey>()

    // Paint objects for drawing
    private val whiteKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val whiteKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.FILL
    }

    private val blackKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2C")
        style = Paint.Style.FILL
    }

    private val blackKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Piano configuration
    private val whiteNotesPattern = arrayOf("C", "D", "E", "F", "G", "A", "B")
    private val blackNotesPattern = arrayOf("C#", "D#", "", "F#", "G#", "A#", "")
    private var numberOfOctaves = 2 // Default to 2 octaves, can be adjusted
    private var startingOctave = 4

    // Key dimensions (will be calculated based on view size)
    private var whiteKeyWidth = 0f
    private var whiteKeyHeight = 0f
    private var blackKeyWidth = 0f
    private var blackKeyHeight = 0f

    // Touch handling
    private var currentlyPressedKeys = mutableSetOf<PianoKey>()

    // Listener for key events
    interface OnKeyPressListener {
        fun onKeyPressed(note: String, octave: Int)
        fun onKeyReleased(note: String, octave: Int)
    }

    private var keyPressListener: OnKeyPressListener? = null

    fun setOnKeyPressListener(listener: OnKeyPressListener) {
        keyPressListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateKeyDimensions()
        createKeys()
    }

    private fun calculateKeyDimensions() {
        val totalWhiteKeys = numberOfOctaves * 7
        whiteKeyWidth = width.toFloat() / totalWhiteKeys
        whiteKeyHeight = height.toFloat()
        blackKeyWidth = whiteKeyWidth * 0.6f
        blackKeyHeight = whiteKeyHeight * 0.6f
    }

    private fun createKeys() {
        whiteKeys.clear()
        blackKeys.clear()
        allKeys.clear()

        var whiteKeyIndex = 0

        // Create keys for each octave
        for (octave in startingOctave until startingOctave + numberOfOctaves) {
            for (noteIndex in whiteNotesPattern.indices) {
                val note = whiteNotesPattern[noteIndex]

                // Create white key
                val whiteKeyRect = RectF(
                    whiteKeyIndex * whiteKeyWidth,
                    0f,
                    (whiteKeyIndex + 1) * whiteKeyWidth,
                    whiteKeyHeight
                )

                val whiteKey = PianoKey(whiteKeyRect, note, octave, false)
                whiteKeys.add(whiteKey)
                allKeys.add(whiteKey)

                // Create black key if it exists for this position
                val blackNote = blackNotesPattern[noteIndex]
                if (blackNote.isNotEmpty()) {
                    val blackKeyX = whiteKeyRect.right - (blackKeyWidth / 2)
                    val blackKeyRect = RectF(
                        blackKeyX,
                        0f,
                        blackKeyX + blackKeyWidth,
                        blackKeyHeight
                    )

                    val blackKey = PianoKey(blackKeyRect, blackNote, octave, true)
                    blackKeys.add(blackKey)
                    allKeys.add(blackKey)
                }

                whiteKeyIndex++
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw white keys first
        for (key in whiteKeys) {
            val paint = if (key.isPressed) whiteKeyPressedPaint else whiteKeyPaint
            canvas.drawRect(key.rect, paint)
            canvas.drawRect(key.rect, borderPaint)
        }

        // Draw black keys on top
        for (key in blackKeys) {
            val paint = if (key.isPressed) blackKeyPressedPaint else blackKeyPaint
            canvas.drawRect(key.rect, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                handleTouchDown(event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                handleTouchUp(event)
            }
            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(event)
            }
        }
        return true
    }

    private fun handleTouchDown(event: MotionEvent) {
        val pointerIndex = event.actionIndex
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        findKeyAtPosition(x, y)?.let { key ->
            if (!key.isPressed) {
                key.isPressed = true
                currentlyPressedKeys.add(key)
                keyPressListener?.onKeyPressed(key.note, key.octave)
                invalidate()
            }
        }
    }

    private fun handleTouchUp(event: MotionEvent) {
        // Release all currently pressed keys
        for (key in currentlyPressedKeys) {
            key.isPressed = false
            keyPressListener?.onKeyReleased(key.note, key.octave)
        }
        currentlyPressedKeys.clear()
        invalidate()
    }

    private fun handleTouchMove(event: MotionEvent) {
        val keysToRelease = mutableSetOf<PianoKey>()
        val keysToPress = mutableSetOf<PianoKey>()

        // Check all active pointers
        for (i in 0 until event.pointerCount) {
            val x = event.getX(i)
            val y = event.getY(i)
            val keyAtPosition = findKeyAtPosition(x, y)

            if (keyAtPosition != null && !keyAtPosition.isPressed) {
                keysToPress.add(keyAtPosition)
            }
        }

        // Find keys that are no longer being touched
        for (key in currentlyPressedKeys) {
            var stillTouched = false
            for (i in 0 until event.pointerCount) {
                val x = event.getX(i)
                val y = event.getY(i)
                if (findKeyAtPosition(x, y) == key) {
                    stillTouched = true
                    break
                }
            }
            if (!stillTouched) {
                keysToRelease.add(key)
            }
        }

        // Release keys
        for (key in keysToRelease) {
            key.isPressed = false
            currentlyPressedKeys.remove(key)
            keyPressListener?.onKeyReleased(key.note, key.octave)
        }

        // Press new keys
        for (key in keysToPress) {
            key.isPressed = true
            currentlyPressedKeys.add(key)
            keyPressListener?.onKeyPressed(key.note, key.octave)
        }

        if (keysToRelease.isNotEmpty() || keysToPress.isNotEmpty()) {
            invalidate()
        }
    }

    private fun findKeyAtPosition(x: Float, y: Float): PianoKey? {
        // Check black keys first (they're on top)
        for (key in blackKeys) {
            if (key.rect.contains(x, y)) {
                return key
            }
        }

        // Then check white keys
        for (key in whiteKeys) {
            if (key.rect.contains(x, y)) {
                return key
            }
        }

        return null
    }

    // Public methods to configure the keyboard
    fun setNumberOfOctaves(octaves: Int) {
        numberOfOctaves = octaves
        if (width > 0 && height > 0) {
            calculateKeyDimensions()
            createKeys()
            invalidate()
        }
    }

    fun setStartingOctave(octave: Int) {
        startingOctave = octave
        if (width > 0 && height > 0) {
            createKeys()
            invalidate()
        }
    }
}