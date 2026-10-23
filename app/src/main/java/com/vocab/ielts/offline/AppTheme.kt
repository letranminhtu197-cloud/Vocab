package com.vocab.ielts.offline

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Thiết kế "học mà chơi" lấy cảm hứng từ phong cách chung của các app học ngoại ngữ phổ biến
 * (nút dày có viền dưới, thanh tiến độ chia đoạn, màu tươi, nhãn chuỗi ngày). Toàn bộ màu,
 * hình khối và hình vẽ ở đây là thiết kế riêng của Vocab; không dùng tài sản của app khác.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2FB344),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F5DC),
    onPrimaryContainer = Color(0xFF0B3D12),
    secondary = Color(0xFFFFC93C),
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFFFFF3D1),
    onSecondaryContainer = Color(0xFF4A3600),
    tertiary = Color(0xFF3D8BFF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCEBFF),
    onTertiaryContainer = Color(0xFF0A2E66),
    background = Color(0xFFFBFDFE),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDF1F5),
    onSurface = Color(0xFF1B1F24),
    onSurfaceVariant = Color(0xFF5B6573),
    outline = Color(0xFFC9D2DC),
    outlineVariant = Color(0xFFE3E8EE),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF5C0F12)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EE07A),
    onPrimary = Color(0xFF07310F),
    primaryContainer = Color(0xFF14401D),
    onPrimaryContainer = Color(0xFFC9F7D2),
    secondary = Color(0xFFFFD45C),
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF4A3A00),
    onSecondaryContainer = Color(0xFFFFE9B0),
    tertiary = Color(0xFF7FB2FF),
    onTertiary = Color(0xFF0A2E66),
    tertiaryContainer = Color(0xFF14315E),
    onTertiaryContainer = Color(0xFFD6E6FF),
    background = Color(0xFF0F1216),
    surface = Color(0xFF171C22),
    surfaceVariant = Color(0xFF232A32),
    onSurface = Color(0xFFE9EEF4),
    onSurfaceVariant = Color(0xFFA3AEBB),
    outline = Color(0xFF3A444F),
    outlineVariant = Color(0xFF2C343D),
    error = Color(0xFFFF8A8F),
    onError = Color(0xFF3B0A0C),
    errorContainer = Color(0xFF4A1719),
    onErrorContainer = Color(0xFFFFDAD9)
)

@Composable
fun VocabTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}

/**
 * Nút kiểu "dày" có cạnh dưới tối hơn, lún xuống khi nhấn — mẫu tương tác phổ biến
 * trong các app học tập. Không dùng hình ảnh hay âm thanh.
 */
@Composable
fun VocabButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    trailingText: String? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val edgeColor = lerp(containerColor, Color.Black, 0.28f)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val verticalShift by animateDpAsState(
        targetValue = if (pressed && enabled) 3.dp else 0.dp,
        label = "vocab-button-press"
    )
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier.clickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = null
        ) { onClick() }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .background(edgeColor.copy(alpha = alpha), shape)
        )
        Surface(
            color = containerColor.copy(alpha = alpha),
            contentColor = contentColor,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = verticalShift)
        ) {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = contentColor)
                if (trailingText != null) {
                    Text(
                        "  $trailingText",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/** Thẻ có viền mảnh, bo tròn lớn — kiểu "card" quen thuộc của app học tập. */
@Composable
fun VocabCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(content = content)
    }
}

/** Thanh tiến độ chia đoạn, giúp người học thấy còn bao nhiêu "chặng". */
@Composable
fun SegmentProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 10
) {
    val safeSegments = segments.coerceIn(1, 20)
    val safeProgress = progress.coerceIn(0f, 1f)
    val filled = safeProgress * safeSegments
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(safeSegments) { index ->
            val fraction = (filled - index).coerceIn(0f, 1f)
            val color = if (fraction > 0f) {
                lerp(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.primary,
                    fraction
                )
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}

/** Vòng tiến độ tròn dùng cho mục tiêu ngày; vẽ bằng Canvas, không cần ảnh. */
@Composable
fun ProgressRing(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    strokeWidth: Dp = 9.dp,
    caption: String = ""
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (safeProgress > 0f) {
                drawArc(
                    color = fill,
                    startAngle = -90f,
                    sweepAngle = 360f * safeProgress,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            if (caption.isNotBlank()) {
                Text(caption, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Nhãn chuỗi ngày học; dùng emoji hệ thống, không kèm tài sản đồ hoạ nào. */
@Composable
fun StreakPill(streak: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("🔥", fontSize = 14.sp)
            Text(
                "$streak ngày",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color
)

/** Hiệu ứng giấy màu chúc mừng khi hoàn thành phiên; vẽ bằng Canvas, không dùng Lottie/asset. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, pieceCount: Int = 70) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer
    )
    val pieces = remember(pieceCount, palette) {
        val random = java.util.Random(20261023L)
        List(pieceCount) {
            ConfettiPiece(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 3f + random.nextFloat() * 5f,
                color = palette[random.nextInt(palette.size)]
            )
        }
    }
    Canvas(modifier) {
        pieces.forEach { piece ->
            drawCircle(
                color = piece.color,
                radius = piece.radius,
                center = Offset(piece.x * size.width, piece.y * size.height)
            )
        }
    }
}
