package com.tk.quicksearch.shared.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.tk.quicksearch.R

internal fun quickSearchFontFamily(): FontFamily =
    FontFamily(
        Font(R.font.google_sans_regular, FontWeight.Normal),
        Font(R.font.google_sans_medium, FontWeight.Medium),
        Font(R.font.google_sans_semibold, FontWeight.SemiBold),
        Font(R.font.google_sans_bold, FontWeight.Bold),
        Font(R.font.google_sans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.google_sans_medium_italic, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.google_sans_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
        Font(R.font.google_sans_bold_italic, FontWeight.Bold, FontStyle.Italic),
    )
