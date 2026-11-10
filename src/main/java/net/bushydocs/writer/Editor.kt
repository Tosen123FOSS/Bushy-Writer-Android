package net.bushydocs.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorScreen(
    dark: Boolean,
    doc: Doc,
    controller: EditorController,
    wordCount: Int,
    charCount: Int,
    savedLabel: String,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onFontChange: (FontChoice) -> Unit,
    onExportMd: () -> Unit,
    onExportJson: () -> Unit,
    onEncrypt: () -> Unit,
    onInsertImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val bg = if (dark) AppBgDark else AppBgLight
    val border = if (dark) BorderDark else BorderLight
    val muted = if (dark) MutedDark else MutedLight
    val font = FontChoice.from(doc.font)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
    ) {
        val narrow = maxWidth < 720.dp
        Column(Modifier.fillMaxSize()) {
            EditorTopBar(
                dark = dark,
                doc = doc,
                narrow = narrow,
                font = font,
                savedLabel = savedLabel,
                onBack = onBack,
                onFontChange = onFontChange,
                onExportMd = onExportMd,
                onExportJson = onExportJson,
                onEncrypt = onEncrypt,
            )
            FormattingToolbar(
                dark = dark,
                controller = controller,
                narrow = narrow,
                font = font,
                onFontChange = onFontChange,
                onInsertImage = onInsertImage,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 820.dp)
                        .fillMaxHeight()
                        .padding(horizontal = if (narrow) 20.dp else 32.dp)
                        .padding(top = 24.dp),
                ) {
                    BasicTextField(
                        value = doc.title,
                        onValueChange = onTitleChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = if (dark) Zinc100 else Zinc900,
                            fontSize = if (narrow) 27.sp else 30.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Jakarta,
                            letterSpacing = (-0.6).sp,
                        ),
                        cursorBrush = SolidColor(if (dark) Brand400 else Brand600),
                        decorationBox = { inner ->
                            Box {
                                if (doc.title.isEmpty()) {
                                    Text(
                                        text = "Untitled Document",
                                        color = if (dark) Zinc600 else Zinc300,
                                        fontSize = if (narrow) 27.sp else 30.sp,
                                        lineHeight = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Jakarta,
                                        letterSpacing = (-0.6).sp,
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))

                    RichTextEditor(
                        html = doc.content,
                        font = font,
                        dark = dark,
                        controller = controller,
                        onHtmlChange = onContentChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }

            EditorFooter(
                dark = dark,
                wordCount = wordCount,
                charCount = charCount,
                narrow = narrow,
            )
        }
    }
}

@Composable
private fun EditorTopBar(
    dark: Boolean,
    doc: Doc,
    narrow: Boolean,
    font: FontChoice,
    savedLabel: String,
    onBack: () -> Unit,
    onFontChange: (FontChoice) -> Unit,
    onExportMd: () -> Unit,
    onExportJson: () -> Unit,
    onEncrypt: () -> Unit,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val border = if (dark) BorderDark else BorderLight
    val muted = if (dark) MutedDark else MutedLight
    var menuOpen by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(49.dp)
                .background(surface)
                .padding(horizontal = if (narrow) 6.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to workspace",
                    tint = if (dark) Zinc400 else Zinc500,
                    modifier = Modifier.size(17.dp),
                )
            }

            if (narrow) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = doc.displayTitle,
                        color = if (dark) Zinc200 else Zinc800,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = savedLabel,
                        color = if (dark) Zinc600 else Zinc400,
                        fontSize = 9.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                    )
                }
                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Document menu",
                            tint = if (dark) Zinc400 else Zinc500,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Export .md", fontSize = 12.sp) },
                            onClick = {
                                menuOpen = false
                                onExportMd()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Backup .json", fontSize = 12.sp) },
                            onClick = {
                                menuOpen = false
                                onExportJson()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (doc.encrypted) "Lock document" else "Encrypt document",
                                    fontSize = 12.sp,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onEncrypt()
                            },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onBack)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Home",
                        color = muted,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = "  /  ",
                        color = if (dark) Zinc600 else Zinc400,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = doc.displayTitle,
                        color = if (dark) Zinc200 else Zinc800,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 190.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "• $savedLabel",
                        color = if (dark) Zinc600 else Zinc400,
                        fontSize = 10.sp,
                        fontStyle = FontStyle.Italic,
                    )
                }

                FontSelector(dark, font, onFontChange)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(border),
                )
                Spacer(Modifier.width(4.dp))
                TopAction("Export .md", dark, onExportMd)
                TopAction("Backup .json", dark, onExportJson)
                EncryptAction(dark, doc.encrypted, onEncrypt)
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
private fun FontSelector(dark: Boolean, font: FontChoice, onFontChange: (FontChoice) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val border = if (dark) Zinc700 else Zinc200
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, border, RoundedCornerShape(6.dp))
                .background(if (dark) Zinc800 else Color.White)
                .clickable { open = true }
                .padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = font.shortLabel,
                color = if (dark) Zinc300 else Zinc600,
                fontSize = 10.sp,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = "Choose font",
                tint = if (dark) Zinc500 else Zinc400,
                modifier = Modifier.size(13.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FontChoice.entries.forEach { choice ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = choice.label,
                            fontSize = 12.sp,
                            fontWeight = if (choice == font) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        open = false
                        onFontChange(choice)
                    },
                )
            }
        }
    }
}

