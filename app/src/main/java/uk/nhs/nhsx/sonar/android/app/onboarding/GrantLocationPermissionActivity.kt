/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.paragraphContainer
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import kotlinx.android.synthetic.main.banner.toolbar_info
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.common.ColorInversionAwareActivity
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.openAppSettings
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

open class GrantLocationPermissionActivity :
    ColorInversionAwareActivity(R.layout.activity_edge_case) {

    @Inject
    lateinit var locationHelper: LocationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        if (Build.VERSION.SDK_INT >= 29) {
            edgeCaseTitle.setText(R.string.grant_location_permission_title)
            paragraphContainer.addAllParagraphs(
                getString(R.string.grant_location_permission_rationale_p1),
                getString(R.string.grant_location_permission_rationale_p2),
                getString(R.string.grant_location_permission_rationale_p3),
                getString(R.string.grant_location_permission_rationale_p4)
            )

            setTitle(R.string.grant_location_permission_title)
        } else {
            edgeCaseTitle.setText(R.string.grant_location_permission_title_pre_10)
            paragraphContainer.addAllParagraphs(
                getString(R.string.grant_location_permission_rationale_p1),
                getString(R.string.grant_location_permission_rationale_p2),
                getString(R.string.grant_location_permission_rationale_pre_10_p3),
                getString(R.string.grant_location_permission_rationale_p4)
            )

            setTitle(R.string.grant_location_permission_title_pre_10)
        }

        takeActionButton.setText(R.string.go_to_app_settings)
        takeActionButton.setOnClickListener {
            openAppSettings()
        }

        toolbar_info.setOnClickListener {
            openUrl(URL_INFO)
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationHelper.locationPermissionsGranted()) {
            finish()
        }
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            takeActionButton.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            takeActionButton.setBackgroundResource(R.drawable.button_round_background)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, GrantLocationPermissionActivity::class.java)
    }
}
