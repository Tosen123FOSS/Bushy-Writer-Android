package net.bushydocs.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun EncryptDialog(
    dark: Boolean,
    docTitle: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val muted = if (dark) MutedDark else MutedLight
    val surface = if (dark) SurfaceDark else SurfaceLight

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(surface)
                .border(
                    1.dp,
                    if (dark) BorderDark else BorderLight,
                    RoundedCornerShape(14.dp),
                )
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (dark) Brand400 else Brand600,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AES-256 Document Encryption",
                    color = if (dark) Zinc100 else Zinc900,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "“$docTitle” will be encrypted on this device with your passphrase. There is no recovery: keep it safe.",
                color = muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(14.dp))

            PasswordField(
                dark = dark,
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                label = "Encryption password",
                visible = visible,
                onToggleVisible = { visible = !visible },
            )
            Spacer(Modifier.height(9.dp))
            PasswordField(
                dark = dark,
                value = confirm,
                onValueChange = {
                    confirm = it
                    error = null
                },
                label = "Confirm password",
                visible = visible,
                onToggleVisible = { visible = !visible },
            )

            if (error != null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = error!!,
                    color = if (dark) Rose500 else Rose600,
                    fontSize = 10.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogTextButton("Cancel", dark, onDismiss)
                Spacer(Modifier.width(8.dp))
                PrimaryButton("Apply & Lock", dark) {
                    when {
                        password.length < 6 -> error = "Use at least 6 characters."
                        password != confirm -> error = "Passwords do not match."
                        else -> onApply(password)
                    }
                }
            }
        }
    }
}

