package com.ezt.priv.shortvideodownloader.ui.more.settings.customview

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import com.ezt.priv.shortvideodownloader.R

class CustomRatingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var userRating: Int = 0
    private var maximumRating: Int = 5
    private var emptyStarDraw: Drawable? = null
    private var filledStarDraw: Drawable? = null
    private var emptyLastStarDraw: Drawable? = null
    private var filledLastStarDraw: Drawable? = null
    private var starPadding: Int = 0

    private var onRatingChangeHandler: RatingChangeListener? = null

    interface RatingChangeListener {
        fun onRatingChanged(rating: Int)
    }

    init {
        orientation = HORIZONTAL

        val typedAttrArray =
            context.obtainStyledAttributes(attrs, R.styleable.CustomRatingBar, defStyleAttr, 0)

        maximumRating = typedAttrArray.getInt(R.styleable.CustomRatingBar_maxRating, 5)
        emptyStarDraw = typedAttrArray.getDrawable(R.styleable.CustomRatingBar_emptyStarDrawable)
        filledStarDraw = typedAttrArray.getDrawable(R.styleable.CustomRatingBar_filledStarDrawable)
        emptyLastStarDraw =
            typedAttrArray.getDrawable(R.styleable.CustomRatingBar_emptyLastStarDrawable)
        filledLastStarDraw =
            typedAttrArray.getDrawable(R.styleable.CustomRatingBar_filledLastStarDrawable)
        starPadding =
            typedAttrArray.getDimensionPixelSize(R.styleable.CustomRatingBar_starSpacing, 0)

        typedAttrArray.recycle()

        setupRatingViews()
    }

    private fun setupRatingViews() {
        removeAllViews()

        for (i in 0 until maximumRating - 1) {
            val starRate = createStarView(i)
            starRate.setImageDrawable(emptyStarDraw)
            addView(starRate)

            // Apply spacing between stars
            if (starPadding > 0) {
                val layoutParams = starRate.layoutParams as LayoutParams
                layoutParams.marginEnd = starPadding
                starRate.layoutParams = layoutParams
            }
        }

        val lastStarRate = createStarView(maximumRating - 1)
        lastStarRate.setImageDrawable(emptyLastStarDraw)
        addView(lastStarRate)

        updateRating()
    }

    private fun createStarView(index: Int): ImageView {
        val starRateView = ImageView(context)
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        starRateView.layoutParams = layoutParams
        starRateView.setOnClickListener {
            userRating = index + 1
            updateRating()
            onRatingChangeHandler?.onRatingChanged(userRating)
        }
        return starRateView
    }

    private fun updateRating() {
        for (i in 0 until childCount - 1) {
            val starRateView = getChildAt(i) as ImageView
            val drawable = if (i < userRating) filledStarDraw else emptyStarDraw
            starRateView.setImageDrawable(drawable)
        }

        val lastStarRateView = getChildAt(childCount - 1) as ImageView
        val drawable = if (userRating == maximumRating) filledLastStarDraw else emptyLastStarDraw
        lastStarRateView.setImageDrawable(drawable)
    }

    fun setRatingChangeListener(listener: RatingChangeListener) {
        this.onRatingChangeHandler = listener
    }

    fun setRating(rating: Int) {
        this.userRating = rating
        updateRating()
    }

    fun setMaxRating(maxRating: Int) {
        this.maximumRating = maxRating
        setupRatingViews()
    }
}
