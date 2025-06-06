package com.hemant.pianobase


import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hemant.pianobase.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentZoom = 100f
    private val minZoom = 50f
    private val maxZoom = 150f
    private var baseKeyWidth = 120
    private var baseKeyHeight = 400

    // Piano configuration
    private val totalOctaves = 7
    private val whiteKeysPerOctave = 7
    private val whiteKeyPattern = arrayOf("C", "D", "E", "F", "G", "A", "B")
    private val blackKeyPositions = arrayOf(1, 2, 4, 5, 6) // Positions where black keys appear
    private val blackKeyNames = arrayOf("C#", "D#", "F#", "G#", "A#")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Enable edge-to-edge display
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pianoScrollView.setOnTouchListener { _, _ -> true }

        setupPianoKeys()
        setupZoomControls()
        setupNavigationSlider()
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
            for (i in 0 until whiteKeysPerOctave) {
                if (blackKeyPositions.contains(i + 1)) {
                    val blackKeyIndex = blackKeyPositions.indexOf(i + 1)
                    val keyName = "${blackKeyNames[blackKeyIndex]}$octave"
                    val blackKey = createBlackKey(keyName)
                    binding.blackKeysRow.addView(blackKey)
                } else {
                    // Add spacer for positions without black keys
                    val spacer = View(this)
                    val layoutParams = LinearLayout.LayoutParams(
                        (baseKeyWidth * currentZoom / 100).toInt(),
                        0
                    )
                    spacer.layoutParams = layoutParams
                    binding.blackKeysRow.addView(spacer)
                }
            }
        }
    }

    private fun createWhiteKey(keyName: String): Button {
        val whiteKey = Button(this).apply {
            text = keyName
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.white_key_text))
            background = ContextCompat.getDrawable(context, R.drawable.white_key_bg)
            elevation = 2f

            setOnClickListener {
                playNote(keyName)
                animateKeyPress(this, true)
            }
        }

        val layoutParams = LinearLayout.LayoutParams(
            (baseKeyWidth * currentZoom / 100).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            marginEnd = 2
        }

        whiteKey.layoutParams = layoutParams
        return whiteKey
    }

    private fun createBlackKey(keyName: String): Button {
        val blackKey = Button(this).apply {
            text = keyName
            textSize = 10f
            setTextColor(ContextCompat.getColor(context, R.color.black_key_text))
            background = ContextCompat.getDrawable(context, R.drawable.black_key_bg)
            elevation = 4f

            setOnClickListener {
                playNote(keyName)
                animateKeyPress(this, false)
            }
        }

        val layoutParams = LinearLayout.LayoutParams(
            (baseKeyWidth * 0.6f * currentZoom / 100).toInt(),
            (baseKeyHeight * 0.6f * currentZoom / 100).toInt()
        ).apply {
            marginEnd = ((baseKeyWidth * 0.4f * currentZoom / 100).toInt())
            marginStart = -((baseKeyWidth * 0.3f * currentZoom / 100).toInt())
        }

        blackKey.layoutParams = layoutParams
        return blackKey
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
                    val maxScroll = binding.pianoScrollView.getChildAt(0).width - binding.pianoScrollView.width
                    val scrollX = (progress / 100f * maxScroll).toInt()
                    binding.pianoScrollView.scrollTo(scrollX, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateKeySize() {
        // Update white keys
        for (i in 0 until binding.whiteKeysRow.childCount) {
            val whiteKey = binding.whiteKeysRow.getChildAt(i)
            val layoutParams = whiteKey.layoutParams as LinearLayout.LayoutParams
            layoutParams.width = (baseKeyWidth * currentZoom / 100).toInt()
            whiteKey.layoutParams = layoutParams
        }

        // Update black keys
        for (i in 0 until binding.blackKeysRow.childCount) {
            val blackKey = binding.blackKeysRow.getChildAt(i)
            val layoutParams = blackKey.layoutParams as LinearLayout.LayoutParams

            if (blackKey is Button) {
                // It's a black key
                layoutParams.width = (baseKeyWidth * 0.6f * currentZoom / 100).toInt()
                layoutParams.height = (baseKeyHeight * 0.6f * currentZoom / 100).toInt()
                layoutParams.marginEnd = ((baseKeyWidth * 0.4f * currentZoom / 100).toInt())
                layoutParams.marginStart = -((baseKeyWidth * 0.3f * currentZoom / 100).toInt())
            } else {
                // It's a spacer
                layoutParams.width = (baseKeyWidth * currentZoom / 100).toInt()
            }

            blackKey.layoutParams = layoutParams
        }
    }

    private fun updateZoomDisplay() {
        binding.zoomPercentage.text = "${currentZoom.toInt()}%"
    }

    private fun playNote(keyName: String) {
        // TODO: Implement actual sound playback
        // For now, just show feedback in logs
        println("Playing note: $keyName")
    }

    private fun animateKeyPress(key: Button, isWhiteKey: Boolean) {
        // Simple press animation
        key.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                key.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
}