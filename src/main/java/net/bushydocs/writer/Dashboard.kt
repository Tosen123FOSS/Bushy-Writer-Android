package net.bushydocs.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Dashboard(
    dark: Boolean,
    docs: List<Doc>,
    layoutMode: String,
    sortMode: String,
    onLayoutChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onNewDoc: () -> Unit,
    onOpenDoc: (Doc) -> Unit,
    onDeleteDoc: (Doc) -> Unit,
    onThemeToggle: () -> Unit,
    onMenu: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val bg = if (dark) AppBgDark else AppBgLight
    val border = if (dark) BorderDark else BorderLight
    val muted = if (dark) MutedDark else MutedLight
    val sorted = remember(docs, sortMode) {
        when (sortMode) {
            "title" -> docs.sortedBy { it.displayTitle.lowercase() }
            else -> docs.sortedByDescending { it.updatedAt }
        }
    }
    val totalWords = docs.sumOf { it.words }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
    ) {
        val columns = when {
            maxWidth >= 1080.dp -> 3
            maxWidth >= 660.dp -> 2
            else -> 1
        }
        val horizontalPadding = if (maxWidth < 600.dp) 18.dp else 32.dp
        val narrowHeader = maxWidth < 760.dp

        Column(Modifier.fillMaxSize()) {
            if (onMenu != null) {
                MobileTopBar(dark, onThemeToggle, onMenu)
            }

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .widthIn(max = 1080.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding)
                        .padding(top = if (onMenu == null) 32.dp else 22.dp, bottom = 40.dp),
                ) {
                    WorkspaceHeader(
                        dark = dark,
                        docCount = docs.size,
                        wordCount = totalWords,
                        narrow = narrowHeader,
                    )

                    Spacer(Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("Recent Documents", dark)
                        Spacer(Modifier.weight(1f))
                        SortControl(dark, sortMode, onSortChange)
                        Spacer(Modifier.width(10.dp))
                        ViewToggle(dark, layoutMode, onLayoutChange)
                    }

                    Spacer(Modifier.height(12.dp))

                    if (sorted.isEmpty()) {
                        EmptyState(dark, onNewDoc)
                    } else if (layoutMode == "list" || columns == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sorted.forEach { doc ->
                                DocumentListRow(
                                    dark = dark,
                                    doc = doc,
                                    onOpen = { onOpenDoc(doc) },
                                    onDelete = { onDeleteDoc(doc) },
                                )
                            }
                        }
                    } else {
                        sorted.chunked(columns).forEach { rowDocs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowDocs.forEach { doc ->
                                    DocumentCard(
                                        dark = dark,
                                        doc = doc,
                                        onOpen = { onOpenDoc(doc) },
                                        onDelete = { onDeleteDoc(doc) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columns - rowDocs.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Stored only on this device",
                            color = muted,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMono,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (dark) "AES-256 ready" else "AES-256 ready",
                            color = if (dark) Zinc500 else Zinc400,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMono,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileTopBar(
    dark: Boolean,
    onThemeToggle: () -> Unit,
    onMenu: () -> Unit,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val border = if (dark) BorderDark else BorderLight
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onMenu),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Open menu",
                    tint = if (dark) Zinc400 else Zinc600,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            BrandLogo(24.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Bushy Writer",
                color = if (dark) Zinc100 else Zinc900,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(6.dp))
            VersionBadge(dark)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onThemeToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = if (dark) Zinc400 else Zinc500,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(border),
        )
    }
}

@Composable
private fun WorkspaceHeader(
    dark: Boolean,
    docCount: Int,
    wordCount: Int,
    narrow: Boolean,
) {
    val muted = if (dark) MutedDark else MutedLight
    if (narrow) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = "Workspace Overview",
                color = if (dark) Zinc100 else Zinc900,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Local rich-text & markdown writing space with offline encryption and media support.",
                color = muted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(16.dp))
            StatsStrip(dark, docCount, wordCount)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Workspace Overview",
                    color = if (dark) Zinc100 else Zinc900,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Local rich-text & markdown writing space with offline encryption and media support.",
                    color = muted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(24.dp))
            StatsStrip(dark, docCount, wordCount)
        }
    }
}

@Composable
private fun StatsStrip(dark: Boolean, docCount: Int, wordCount: Int) {
    val surface = if (dark) Zinc900 else Zinc50
    val border = if (dark) BorderDark else BorderLight
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        StatItem(
            dark = dark,
            icon = Icons.Outlined.FileCopy,
            tint = if (dark) Brand400 else Brand500,
            value = docCount.toString(),
            label = "Docs",
        )
        Box(
            Modifier
                .width(1.dp)
                .height(14.dp)
                .background(if (dark) Zinc800 else Zinc200),
        )
        StatItem(
            dark = dark,
            icon = Icons.AutoMirrored.Outlined.FormatAlignLeft,
            tint = if (dark) Blue500 else Blue500,
            value = formatCount(wordCount),
            label = "Words",
        )
        Box(
            Modifier
                .width(1.dp)
                .height(14.dp)
                .background(if (dark) Zinc800 else Zinc200),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brand500),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Vault Ready",
                color = if (dark) Zinc400 else Zinc500,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun StatItem(
    dark: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    value: String,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            color = if (dark) Zinc100 else Zinc900,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = if (dark) Zinc500 else Zinc500,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun SortControl(dark: Boolean, sortMode: String, onSortChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = if (sortMode == "title") "Title A-Z" else "Date Updated"
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { open = true }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (dark) Zinc300 else Zinc700,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = "Sort documents",
                tint = if (dark) Zinc500 else Zinc400,
                modifier = Modifier.size(15.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Date Updated", fontSize = 12.sp) },
                onClick = {
                    onSortChange("recent")
                    open = false
                },
            )
            DropdownMenuItem(
                text = { Text("Title A-Z", fontSize = 12.sp) },
                onClick = {
                    onSortChange("title")
                    open = false
                },
            )
        }
    }
}