@Composable
private fun TopAction(label: String, dark: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (dark) Zinc400 else Zinc600,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp),
    )
}

@Composable
private fun EncryptAction(dark: Boolean, encrypted: Boolean, onClick: () -> Unit) {
    val bg = if (dark) Zinc800 else Zinc100
    val fg = if (dark) Zinc200 else Zinc700
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (encrypted) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            contentDescription = null,
            tint = if (dark) Brand400 else Brand600,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = if (encrypted) "Lock" else "Encrypt",
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FormattingToolbar(
    dark: Boolean,
    controller: EditorController,
    narrow: Boolean,
    font: FontChoice,
    onFontChange: (FontChoice) -> Unit,
    onInsertImage: () -> Unit,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val border = if (dark) BorderDark else BorderLight
    val scroll = rememberScrollState()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surface)
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (narrow) {
                FontSelector(dark, font, onFontChange)
                ToolbarDivider(dark)
            }
            FormatButton("B", dark, weight = FontWeight.Bold) { controller.toggleBold() }
            FormatButton("I", dark, italic = true) { controller.toggleItalic() }
            FormatButton("U", dark, underline = true) { controller.toggleUnderline() }
            FormatButton("S", dark, strike = true) { controller.toggleStrike() }
            ToolbarDivider(dark)
            FormatButton("H1", dark, weight = FontWeight.Bold) { controller.toggleHeading(1) }
            FormatButton("H2", dark, weight = FontWeight.Bold) { controller.toggleHeading(2) }
            FormatButton("¶", dark) { controller.paragraphStyle() }
            ToolbarDivider(dark)
            FormatButton("• List", dark) { controller.toggleBullet() }
            FormatButton("1. List", dark) { controller.toggleNumbered() }
            FormatButton("“ Quote", dark) { controller.toggleQuote() }
            ToolbarDivider(dark)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onInsertImage)
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Insert image",
                        tint = if (dark) Brand400 else Brand600,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Image",
                        color = if (dark) Brand400 else Brand600,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            FormatButton("Clear", dark, tint = if (dark) Rose500 else Rose600) {
                controller.clearFormatting()
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
private fun FormatButton(
    label: String,
    dark: Boolean,
    weight: FontWeight = FontWeight.Normal,
    italic: Boolean = false,
    underline: Boolean = false,
    strike: Boolean = false,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = tint ?: if (dark) Zinc300 else Zinc700,
            fontSize = 10.sp,
            fontWeight = weight,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when {
                underline -> TextDecoration.Underline
                strike -> TextDecoration.LineThrough
                else -> null
            },
        )
    }
}

@Composable
private fun ToolbarDivider(dark: Boolean) {
    Box(
        Modifier
            .padding(horizontal = 3.dp)
            .width(1.dp)
            .height(16.dp)
            .background(if (dark) Zinc800 else Zinc200),
    )
}

@Composable
private fun EditorFooter(dark: Boolean, wordCount: Int, charCount: Int, narrow: Boolean) {
    val surface = if (dark) Zinc900 else Zinc50
    val border = if (dark) BorderDark else BorderLight
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surface)
                .padding(horizontal = if (narrow) 16.dp else 20.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$wordCount words",
                color = if (dark) Zinc500 else Zinc400,
                fontSize = 10.sp,
                fontFamily = JetBrainsMono,
            )
            FooterDot(dark)
            Text(
                text = "$charCount characters",
                color = if (dark) Zinc500 else Zinc400,
                fontSize = 10.sp,
                fontFamily = JetBrainsMono,
            )
            FooterDot(dark)
            Text(
                text = "${TextTools.readingMinutes(wordCount)} min read",
                color = if (dark) Zinc500 else Zinc400,
                fontSize = 10.sp,
                fontFamily = JetBrainsMono,
            )
            if (!narrow) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Local Encrypted Vault",
                    color = if (dark) Zinc600 else Zinc400,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMono,
                )
            }
        }
    }
}

@Composable
private fun FooterDot(dark: Boolean) {
    Text(
        text = "  •  ",
        color = if (dark) Zinc700 else Zinc300,
        fontSize = 10.sp,
    )
}
