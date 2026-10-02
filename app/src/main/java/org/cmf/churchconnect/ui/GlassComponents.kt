package org.cmf.churchconnect.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** A restrained gradient background; no blur or continuous effects are used. */
@Composable
fun GlassBackdrop(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = isGlassDarkMode()
    val stops = if (dark) {
        listOf(Color(0xFF0A1422), Color(0xFF10223A), Color(0xFF0D192A))
    } else {
        listOf(Color(0xFFEEF5FB), Color(0xFFE7F0F8), Color(0xFFF4F8FC))
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(stops))
    ) {
        Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = if (dark) 0.12f else 0.08f)))
        content()
    }
}

/** Frosted-looking surface built with translucency, a fine edge, and restrained elevation. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dark = isGlassDarkMode()
    val surfaceColor = when {
        emphasized -> Color.Transparent
        dark -> colors.surface.copy(alpha = 0.94f)
        else -> colors.surface.copy(alpha = 0.96f)
    }
    val edgeColor = if (emphasized) {
        GlassTokens.heroHighlight.copy(alpha = 0.58f)
    } else {
        colors.outline.copy(alpha = if (dark) 0.50f else 0.42f)
    }
    val border = BorderStroke(GlassTokens.borderWidth, edgeColor)
    val contentColor = if (emphasized) Color.White else colors.onSurface

    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = surfaceColor,
            contentColor = contentColor,
            border = border,
            tonalElevation = GlassTokens.elevationLow,
            shadowElevation = GlassTokens.elevationCard
        ) {
            GlassCardBody(emphasized, content)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = surfaceColor,
            contentColor = contentColor,
            border = border,
            tonalElevation = GlassTokens.elevationLow,
            shadowElevation = GlassTokens.elevationCard
        ) {
            GlassCardBody(emphasized, content)
        }
    }
}

@Composable
private fun GlassCardBody(emphasized: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val dark = isGlassDarkMode()
    val fill = if (dark) {
        Brush.linearGradient(listOf(Color(0xFF153959), Color(0xFF0F4960), Color(0xFF17355B)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF0C4A78), Color(0xFF08617A), Color(0xFF124477)))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (emphasized) Modifier.background(fill) else Modifier),
        content = content
    )
}

/** Material 3 action with the app-wide corner and minimum touch-size tokens. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        content = content
    )
}

/** Keeps icon actions at least 48dp while preserving Material's touch and focus behavior. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.sizeIn(minWidth = GlassTokens.minimumTouchTarget, minHeight = GlassTokens.minimumTouchTarget),
        content = content
    )
}

/** Compact status treatment using semantic color roles from the current light/dark palette. */
@Composable
fun GlassBadge(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = colors.primaryContainer.copy(alpha = if (isGlassDarkMode()) 0.92f else 0.84f),
        contentColor = colors.onPrimaryContainer,
        border = BorderStroke(GlassTokens.borderWidth, colors.outline.copy(alpha = 0.42f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
