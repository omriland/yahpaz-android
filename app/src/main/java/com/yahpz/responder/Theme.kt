package com.yahpz.responder

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yahpz.domain.StampTone

object FieldTheme {
    val page = Color(0xFFF6F8FA)
    val raised = Color(0xFFFFFFFF)
    val sunken = Color(0xFFEDF1F5)
    val textPrimary = Color(0xFF0F1B2D)
    val textSecondary = Color(0xFF445A73)
    val textMuted = Color(0xFF5B6F86)
    val textOnAccent = Color.White
    val accent = Color(0xFF1D4E89)
    val accentHover = Color(0xFF17416E)
    val accentSubtle = Color(0xFFEDF4FB)
    val hairline = Color(0x1F0F1B2D)
    val strong = Color(0x8C0F1B2D)
    val done = Color(0xFF2E7D5B)
    val doneTint = Color(0xFFE3F1EA)
    val alert = Color(0xFFB3382F)
    val alertTint = Color(0xFFF9E9E7)
    val partial = Color(0xFFB07C24)
    val partialTint = Color(0xFFF7EEDC)
    val pending = Color(0xFF1D4E89)
    val draft = Color(0xFF5B6F86)
    val shadow = Color(0x290F1B2D)
}

object CommandTheme {
    val page = Color(0xFF182A47)
    val raised = Color(0xFF213656)
    val sunken = Color(0xFF122036)
    val textPrimary = Color(0xFFF2F6FA)
    val textSecondary = Color(0xFFC3CEDC)
    val textMuted = Color(0xFF9FB0C4)
    val accent = Color(0xFF8FBCEB)
    val accentFill = Color(0xFF2E6CB4)
    val hairline = Color(0x26F2F6FA)
}

private val plex: FontFamily
    @Composable get() = FontFamily(
        Font(R.font.ibm_plex_sans_hebrew_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_hebrew_medium, FontWeight.Medium),
        Font(R.font.ibm_plex_sans_hebrew_semibold, FontWeight.SemiBold),
        Font(R.font.ibm_plex_sans_hebrew_bold, FontWeight.Bold),
    )

private val plexMono: FontFamily
    @Composable get() = FontFamily(Font(R.font.ibm_plex_mono_regular, FontWeight.Normal))

private val suez: FontFamily
    @Composable get() = FontFamily(Font(R.font.suez_one_regular, FontWeight.Normal))

object TypeScale {
    val brand: TextStyle
        @Composable get() = TextStyle(fontFamily = suez, fontSize = 44.sp, color = CommandTheme.textPrimary)
    val title: TextStyle
        @Composable get() = TextStyle(fontFamily = plex, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    val section: TextStyle
        @Composable get() = TextStyle(fontFamily = plex, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    val body: TextStyle
        @Composable get() = TextStyle(fontFamily = plex, fontWeight = FontWeight.Normal, fontSize = 17.sp)
    val bodyStrong: TextStyle
        @Composable get() = TextStyle(fontFamily = plex, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    val label: TextStyle
        @Composable get() = TextStyle(fontFamily = plex, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.13.sp)
    val caption: TextStyle
        @Composable get() = TextStyle(fontFamily = plex, fontWeight = FontWeight.Normal, fontSize = 12.sp)
    val stamp: TextStyle
        @Composable get() = TextStyle(fontFamily = plex, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.72.sp)
    val numeric: TextStyle
        @Composable get() = TextStyle(fontFamily = plexMono, fontWeight = FontWeight.Normal, fontSize = 16.sp)
}

val StampTone.ink: Color
    get() = when (this) {
        StampTone.DONE -> FieldTheme.done
        StampTone.PARTIAL -> FieldTheme.partial
        StampTone.PENDING -> FieldTheme.pending
        StampTone.DRAFT -> FieldTheme.draft
    }

val StampTone.tint: Color
    get() = when (this) {
        StampTone.DONE -> FieldTheme.doneTint
        StampTone.PARTIAL -> FieldTheme.partialTint
        StampTone.PENDING -> FieldTheme.accentSubtle
        StampTone.DRAFT -> FieldTheme.sunken
    }

@Composable
fun yahpazTypography(): Typography {
    val body = TypeScale.body
    return Typography(
        bodyLarge = body,
        bodyMedium = body,
        titleLarge = TypeScale.title,
        labelLarge = TypeScale.label,
    )
}
