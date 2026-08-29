package com.careerops.share

import android.content.Context

object AppPreferences {
    private const val FILE_NAME = "careerops_share_preferences"
    private const val KEY_ACTION = "default_action"
    private const val KEY_DESTINATION = "default_destination"

    fun loadAction(context: Context): CareerOpsAction =
        CareerOpsAction.fromId(
            preferences(context).getString(KEY_ACTION, CareerOpsAction.ANALYZE.id)
        )

    fun saveAction(context: Context, action: CareerOpsAction) {
        preferences(context)
            .edit()
            .putString(KEY_ACTION, action.id)
            .apply()
    }

    fun loadDestination(context: Context): DestinationProfile =
        DestinationCatalog.fromId(
            preferences(context).getString(KEY_DESTINATION, DestinationCatalog.CHATGPT.id)
        )

    fun saveDestination(context: Context, destination: DestinationProfile) {
        preferences(context)
            .edit()
            .putString(KEY_DESTINATION, destination.id)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
