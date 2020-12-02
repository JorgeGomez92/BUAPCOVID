/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.postDelayed
import kotlinx.android.synthetic.main.reference_code_panel.view.copy_content
import kotlinx.android.synthetic.main.reference_code_panel.view.copy_content_label
import kotlinx.android.synthetic.main.reference_code_panel.view.divider
import kotlinx.android.synthetic.main.reference_code_panel.view.referenceCodeErrorView
import kotlinx.android.synthetic.main.reference_code_panel.view.referencePanelContainer
import kotlinx.android.synthetic.main.reference_code_panel.view.reference_code
import uk.nhs.nhsx.sonar.android.app.R

class ReferenceCodePanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.reference_code_panel, this)

        copy_content.setOnClickListener {
            if (!isCopyClicked) setCopyClickListener()
        }
    }

    private var isCopyClicked: Boolean = false

    fun setState(state: ReferenceCodeViewModel.State) {
        when (state) {
            ReferenceCodeViewModel.State.Loading -> handleLoading()
            ReferenceCodeViewModel.State.Error -> handleError()
            is ReferenceCodeViewModel.State.Loaded -> handleLoaded(state.code.value)
        }
    }

    private fun handleLoaded(code: String) {
        divider.background = context.getDrawable(R.color.colorPrimary)
        referencePanelContainer.visibility = View.VISIBLE
        referenceCodeErrorView.visibility = View.GONE
        reference_code.text = code
    }

    private fun handleError() {
        referencePanelContainer.visibility = View.GONE
        referenceCodeErrorView.visibility = View.VISIBLE
    }

    private fun handleLoading() {
        referencePanelContainer.visibility = View.VISIBLE
        referenceCodeErrorView.visibility = View.GONE
        reference_code.text = context.getString(R.string.loading)
    }

    private fun setCopyClickListener() {
        isCopyClicked = true

        getClipboardManager().apply {
            val clip = ClipData.newPlainText("Reference code", reference_code.text)
            setPrimaryClip(clip)
        }

        copy_content_label.text = context.getString(R.string.copied)
        copy_content_label.announceForAccessibility(context.getString(R.string.copied))

        postDelayed(COPIED_TEXT_DISPLAY_DURATION) {
            copy_content_label.text = context.getString(R.string.copy)
            isCopyClicked = false
        }
    }

    private fun getClipboardManager(): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    companion object {
        private const val COPIED_TEXT_DISPLAY_DURATION = 3000L
    }
}
