package com.abaan.wrap.ui.theme

import androidx.compose.ui.graphics.Color
import com.abaan.wrap.shared.model.SpendCategory

/** Fixed mid-tone colours per spending category; white text reads on all of them in light and dark themes. */
fun SpendCategory.color(): Color = when (this) {
    SpendCategory.FUEL -> Color(0xFFE65100)
    SpendCategory.TRANSPORT -> Color(0xFF1E88E5)
    SpendCategory.FOOD -> Color(0xFF43A047)
    SpendCategory.KIT -> Color(0xFF8E24AA)
    SpendCategory.PHONE -> Color(0xFF00897B)
    SpendCategory.SOFTWARE -> Color(0xFF3949AB)
    SpendCategory.PERMITS -> Color(0xFF6D4C41)
    SpendCategory.OTHER -> Color(0xFF757575)
}
