package com.careerops.share

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent

sealed class TransportResult {
    data object Sent : TransportResult()
    data class Failed(val reason: String) : TransportResult()
}

interface CareerOpsTransport {
    val type: TransportType

    fun send(
        activity: Activity,
        payload: String,
        destination: DestinationProfile
    ): TransportResult
}

object AndroidAppTransport : CareerOpsTransport {
    override val type = TransportType.ANDROID_APP

    override fun send(
        activity: Activity,
        payload: String,
        destination: DestinationProfile
    ): TransportResult {
        val packageName = destination.packageName
            ?: return TransportResult.Failed("Destination package is not configured")

        return try {
            activity.startActivity(
                makeShareIntent(activity, payload).apply {
                    setPackage(packageName)
                }
            )
            TransportResult.Sent
        } catch (_: ActivityNotFoundException) {
            TransportResult.Failed("${destination.displayName} app not found")
        }
    }
}

object AndroidChooserTransport : CareerOpsTransport {
    override val type = TransportType.ANDROID_CHOOSER

    override fun send(
        activity: Activity,
        payload: String,
        destination: DestinationProfile
    ): TransportResult {
        activity.startActivity(
            Intent.createChooser(
                makeShareIntent(activity, payload),
                "Send CareerOps request"
            )
        )
        return TransportResult.Sent
    }
}

object TransportRegistry {
    fun transportFor(destination: DestinationProfile): CareerOpsTransport? =
        when (destination.transportType) {
            TransportType.ANDROID_APP -> AndroidAppTransport
            TransportType.ANDROID_CHOOSER -> AndroidChooserTransport
            TransportType.HTTP_POST -> null
        }
}

private fun makeShareIntent(activity: Activity, payload: String) =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, payload)
        putExtra(Intent.EXTRA_SUBJECT, "CareerOps job request")
        putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(ComponentName(activity, MainActivity::class.java))
        )
    }
