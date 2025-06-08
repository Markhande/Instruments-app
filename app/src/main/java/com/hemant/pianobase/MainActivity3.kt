package com.hemant.pianobase

import com.hemant.pianobase.databinding.ActivityMain3Binding

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

class MainActivity3 : AppCompatActivity() {
    private lateinit var binding: ActivityMain3Binding
    private lateinit var soundPool: SoundPool
    private lateinit var metronomeHandler: Handler
    private lateinit var metronomeRunnable: Runnable

    // Piano configuration
    private var currentOctave = 4
    private var currentVolume = 0.8f
    private var isRecording = false
    private var isPlaying = false
    private var isMetronomeActive = false
    private var showLabels = true
    private var chordMode = false
    private var metronometempo = 120 // BPM

    // Sound and key mappings
    private val soundMap = mutableMapOf<String, Int>()
    private val keyViews = mutableMapOf<String, Button>()
    private val pressedKeys = mutableSetOf<String>()
    private val recordedNotes = mutableListOf<RecordedNote>()

    // Piano notes configuration
    private val whiteKeys = listOf("C", "D", "E", "F", "G", "A", "B")
    private val blackKeys = listOf("C#", "D#", "", "F#", "G#", "A#", "") // Empty strings for positions without black keys
    private val scaleTypes = listOf("Major", "Minor", "Pentatonic", "Blues", "Chromatic")

    // Key layout measurements
    private val whiteKeyWidth = 60
    private val blackKeyWidth = 40
    private val blackKeyHeight = 200
    private val keyboardHeight = 300

    data class RecordedNote(
        val note: String,
        val timestamp: Long,
        val duration: Long = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeSoundPool()
        setupPianoKeyboard()
        setupControls()
        setupMetronome()
        loadSounds()
    }

    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    private fun loadSounds() {
        // Load piano sounds from assets folder
        val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val octaves = listOf(3, 4, 5, 6)

        for (octave in octaves) {
            for (note in notes) {
                val noteKey = "$note$octave"
                try {
                    // Assuming sound files are named like "c4.wav", "c_sharp4.wav", etc.
                    val fileName = note.replace("#", "_sharp").lowercase() + octave
                    val assetFileDescriptor = assets.openFd("sounds/$fileName.wav")
                    val soundId = soundPool.load(assetFileDescriptor, 1)
                    soundMap[noteKey] = soundId
                } catch (e: Exception) {
                    // Handle missing sound files
                    println("Sound file not found: $note$octave")
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPianoKeyboard() {
        createPianoKeys()

        // Handle touch events for multi-touch support
        binding.pianoKeysContainer.setOnTouchListener { _, event ->
            handleMultiTouch(event)
            true
        }
    }

    private fun createPianoKeys() {
        binding.whiteKeysLayout.removeAllViews()
        binding.blackKeysLayout.removeAllViews()

        val totalOctaves = 2 // Show 2 octaves
        var blackKeyOffset = 0

        // Create white keys
        for (octave in currentOctave..(currentOctave + totalOctaves - 1)) {
            for (whiteKey in whiteKeys) {
                val noteKey = "$whiteKey$octave"
                val button = createWhiteKey(noteKey, whiteKey)
                binding.whiteKeysLayout.addView(button)
                keyViews[noteKey] = button
            }
        }

        // Create black keys with proper positioning
        for (octave in currentOctave..(currentOctave + totalOctaves - 1)) {
            for (i in blackKeys.indices) {
                if (blackKeys[i].isNotEmpty()) {
                    val blackKey = blackKeys[i]
                    val noteKey = "$blackKey$octave"
                    val button = createBlackKey(noteKey, blackKey, blackKeyOffset)
                    binding.blackKeysLayout.addView(button)
                    keyViews[noteKey] = button
                }
                blackKeyOffset += whiteKeyWidth + 2 // Account for white key width and margins
            }
            blackKeyOffset = (octave - currentOctave + 1) * 7 * (whiteKeyWidth + 2) // Reset for next octave
        }
    }

    private fun createWhiteKey(noteKey: String, displayNote: String): Button {
        val button = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(whiteKeyWidth),
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dpToPx(1), 0, dpToPx(1), 0)
            }

            setBackgroundResource(R.drawable.white_key_background)
            text = if (showLabels) displayNote else ""
            setTextColor(resources.getColor(R.color.text_on_white_key, null))
            textSize = 12f
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dpToPx(16))
            tag = noteKey
            elevation = dpToPx(2).toFloat()
        }

        setupKeyTouchListener(button, noteKey)
        return button
    }

    private fun createBlackKey(noteKey: String, displayNote: String, offset: Int): Button {
        val button = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(blackKeyWidth),
                dpToPx(blackKeyHeight)
            ).apply {
                leftMargin = dpToPx(offset + (whiteKeyWidth - blackKeyWidth) / 2)
            }

