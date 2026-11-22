package com.fidzzz.wifisystem.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun CardTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}

@Composable
fun Tag(text: String, warn: Boolean = false) {
    val c = if (warn) Color(0xFFFFB347) else MaterialTheme.colorScheme.primary
    Surface(shape = RoundedCornerShape(50), color = c.copy(alpha = 0.16f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = c
        )
    }
}

@Composable
fun Page(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
fun LatencyChart(values: List<Long?>, modifier: Modifier = Modifier.fillMaxWidth().height(110.dp)) {
    val line = MaterialTheme.colorScheme.primary
    val bad = Color(0xFFFF5C5C)
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Canvas(modifier) {
        drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), 1f)
        if (values.size < 2) return@Canvas
        val max = (values.filterNotNull().maxOrNull() ?: 1L).coerceAtLeast(10L).toFloat()
        val dx = size.width / (values.size - 1)
        val path = Path()
        var started = false
        values.forEachIndexed { i, v ->
            val x = i * dx
            if (v == null) {
                drawCircle(bad, 4f, Offset(x, size.height - 4f))
                started = false
            } else {
                val y = size.height - (v / max) * (size.height - 8f) - 4f
                if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
            }
        }
        drawPath(path, line, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun FidzzLogo(logoSize: Dp = 96.dp) {
    val blue = Color(0xFF2F8BFF)
    Canvas(Modifier.size(logoSize)) {
        val w = this.size.width
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF0E1A33), Color(0xFF080B12))),
            cornerRadius = CornerRadius(w * 0.24f)
        )
        val cx = w / 2
        val cy = w * 0.68f
        listOf(0.17f, 0.30f, 0.43f).forEach { r ->
            val rad = w * r
            drawArc(
                color = blue, startAngle = 225f, sweepAngle = 90f, useCenter = false,
                topLeft = Offset(cx - rad, cy - rad), size = Size(rad * 2, rad * 2),
                style = Stroke(w * 0.06f, cap = StrokeCap.Round)
            )
        }
        drawCircle(blue, w * 0.05f, Offset(cx, cy))
    }
}
