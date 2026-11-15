package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class AlapExtendedColors(
    val chatWallpaper: Color,
    val outgoingBubble: Color,
    val onOutgoingBubble: Color,
    val incomingBubble: Color,
    val onIncomingBubble: Color,
    val readTick: Color,
    val mutedText: Color,
    val subtleDivider: Color
)

val LocalAlapColors = staticCompositionLocalOf {
    AlapExtendedColors(
        chatWallpaper = LightChatWallpaper,
        outgoingBubble = AlapEmeraldTintLight,
        onOutgoingBubble = LightTextPrimary,
        incomingBubble = LightIncomingBubble,
        onIncomingBubble = LightTextPrimary,
        readTick = ReadTickBlue,
        mutedText = LightTextSecondary,
        subtleDivider = LightOutline
    )
}

private val AlapLightColorScheme = lightColorScheme(
    primary = AlapEmerald,
    onPrimary = Color.White,
    primaryContainer = AlapMintContainer,
    onPrimaryContainer = AlapEmeraldDeep,
    secondary = AlapEmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = AlapEmeraldTintLight,
    onSecondaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceContainerLowest = LightBackground,
    surfaceContainerLow = Color(0xFFFAFBFB),
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFF0F2F4),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = AlapDanger,
    onError = Color.White
)

private val AlapShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun AlapTheme(
    content: @Composable () -> Unit
) {
    val extendedColors = AlapExtendedColors(
        chatWallpaper = LightChatWallpaper,
        outgoingBubble = AlapEmeraldTintLight,
        onOutgoingBubble = LightTextPrimary,
        incomingBubble = LightIncomingBubble,
        onIncomingBubble = LightTextPrimary,
        readTick = ReadTickBlue,
        mutedText = LightTextSecondary,
        subtleDivider = LightOutline
    )

    CompositionLocalProvider(LocalAlapColors provides extendedColors) {
        MaterialTheme(
            colorScheme = AlapLightColorScheme,
            typography = AlapTypography,
            shapes = AlapShapes,
            content = content
        )
    }
}
