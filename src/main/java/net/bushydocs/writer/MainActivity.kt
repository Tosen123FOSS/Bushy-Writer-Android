package net.bushydocs.writer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BushyApp()
        }
    }
}

@Composable
private fun BushyApp() {
    val context = LocalContext.current
    val store = remember { DocumentStore(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var dark by remember { mutableStateOf(store.darkTheme) }
    var docs by remember { mutableStateOf(store.loadDocs()) }
    var layoutMode by remember { mutableStateOf(store.layoutMode) }
    var sortMode by remember { mutableStateOf(store.sortMode) }
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }
    var savedLabel by remember { mutableStateOf("Saved") }

    val keys = remember { mutableStateMapOf<String, SecretKeySpec>() }
    val controller = remember { EditorController(context) }

    var encryptTargetId by remember { mutableStateOf<String?>(null) }
    var unlockTargetId by remember { mutableStateOf<String?>(null) }
    var unlockError by remember { mutableStateOf<String?>(null) }
    var unlockBusy by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }
    var imageDialogOpen by remember { mutableStateOf(false) }
    var pickedImagePath by remember { mutableStateOf<String?>(null) }
    var pickedImageName by remember { mutableStateOf<String?>(null) }
    var downloadingImage by remember { mutableStateOf(false) }

    val activeDoc = docs.firstOrNull { it.id == activeId }
    val canEdit = activeDoc != null && (!activeDoc.encrypted || keys.containsKey(activeDoc.id))
    val editingDoc = activeDoc?.takeIf { canEdit }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    /** Flushes state to private storage; returns the updated document list. */
    fun persist(): List<Doc> {
        val updated = store.saveDocs(docs, keys)
        docs = updated
        savedLabel = "Saved ${TextTools.timeLabel(System.currentTimeMillis())}"
        return updated
    }

    fun openDoc(doc: Doc) {
        if (doc.encrypted && !keys.containsKey(doc.id)) {
            unlockTargetId = doc.id
            unlockError = null
            unlockBusy = false
        } else {
            savedLabel = "Saved ${TextTools.timeLabel(doc.updatedAt)}"
            activeId = doc.id
        }
    }

    fun createDoc(title: String, content: String) {
        val newDoc = Doc(
            id = "doc_${UUID.randomUUID()}",
            title = title.ifBlank { "Untitled Document" },
            content = content,
            words = TextTools.countWordsFromHtml(content),
        )
        docs = docs + newDoc
        persist()
        savedLabel = "Saved ${TextTools.timeLabel(System.currentTimeMillis())}"
        activeId = newDoc.id
    }

    fun saveTitle(id: String, title: String) {
        docs = docs.map {
            if (it.id == id) it.copy(title = title, updatedAt = System.currentTimeMillis()) else it
        }
    }

    fun saveContent(id: String, html: String) {
        docs = docs.map {
            if (it.id == id) {
                it.copy(
                    content = html,
                    words = TextTools.countWordsFromHtml(html),
                    updatedAt = System.currentTimeMillis(),
                )
            } else {
                it
            }
        }
    }

    fun encryptDoc(id: String, password: String) {
        val doc = docs.firstOrNull { it.id == id } ?: return
        val salt = Crypto.newSalt()
        val key = Crypto.deriveKey(password, salt)
        val encrypted = Crypto.encrypt(key, doc.content)
        val words = TextTools.countWordsFromHtml(doc.content)
        docs = docs.map {
            if (it.id == id) {
                it.copy(
                    content = "",
                    encrypted = true,
                    cipher = encrypted.first,
                    salt = Base64.encodeToString(salt, Base64.NO_WRAP),
                    iv = encrypted.second,
                    words = words,
                    updatedAt = System.currentTimeMillis(),
                )
            } else {
                it
            }
        }
        keys.remove(id)
        persist()
        encryptTargetId = null
        activeId = null
        message("Document encrypted and locked.")
    }

    fun lockDoc(id: String) {
        val key = keys[id]
        val now = System.currentTimeMillis()
        docs = docs.map {
            if (it.id != id) {
                it
            } else if (key != null) {
                // Flush any edits made since the last autosave before dropping the key.
                val encrypted = Crypto.encrypt(key, it.content)
                it.copy(
                    content = "",
                    cipher = encrypted.first,
                    iv = encrypted.second,
                    updatedAt = now,
                )
            } else {
                it.copy(content = "", updatedAt = now)
            }
        }
        keys.remove(id)
        persist()
        activeId = null
        message("Document locked.")
    }

    fun unlockDoc(id: String, password: String) {
        val doc = docs.firstOrNull { it.id == id } ?: return
        unlockBusy = true
        unlockError = null
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val salt = Base64.decode(doc.salt, Base64.NO_WRAP)
                    val key = Crypto.deriveKey(password, salt)
                    val plain = Crypto.decrypt(key, doc.cipher, doc.iv)
                    key to plain
                }
            }
            result.onSuccess { (key, plain) ->
                keys[id] = key
                docs = docs.map {
                    if (it.id == id) {
                        it.copy(
                            content = plain,
                            words = TextTools.countWordsFromHtml(plain),
                        )
                    } else {
                        it
                    }
                }
                savedLabel = "Saved ${TextTools.timeLabel(doc.updatedAt)}"
                unlockBusy = false
                unlockTargetId = null
                activeId = id
            }.onFailure {
                unlockBusy = false
                unlockError = "Incorrect passphrase. Try again."
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = displayNameFor(context, uri) ?: "Selected image"
            scope.launch {
                val path = withContext(Dispatchers.IO) { store.importImage(uri) }
                if (path != null) {
                    pickedImagePath = path
                    pickedImageName = name
                } else {
                    message("Could not read that image.")
                }
            }
        }
    }

    val exportMd = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        if (uri != null) {
            val doc = docs.firstOrNull { it.id == activeId }
            if (doc != null) {
                val markdown = buildMarkdown(doc)
                writeExport(context, uri, markdown) {
                    message("Markdown exported.")
                }
            }
        }
    }

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val fresh = persist()
            val doc = fresh.firstOrNull { it.id == activeId }
            if (doc != null) {
                writeExport(context, uri, docToJson(doc)) {
                    message("Backup exported.")
                }
            }
        }
    }

    val view = LocalView.current
    val activity = context.findActivity()
    if (!view.isInEditMode && activity != null) {
        androidx.compose.runtime.SideEffect {
            val controllerCompat = WindowCompat.getInsetsController(activity.window, view)
            controllerCompat.isAppearanceLightStatusBars = !dark
            controllerCompat.isAppearanceLightNavigationBars = !dark
        }
    }

    LaunchedEffect(activeId, docs) {
        val id = activeId ?: return@LaunchedEffect
        val doc = docs.firstOrNull { it.id == id }
        if (doc == null) {
            activeId = null
        } else if (doc.encrypted && !keys.containsKey(id)) {
            unlockTargetId = id
            unlockError = null
            unlockBusy = false
        }
    }

    LaunchedEffect(dark) {
        store.darkTheme = dark
    }

    LaunchedEffect(activeId, editingDoc?.title, editingDoc?.content, editingDoc?.font) {
        val doc = editingDoc ?: return@LaunchedEffect
        if (doc.encrypted && !keys.containsKey(doc.id)) return@LaunchedEffect
        delay(550)
        val updated = store.saveDocs(docs, keys)
        docs = updated
        savedLabel = "Saved ${TextTools.timeLabel(System.currentTimeMillis())}"
    }

    val latestDocs = rememberUpdatedState(docs)
    DisposableEffect(Unit) {
        onDispose {
            store.saveDocs(latestDocs.value, keys)
        }
    }

    BackHandler(enabled = drawerState.isOpen || activeId != null) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            persist()
            activeId = null
        }
    }

    BushyTheme(dark = dark) {
        val bg = if (dark) AppBgDark else AppBgLight
        val surface = if (dark) SurfaceDark else SurfaceLight
        Surface(modifier = Modifier.fillMaxSize(), color = bg) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding(),
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 900.dp

                    @Composable
                    fun AppContent() {
                        if (editingDoc != null) {
                            EditorScreen(
                                dark = dark,
                                doc = editingDoc,
                                controller = controller,
                                wordCount = editingDoc.words,
                                charCount = remember(editingDoc.content) {
                                    TextTools.plain(editingDoc.content).length
                                },
                                savedLabel = savedLabel,
                                onBack = {
                                    persist()
                                    activeId = null
                                },
                                onTitleChange = { title ->
                                    saveTitle(editingDoc.id, title)
                                },
                                onContentChange = { html ->
                                    saveContent(editingDoc.id, html)
                                },
                                onFontChange = { choice ->
                                    docs = docs.map {
                                        if (it.id == editingDoc.id) {
                                            it.copy(
                                                font = choice.key,
                                                updatedAt = System.currentTimeMillis(),
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                },
                                onExportMd = {
                                    exportMd.launch(exportName(editingDoc.displayTitle, "md"))
                                },
                                onExportJson = {
                                    exportJson.launch(exportName(editingDoc.displayTitle, "json"))
                                },
                                onEncrypt = {
                                    if (editingDoc.encrypted) {
                                        lockDoc(editingDoc.id)
                                    } else {
                                        encryptTargetId = editingDoc.id
                                    }
                                },
                                onInsertImage = {
                                    pickedImagePath = null
                                    pickedImageName = null
                                    imageDialogOpen = true
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Dashboard(
                                dark = dark,
                                docs = docs,
                                layoutMode = layoutMode,
                                sortMode = sortMode,
                                onLayoutChange = {
                                    layoutMode = it
                                    store.layoutMode = it
                                },
                                onSortChange = {
                                    sortMode = it
                                    store.sortMode = it
                                },
                                onNewDoc = {
                                    activeId = null
                                    createDoc("Untitled Document", "")
                                },
                                onOpenDoc = { doc -> openDoc(doc) },
                                onDeleteDoc = { doc -> deleteTargetId = doc.id },
                                onThemeToggle = { dark = !dark },
                                onMenu = if (wide) {
                                    null
                                } else {
                                    { scope.launch { drawerState.open() } }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    if (wide) {
                        Row(Modifier.fillMaxSize()) {
                            Sidebar(
                                dark = dark,
                                docs = docs,
                                activeId = editingDoc?.id,
                                storageLabel = store.storageLabel(),
                                onHome = {
                                    persist()
                                    activeId = null
                                },
                                onNewDoc = {
                                    activeId = null
                                    createDoc("Untitled Document", "")
                                },
                                onOpenDoc = { openDoc(it) },
                                onDeleteDoc = { deleteTargetId = it.id },
                                onTemplate = { key ->
                                    val template = Templates.create(key)
                                    createDoc(template.first, template.second)
                                },
                                onThemeToggle = { dark = !dark },
                                modifier = Modifier.fillMaxHeight(),
                            )
                            Box(
                                Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(if (dark) BorderDark else BorderLight),
                            )
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                AppContent()
                            }
                        }
                    } else {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet(
                                    modifier = Modifier.width(256.dp),
                                    drawerShape = RoundedCornerShape(0.dp),
                                    drawerContainerColor = surface,
                                    windowInsets = WindowInsets(0, 0, 0, 0),
                                ) {
                                    Sidebar(
                                        dark = dark,
                                        docs = docs,
                                        activeId = editingDoc?.id,
                                        storageLabel = store.storageLabel(),
                                        onHome = {
                                            persist()
                                            activeId = null
                                            scope.launch { drawerState.close() }
                                        },
                                        onNewDoc = {
                                            activeId = null
                                            createDoc("Untitled Document", "")
                                            scope.launch { drawerState.close() }
                                        },
                                        onOpenDoc = {
                                            openDoc(it)
                                            scope.launch { drawerState.close() }
                                        },
                                        onDeleteDoc = { deleteTargetId = it.id },
                                        onTemplate = { key ->
                                            val template = Templates.create(key)
                                            createDoc(template.first, template.second)
                                            scope.launch { drawerState.close() }
                                        },
                                        onThemeToggle = { dark = !dark },
                                        modifier = Modifier.fillMaxHeight(),
                                    )
                                }
                            },
                        ) {
                            AppContent()
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                ) { data ->
                    Snackbar(
                        containerColor = if (dark) Zinc800 else Zinc900,
                        contentColor = Zinc100,
                        shape = RoundedCornerShape(9.dp),
                    ) {
                        Text(data.visuals.message, color = Zinc100)
                    }
                }
            }

            encryptTargetId?.let { id ->
                val doc = docs.firstOrNull { it.id == id }
                if (doc != null) {
                    EncryptDialog(
                        dark = dark,
                        docTitle = doc.displayTitle,
                        onDismiss = { encryptTargetId = null },
                        onApply = { password -> encryptDoc(id, password) },
                    )
                }
            }

            unlockTargetId?.let { id ->
                val doc = docs.firstOrNull { it.id == id }
                if (doc != null) {
                    UnlockDialog(
                        dark = dark,
                        docTitle = doc.displayTitle,
                        busy = unlockBusy,
                        error = unlockError,
                        onDismiss = {
                            unlockTargetId = null
                            unlockError = null
                        },
                        onUnlock = { password -> unlockDoc(id, password) },
                    )
                }
            }

            deleteTargetId?.let { id ->
                val doc = docs.firstOrNull { it.id == id }
                if (doc != null) {
                    DeleteConfirmDialog(
                        dark = dark,
                        docTitle = doc.displayTitle,
                        onDismiss = { deleteTargetId = null },
                        onConfirm = {
                            keys.remove(id)
                            docs = docs.filterNot { it.id == id }
                            persist()
                            if (activeId == id) activeId = null
                            deleteTargetId = null
                            message("Document deleted.")
                        },
                    )
                }
            }

            if (imageDialogOpen) {
                ImageInsertDialog(
                    dark = dark,
                    pickedName = pickedImageName,
                    hasPickedFile = pickedImagePath != null,
                    downloading = downloadingImage,
                    onDismiss = { imageDialogOpen = false },
                    onPickFile = { imagePicker.launch("image/*") },
                    onInsertFile = {
                        val path = pickedImagePath
                        if (path != null) {
                            controller.insertImage(path)
                            imageDialogOpen = false
                            pickedImagePath = null
                            pickedImageName = null
                            message("Image inserted.")
                        }
                    },
                    onInsertUrl = { url ->
                        downloadingImage = true
                        scope.launch {
                            val bytes = withContext(Dispatchers.IO) { fetchImageBytes(url) }
                            val path = if (bytes != null) {
                                withContext(Dispatchers.IO) { store.saveImageBytes(bytes) }
                            } else {
                                null
                            }
                            downloadingImage = false
                            if (path != null) {
                                controller.insertImage(path)
                                imageDialogOpen = false
                                message("Image inserted.")
                            } else {
                                message("Could not download that image.")
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? Activity
}

private fun displayNameFor(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    val value = cursor.getString(index)
                    if (!value.isNullOrBlank()) return value
                }
            }
        } catch (_: Exception) {
            // Fall through to the path-based name below.
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
}

private fun exportName(title: String, extension: String): String {
    val base = title.lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "bushy_document" }
    return "$base.$extension"
}

private fun writeExport(context: Context, uri: Uri, content: String, onDone: () -> Unit) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("Unable to open output stream")
    }.onSuccess { onDone() }
}

private fun buildMarkdown(doc: Doc): String {
    val plain = TextTools.plain(doc.content)
    return buildString {
        append("# ")
        append(doc.displayTitle)
        append("\n\n")
        append(plain)
        append('\n')
    }
}

private fun docToJson(doc: Doc): String {
    val objectDoc = JSONObject().apply {
        put("id", doc.id)
        put("title", doc.displayTitle)
        put("createdAt", doc.createdAt)
        put("updatedAt", doc.updatedAt)
        put("encrypted", doc.encrypted)
        put("font", doc.font)
        put("words", doc.words)
        if (doc.encrypted) {
            put("content", "")
            put("cipher", doc.cipher)
            put("salt", doc.salt)
            put("iv", doc.iv)
        } else {
            put("content", doc.content)
        }
    }
    return objectDoc.toString(2)
}

private fun fetchImageBytes(url: String): ByteArray? {
    val parsed = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    val scheme = parsed.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return null
    val connection = runCatching {
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 18_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "BushyWriter/0.1 (local image insert)")
        }
    }.getOrNull() ?: return null
    return try {
        if (connection.responseCode !in 200..299) {
            null
        } else {
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > 12 * 1024 * 1024) return null
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        }
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}
