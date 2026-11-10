package net.bushydocs.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SidebarWidth = 256.dp

@Composable
fun Sidebar(
    dark: Boolean,
    docs: List<Doc>,
    activeId: String?,
    storageLabel: String,
    onHome: () -> Unit,
    onNewDoc: () -> Unit,
    onOpenDoc: (Doc) -> Unit,
    onDeleteDoc: (Doc) -> Unit,
    onTemplate: (String) -> Unit,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = if (dark) Zinc900 else androidx.compose.ui.graphics.Color.White
    val border = if (dark) BorderDark else BorderLight
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .width(SidebarWidth)
            .fillMaxHeight()
            .background(surface)
            .padding(top = 16.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            BrandRow(dark = dark, onThemeToggle = onThemeToggle)
        }

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 12.dp),
        ) {
            NavItem(
                dark = dark,
                icon = Icons.Outlined.Dashboard,
                label = "Workspace",
                selected = activeId == null,
                onClick = onHome,
            )
            NavItem(
                dark = dark,
                icon = Icons.Outlined.AddCircleOutline,
                label = "New Document",
                selected = false,
                iconTint = if (dark) Brand400 else Brand500,
                onClick = onNewDoc,
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Documents", dark)
                Spacer(Modifier.weight(1f))
                Text(
                    text = docs.size.toString(),
                    color = if (dark) Zinc500 else Zinc400,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMono,
                )
            }
            Spacer(Modifier.height(4.dp))

            if (docs.isEmpty()) {
                Text(
                    text = "No documents yet",
                    color = if (dark) Zinc500 else Zinc400,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            } else {
                docs.forEach { doc ->
                    DocRow(
                        dark = dark,
                        doc = doc,
                        selected = doc.id == activeId,
                        onClick = { onOpenDoc(doc) },
                        onDelete = { onDeleteDoc(doc) },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            ThinDivider(dark)
            Spacer(Modifier.height(14.dp))

            SectionLabel(
                text = "Quick Templates",
                dark = dark,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Spacer(Modifier.height(6.dp))
            TemplateItem(dark, Icons.Outlined.CalendarMonth, "Meeting Notes") { onTemplate("meeting") }
            TemplateItem(dark, Icons.Outlined.Code, "Tech Spec") { onTemplate("spec") }
            TemplateItem(dark, Icons.AutoMirrored.Outlined.MenuBook, "Daily Journal") { onTemplate("journal") }
            Spacer(Modifier.height(10.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (dark) Zinc900 else Zinc50)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brand500),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "Encrypted Vault",
                color = if (dark) Zinc400 else Zinc500,
                fontSize = 11.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = storageLabel,
                color = if (dark) Zinc500 else Zinc400,
                fontSize = 10.sp,
                fontFamily = JetBrainsMono,
            )
        }
    }
}

@Composable
private fun NavItem(
    dark: Boolean,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit,
) {
    val bg = when {
        selected && dark -> Zinc800
        selected -> Zinc100
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val contentColor = when {
        selected && dark -> Zinc100
        selected -> Zinc900
        dark -> Zinc400
        else -> Zinc600
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint ?: contentColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DocRow(
    dark: Boolean,
    doc: Doc,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val bg = when {
        selected && dark -> Zinc800
        selected -> Zinc100
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val contentColor = when {
        selected && dark -> Zinc100
        selected -> Zinc900
        dark -> Zinc400
        else -> Zinc600
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (doc.encrypted) Icons.Outlined.Lock else Icons.Outlined.Description,
            contentDescription = null,
            tint = if (doc.encrypted) {
                if (dark) Brand400 else Brand600
            } else {
                if (dark) Zinc500 else Zinc400
            },
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = doc.displayTitle,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete ${doc.displayTitle}",
                tint = if (dark) Zinc600 else Zinc300,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun TemplateItem(
    dark: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (dark) Zinc500 else Zinc400,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            color = if (dark) Zinc400 else Zinc600,
            fontSize = 12.sp,
        )
    }
}
