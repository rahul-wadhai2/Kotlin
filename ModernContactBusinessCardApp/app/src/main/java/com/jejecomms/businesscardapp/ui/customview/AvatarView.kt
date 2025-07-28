package com.jejecomms.businesscardapp.ui.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.BlurMaskFilter
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt

/**
 * A custom View that displays either a circular avatar image or initials as a fallback.
 * It includes a background circle, a gradient border, and a shadow.
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Properties for initials display
    private var initials: String = "RW"
    private var circleColor = "#E0E0E0".toColorInt() // Background color for initials
    private var textColor = Color.BLACK

    // New property for the avatar image
    private var avatarBitmap: Bitmap? = null

    // Border properties
    private var borderWidthDp = 4f // Default border width in DP

    // Paint objects
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = circleColor
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // strokeWidth will be set in onSizeChanged based on borderWidthDp
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        // textSize will be set in onSizeChanged
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#33000000".toColorInt() // 20% transparent black for a subtle shadow
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL) // Blur radius for the shadow
    }

    // Drawing-related dimensions
    private val textBounds = Rect()
    private var viewSize = 0f // The smaller of width/height of the view, ensuring a square avatar
    private var centerX = 0f
    private var centerY = 0f

    // Radii calculations:
    private var drawRadius = 0f // The radius at which the border circle will be drawn (center of stroke)
    private var contentRadius = 0f // Radius of the inner content area (where image or initials are drawn)

    /**
     * Sets the initials to be displayed when no avatar image is available.
     * If an image was previously set, it will be cleared.
     * @param initials The two-letter initials (e.g., "JD"). Will be truncated to 2 chars and uppercased.
     */
    fun setInitials(initials: String) {
        this.initials = initials.take(2).uppercase() // Ensure max 2 chars, uppercase for consistency
        this.avatarBitmap = null // Clear any existing bitmap
        invalidate() // Request a redraw of the view
    }

    /**
     * Sets the Bitmap image to be displayed as the avatar.
     * If null, the initials will be displayed as a fallback.
     * @param bitmap The Bitmap image to display.
     */
    fun setAvatarBitmap(bitmap: Bitmap?) {
        this.avatarBitmap = bitmap
        invalidate() // Request a redraw of the view with the new bitmap or initials
    }

    /**
     * Converts a DP value to pixels based on the device's display density.
     */
    @Px
    private fun Float.convertDpToPx(): Float {
        return this * context.resources.displayMetrics.density
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Use the smaller dimension to ensure the avatar is always a perfect circle
        viewSize = w.toFloat().coerceAtMost(h.toFloat())
        centerX = w / 2f
        centerY = h / 2f

        // Set the border stroke width in pixels
        borderPaint.strokeWidth = borderWidthDp.convertDpToPx()

        // Calculate the radius for drawing the border circle.
        // This radius ensures the OUTER edge of the stroke is at viewSize / 2f.
        drawRadius = (viewSize / 2f) - (borderPaint.strokeWidth / 2f)

        // Calculate the radius for the content (image or initials background).
        // This radius ensures the content fits entirely inside the border.
        contentRadius = drawRadius - (borderPaint.strokeWidth / 2f)

        // Adjust text size to fit within the content circle
        textPaint.textSize = contentRadius * 0.8f // Initials take up 80% of the content circle's radius

        // Setup gradient for the border. It spans the entire view size.
        borderPaint.shader = LinearGradient(
            0f, 0f, viewSize, viewSize,
            ContextCompat.getColor(context, android.R.color.holo_purple), // Start color
            ContextCompat.getColor(context, android.R.color.holo_blue_light), // End color
            Shader.TileMode.CLAMP // Do not repeat the gradient
        )
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw the shadow layer first. It should correspond to the overall view shape.
        // The shadow should be drawn slightly inside the outer boundary to avoid clipping,
        // and its radius should be related to the `drawRadius` of the border.
        // Subtracting shadowPaint.maskFilter's radius (15f) or a proportion of it from drawRadius
        // might be needed for perfect shadow containment, but outerRadius - 2 was a simple approach.
        // Let's use drawRadius for the shadow's path, as it's the center of the border.
        canvas.drawCircle(centerX, centerY + 5, drawRadius, shadowPaint)

        // 2. Draw the main content: either the avatar image or the initials.
        if (avatarBitmap != null) {
            // If an image is provided, draw it within the content circle.
            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val shader = BitmapShader(avatarBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

            val bitmapWidth = avatarBitmap!!.width.toFloat()
            val bitmapHeight = avatarBitmap!!.height.toFloat()
            val contentDiameter = contentRadius * 2 // Diameter of the content circle

            // Calculate scale to make the bitmap fill the content circle.
            // We take the maximum of width/height scales to ensure the circle is fully covered,
            // which might result in some cropping of the bitmap.
            val scale = maxOf(contentDiameter / bitmapWidth, contentDiameter / bitmapHeight)

            val matrix = Matrix()
            matrix.setScale(scale, scale)

            // Calculate translation to center the scaled bitmap within the content circle.
            val scaledBitmapWidth = bitmapWidth * scale
            val scaledBitmapHeight = bitmapHeight * scale
            val translateX = centerX - (scaledBitmapWidth / 2)
            val translateY = centerY - (scaledBitmapHeight / 2)
            matrix.postTranslate(translateX, translateY)

            shader.setLocalMatrix(matrix) // Apply the scaling and translation to the shader
            imagePaint.shader = shader // Set the shader to the paint
            canvas.drawCircle(centerX, centerY, contentRadius, imagePaint) // Draw the image as a circle

        } else {
            // If no image is provided, draw the background circle for initials.
            canvas.drawCircle(centerX, centerY, contentRadius, circlePaint)

            // Calculate text position to center it vertically within the circle.
            textPaint.getTextBounds(initials, 0, initials.length, textBounds)
            val textY = centerY - textBounds.exactCenterY()

            // Draw the initials.
            canvas.drawText(initials, centerX, textY, textPaint)
        }

        // 3. Draw the gradient border. This is drawn on top of the content.
        // The border is centered on `drawRadius`, ensuring its outer edge is within view bounds.
        canvas.drawCircle(centerX, centerY, drawRadius, borderPaint)
    }
}