import android.graphics.*
import android.graphics.drawable.Drawable
import kotlin.math.abs
import androidx.core.graphics.toColorInt

/**
 * Creates a placeholder drawable for the avatar based on the provided name.
 */
interface AvatarColorProvider {
    val avatarBackgroundColor: Int
}
fun createAvatarPlaceholder(name: String): Drawable {
    val avatarColors = listOf(
        "#c38df0", "#FF5722", "#dbbc79", "#f06e9c",
        "#3F51B5", "#2196F3", "#00BCD4", "#009688",
        "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B"
    )

    // The color is determined here
    val determinedColor = generateColorFromName(name, avatarColors)

    return object : Drawable(), AvatarColorProvider { // Implement the interface
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = determinedColor // Use the stored color
            style = Paint.Style.FILL
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE //text is always white on this background
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        // Getter for the background color
        override val avatarBackgroundColor: Int
            get() = paint.color // Or directly 'determinedColor'

        override fun draw(canvas: Canvas) {
            val bounds = bounds
            val radius = bounds.width() / 2f
            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()

            val gradient = LinearGradient(
                centerX - radius, centerY,
                centerX + radius, centerY,
                Color.parseColor("#6366F1"),
                Color.parseColor("#FF00FF"),
                Shader.TileMode.CLAMP
            )
            borderPaint.shader = gradient
            canvas.drawCircle(centerX, centerY, radius, borderPaint)
            canvas.drawCircle(centerX, centerY, radius - borderPaint.strokeWidth / 2, paint)

            val firstChar = if (name.isNotEmpty()) name[0].uppercaseChar().toString() else "?"
            canvas.drawText(
                firstChar,
                centerX,
                centerY - (textPaint.descent() + textPaint.ascent()) / 2,
                textPaint
            )
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
            textPaint.alpha = alpha
            borderPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
            textPaint.colorFilter = colorFilter
            borderPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}

/**
 * Generates a color based on the provided name.
 */
private fun generateColorFromName(name: String, colors: List<String>): Int {
    val hash = name.hashCode()
    val index = abs(hash % colors.size)
    return colors[index].toColorInt()
}



