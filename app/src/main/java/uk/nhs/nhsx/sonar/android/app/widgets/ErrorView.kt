/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_error.view.errorDescription
import kotlinx.android.synthetic.main.view_error.view.errorTitle
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.isInversionModeEnabled

class ErrorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) handleInversion(context.isInversionModeEnabled())
    }

    override fun announceForAccessibility(error: CharSequence) {
        val title = context.getString(R.string.error_title)
        super.announceForAccessibility("$title\n$error")
    }

    private fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled)
            setBackgroundColor(context.getColor(R.color.white))
        else
            setBackgroundColor(context.getColor(R.color.error_background))
    }

    private fun initializeViews() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_error, this)
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ErrorView, 0, 0)
            .apply {
                val description: String? = getString(R.styleable.ErrorView_error_description)
                val title: String? = getString(R.styleable.ErrorView_error_title)

                errorDescription.text = description
                title?.let { errorTitle.text = it }
                recycle()
            }
    }
}
