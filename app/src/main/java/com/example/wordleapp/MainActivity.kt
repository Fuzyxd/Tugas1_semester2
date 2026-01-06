package com.example.wordleapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var wordleGame: WordleGame
    private lateinit var gameGridContainer: LinearLayout
    private lateinit var wordInput: EditText
    private lateinit var submitButton: Button
    private lateinit var deleteButton: Button
    private lateinit var newGameButton: Button
    private lateinit var hintButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var winCountTextView: TextView
    private lateinit var streakTextView: TextView
    private lateinit var bestScoreTextView: TextView
    private lateinit var videoContainer: LinearLayout
    private lateinit var videoView: VideoView

    private lateinit var cellViews: Array<Array<TextView>>

    // Easter Egg trigger
    private val EASTER_EGG_WORD = "REHAN"
    private var easterEggTriggered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModel with correct ViewModelProvider
        wordleGame = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[WordleGame::class.java]

        initializeViews()
        setupGameGrid()
        setupListeners()
        setupEasterEgg()
        updateStatistics()
    }

    private fun initializeViews() {
        gameGridContainer = findViewById(R.id.gameGridContainer)
        wordInput = findViewById(R.id.wordInput)
        submitButton = findViewById(R.id.submitButton)
        deleteButton = findViewById(R.id.deleteButton)
        newGameButton = findViewById(R.id.newGameButton)
        hintButton = findViewById(R.id.hintButton)
        statusTextView = findViewById(R.id.statusTextView)
        winCountTextView = findViewById(R.id.winCountTextView)
        streakTextView = findViewById(R.id.streakTextView)
        bestScoreTextView = findViewById(R.id.bestScoreTextView)
        videoContainer = findViewById(R.id.videoContainer)
        videoView = findViewById(R.id.videoView)
    }

    private fun setupEasterEgg() {
        // Sembunyikan video container awalnya
        videoContainer.visibility = View.GONE

        // Setup video completion listener
        videoView.setOnCompletionListener {
            // Setelah video selesai, kembali ke game
            videoContainer.visibility = View.GONE
            videoContainer.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            easterEggTriggered = false

            // Tampilkan semua elemen UI kembali
            showAllUIElements(true)

            // Reset game state
            resetGrid()
            wordInput.text.clear()
            statusTextView.text = "Masukkan kata 5 huruf"
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }

    private fun setupGameGrid() {
        // Inisialisasi array cellViews
        cellViews = Array(6) { Array(5) { TextView(this) } }

        // Clear existing views
        gameGridContainer.removeAllViews()

        // Create 6 rows x 5 columns grid dengan ukuran yang lebih sesuai
        for (row in 0 until 6) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = 6.dpToPx()
                }
                gravity = Gravity.CENTER_HORIZONTAL
            }

            for (col in 0 until 5) {
                val cell = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(56.dpToPx(), 56.dpToPx()).apply {
                        if (col < 4) marginEnd = 4.dpToPx()
                    }
                    gravity = android.view.Gravity.CENTER
                    textSize = 28f
                    setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                    setBackgroundResource(R.drawable.cell_empty)
                    isAllCaps = true
                    // Perbaikan: gunakan setTypeface untuk bold
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }

                cellViews[row][col] = cell
                rowLayout.addView(cell)
            }

            gameGridContainer.addView(rowLayout)
        }
    }

    private fun setupListeners() {
        submitButton.setOnClickListener {
            val guess = wordInput.text.toString().trim().uppercase()

            // Cek Easter Egg
            if (guess == EASTER_EGG_WORD && !easterEggTriggered) {
                triggerEasterEgg()
                return@setOnClickListener
            }

            when (val result = wordleGame.submitGuess(guess)) {
                is WordleGame.GuessResult.CORRECT -> {
                    updateGridWithFeedback(guess, wordleGame.getLastFeedback())
                    statusTextView.text = "🎉 Selamat! Kamu menang! 🎉"
                    statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                    updateStatistics()
                }
                is WordleGame.GuessResult.VALID -> {
                    updateGridWithFeedback(guess, result.feedback)
                    wordInput.text.clear()
                    statusTextView.text = "Lanjutkan..."
                    statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                }
                WordleGame.GuessResult.INVALID_LENGTH -> {
                    Toast.makeText(this, "Kata harus 5 huruf!", Toast.LENGTH_SHORT).show()
                }
                WordleGame.GuessResult.OUT_OF_ATTEMPTS -> {
                    updateGridWithFeedback(guess, wordleGame.getLastFeedback())
                    statusTextView.text = "❌ Game Over! Kata yang benar: ${wordleGame.getTargetWord()}"
                    statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                    updateStatistics()
                }
                WordleGame.GuessResult.GAME_OVER -> {
                    Toast.makeText(this, "Game sudah selesai!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        deleteButton.setOnClickListener {
            val currentText = wordInput.text.toString()
            if (currentText.isNotEmpty()) {
                wordInput.setText(currentText.substring(0, currentText.length - 1))
            }
        }

        newGameButton.setOnClickListener {
            wordleGame.startNewGame()
            resetGrid()
            wordInput.text.clear()
            statusTextView.text = "Masukkan kata 5 huruf"
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            updateStatistics()
        }

        hintButton.setOnClickListener {
            if (!wordleGame.isGameOver()) {
                val hint = wordleGame.getHint()
                Toast.makeText(this, "💡 Petunjuk: Huruf pertama adalah $hint", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Game sudah selesai!", Toast.LENGTH_SHORT).show()
            }
        }

        // Auto-uppercase and limit to 5 characters
        wordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                wordInput.removeTextChangedListener(this)
                val text = s.toString().uppercase().take(5)
                wordInput.setText(text)
                wordInput.setSelection(text.length)
                wordInput.addTextChangedListener(this)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun triggerEasterEgg() {
        easterEggTriggered = true

        // Sembunyikan semua elemen UI
        showAllUIElements(false)

        // Tampilkan video container dengan background hitam
        videoContainer.visibility = View.VISIBLE
        videoContainer.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))

        try {
            // Load dan play video jumpscare
            val videoUri = "android.resource://${packageName}/${R.raw.jumpscare}"
            videoView.setVideoPath(videoUri)
            videoView.start()
        } catch (e: Exception) {
            e.printStackTrace()
            // Jika video tidak ditemukan, tampilkan efek alternatif
            Toast.makeText(this, "🎃 BOO! Easter Egg Activated! 🎃", Toast.LENGTH_LONG).show()

            // Delay lalu kembali normal
            videoContainer.postDelayed({
                videoContainer.visibility = View.GONE
                showAllUIElements(true)
                easterEggTriggered = false
            }, 2000)
        }
    }

    private fun showAllUIElements(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE

        gameGridContainer.visibility = visibility
        wordInput.visibility = visibility
        submitButton.visibility = visibility
        deleteButton.visibility = visibility
        newGameButton.visibility = visibility
        hintButton.visibility = visibility
        statusTextView.visibility = visibility
        findViewById<LinearLayout>(R.id.statsContainer).visibility = visibility
        findViewById<TextView>(R.id.titleTextView).visibility = visibility
    }

    private fun updateGridWithFeedback(guess: String, feedback: List<WordleGame.LetterFeedback>) {
        val currentAttempt = wordleGame.getCurrentAttempt() - 1

        if (currentAttempt < 0 || currentAttempt >= 6) return

        for (i in guess.indices) {
            if (i < 5) {
                val cell = cellViews[currentAttempt][i]
                cell.text = guess[i].toString()

                when (feedback[i]) {
                    WordleGame.LetterFeedback.CORRECT -> {
                        cell.setBackgroundResource(R.drawable.cell_correct)
                    }
                    WordleGame.LetterFeedback.PRESENT -> {
                        cell.setBackgroundResource(R.drawable.cell_present)
                    }
                    WordleGame.LetterFeedback.ABSENT -> {
                        cell.setBackgroundResource(R.drawable.cell_absent)
                    }
                }
            }
        }
    }

    private fun resetGrid() {
        for (row in 0 until 6) {
            for (col in 0 until 5) {
                val cell = cellViews[row][col]
                cell.text = ""
                cell.setBackgroundResource(R.drawable.cell_empty)
            }
        }
    }

    private fun updateStatistics() {
        val stats = wordleGame.getStatistics()
        winCountTextView.text = "Menang: ${stats.winCount}"
        streakTextView.text = "Streak: ${stats.currentStreak}"
        bestScoreTextView.text = "Best: ${stats.bestScore}"
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
}