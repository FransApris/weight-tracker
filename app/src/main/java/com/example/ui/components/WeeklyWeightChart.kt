package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WeightEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalTextApi::class)
@Composable
fun WeeklyWeightChart(
    entries: List<WeightEntry>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    // Filter to last 7 entries for weekly view and sort chronologically (oldest first)
    val chartData = remember(entries) {
        entries.take(7).sortedBy { it.dateEpochDay }
    }

    // Interactive point selection indices
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Grafik Perubahan Mingguan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Menampilkan hingga 7 entri rekaman terakhir secara kronologis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (chartData.isNotEmpty()) {
                    val weightDiff = chartData.last().weight - chartData.first().weight
                    val diffString = String.format(Locale.US, "%.1f", weightDiff)
                    val indicatorColor = if (weightDiff <= 0.0) Color(0xFF2D6A4F) else Color(0xFFB33A3A)
                    val indicatorBg = indicatorColor.copy(alpha = 0.12f)
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(indicatorBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (weightDiff <= 0.0) "$diffString kg ↓" else "+$diffString kg ↑",
                            color = indicatorColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (chartData.size < 2) {
                // Not enough data placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚖️",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Kurang Data Untuk Grafik",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Silakan catat minimal 2 hari berat badan untuk mengaktifkan line chart progres mingguan secara dinamis.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                }
            } else {
                // Weights range computation for Y-axis fitting
                val weights = chartData.map { it.weight.toFloat() }
                val maxWeight = weights.maxOrNull() ?: 100f
                val minWeight = weights.minOrNull() ?: 50f
                
                // Add some padding to max and min boundaries
                val yMax = maxWeight + 1.5f
                val yMin = (minWeight - 1.5f).coerceAtLeast(0f)
                val weightRange = if (yMax == yMin) 1f else (yMax - yMin)

                // Animation for drawn lines
                var animationPlayed by remember { mutableStateOf(false) }
                LaunchedEffect(chartData) {
                    animationPlayed = true
                }
                val animProgress by animateFloatAsState(
                    targetValue = if (animationPlayed) 1f else 0f,
                    animationSpec = tween(1000)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp, end = 12.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingLeft = 100f // space for Y-axis values
                        if (width <= paddingLeft + 50f || height <= 50f) return@Canvas
                        
                        val chartWidth = width - paddingLeft
                        val spacingX = chartWidth / (chartData.size - 1)

                        // 1. Draw horizontal GRID levels & Y-axis labels
                        val levelsCount = 4
                        for (i in 0 until levelsCount) {
                            val yRatio = i.toFloat() / (levelsCount - 1)
                            val yPos = height * (1 - yRatio)
                            val value = yMin + (yRatio * weightRange)

                            // Grid dotted lines
                            drawLine(
                                color = gridColor,
                                start = Offset(paddingLeft, yPos),
                                end = Offset(width, yPos),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            // Y-axis label text
                            val yText = String.format(Locale.getDefault(), "%.1f", value)
                            drawText(
                                textMeasurer = textMeasurer,
                                text = yText,
                                topLeft = Offset(10f, yPos - 20f),
                                style = TextStyle(
                                    color = onSurfaceColor.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        // 2. Compute Points coordinates
                        val points = chartData.mapIndexed { index, entry ->
                            val x = paddingLeft + (index * spacingX)
                            val yRatio = (entry.weight.toFloat() - yMin) / weightRange
                            val y = height - (yRatio * height)
                            Offset(x, y)
                        }

                        // 3. Draw gradient background fill under the path
                        val fillPath = Path().apply {
                            moveTo(paddingLeft, height)
                            for (i in points.indices) {
                                val pt = points[i]
                                // animate drawing width
                                if (pt.x <= paddingLeft + (chartWidth * animProgress)) {
                                    lineTo(pt.x, pt.y)
                                }
                            }
                            val lastVisibleX = paddingLeft + (chartWidth * animProgress)
                            val clampedLastX = points.last().x.coerceAtMost(lastVisibleX)
                            
                            // interpolate last Y if animated intermediate
                            val lastVisibleIndex = points.indexOfFirst { it.x > lastVisibleX }
                            val closingY = if (lastVisibleIndex > 0) {
                                val prev = points[lastVisibleIndex - 1]
                                val next = points[lastVisibleIndex]
                                val fraction = (clampedLastX - prev.x) / (next.x - prev.x)
                                prev.y + (fraction * (next.y - prev.y))
                            } else {
                                points.last().y
                            }

                            lineTo(clampedLastX, closingY)
                            lineTo(clampedLastX, height)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.01f)
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // 4. Draw the actual curve line connecting data points
                        val linePath = Path().apply {
                            if (points.isNotEmpty()) {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    val currentX = points[i].x
                                    val currentY = points[i].y
                                    
                                    // Smooth bezier curve calculations
                                    val previousX = points[i - 1].x
                                    val previousY = points[i - 1].y
                                    
                                    val controlX1 = previousX + (currentX - previousX) / 2f
                                    val controlY1 = previousY
                                    val controlX2 = previousX + (currentX - previousX) / 2f
                                    val controlY2 = currentY
                                    
                                    if (currentX <= paddingLeft + (chartWidth * animProgress)) {
                                        cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
                                    } else {
                                        // Draw up to animated boundary
                                        val remainingWidth = (paddingLeft + (chartWidth * animProgress)) - previousX
                                        if (remainingWidth > 0) {
                                            val fraction = remainingWidth / (currentX - previousX)
                                            val targetX = previousX + remainingWidth
                                            val targetY = previousY + (fraction * (currentY - previousY))
                                            lineTo(targetX, targetY)
                                        }
                                        break
                                    }
                                }
                            }
                        }

                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 7f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // 5. Draw interactive Data Nodes/Circles
                        for (i in points.indices) {
                            val pt = points[i]
                            if (pt.x <= paddingLeft + (chartWidth * animProgress)) {
                                // Draw node shadow
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.15f),
                                    radius = 16f,
                                    center = pt + Offset(0f, 4f)
                                )

                                // Draw main node background
                                drawCircle(
                                    color = surfaceColor,
                                    radius = 14f,
                                    center = pt
                                )

                                // Draw node core line dot
                                drawCircle(
                                    color = primaryColor,
                                    radius = 8f,
                                    center = pt
                                )

                                // Simple text above nodes representing real kg
                                val entry = chartData[i]
                                val metricText = "${entry.weight}kg"
                                val formattedDateStr = try {
                                    val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val outputSdf = SimpleDateFormat("EE", Locale("id", "ID")) // "Sen", "Sel"...
                                    val dateObj = inputSdf.parse(entry.dateString)
                                    outputSdf.format(dateObj ?: Date())
                                } catch (e: Exception) {
                                    entry.dateString.takeLast(2)
                                }

                                // Render weight kilograms
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = metricText,
                                    topLeft = Offset(pt.x - 38f, pt.y - 50f),
                                    style = TextStyle(
                                        color = onSurfaceColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                )

                                // Render X axis day of week strings
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = formattedDateStr,
                                    topLeft = Offset(pt.x - 24f, height + 8f),
                                    style = TextStyle(
                                        color = onSurfaceColor.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
