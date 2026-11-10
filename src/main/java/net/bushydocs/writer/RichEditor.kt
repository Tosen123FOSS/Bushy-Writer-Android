package net.bushydocs.writer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.BulletSpan
import android.text.style.CharacterStyle
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import java.io.File
import kotlin.math.max
import kotlin.math.min

class RichEditText(context: Context) : EditText(context) {
    var selectionListener: ((Int, Int) -> Unit)? = null
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        selectionListener?.invoke(selStart, selEnd)
    }
}

/**
 * Bridges Compose toolbar actions to a native, span-based rich text editor.
 * Content is persisted as HTML, which keeps bold/italic/underline/headings,
 * lists and images intact across app restarts.
 */
class EditorController(private val context: Context) {

    var editText: RichEditText? = null
        private set
    private var onChanged: ((String) -> Unit)? = null
    private var lastHtml: String = ""
    private var selStart = 0
    private var selEnd = 0
    private var suppress = false
    var accentColor: Int = 0xFF059669.toInt()
    var maxImageWidthPx: Int = 900

    fun attach(edit: RichEditText, callback: (String) -> Unit) {
        onChanged = callback
        if (editText === edit) return
        editText = edit
        edit.selectionListener = { start, end ->
            if (!suppress) {
                selStart = start
                selEnd = end
            }
        }
        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (suppress || s == null) return
                val html = spansToHtml(s)
                lastHtml = html
                onChanged?.invoke(html)
            }
        })
    }

    fun currentHtml(): String = lastHtml

    fun setHtml(html: String) {
        val edit = editText ?: return
        suppress = true
        val spanned = htmlToSpanned(html)
        edit.setText(spanned, android.widget.TextView.BufferType.SPANNABLE)
        edit.setSelection(0)
        lastHtml = html
        selStart = 0
        selEnd = 0
        suppress = false
    }

    fun updateAppearance(
        textColor: Color,
        hintColor: Color,
        font: FontChoice,
        accent: Color,
    ) {
        val edit = editText ?: return
        accentColor = accent.toArgb()
        edit.setTextColor(textColor.toArgb())
        edit.setHintTextColor(hintColor.toArgb())
        edit.setLinkTextColor(accent.toArgb())
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        edit.setLineSpacing(0f, 1.38f)
        edit.typeface = typefaceFor(font)
        edit.setPadding(0, 0, 0, 0)
        edit.gravity = Gravity.TOP or Gravity.START
        edit.background = null
        edit.isVerticalScrollBarEnabled = false
        edit.overScrollMode = View.OVER_SCROLL_ALWAYS
        edit.minLines = 12
        edit.inputType = android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        edit.imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
        maxImageWidthPx = (context.resources.displayMetrics.widthPixels * 0.92f).toInt()
            .coerceAtLeast(480)
    }

    private fun typefaceFor(font: FontChoice): Typeface {
        val id = when (font) {
            FontChoice.SERIF -> R.font.source_serif_regular
            FontChoice.SANS -> R.font.plus_jakarta_regular
            FontChoice.MONO -> R.font.jetbrains_mono_regular
        }
        return ResourcesCompat.getFont(context, id) ?: Typeface.SERIF
    }

    private fun range(edit: RichEditText): Pair<Int, Int> {
        val textLength = edit.text.length
        val start = if (selStart in 0..textLength) selStart else edit.selectionStart.coerceAtLeast(0)
        val end = if (selEnd in 0..textLength) selEnd else edit.selectionEnd.coerceAtLeast(0)
        return min(start, end) to max(start, end)
    }

    private fun emit() {
        val edit = editText ?: return
        val html = spansToHtml(edit.text)
        lastHtml = html
        onChanged?.invoke(html)
    }

    private fun reselect(edit: RichEditText, start: Int, end: Int = start) {
        val textLength = edit.text.length
        val safeStart = start.coerceIn(0, textLength)
        val safeEnd = end.coerceIn(0, textLength)
        edit.setSelection(min(safeStart, safeEnd), max(safeStart, safeEnd))
        selStart = safeStart
        selEnd = safeEnd
        edit.requestFocus()
    }

    private fun toggleStyle(
        edit: RichEditText,
        spanClass: Class<*>,
        matches: (Any) -> Boolean,
        factory: () -> Any,
    ) {
        val (start, end) = range(edit)
        val text = edit.text
        val existing = text.getSpans(start, end, spanClass).filter(matches)
        val covers = existing.any {
            text.getSpanStart(it) <= start && text.getSpanEnd(it) >= end
        }
        if (covers && end > start) {
            existing.forEach { text.removeSpan(it) }
        } else {
            val span = factory()
            val flags = if (start == end) {
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            } else {
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            }
            text.setSpan(span, start, end, flags)
        }
        reselect(edit, start, end)
        emit()
    }

    fun toggleBold() {
        val edit = editText ?: return
        toggleStyle(edit, StyleSpan::class.java, { span ->
            span is StyleSpan && span.style and Typeface.BOLD != 0
        }) { StyleSpan(Typeface.BOLD) }
    }

    fun toggleItalic() {
        val edit = editText ?: return
        toggleStyle(edit, StyleSpan::class.java, { span ->
            span is StyleSpan && span.style and Typeface.ITALIC != 0
        }) { StyleSpan(Typeface.ITALIC) }
    }

    fun toggleUnderline() {
        val edit = editText ?: return
        toggleStyle(edit, UnderlineSpan::class.java, { true }) { UnderlineSpan() }
    }

    fun toggleStrike() {
        val edit = editText ?: return
        toggleStyle(edit, StrikethroughSpan::class.java, { true }) { StrikethroughSpan() }
    }

    private fun paragraphBounds(edit: RichEditText, start: Int, end: Int): Pair<Int, Int> {
        val text = edit.text
        val lineStart = if (start <= 0) 0 else text.lastIndexOf('\n', start - 1) + 1
        val lineEnd = text.indexOf('\n', end).let { if (it == -1) text.length else it }
        return lineStart to max(lineStart, lineEnd)
    }

    fun toggleHeading(level: Int) {
        val edit = editText ?: return
        val (start, end) = range(edit)
        val (lineStart, lineEnd) = paragraphBounds(edit, start, end)
        val text = edit.text
        val sizes = text.getSpans(lineStart, lineEnd, RelativeSizeSpan::class.java)
        val already = sizes.any { it.sizeChange > 1.15f }
        text.getSpans(lineStart, lineEnd, RelativeSizeSpan::class.java).forEach { text.removeSpan(it) }
        text.getSpans(lineStart, lineEnd, AbsoluteSizeSpan::class.java).forEach { text.removeSpan(it) }
        if (!already) {
            val size = if (level <= 1) 1.7f else 1.35f
            text.setSpan(
                RelativeSizeSpan(size),
                lineStart,
                lineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            text.setSpan(
                StyleSpan(Typeface.BOLD),
                lineStart,
                lineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        reselect(edit, lineStart, lineEnd)
        emit()
    }

    fun paragraphStyle() {
        val edit = editText ?: return
        val (start, end) = range(edit)
        val (lineStart, lineEnd) = paragraphBounds(edit, start, end)
        val text = edit.text
        text.getSpans(lineStart, lineEnd, RelativeSizeSpan::class.java).forEach { text.removeSpan(it) }
        text.getSpans(lineStart, lineEnd, AbsoluteSizeSpan::class.java).forEach { text.removeSpan(it) }
        text.getSpans(lineStart, lineEnd, StyleSpan::class.java)
            .filter { it.style and Typeface.BOLD != 0 }
            .forEach { text.removeSpan(it) }
        reselect(edit, lineStart, lineEnd)
        emit()
    }

    fun toggleBullet() {
        val edit = editText ?: return
        val (start, end) = range(edit)
        val (lineStart, lineEnd) = paragraphBounds(edit, start, end)
        if (lineStart >= lineEnd) return
        val text = edit.text
        val existing = text.getSpans(lineStart, lineEnd, BulletSpan::class.java)
        if (existing.isNotEmpty()) {
            existing.forEach { text.removeSpan(it) }
        } else {
            text.getSpans(lineStart, lineEnd, QuoteSpan::class.java).forEach { text.removeSpan(it) }
            text.setSpan(
                BulletSpan(dp(18), accentColor),
                lineStart,
                lineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        reselect(edit, lineStart, lineEnd)
        emit()
    }

    fun toggleNumbered() {
        val edit = editText ?: return
        val (start, end) = range(edit)
        val (lineStart, lineEnd) = paragraphBounds(edit, start, end)
        if (lineStart >= lineEnd) return
        val text = edit.text
        val lines = mutableListOf<Int>()
        var cursor = lineStart
        while (cursor <= lineEnd) {
            lines += cursor
            val next = text.indexOf('\n', cursor)
            if (next == -1 || next >= lineEnd) break
            cursor = next + 1
        }
        val allNumbered = lines.all { pos ->
            text.substring(pos, min(text.length, pos + 4)).matches(Regex("^\\d+\\.\\s.*"))
        }
        if (allNumbered) {
            lines.asReversed().forEach { pos ->
                val match = Regex("^\\d+\\.\\s").find(text.substring(pos)) ?: return@forEach
                text.delete(pos, pos + match.value.length)
            }
        } else {
            lines.asReversed().forEachIndexed { index, pos ->
                val prefix = "${lines.size - index}. "
                if (!text.substring(pos).matches(Regex("^\\d+\\.\\s.*"))) {
                    text.insert(pos, prefix)
                }
            }
            val newStart = text.lastIndexOf('\n', start - 1) + 1
            reselect(edit, newStart.coerceAtLeast(lineStart))
            emit()
            return
        }
        reselect(edit, lineStart)
        emit()
    }

    fun toggleQuote() {
        val edit = editText ?: return
        val (start, end) = range(edit)
        val (lineStart, lineEnd) = paragraphBounds(edit, start, end)
        if (lineStart >= lineEnd) return
        val text = edit.text
        val existing = text.getSpans(lineStart, lineEnd, QuoteSpan::class.java)
        if (existing.isNotEmpty()) {
            existing.forEach { text.removeSpan(it) }
        } else {
            text.getSpans(lineStart, lineEnd, BulletSpan::class.java).forEach { text.removeSpan(it) }
            text.setSpan(
                QuoteSpan(accentColor),
                lineStart,
                lineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        reselect(edit, lineStart, lineEnd)
        emit()
    }

    fun clearFormatting() {
        val edit = editText ?: return
        val (start, end) = range(edit)
        val text = edit.text
        text.getSpans(start, end, CharacterStyle::class.java)
            .filter { it !is ImageSpan }
            .forEach { text.removeSpan(it) }
        text.getSpans(start, end, BulletSpan::class.java).forEach { text.removeSpan(it) }
        text.getSpans(start, end, QuoteSpan::class.java).forEach { text.removeSpan(it) }
        text.getSpans(start, end, LeadingMarginSpan::class.java).forEach { text.removeSpan(it) }
        reselect(edit, start, end)
        emit()
    }

    fun insertImage(path: String) {
        val edit = editText ?: return
        val bitmap = decodeBounded(path) ?: return
        val maxWidth = maxImageWidthPx
        val width = min(bitmap.width, maxWidth)
        val height = (bitmap.height * (width.toFloat() / bitmap.width)).toInt().coerceAtLeast(1)
        val scaled = if (width != bitmap.width) {
            Bitmap.createScaledBitmap(bitmap, width, height, true).also { bitmap.recycle() }
        } else {
            bitmap
        }
        val drawable = BitmapDrawable(context.resources, scaled).apply {
            setBounds(0, 0, scaled.width, scaled.height)
        }
        val span = ImageSpan(drawable, path, ImageSpan.ALIGN_BASELINE)
        val (start, end) = range(edit)
        val text = edit.text
        if (end > start) text.delete(start, end)
        text.insert(start, "\uFFFC")
        text.setSpan(span, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        reselect(edit, start + 1)
        emit()
    }

    private fun decodeBounded(path: String): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (opts.outWidth / (sample * 2) >= maxImageWidthPx) sample *= 2
        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(path, decode)
    }

    private fun htmlToSpanned(html: String): Spanned {
        if (html.isBlank()) return html.toSpanned()
        val getter = Html.ImageGetter { source ->
            runCatching {
                if (source.isBlank()) return@runCatching null
                val file = File(source.removePrefix("file://"))
                if (!file.exists()) return@runCatching null
                val bitmap = decodeBounded(file.absolutePath) ?: return@runCatching null
                val width = min(bitmap.width, maxImageWidthPx)
                val height = (bitmap.height * (width.toFloat() / bitmap.width)).toInt().coerceAtLeast(1)
                val scaled = if (width != bitmap.width) {
                    Bitmap.createScaledBitmap(bitmap, width, height, true).also { bitmap.recycle() }
                } else {
                    bitmap
                }
                BitmapDrawable(context.resources, scaled).apply {
                    setBounds(0, 0, scaled.width, scaled.height)
                }
            }.getOrNull()
        }
        return runCatching {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, getter, null)
        }.getOrDefault(html.toSpanned()).let { normalizeHeadingSizes(it) }
    }

    /** Maps Android's stock heading sizes to the sizes used by the app's toolbar. */
    private fun normalizeHeadingSizes(spanned: Spanned): Spanned {
        val spannable = spanned as? Spannable ?: return spanned
        spannable.getSpans(0, spannable.length, RelativeSizeSpan::class.java).forEach { span ->
            val replacement = when {
                span.sizeChange >= 1.45f -> 1.7f
                span.sizeChange > 1.15f -> 1.35f
                else -> return@forEach
            }
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            val flags = spannable.getSpanFlags(span)
            spannable.removeSpan(span)
            spannable.setSpan(RelativeSizeSpan(replacement), start, end, flags)
        }
        return spannable
    }

    private fun String.toSpanned(): Spanned {
        val text = android.text.SpannableStringBuilder(this)
        return text
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}

@Composable
fun RichTextEditor(
    html: String,
    font: FontChoice,
    dark: Boolean,
    controller: EditorController,
    onHtmlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textColor = if (dark) Zinc200 else Zinc800
    val hintColor = if (dark) Zinc500 else Zinc400
    val accent = if (dark) Brand400 else Brand600
    val placeholder = "Start typing your ideas here, or paste / drop an image..."

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            RichEditText(ctx).apply {
                hint = placeholder
                setHintTextColor(hintColor.toArgb())
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setPadding(0, 0, 0, 0)
                controller.attach(this, onHtmlChange)
                controller.updateAppearance(textColor, hintColor, font, accent)
                controller.setHtml(html)
            }
        },
        update = { edit ->
            controller.attach(edit, onHtmlChange)
            controller.updateAppearance(textColor, hintColor, font, accent)
            if (html != controller.currentHtml()) {
                controller.setHtml(html)
            }
        },
    )
}

private data class HtmlBlock(val start: Int, val end: Int, val type: String)

/**
 * Serializes the editor's spans into HTML that keeps the structures the toolbar
 * creates (headings, bullet lists, quotes and images) instead of flattening them
 * like [Html.toHtml] does.
 */
private fun spansToHtml(text: Spanned): String {
    if (text.isEmpty()) return ""

    val blocks = ArrayList<HtmlBlock>()
    var lineStart = 0
    while (true) {
        var lineEnd = text.indexOf('\n', lineStart)
        if (lineEnd < 0) lineEnd = text.length
        if (lineEnd > lineStart) {
            val quote = text.getSpans(lineStart, lineEnd, QuoteSpan::class.java).isNotEmpty()
            val bullet = text.getSpans(lineStart, lineEnd, BulletSpan::class.java).isNotEmpty()
            val headingSize = text.getSpans(lineStart, lineEnd, RelativeSizeSpan::class.java)
                .maxOfOrNull { it.sizeChange } ?: 1f
            val type = when {
                quote -> "quote"
                bullet -> "li"
                headingSize > 1.55f -> "h1"
                headingSize > 1.15f -> "h2"
                else -> "p"
            }
            blocks += HtmlBlock(lineStart, lineEnd, type)
        }
        if (lineEnd >= text.length) break
        lineStart = lineEnd + 1
    }

    val out = StringBuilder()
    var i = 0
    while (i < blocks.size) {
        when (blocks[i].type) {
            "li" -> {
                out.append("<ul>")
                while (i < blocks.size && blocks[i].type == "li") {
                    out.append("<li>")
                    out.append(inlineHtml(text, blocks[i].start, blocks[i].end))
                    out.append("</li>")
                    i++
                }
                out.append("</ul>\n")
            }
            "quote" -> {
                out.append("<blockquote>")
                out.append(inlineHtml(text, blocks[i].start, blocks[i].end))
                out.append("</blockquote>\n")
                i++
            }
            "h1" -> {
                out.append("<h1>")
                out.append(inlineHtml(text, blocks[i].start, blocks[i].end))
                out.append("</h1>\n")
                i++
            }
            "h2" -> {
                out.append("<h2>")
                out.append(inlineHtml(text, blocks[i].start, blocks[i].end))
                out.append("</h2>\n")
                i++
            }
            else -> {
                out.append("<p>")
                out.append(inlineHtml(text, blocks[i].start, blocks[i].end))
                out.append("</p>\n")
                i++
            }
        }
    }
    return out.toString()
}

private fun inlineHtml(text: Spanned, start: Int, end: Int): String {
    if (start >= end) return ""
    val styleSpans = text.getSpans(start, end, StyleSpan::class.java)
    val underlineSpans = text.getSpans(start, end, UnderlineSpan::class.java)
    val strikeSpans = text.getSpans(start, end, StrikethroughSpan::class.java)
    val imageSpans = text.getSpans(start, end, ImageSpan::class.java)

    val points = sortedSetOf(start, end)
    listOf(styleSpans, underlineSpans, strikeSpans, imageSpans).forEach { spans ->
        spans.forEach { span ->
            points += text.getSpanStart(span).coerceIn(start, end)
            points += text.getSpanEnd(span).coerceIn(start, end)
        }
    }

    val ordered = points.toList()
    val out = StringBuilder()
    for (k in 0 until ordered.size - 1) {
        val a = ordered[k]
        val b = ordered[k + 1]
        if (a >= b) continue

        val image = imageSpans.firstOrNull {
            text.getSpanStart(it) <= a && text.getSpanEnd(it) >= b
        }
        if (image != null) {
            out.append("<img src=\"").append(escapeHtml(image.source.orEmpty())).append("\">")
            continue
        }

        val bold = styleSpans.any {
            text.getSpanStart(it) <= a && text.getSpanEnd(it) >= b &&
                it.style and Typeface.BOLD != 0
        }
        val italic = styleSpans.any {
            text.getSpanStart(it) <= a && text.getSpanEnd(it) >= b &&
                it.style and Typeface.ITALIC != 0
        }
        val underline = underlineSpans.any {
            text.getSpanStart(it) <= a && text.getSpanEnd(it) >= b
        }
        val strike = strikeSpans.any {
            text.getSpanStart(it) <= a && text.getSpanEnd(it) >= b
        }
        val raw = text.subSequence(a, b).toString().replace("\uFFFC", "")
        if (raw.isEmpty()) continue

        if (bold) out.append("<b>")
        if (italic) out.append("<i>")
        if (underline) out.append("<u>")
        if (strike) out.append("<strike>")
        out.append(escapeHtml(raw))
        if (strike) out.append("</strike>")
        if (underline) out.append("</u>")
        if (italic) out.append("</i>")
        if (bold) out.append("</b>")
    }
    return out.toString()
}

private fun escapeHtml(value: String): String {
    val out = StringBuilder(value.length)
    value.forEach { c ->
        when (c) {
            '&' -> out.append("&amp;")
            '<' -> out.append("&lt;")
            '>' -> out.append("&gt;")
            '"' -> out.append("&quot;")
            else -> out.append(c)
        }
    }
    return out.toString()
}