@Composable
private fun ViewToggle(dark: Boolean, layoutMode: String, onLayoutChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (dark) Zinc900 else Zinc100)
            .border(
                1.dp,
                if (dark) BorderDark else Zinc200,
                RoundedCornerShape(8.dp),
            )
            .padding(2.dp),
    ) {
        ViewToggleButton(
            dark = dark,
            icon = Icons.Outlined.GridView,
            label = "Grid view",
            selected = layoutMode == "grid",
        ) { onLayoutChange("grid") }
        ViewToggleButton(
            dark = dark,
            icon = Icons.AutoMirrored.Outlined.ViewList,
            label = "List view",
            selected = layoutMode == "list",
        ) { onLayoutChange("list") }
    }
}

@Composable
private fun ViewToggleButton(
    dark: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 26.dp, height = 22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    selected && dark -> Zinc800
                    selected -> Color.White
                    else -> Color.Transparent
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                selected && dark -> Zinc100
                selected -> Zinc800
                dark -> Zinc500
                else -> Zinc400
            },
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun DocumentCard(
    dark: Boolean,
    doc: Doc,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val border = if (dark) BorderDark else BorderLight
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DocIcon(dark, doc)
            Spacer(Modifier.weight(1f))
            if (doc.encrypted) EncryptedChip(dark)
            Spacer(Modifier.width(2.dp))
            DeleteAction(dark, doc, onDelete)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = doc.displayTitle,
            color = if (dark) Zinc100 else Zinc900,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (doc.encrypted) {
                "Encrypted document. Unlock with your passphrase to read."
            } else {
                TextTools.preview(doc.content)
            },
            color = if (dark) Zinc500 else Zinc500,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = TextTools.dateLabel(doc.updatedAt),
                color = if (dark) Zinc500 else Zinc400,
                fontSize = 10.sp,
                fontFamily = JetBrainsMono,
            )
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier
                    .size(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (dark) Zinc700 else Zinc300),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "${doc.words} words",
                color = if (dark) Zinc500 else Zinc400,
                fontSize = 10.sp,
                fontFamily = JetBrainsMono,
            )
        }
    }
}

@Composable
private fun DocumentListRow(
    dark: Boolean,
    doc: Doc,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val border = if (dark) BorderDark else BorderLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DocIcon(dark, doc)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = doc.displayTitle,
                    color = if (dark) Zinc100 else Zinc900,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (doc.encrypted) {
                    Spacer(Modifier.width(7.dp))
                    EncryptedChip(dark)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (doc.encrypted) {
                    "Encrypted document — unlock to read"
                } else {
                    TextTools.preview(doc.content, 100)
                },
                color = if (dark) Zinc500 else Zinc500,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = TextTools.dateLabel(doc.updatedAt),
                color = if (dark) Zinc500 else Zinc400,
                fontSize = 10.sp,
                fontFamily = JetBrainsMono,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${doc.words} words",
                color = if (dark) Zinc500 else Zinc400,
                fontSize = 10.sp,
                fontFamily = JetBrainsMono,
            )
        }
        Spacer(Modifier.width(4.dp))
        DeleteAction(dark, doc, onDelete)
    }
}

@Composable
private fun DocIcon(dark: Boolean, doc: Doc) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (dark) Color(0xFF052E24) else Brand50),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (doc.encrypted) Icons.Outlined.Lock else Icons.Outlined.Description,
            contentDescription = null,
            tint = if (dark) Brand400 else Brand600,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun EncryptedChip(dark: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (dark) Color(0xFF052E24) else Brand50)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = if (dark) Brand400 else Brand700,
            modifier = Modifier.size(9.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "AES",
            color = if (dark) Brand400 else Brand700,
            fontSize = 8.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DeleteAction(dark: Boolean, doc: Doc, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onDelete),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = "Delete ${doc.displayTitle}",
            tint = if (dark) Zinc600 else Zinc400,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun EmptyState(dark: Boolean, onNewDoc: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (dark) BorderDark else BorderLight,
                RoundedCornerShape(12.dp),
            )
            .padding(vertical = 52.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(if (dark) Zinc900 else Zinc100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                contentDescription = null,
                tint = if (dark) Zinc500 else Zinc400,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(13.dp))
        Text(
            text = "No documents yet",
            color = if (dark) Zinc200 else Zinc800,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Create your first document or start from a template.",
            color = if (dark) Zinc500 else Zinc500,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (dark) Brand500 else Brand600)
                .clickable(onClick = onNewDoc)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "New Document",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatCount(value: Int): String =
    if (value >= 1000) String.format(java.util.Locale.US, "%.1fk", value / 1000.0) else value.toString()
