package net.bushydocs.writer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.Html
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// ---------------------------------------------------------------------------
// Documents
// ---------------------------------------------------------------------------
data class Doc(
    val id: String,
    val title: String,
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val encrypted: Boolean = false,
    val cipher: String = "",
    val salt: String = "",
    val iv: String = "",
    val font: String = "serif",
    val words: Int = 0,
) {
    val displayTitle: String get() = title.ifBlank { "Untitled Document" }
}

enum class FontChoice(val key: String, val label: String, val shortLabel: String) {
    SERIF("serif", "Serif (Source Serif)", "Serif"),
    SANS("sans", "Sans (Jakarta)", "Sans"),
    MONO("mono", "Mono (JetBrains)", "Mono");

    companion object {
        fun from(key: String?): FontChoice = entries.firstOrNull { it.key == key } ?: SERIF
    }
}

object Templates {
    private fun today(): String =
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())

    fun create(key: String): Pair<String, String> = when (key) {
        "meeting" -> "Meeting Notes" to """
            <h2>Agenda</h2>
            <ul><li>Topic 1</li><li>Topic 2</li></ul>
            <h2>Notes</h2>
            <p>Capture the important points here.</p>
            <h2>Action Items</h2>
            <ul><li>[ ] Task 1</li><li>[ ] Task 2</li></ul>
        """.trimIndent()

        "spec" -> "Technical Spec" to """
            <h2>Overview</h2>
            <p>Describe the problem and the outcome you want to create.</p>
            <h2>Architecture</h2>
            <p>Detail the design, the moving parts, and the trade-offs.</p>
            <h2>Requirements</h2>
            <ul><li>Functional requirement</li><li>Non-functional requirement</li></ul>
            <h2>Decisions</h2>
            <ul><li>[ ] Decision to make</li></ul>
        """.trimIndent()

        "journal" -> "Daily Journal" to """
            <h2>Reflections</h2>
            <p>Today was a productive day...</p>
            <h2>Priorities</h2>
            <ul><li>Top goal</li><li>Second priority</li></ul>
            <p><b>Date:</b> ${today()}</p>
        """.trimIndent()

        else -> "Untitled Document" to ""
    }
}

// ---------------------------------------------------------------------------
// AES-256-GCM encryption with PBKDF2 key derivation. Keys live only in memory.
// ---------------------------------------------------------------------------
object Crypto {
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    fun encrypt(key: SecretKeySpec, plainText: String): Pair<String, String> {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP) to
            Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    fun decrypt(key: SecretKeySpec, cipherText: String, ivText: String): String {
        val iv = Base64.decode(ivText, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(
            cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP)),
            Charsets.UTF_8,
        )
    }
}

