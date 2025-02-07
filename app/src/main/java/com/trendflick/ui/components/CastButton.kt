package com.trendflick.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun CastButton(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val context = LocalContext.current
    
    IconButton(
        onClick = { openCastSettings(context) },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Cast,
            contentDescription = "Cast screen",
            tint = tint
        )
    }
}

private fun openCastSettings(context: Context) {
    val intent = Intent(Settings.ACTION_CAST_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
} 