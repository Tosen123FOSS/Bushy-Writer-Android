package net.bushydocs.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AppBgLight = Zinc50
val AppBgDark = Zinc950
val SurfaceLight = Color.White
val SurfaceDark = Zinc900
val BorderLight = Zinc200
val BorderDark = Zinc800
val TextLight = Zinc800
val TextDark = Zinc100
val MutedLight = Zinc500
val MutedDark = Zinc400

@Composable
fun BrandLogo(size: Dp = 28.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(listOf(Brand500, Color(0xFF34D399), Brand700)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Park,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.64f),
        )
    }
}

@Composable
fun VersionBadge(dark: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (dark) Zinc800 else Zinc100)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(
            text = "v0.1",
            color = if (dark) Zinc400 else Zinc500,
            fontSize = 9.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun BrandRow(
    dark: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showThemeToggle: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandLogo(28.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Bushy Writer",
            color = if (dark) Zinc100 else Zinc900,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.sp,
        )
        Spacer(Modifier.width(6.dp))
        VersionBadge(dark)
        if (showThemeToggle) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .clickable(onClick = onThemeToggle)
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = if (dark) Zinc400 else Zinc500,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, dark: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = if (dark) Zinc500 else Zinc400,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = modifier,
    )
}

@Composable
fun ThinDivider(dark: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (dark) BorderDark else BorderLight),
    )
}

@Composable
fun StatChip(
    dark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (dark) Zinc900 else Zinc50)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}