            setBackgroundResource(R.drawable.black_key_background)
            text = if (showLabels) displayNote else ""
            setTextColor(resources.getColor(R.color.text_on_black_key, null))
            textSize = 10f
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dpToPx(12))
            tag = noteKey
            elevation = dpToPx(4).toFloat()
        }

        setupKeyTouchListener(button, noteKey)
        return button
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupKeyTouchListener(button: Button, noteKey: String) {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressKey(noteKey)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    releaseKey(noteKey)
                    true
                }
                else -> false
            }
        }
    }

    private fun handleMultiTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val key = findKeyAtPosition(x, y)
                key?.let { pressKey(it) }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val key = findKeyAtPosition(x, y)
                key?.let { releaseKey(it) }
            }

            MotionEvent.ACTION_MOVE -> {
                // Handle finger movement across keys
                for (i in 0 until event.pointerCount) {
                    val x = event.getX(i)
                    val y = event.getY(i)
                    val key = findKeyAtPosition(x, y)
                    // Additional logic for smooth key transitions
                }
            }
        }
        return true
    }

    private fun findKeyAtPosition(x: Float, y: Float): String? {
        // Check black keys first (they're on top)
        for ((noteKey, button) in keyViews) {
            if (noteKey.contains("#")) {
                val location = IntArray(2)
                button.getLocationOnScreen(location)
                val buttonX = location[0].toFloat()
                val buttonY = location[1].toFloat()

                if (x >= buttonX && x <= buttonX + button.width &&
                    y >= buttonY && y <= buttonY + button.height) {
                    return noteKey
                }
            }
        }

        // Then check white keys
        for ((noteKey, button) in keyViews) {
            if (!noteKey.contains("#")) {
                val location = IntArray(2)
                button.getLocationOnScreen(location)
                val buttonX = location[0].toFloat()
                val buttonY = location[1].toFloat()

                if (x >= buttonX && x <= buttonX + button.width &&
                    y >= buttonY && y <= buttonY + button.height) {
                    return noteKey
                }
            }
        }

        return null
    }

    private fun pressKey(noteKey: String) {
        if (pressedKeys.contains(noteKey)) return

        pressedKeys.add(noteKey)
        keyViews[noteKey]?.isPressed = true

        // Play sound
        soundMap[noteKey]?.let { soundId ->
            soundPool.play(soundId, currentVolume, currentVolume, 1, 0, 1.0f)
        }

        // Update current note display
        updateCurrentNoteDisplay(noteKey)

        // Record note if recording
        if (isRecording) {
            recordedNotes.add(RecordedNote(noteKey, System.currentTimeMillis()))
        }

        // Visual feedback
        animateKeyPress(noteKey)
    }

    private fun releaseKey(noteKey: String) {
        if (!pressedKeys.contains(noteKey)) return

        pressedKeys.remove(noteKey)
        keyViews[noteKey]?.isPressed = false

        // Update recorded note duration
        if (isRecording && recordedNotes.isNotEmpty()) {
            val lastNote = recordedNotes.lastOrNull { it.note == noteKey }
            lastNote?.let {
                val updatedNote = it.copy(duration = System.currentTimeMillis() - it.timestamp)
                recordedNotes[recordedNotes.lastIndexOf(it)] = updatedNote
            }
        }

        // Clear current note display if no keys pressed
        if (pressedKeys.isEmpty()) {
            binding.tvCurrentNote.text = "--"
        }
    }

    private fun updateCurrentNoteDisplay(noteKey: String) {
        val displayText = if (chordMode && pressedKeys.size > 1) {
            pressedKeys.joinToString(" + ") { it.replace(currentOctave.toString(), "") }
        } else {
            noteKey.replace(currentOctave.toString(), "")
        }
        binding.tvCurrentNote.text = displayText
    }

    private fun animateKeyPress(noteKey: String) {
        keyViews[noteKey]?.let { button ->
            button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }

    private fun setupControls() {
        // Volume control
        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentVolume = progress / 100f
                binding.tvVolumeValue.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Octave controls
        binding.btnOctaveDown.setOnClickListener {
            if (currentOctave > 2) {
                currentOctave--
                binding.tvCurrentOctave.text = currentOctave.toString()
                createPianoKeys()
            }
        }

        binding.btnOctaveUp.setOnClickListener {
            if (currentOctave < 6) {
                currentOctave++
                binding.tvCurrentOctave.text = currentOctave.toString()
                createPianoKeys()
            }
        }

        // Labels toggle
        binding.switchShowLabels.setOnCheckedChangeListener { _, isChecked ->
            showLabels = isChecked
            updateKeyLabels()
        }

        // Chord mode toggle
        binding.switchChordMode.setOnCheckedChangeListener { _, isChecked ->
            chordMode = isChecked
        }

        // Recording controls
        binding.btnRecord.setOnClickListener {
            toggleRecording()
        }

        binding.btnPlay.setOnClickListener {
            playRecording()
        }

        binding.btnMetronome.setOnClickListener {
            toggleMetronome()
        }

        // Scale highlighting
        setupScaleSpinner()
        binding.btnHighlightScale.setOnClickListener {
            highlightScale()
        }
    }

    private fun updateKeyLabels() {
        keyViews.forEach { (noteKey, button) ->
            val displayNote = noteKey.replace(currentOctave.toString(), "")
            button.text = if (showLabels) displayNote else ""
        }
    }

    private fun toggleRecording() {
        isRecording = !isRecording
        if (isRecording) {
            recordedNotes.clear()
            binding.btnRecord.setColorFilter(resources.getColor(R.color.recording_active, null))
            showToast("Recording started")
        } else {
            binding.btnRecord.clearColorFilter()
            showToast("Recording stopped. ${recordedNotes.size} notes recorded")
        }
    }

    private fun playRecording() {
        if (recordedNotes.isEmpty()) {
            showToast("No recording to play")
            return
        }

        isPlaying = true
        binding.btnPlay.setColorFilter(resources.getColor(R.color.playing_active, null))

        val startTime = System.currentTimeMillis()
        val firstNoteTime = recordedNotes.firstOrNull()?.timestamp ?: 0

        recordedNotes.forEach { note ->
            val delay = note.timestamp - firstNoteTime
            Handler(Looper.getMainLooper()).postDelayed({
                soundMap[note.note]?.let { soundId ->
                    soundPool.play(soundId, currentVolume, currentVolume, 1, 0, 1.0f)
                }
                // Visual feedback
                keyViews[note.note]?.let { button ->
                    button.isPressed = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        button.isPressed = false
                    }, note.duration.coerceAtLeast(200))
                }
            }, delay)
        }

        // Reset play button after playback
        val totalDuration = recordedNotes.maxByOrNull { it.timestamp + it.duration }
            ?.let { it.timestamp + it.duration - firstNoteTime } ?: 0
        Handler(Looper.getMainLooper()).postDelayed({
            isPlaying = false
            binding.btnPlay.clearColorFilter()
        }, totalDuration + 1000)
    }

    private fun setupMetronome() {
        metronomeHandler = Handler(Looper.getMainLooper())
        metronomeRunnable = object : Runnable {
            override fun run() {
                if (isMetronomeActive) {
                    // Play metronome click sound
                    soundMap["metronome"]?.let { soundId ->
                        soundPool.play(soundId, 0.5f, 0.5f, 1, 0, 1.0f)
                    }

                    // Visual metronome feedback
                    binding.btnMetronome.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(100)
                        .withEndAction {
                            binding.btnMetronome.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start()
                        }
                        .start()

                   // val interval = 60000L / metronomeempo // Convert BPM to milliseconds
                    // metronomeHandler.postDelayed(this, interval)
                }
            }
        }
    }

    private fun toggleMetronome() {
        isMetronomeActive = !isMetronomeActive
        if (isMetronomeActive) {
            binding.btnMetronome.setColorFilter(resources.getColor(R.color.metronome_active, null))
            metronomeHandler.post(metronomeRunnable)
            showToast("Metronome started")
        } else {
            binding.btnMetronome.clearColorFilter()
            metronomeHandler.removeCallbacks(metronomeRunnable)
            showToast("Metronome stopped")
        }
    }

    private fun setupScaleSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, scaleTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerScale.adapter = adapter
    }

    private fun highlightScale() {
        val selectedScale = binding.spinnerScale.selectedItem.toString()
        val rootNote = "C" // You can make this configurable

        // Clear previous highlights
        keyViews.values.forEach { button ->
        //    button.clearColorFilter()
        }

        // Get scale notes
        val scaleNotes = getScaleNotes(rootNote, selectedScale)

        // Highlight scale notes
        keyViews.forEach { (noteKey, button) ->
            val note = noteKey.replace(Regex("\\d"), "")
            if (scaleNotes.contains(note)) {
              //  button.setColorFilter(resources.getColor(R.color.scale_highlight, null))
            }
        }

        showToast("$selectedScale scale highlighted")
    }

    private fun getScaleNotes(root: String, scaleType: String): List<String> {
        val chromatic = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val rootIndex = chromatic.indexOf(root)

        val intervals = when (scaleType) {
            "Major" -> listOf(0, 2, 4, 5, 7, 9, 11)
            "Minor" -> listOf(0, 2, 3, 5, 7, 8, 10)
            "Pentatonic" -> listOf(0, 2, 4, 7, 9)
            "Blues" -> listOf(0, 3, 5, 6, 7, 10)
            "Chromatic" -> (0..11).toList()
            else -> listOf(0, 2, 4, 5, 7, 9, 11) // Default to major
        }

        return intervals.map { interval ->
            chromatic[(rootIndex + interval) % 12]
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
        metronomeHandler.removeCallbacks(metronomeRunnable)
    }

    override fun onPause() {
        super.onPause()
        // Stop all sounds and clear pressed keys
        pressedKeys.clear()
        keyViews.values.forEach { it.isPressed = false }

        if (isMetronomeActive) {
            metronomeHandler.removeCallbacks(metronomeRunnable)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isMetronomeActive) {
            metronomeHandler.post(metronomeRunnable)
        }
    }
}