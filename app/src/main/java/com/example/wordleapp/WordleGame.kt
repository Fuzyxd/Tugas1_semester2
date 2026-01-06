package com.example.wordleapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random

class WordleGame(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val WORD_LENGTH = 5
        private const val MAX_ATTEMPTS = 6
        private const val PREFS_NAME = "WordlePrefs"
        private const val KEY_WIN_COUNT = "winCount"
        private const val KEY_STREAK = "streak"
        private const val KEY_BEST_SCORE = "bestScore"
        private const val KEY_USED_WORDS = "usedWords"
    }

    // Game state
    private var targetWord = ""
    private var currentAttempt = 0
    private var gameWon = false
    private var gameOver = false
    private var lastFeedback = listOf<LetterFeedback>()

    // Statistics
    private var winCount = 0
    private var currentStreak = 0
    private var bestScore = 0
    private val usedWords = mutableSetOf<String>()

    private val sharedPreferences: SharedPreferences
    private val gson = Gson()

    // Indonesian 5-letter words (filtered) - Hanya untuk target kata
    private val wordList = listOf(
        "ABADI", "AKUAT", "ALAMI", "AMPUH", "ANGIN", "ANJAK", "ASING", "AKUAT",
        "BALIK", "BANGSA", "BANTU", "BARAT", "BARIS", "BASUH", "BATAS",
        "BAWAH", "BAYAR", "BELAJAR", "BENAR", "BENDA", "BENTUK", "BERANI", "BERITA",
        "BESAR", "BETUL", "BIDANG", "BIKIN", "BINTANG", "BISMI", "BISA", "BUKAN",
        "BUKU", "BULAN", "BUMI", "BURUK", "CABANG", "CANTIK", "CARI", "CATAT",
        "CEPAT", "CERITA", "CINTA", "CITA", "CUKUP", "DAERAH", "DAGANG", "DAHSYAT",
        "DAHULU", "DAKWAH", "DALAM", "DANAU", "DASAR", "DATANG", "DAUN", "DEKAT",
        "DENGAN", "DESA", "DIAM", "DINGIN", "DIRI", "DOA", "DUA", "DUNIA",
        "EKOR", "FIKIR", "GAGAL", "GAMBAR", "GANTI", "GARAM", "GELAP", "GEMA",
        "GERAK", "GIGI", "GILA", "GOLONG", "GUNUNG", "HABIS", "HADIR", "HAJAT",
        "HALUS", "HAMPIR", "HANGAT", "HANYA", "HARAP", "HARI", "HATI", "HAWA",
        "HERAN", "HIDUP", "HILANG", "HITAM", "HORMAT", "HUJAN", "IBU", "IKAN",
        "ILMU", "IMAN", "INGAT", "INJIL", "ISI", "ISTRI", "JADI", "JAGA",
        "JAHAT", "JAJAN", "JALAN", "JAMAN", "JANGAN", "JANTUNG", "JARAK",
        "JARUM", "JASA", "JATUH", "JAUH", "JEJAK", "JELAS", "JEMPUT", "JENIS",
        "JERUK", "JIHAD", "JUGA", "JUMAT", "JUMLAH", "JUTA", "KABAR", "KACANG",
        "KADANG", "KAIN", "KAJIAN", "KAKAK", "KALAU", "KALIMAT", "KAMAR", "KAMI",
        "KANAN", "KANTOR", "KAPAL", "KAPAN", "KARENA", "KATA", "KAYA", "KEBAIKAN",
        "KEBUN", "KECIL", "KEDUA", "KEGIATAN", "KEHIDUPAN", "KEJADIAN", "KEKASIH",
        "KELAS", "KELUAR", "KEMBALI", "KEMUDIAN", "KENAL", "KEPALA", "KERETA",
        "KERJA", "KERUSAKAN", "KETIKA", "KETURUNAN", "KHUSUS", "KIRI", "KISAH",
        "KITA", "KOMPUTER", "KOTA", "KUAT", "KUCING", "KUNCI", "LAGI", "LAHIR",
        "LAIN", "LAJU", "LAKI", "LAMA", "LANGIT", "LAPANG", "LAPOR", "LARI",
        "LATAR", "LATIH", "LAUT", "LEBAR", "LEBIH", "LELAKI", "LEMAH", "LENGKAP",
        "LETAK", "LIHAT", "LIMPAH", "LINTAS", "LUAR", "LUAS", "LUKA", "LULUS",
        "MAAF", "MABUK", "MADU", "MAHKOTA", "MAKAN", "MAKNA", "MALAM", "MALU",
        "MAMPU", "MANDI", "MANFAAT", "MANUSIA", "MASALAH", "MASIH", "MASUK",
        "MATA", "MATI", "MAU", "MEDIA", "MELIHAT", "MEMANG", "MEMBACA", "MEMBERI",
        "MEMILIKI", "MEMUKUL", "MENAATI", "MENANG", "MENANTI", "MENCARI", "MENDAPAT",
        "MENGAJAR", "MENJADI", "MENONTON", "MENTAL", "MENTARI", "MENUNGGU",
        "MENURUT", "MERAH", "MEREKA", "MESIN", "MINUM", "MISAL", "MODERN",
        "MOHON", "MOTOR", "MUDA", "MUDAH", "MUJIZAT", "MULAI", "MULIA", "MURAH",
        "MURID", "MUSIM", "NAIK", "NAMA", "NANTI", "NASIB", "NEGARA", "NIKAH",
        "NILAI", "NOMOR", "NYATA", "NYAWA", "OBAT", "ORANG", "PADA", "PAHALA",
        "PAHAM", "PAKAI", "PAKSA", "PALING", "PANAS", "PANDAI", "PANGGIL",
        "PANJANG", "PANTAS", "PARA", "PARKIR", "PARTI", "PASAR", "PASIR",
        "PATUH", "PASTI", "PATAH", "PAYUNG", "PEDANG", "PEGANG", "PEKERJAAN",
        "PELAJAR", "PELAN", "PELANGI", "PELUK", "PEMBACA", "PENA", "PENDEK",
        "PENUH", "PERANG", "PERCAYA", "PERGI", "PERHATIAN", "PERLU", "PERMATA",
        "PERMISI", "PERNAH", "PERPUSTAKAAN", "PESAN", "PESAWAT", "PETANI",
        "PIKIR", "PINDAH", "PINTAR", "PINTU", "PISAU", "POHON", "POKOK",
        "POLISI", "PULANG", "PUNYA", "PUTIH", "PUTRA", "RAHASIA", "RAJA",
        "RAKIT", "RAMAI", "RAPI", "RASA", "RATUS", "RAWA", "RAYA", "RELA",
        "RENDAH", "RENUNG", "RIBU", "RINDU", "RODA", "ROHANI", "RUMAH",
        "RUPIAH", "RUSA", "SAAT", "SABAR", "SABDA", "SABTU", "SAHABAT",
        "SAJA", "SAKIT", "SALAH", "SALAM", "SAMA", "SAMPAI", "SANGAT",
        "SANTRI", "SAPU", "SARAPAN", "SATU", "SAUDARA", "SAYA", "SAYANG",
        "SEBAB", "SEBELAH", "SEBENAR", "SEDANG", "SEDAP", "SEDIH", "SEGALA",
        "SEGERA", "SEHAT", "SEJAHTERA", "SEJARAH", "SEKARANG", "SEKOLAH",
        "SELALU", "SELAMAT", "SELANJUT", "SELASA", "SELESAI", "SELURUH",
        "SEMANGAT", "SEMBAH", "SEMESTA", "SEMUA", "SENANG", "SENDIRI",
        "SENI", "SENJATA", "SENTUH", "SEPASANG", "SEPEDA", "SERATUS",
        "SERIBU", "SERU", "SESUAI", "SESUATU", "SETIAP", "SIANG", "SIBUK",
        "SIDANG", "SIHAT", "SIKAP", "SILA", "SIMPAN", "SINAR", "SINGGAT",
        "SIAPA", "SISA", "SISTEM", "SITU", "SUARA", "SUDAH", "SUKAR",
        "SUKA", "SUKSES", "SULIT", "SUMPAH", "SUNGAI", "SURAT", "SURGA",
        "SUSAH", "SUSU", "TABUNG", "TADI", "TAHUN", "TAHU", "TAKUT",
        "TAMAN", "TAMPIL", "TANAH", "TANDA", "TANGAN", "TANGGAL",
        "TANGGUNG", "TANPA", "TAPI", "TARUH", "TAS", "TAWAR", "TEKAN",
        "TEKNIK", "TEKS", "TELAH", "TELAPAK", "TELUR", "TEMAN", "TEMBAK",
        "TEMA", "TEMPAT", "TENANG", "TENDA", "TENGAH", "TENTANG", "TENTARA",
        "TENTU", "TENUN", "TERANG", "TERIMA", "TERLALU", "TERUS", "TETAP",
        "TETAPI", "TIANG", "TIDAK", "TIDUR", "TIAP", "TIKET", "TIMBUL",
        "TIMUR", "TINGGI", "TINGKAT", "TIPU", "TITIK", "TITIP", "TOLONG",
        "TOPI", "TUAN", "TUJUH", "TUJUAN", "TULANG", "TULIS", "TUMBUH",
        "TURUN", "TUTUP", "UANG", "UJIAN", "UKUR", "ULANG", "UMAT",
        "UMUM", "UNDANG", "UNTUK", "UPAYA", "URUS", "USAHA", "UTAMA",
        "UTARA", "WAKTU", "WALAU", "WANITA", "WARGA", "WARIS", "WARNA",
        "WATAK", "WAYANG", "YAKIN", "YANG", "ZAMAN"
    ).filter { it.length == 5 }.distinct()

    init {
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadStatistics()
        startNewGame()
    }

    fun startNewGame() {
        // Get random word
        val availableWords = wordList.filter { !usedWords.contains(it) }

        targetWord = if (availableWords.isEmpty()) {
            usedWords.clear()
            wordList[Random.nextInt(wordList.size)]
        } else {
            availableWords[Random.nextInt(availableWords.size)]
        }

        usedWords.add(targetWord)
        if (usedWords.size > 50) {
            val recentWords = usedWords.toList().takeLast(50).toMutableSet()
            usedWords.clear()
            usedWords.addAll(recentWords)
        }

        currentAttempt = 0
        gameWon = false
        gameOver = false
        lastFeedback = emptyList()

        saveUsedWords()
    }

    fun submitGuess(guess: String): GuessResult {
        if (guess.length != WORD_LENGTH) {
            return GuessResult.INVALID_LENGTH
        }

        // HAPUS validasi kata tidak valid - terima semua kata 5 huruf
        // if (!isValidWord(guess)) {
        //     return GuessResult.INVALID_WORD
        // }

        if (gameOver) {
            return GuessResult.GAME_OVER
        }

        lastFeedback = checkGuess(guess)
        currentAttempt++

        if (guess.uppercase() == targetWord) {
            gameWon = true
            gameOver = true
            updateStatistics(true)
            return GuessResult.CORRECT
        }

        if (currentAttempt >= MAX_ATTEMPTS) {
            gameOver = true
            updateStatistics(false)
            return GuessResult.OUT_OF_ATTEMPTS
        }

        return GuessResult.VALID(lastFeedback)
    }

    private fun checkGuess(guess: String): List<LetterFeedback> {
        val feedback = mutableListOf<LetterFeedback>()
        val targetChars = targetWord.toCharArray()
        val guessChars = guess.uppercase().toCharArray()

        // First pass: mark correct letters
        val marked = BooleanArray(WORD_LENGTH) { false }

        for (i in guessChars.indices) {
            if (guessChars[i] == targetChars[i]) {
                feedback.add(LetterFeedback.CORRECT)
                marked[i] = true
            } else {
                feedback.add(LetterFeedback.ABSENT)
            }
        }

        // Second pass: mark present letters
        for (i in guessChars.indices) {
            if (feedback[i] == LetterFeedback.ABSENT) {
                for (j in targetChars.indices) {
                    if (!marked[j] && guessChars[i] == targetChars[j]) {
                        feedback[i] = LetterFeedback.PRESENT
                        marked[j] = true
                        break
                    }
                }
            }
        }

        return feedback
    }

    // Fungsi ini sekarang tidak digunakan, tapi biarkan untuk referensi
    fun isValidWord(word: String): Boolean {
        // Selalu return true karena kita menerima semua kata 5 huruf
        return word.length == WORD_LENGTH
    }

    fun getHint(): String {
        return targetWord.substring(0, 1)
    }

    fun getTargetWord(): String {
        return targetWord
    }

    fun getCurrentAttempt(): Int {
        return currentAttempt
    }

    fun isGameWon(): Boolean {
        return gameWon
    }

    fun isGameOver(): Boolean {
        return gameOver
    }

    fun getLastFeedback(): List<LetterFeedback> {
        return lastFeedback
    }

    private fun updateStatistics(won: Boolean) {
        if (won) {
            winCount++
            currentStreak++
            val attemptsUsed = currentAttempt
            if (attemptsUsed < bestScore || bestScore == 0) {
                bestScore = attemptsUsed
            }
        } else {
            currentStreak = 0
        }

        saveStatistics()
    }

    private fun loadStatistics() {
        winCount = sharedPreferences.getInt(KEY_WIN_COUNT, 0)
        currentStreak = sharedPreferences.getInt(KEY_STREAK, 0)
        bestScore = sharedPreferences.getInt(KEY_BEST_SCORE, 0)

        val usedWordsJson = sharedPreferences.getString(KEY_USED_WORDS, null)
        if (usedWordsJson != null) {
            val type = object : TypeToken<MutableSet<String>>() {}.type
            usedWords.clear()
            usedWords.addAll(gson.fromJson(usedWordsJson, type))
        }
    }

    private fun saveStatistics() {
        sharedPreferences.edit().apply {
            putInt(KEY_WIN_COUNT, winCount)
            putInt(KEY_STREAK, currentStreak)
            putInt(KEY_BEST_SCORE, bestScore)
            apply()
        }
    }

    private fun saveUsedWords() {
        val usedWordsJson = gson.toJson(usedWords)
        sharedPreferences.edit().putString(KEY_USED_WORDS, usedWordsJson).apply()
    }

    fun getStatistics(): Statistics {
        return Statistics(winCount, currentStreak, bestScore)
    }

    data class Statistics(
        val winCount: Int,
        val currentStreak: Int,
        val bestScore: Int
    )

    sealed class GuessResult {
        object CORRECT : GuessResult()
        data class VALID(val feedback: List<LetterFeedback>) : GuessResult()
        object INVALID_LENGTH : GuessResult()
        // HAPUS object INVALID_WORD : GuessResult()
        object OUT_OF_ATTEMPTS : GuessResult()
        object GAME_OVER : GuessResult()
    }

    enum class LetterFeedback {
        CORRECT,    // Hijau
        PRESENT,    // Kuning
        ABSENT      // Abu-abu
    }
}