import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    /*val robotoFontFamily = FontFamily(
        Font("font/roboto_regular.ttf", FontWeight.Normal),
        Font("font/roboto_medium.ttf", FontWeight.Medium),
        Font("font/roboto_bold.ttf", FontWeight.Bold)
    )

    val typography = androidx.compose.material.Typography(
        defaultFontFamily = robotoFontFamily
    )*/

    val lightColors = lightColorScheme(
        primary = Color(0xFF90CAF9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF42A5F5),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFF80DEEA),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF00ACC1),
        onSecondaryContainer = Color.White,
        background = Color(0xFFE3F2FD),
        onBackground = Color.Black,
        surface = Color(0xFFFFFFFF),
        onSurface = Color.Black,
        error = Color(0xFFB00020),
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = lightColors,
        //typography = typography,
        content = content
    )
}
