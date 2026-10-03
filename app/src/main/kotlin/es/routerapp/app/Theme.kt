package es.routerapp.app

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive theme: wallpaper-based dynamic colour on Android 12+, a blue palette (matching
 * the launcher icon) on older versions, and the expressive spring-based motion scheme.
 */
@Composable
fun RouterTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (Build.VERSION.SDK_INT >= 31) {
        val ctx = LocalContext.current
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else if (dark) {
        darkColorScheme(
            primary = Color(0xFFA8C8FF), onPrimary = Color(0xFF00315F), primaryContainer = Color(0xFF004786),
            onPrimaryContainer = Color(0xFFD6E3FF), secondaryContainer = Color(0xFF3E4759), tertiaryContainer = Color(0xFF583E5B),
        )
    } else {
        expressiveLightColorScheme().copy(
            primary = Color(0xFF1565C0), primaryContainer = Color(0xFFD6E3FF), onPrimaryContainer = Color(0xFF001B3E),
        )
    }
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = MotionScheme.expressive(), content = content)
}
