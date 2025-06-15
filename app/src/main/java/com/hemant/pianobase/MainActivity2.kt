package com.hemant.pianobase

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hemant.pianobase.databinding.ActivityMain2Binding
import kotlin.math.abs

class MainActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding
    private var currentZoom = 100f
    private val minZoom = 50f
    private val maxZoom = 150f
    private var baseKeyWidth = 120
    private var baseKeyHeight = 400

    // Gesture detection for swipe key triggers
    private lateinit var gestureDetector: GestureDetector
    private val swipeThreshold = 100
    private val swipeVelocityThreshold = 100
    private var isScrollLocked = true // Lock horizontal scrolling

    private var hide = true

    // Piano configuration
    private val totalOctaves = 7
    private val whiteKeysPerOctave = 7
    private val whiteKeyPattern = arrayOf("C", "D", "E", "F", "G", "A", "B")
    private val blackKeyPositions = arrayOf(1, 2, 4, 5, 6) // Positions where black keys appear (after C, D, F, G, A)
    private val blackKeyNames = arrayOf("C#", "D#", "F#", "G#", "A#")

    private lateinit var soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Enable edge-to-edge display
        enableEdgeToEdge()

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10) // Max number of simultaneous notes
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds into SoundPool and store the IDs
        soundMap["one"] = soundPool.load(this, R.raw.one, 1)
        soundMap["two"] = soundPool.load(this, R.raw.two, 1)
        soundMap["three"] = soundPool.load(this, R.raw.three, 1)
        soundMap["four"] = soundPool.load(this, R.raw.four, 1)
        soundMap["five"] = soundPool.load(this, R.raw.five, 1)
        soundMap["six"] = soundPool.load(this, R.raw.six, 1)
        soundMap["seven"] = soundPool.load(this, R.raw.seven, 1)
        soundMap["eight"] = soundPool.load(this, R.raw.eight, 1)
        soundMap["nine"] = soundPool.load(this, R.raw.nine, 1)
        soundMap["ten"] = soundPool.load(this, R.raw.ten, 1)
        soundMap["eleven"] = soundPool.load(this, R.raw.eleven, 1)
        soundMap["twelve"] = soundPool.load(this, R.raw.twelve, 1)
        soundMap["thirteen"] = soundPool.load(this, R.raw.thirteen, 1)
        soundMap["fourteen"] = soundPool.load(this, R.raw.fourteen, 1)

        setupPianoKeys()
        setupZoomControls()
        setupNavigationSlider()
        setupSwipeGestures()
        lockHorizontalScrolling()

        binding.layoutShowHide.setOnClickListener {
            if (hide){
                binding.navigationSlider.visibility = View.GONE
                binding.zoomControlLayout.visibility = View.GONE
                binding.navigationSliderLayout.visibility = View.GONE
                binding.layoutShowHide.setBackgroundResource(R.drawable.ic_arrow_down)
                hide = !hide
            }else{
                binding.navigationSlider.visibility = View.VISIBLE
                binding.zoomControlLayout.visibility = View.VISIBLE
                binding.navigationSliderLayout.visibility = View.VISIBLE
                binding.layoutShowHide.setBackgroundResource(R.drawable.ic_arrow_up)
                hide = !hide
            }
        }
        binding.pianoScrollView.setOnTouchListener({ v, event -> true })
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupPianoKeys() {
        createWhiteKeys()
        createBlackKeys()
        updateKeySize()
    }

    private fun createWhiteKeys() {
        binding.whiteKeysRow.removeAllViews()

        for (octave in 1..totalOctaves) {
            for (keyIndex in whiteKeyPattern.indices) {
                val keyName = "${whiteKeyPattern[keyIndex]}$octave"
                val whiteKey = createWhiteKey(keyName)
                binding.whiteKeysRow.addView(whiteKey)
            }
        }
    }

    private fun createBlackKeys() {
        binding.blackKeysRow.removeAllViews()

        for (octave in 1..totalOctaves) {
            // Black keys pattern: C# D# _ F# G# A# _
            // Positions: after C, D, skip E, after F, G, A, skip B
            val blackKeyPattern = arrayOf(
                "C#$octave", "D#$octave", null, "F#$octave", "G#$octave", "A#$octave", null
            )

            for (i in blackKeyPattern.indices) {
                if (blackKeyPattern[i] != null) {
                    val blackKey = createBlackKey(blackKeyPattern[i]!!)
                    binding.blackKeysRow.addView(blackKey)
                } else {
                    // Add spacer for E-F and B-C gaps
                    val spacer = createBlackKeySpacer()
                    binding.blackKeysRow.addView(spacer)
                }
            }
        }
    }

    private fun createWhiteKey(keyName: String): Button {
        val whiteKey = Button(this).apply {
            text = keyName
            textSize = 10f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

            // Create white key background programmatically
            background = createWhiteKeyDrawable()
            elevation = 2f

            // Add padding - text at bottom
            setPadding(4, 4, 4, 20)

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        playNote(keyName)
                        animateKeyPress(this, true)
                        true
                    }
                    else -> false
                }
            }
        }

        val layoutParams = LinearLayout.LayoutParams(
            (baseKeyWidth * currentZoom / 100).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            marginEnd = 1 // Minimal gap between keys
        }

        whiteKey.layoutParams = layoutParams
        return whiteKey
    }

    private fun createBlackKey(keyName: String): Button {
        val blackKey = Button(this).apply {
            text = keyName
            textSize = 8f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

            // Create black key background programmatically
            background = createBlackKeyDrawable()
            elevation = 8f // Much higher elevation than white keys

            // Add padding - text at bottom
            setPadding(2, 2, 2, 12)

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        playNote(keyName)
                        animateKeyPress(this, false)
                        true
                    }
                    else -> false
                }
            }
        }

        val whiteKeyWidth = (baseKeyWidth * currentZoom / 10/0).toInt()
        val blackKeyWidth = (whiteKeyWidth * 0.6f).toInt()
        val blackKeyHeight = (baseKeyHeight * 0.6f * currentZoom / 100).toInt()

        val layoutParams = LinearLayout.LayoutParams(
            blackKeyWidth,
            blackKeyHeight
        ).apply {
            // Position black key to be centered between white keys
            val offsetWidth = (whiteKeyWidth - blackKeyWidth) / 2
            marginStart = offsetWidth
            marginEnd = offsetWidth + 1
        }

        blackKey.layoutParams = layoutParams
        return blackKey
    }

    private fun createBlackKeySpacer(): View {
        val spacer = View(this)
        val whiteKeyWidth = (baseKeyWidth * currentZoom / 100).toInt()
        val layoutParams = LinearLayout.LayoutParams(
            whiteKeyWidth,
            0
        ).apply {
            marginEnd = 1
        }
        spacer.layoutParams = layoutParams
        return spacer
    }

    private fun createWhiteKeyDrawable(): Drawable {
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            colors = intArrayOf(
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#F5F5F5"),
                Color.parseColor("#EEEEEE")
            )
            gradientType = GradientDrawable.LINEAR_GRADIENT
            orientation = GradientDrawable.Orientation.TOP_BOTTOM

            // Add subtle border
            setStroke(1, Color.parseColor("#DDDDDD"))

            // Sharp corners like real piano
            cornerRadius = 0f
        }

        return shape
    }

    private fun createBlackKeyDrawable(): Drawable {
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            colors = intArrayOf(
                Color.parseColor("#333333"),
                Color.parseColor("#1A1A1A"),
                Color.parseColor("#000000")
            )
            gradientType = GradientDrawable.LINEAR_GRADIENT
            orientation = GradientDrawable.Orientation.TOP_BOTTOM

            // Add subtle border
            setStroke(1, Color.parseColor("#555555"))

            // Slightly rounded corners for black keys
            cornerRadius = 3f
        }

        return shape
    }

    private fun setupZoomControls() {
        binding.zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentZoom = minZoom + (progress / 100f) * (maxZoom - minZoom)
                    updateZoomDisplay()
                    updateKeySize()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.zoomInBtn.setOnClickListener {
            val newProgress = minOf(binding.zoomSlider.progress + 10, 100)
            binding.zoomSlider.progress = newProgress
        }

        binding.zoomOutBtn.setOnClickListener {
            val newProgress = maxOf(binding.zoomSlider.progress - 10, 0)
            binding.zoomSlider.progress = newProgress
        }

        updateZoomDisplay()
    }

    private fun setupNavigationSlider() {
        binding.navigationSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    scrollToPosition(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Update navigation slider when user scrolls manually
        binding.pianoScrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            updateNavigationSlider(scrollX)
        }
    }

    private fun setupSwipeGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                return if (abs(diffX) > abs(diffY)) {
                    // Horizontal swipe detected
                    if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                        if (diffX > 0) {
                            // Swipe right - trigger next key
                            onSwipeRight()
                        } else {
                            // Swipe left - trigger previous key
                            onSwipeLeft()
                        }
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        })

        // Set touch listener on the piano keys area
        binding.pianoKeysContainer.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            false // Allow other touch events to be processed
        }
    }

    private fun onSwipeLeft() {
        triggerKeyFromSwipe(false)
    }

    private fun onSwipeRight() {
        triggerKeyFromSwipe(false)
    }

    private fun triggerKeyFromSwipe(isNext: Boolean) {
        // Get the current visible area center
        val scrollX = binding.pianoScrollView.scrollX
        val viewWidth = binding.pianoScrollView.width
        val centerX = scrollX + viewWidth / 2

        // Find the key at the center position
        val keyWidth = (baseKeyWidth * currentZoom / 100).toInt()
        val keyIndex = (centerX / keyWidth).toInt()

        // Calculate which key to trigger
        val targetIndex = if (isNext) {
            minOf(keyIndex + 1, binding.whiteKeysRow.childCount - 1)
        } else {
            maxOf(keyIndex - 1, 0)
        }

        // Trigger the key
        if (targetIndex >= 0 && targetIndex < binding.whiteKeysRow.childCount) {
            val targetKey = binding.whiteKeysRow.getChildAt(targetIndex) as? Button
            targetKey?.let {
                playNote(it.text.toString())
                animateKeyPress(it, true)

                // Provide haptic feedback
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }

    private fun lockHorizontalScrolling() {
        // Disable touch scrolling on the HorizontalScrollView
        binding.pianoScrollView.setOnTouchListener { v, event ->
            // Let gesture detector handle the touch first
            val gestureHandled = gestureDetector.onTouchEvent(event)

            // Block all scrolling - only allow slider navigation
            if (isScrollLocked) {
                return@setOnTouchListener gestureHandled
            }

            false
        }
    }

    private fun scrollToPosition(progress: Int) {
        val maxScroll = getMaxScrollX()
        val scrollX = (progress / 100f * maxScroll).toInt()
        binding.pianoScrollView.smoothScrollTo(scrollX, 0)
    }

    private fun updateNavigationSlider(scrollX: Int) {
        val maxScroll = getMaxScrollX()
        if (maxScroll > 0) {
            val progress = (scrollX * 100f / maxScroll).toInt()
            binding.navigationSlider.progress = progress
        }
    }

    private fun getMaxScrollX(): Int {
        val pianoKeysContainer = binding.pianoScrollView.getChildAt(0)
        return maxOf(0, pianoKeysContainer?.width ?: 0 - binding.pianoScrollView.width)
    }

    private fun updateKeySize() {
        // Update white keys
        for (i in 0 until binding.whiteKeysRow.childCount) {
            val whiteKey = binding.whiteKeysRow.getChildAt(i)
            val layoutParams = whiteKey.layoutParams as LinearLayout.LayoutParams
            layoutParams.width = (baseKeyWidth * currentZoom / 100).toInt()
            whiteKey.layoutParams = layoutParams
        }

        // Update black keys and spacers
        for (i in 0 until binding.blackKeysRow.childCount) {
            val blackKeyView = binding.blackKeysRow.getChildAt(i)
            val layoutParams = blackKeyView.layoutParams as LinearLayout.LayoutParams
            val whiteKeyWidth = (baseKeyWidth * currentZoom / 100).toInt()

            if (blackKeyView is Button) {
                // It's a black key
                val blackKeyWidth = (whiteKeyWidth * 0.6f).toInt()
                val blackKeyHeight = (baseKeyHeight * 0.6f * currentZoom / 100).toInt()
                val offsetWidth = (whiteKeyWidth - blackKeyWidth) / 2

                layoutParams.width = blackKeyWidth
                layoutParams.height = blackKeyHeight
                layoutParams.marginStart = offsetWidth
                layoutParams.marginEnd = offsetWidth + 1
            } else {
                // It's a spacer
                layoutParams.width = whiteKeyWidth
                layoutParams.marginEnd = 1
            }

            blackKeyView.layoutParams = layoutParams
        }
    }

    private fun updateZoomDisplay() {
        binding.zoomPercentage.text = "${currentZoom.toInt()}%"
    }

    private fun playNote(keyName: String) {
        println("Playing note: $keyName")

        // Show visual feedback
        //playSoundEffect("one")
        when(keyName){
            "C1" -> playSoundEffect("one")
            "C#1" -> playSoundEffect("two")
            "D1" -> playSoundEffect("three")
            "D#1" -> playSoundEffect("four")
            "E1" -> playSoundEffect("five")
            "F1" -> playSoundEffect("six")
            "F#1" -> playSoundEffect("seven")
            "G1" -> playSoundEffect("eight")
            "G#1" -> playSoundEffect("nine")
            "A1" -> playSoundEffect("ten")
            "A#1" -> playSoundEffect("eleven")
            "B1" -> playSoundEffect("thirteen")
            "C2" -> playSoundEffect("fourteen")
        }

        showToast("Playing: $keyName")
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun animateKeyPress(key: Button, isWhiteKey: Boolean) {
        // Different animation for white and black keys
        val scaleAmount = if (isWhiteKey) 0.96f else 0.92f

        key.animate()
            .scaleX(scaleAmount)
            .scaleY(scaleAmount)
            .setDuration(80)
            .withEndAction {
                key.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .start()
            }
            .start()

        // Add a subtle color change during press
        if (isWhiteKey) {
            key.setBackgroundColor(Color.parseColor("#E8E8E8"))
            key.postDelayed({
                key.background = createWhiteKeyDrawable()
            }, 120)
        } else {
            key.setBackgroundColor(Color.parseColor("#444444"))
            key.postDelayed({
                key.background = createBlackKeyDrawable()
            }, 120)
        }
    }

    private fun playSoundEffect(name: String) {
        soundMap[name]?.let { soundId ->
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }
}