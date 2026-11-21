package com.example.threatlens.ui.theme

import androidx.annotation.DimenRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.TextUnit

/**
 * Resolves an sp [TextUnit] from a dimension resource defined in `dimens.xml`.
 */
@Composable
fun textDimensionResource(@DimenRes id: Int): TextUnit {
    val density = LocalDensity.current
    val dpValue = dimensionResource(id)
    return with(density) { dpValue.toSp() }
}