@Composable
fun UnlockDialog(
    dark: Boolean,
    docTitle: String,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onUnlock: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val muted = if (dark) MutedDark else MutedLight
    val surface = if (dark) SurfaceDark else SurfaceLight

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(surface)
                .border(
                    1.dp,
                    if (dark) BorderDark else BorderLight,
                    RoundedCornerShape(14.dp),
                )
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (dark) Color(0xFF052E24) else Brand50),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (dark) Brand400 else Brand600,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Unlock Document",
                        color = if (dark) Zinc100 else Zinc900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = docTitle,
                        color = muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Enter the passphrase you used to encrypt this document. It never leaves your device.",
                color = muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(14.dp))
            PasswordField(
                dark = dark,
                value = password,
                onValueChange = { password = it },
                label = "Passphrase",
                visible = visible,
                onToggleVisible = { visible = !visible },
                enabled = !busy,
            )
            if (error != null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = error,
                    color = if (dark) Rose500 else Rose600,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogTextButton("Cancel", dark) {
                    if (!busy) onDismiss()
                }
                Spacer(Modifier.width(8.dp))
                PrimaryButton(if (busy) "Unlocking…" else "Unlock", dark, enabled = !busy) {
                    if (password.isEmpty()) return@PrimaryButton
                    onUnlock(password)
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    dark: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    enabled: Boolean = true,
) {
    val textColor = if (dark) Zinc100 else Zinc800
    val border = if (dark) Zinc700 else Zinc200
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 13.sp,
            color = textColor,
            fontFamily = Jakarta,
        ),
        placeholder = {
            Text(label, fontSize = 12.sp, color = if (dark) Zinc600 else Zinc400)
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Icon(
                imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (visible) "Hide password" else "Show password",
                tint = if (dark) Zinc500 else Zinc400,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onToggleVisible),
            )
        },
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (dark) Brand500 else Brand500,
            unfocusedBorderColor = border,
            cursorColor = Brand500,
            focusedContainerColor = if (dark) Zinc800 else Color.White,
            unfocusedContainerColor = if (dark) Zinc800 else Color.White,
            disabledContainerColor = if (dark) Zinc800.copy(alpha = 0.7f) else Zinc50,
            disabledBorderColor = border,
            disabledTextColor = if (dark) Zinc400 else Zinc500,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ImageInsertDialog(
    dark: Boolean,
    pickedName: String?,
    hasPickedFile: Boolean,
    downloading: Boolean,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onInsertFile: () -> Unit,
    onInsertUrl: (String) -> Unit,
) {
    var tab by remember { mutableStateOf("file") }
    var url by remember { mutableStateOf("") }
    val surface = if (dark) SurfaceDark else SurfaceLight
    val border = if (dark) BorderDark else BorderLight
    val muted = if (dark) MutedDark else MutedLight

    Dialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(surface)
                .border(1.dp, border, RoundedCornerShape(14.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = if (dark) Brand400 else Brand600,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Insert Image",
                    color = if (dark) Zinc100 else Zinc900,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Close",
                    color = muted,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { if (!downloading) onDismiss() }
                        .padding(6.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.dp, Color.Transparent),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ImageTab("Upload File", tab == "file", dark) { tab = "file" }
                ImageTab("Direct URL", tab == "url", dark) { tab = "url" }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(border),
            )

            Spacer(Modifier.height(14.dp))

            if (tab == "file") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onPickFile)
                        .padding(vertical = 22.dp, horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.UploadFile,
                        contentDescription = null,
                        tint = if (dark) Zinc500 else Zinc400,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tap to select an image",
                        color = if (dark) Zinc300 else Zinc700,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "PNG, JPG, GIF, WebP",
                        color = if (dark) Zinc600 else Zinc400,
                        fontSize = 9.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (pickedName != null) "Selected: $pickedName" else "No image selected yet",
                    color = muted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            "https://example.com/image.jpg",
                            fontSize = 12.sp,
                            color = if (dark) Zinc600 else Zinc400,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = if (dark) Zinc500 else Zinc400,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = if (dark) Zinc100 else Zinc800,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand500,
                        unfocusedBorderColor = border,
                        cursorColor = Brand500,
                        focusedContainerColor = if (dark) Zinc800 else Color.White,
                        unfocusedContainerColor = if (dark) Zinc800 else Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "The image is downloaded once and stored inside this app.",
                    color = muted,
                    fontSize = 9.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogTextButton("Cancel", dark) {
                    if (!downloading) onDismiss()
                }
                Spacer(Modifier.width(8.dp))
                PrimaryButton(
                    label = if (downloading) "Downloading…" else "Insert",
                    dark = dark,
                    enabled = !downloading && if (tab == "file") hasPickedFile else url.isNotBlank(),
                ) {
                    if (tab == "file") onInsertFile() else onInsertUrl(url.trim())
                }
            }
        }
    }
}

@Composable
private fun ImageTab(label: String, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) {
                if (dark) Brand400 else Brand600
            } else {
                if (dark) Zinc500 else Zinc500
            },
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .width(if (selected) 68.dp else 58.dp)
                .height(2.dp)
                .background(if (selected) Brand500 else Color.Transparent),
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    dark: Boolean,
    docTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val surface = if (dark) SurfaceDark else SurfaceLight
    val border = if (dark) BorderDark else BorderLight
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(surface)
                .border(1.dp, border, RoundedCornerShape(14.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (dark) Color(0xFF3F1D24) else Color(0xFFFFE4E6)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = if (dark) Rose500 else Rose600,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Delete Document",
                    color = if (dark) Zinc100 else Zinc900,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "“$docTitle” will be permanently removed from this device. This cannot be undone.",
                color = if (dark) MutedDark else MutedLight,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogTextButton("Cancel", dark, onDismiss)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (dark) Rose600 else Rose600)
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "Delete",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogTextButton(label: String, dark: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (dark) Zinc400 else Zinc600,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun PrimaryButton(
    label: String,
    dark: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(
                when {
                    !enabled && dark -> Zinc800
                    !enabled -> Zinc200
                    dark -> Zinc100
                    else -> Zinc900
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "Unlocking…" || label == "Downloading…") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(11.dp),
                    strokeWidth = 1.5.dp,
                    color = if (dark) Zinc900 else Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    color = if (dark) Zinc900 else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            Text(
                text = label,
                color = if (dark) Zinc900 else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