// ---------------------------------------------------------------------------
// Local storage. Everything stays inside the app's private data directory.
// ---------------------------------------------------------------------------
class DocumentStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("bushy_writer_v01", Context.MODE_PRIVATE)

    val imagesDir: File
        get() = File(context.filesDir, "images").also { if (!it.exists()) it.mkdirs() }

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark", false)
        set(value) = prefs.edit().putBoolean("dark", value).apply()

    var layoutMode: String
        get() = prefs.getString("layout", "grid") ?: "grid"
        set(value) = prefs.edit().putString("layout", value).apply()

    var sortMode: String
        get() = prefs.getString("sort", "recent") ?: "recent"
        set(value) = prefs.edit().putString("sort", value).apply()

    fun loadDocs(): List<Doc> {
        val raw = prefs.getString("docs", null)
        val docs = mutableListOf<Doc>()
        if (raw != null) {
            runCatching {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    docs += Doc(
                        id = o.optString("id"),
                        title = o.optString("title", "Untitled Document"),
                        content = if (o.optBoolean("encrypted")) "" else o.optString("content", ""),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                        createdAt = o.optLong("createdAt", o.optLong("updatedAt", System.currentTimeMillis())),
                        encrypted = o.optBoolean("encrypted", false),
                        cipher = o.optString("cipher", ""),
                        salt = o.optString("salt", ""),
                        iv = o.optString("iv", ""),
                        font = o.optString("font", "serif"),
                        words = o.optInt("words", 0),
                    )
                }
            }
        }

        // A friendly first-run workspace, matching the starter draft on the site.
        if (raw == null && !prefs.getBoolean("initialized", false)) {
            prefs.edit().putBoolean("initialized", true).apply()
            val starter = Doc(
                id = "doc_${System.currentTimeMillis()}",
                title = "Untitled Document",
                content = "",
            )
            docs += starter
            saveDocs(listOf(starter), emptyMap())
        }
        return docs
    }

    /**
     * Writes all documents. Encrypted documents are re-encrypted only when their
     * in-memory key is available (i.e. the doc is unlocked); locked documents keep
     * their existing ciphertext untouched.
     */
    fun saveDocs(docs: List<Doc>, keys: Map<String, SecretKeySpec>): List<Doc> {
        val array = JSONArray()
        val updated = ArrayList<Doc>(docs.size)
        docs.forEach { doc ->
            var cipher = doc.cipher
            var salt = doc.salt
            var iv = doc.iv
            if (doc.encrypted) {
                val key = keys[doc.id]
                if (key != null) {
                    // The key was derived from this salt when the doc was unlocked.
                    val encrypted = Crypto.encrypt(key, doc.content)
                    cipher = encrypted.first
                    iv = encrypted.second
                }
                updated += doc.copy(cipher = cipher, salt = salt, iv = iv)
            } else {
                updated += doc.copy(cipher = "", salt = "", iv = "")
            }

            val o = JSONObject().apply {
                put("id", doc.id)
                put("title", doc.displayTitle)
                put("updatedAt", doc.updatedAt)
                put("createdAt", doc.createdAt)
                put("encrypted", doc.encrypted)
                put("font", doc.font)
                put("words", doc.words)
                if (doc.encrypted) {
                    put("content", "")
                    put("cipher", cipher)
                    put("salt", salt)
                    put("iv", iv)
                } else {
                    put("content", doc.content)
                }
            }
            array.put(o)
        }
        prefs.edit().putString("docs", array.toString()).apply()
        return updated
    }

    fun storageBytes(): Long {
        val json = prefs.getString("docs", "")?.toByteArray(Charsets.UTF_8)?.size?.toLong() ?: 0L
        val imageBytes = imagesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return json + imageBytes
    }

    fun storageLabel(): String {
        val bytes = storageBytes()
        return if (bytes < 1024) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        }
    }

    /** Copies a picked image into app-private storage, scaled for a phone screen. */
    fun importImage(uri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / sample > 1800) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val scaled = scaleDown(bitmap, 1400)
            val file = File(imagesDir, "img_${UUID.randomUUID()}.jpg")
            file.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            if (scaled !== bitmap) bitmap.recycle()
            scaled.recycle()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Saves bytes returned by a download. */
    fun saveImageBytes(bytes: ByteArray): String? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val scaled = scaleDown(bitmap, 1400)
            val file = File(imagesDir, "img_${UUID.randomUUID()}.jpg")
            file.outputStream().use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 88, out) }
            if (scaled !== bitmap) bitmap.recycle()
            scaled.recycle()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun downscaleForDisplay(path: String, maxWidthPx: Int): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (opts.outWidth / (sample * 2) >= maxWidthPx) sample *= 2
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(path, decodeOpts)
    }.getOrNull()

    private fun scaleDown(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        return Bitmap.createScaledBitmap(
            bitmap,
            maxWidth,
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }
}

// ---------------------------------------------------------------------------
// Small text helpers shared by the dashboard and editor.
// ---------------------------------------------------------------------------
object TextTools {
    fun plain(html: String): String {
        if (html.isBlank()) return ""
        return runCatching {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
                .replace('\uFFFC', ' ')
                .replace(Regex("[ \\t]+"), " ")
                .replace(Regex("\\n{3,}"), "\n\n")
                .trim()
        }.getOrDefault(html)
    }

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.split(Regex("\\s+")).count { token ->
            token.any { it.isLetterOrDigit() }
        }
    }

    fun countWordsFromHtml(html: String): Int = countWords(plain(html))

    fun preview(html: String, max: Int = 140): String {
        val text = plain(html)
        if (text.isBlank()) return "Empty document..."
        return if (text.length <= max) text else text.take(max).trimEnd() + "…"
    }

    fun dateLabel(ms: Long): String =
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))

    fun timeLabel(ms: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

    fun readingMinutes(words: Int): Int = if (words == 0) 0 else (words + 199) / 200
}
