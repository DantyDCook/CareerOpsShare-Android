package com.careerops.share

import android.app.Activity
import android.content.Context
import android.content.Intent

object IncomingShareReader {
    fun read(context: Context, incoming: Intent?): JobShareIntake? {
        if (incoming?.action != Intent.ACTION_SEND || incoming.type?.startsWith("text/") != true) {
            return null
        }

        val sharedText = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: incoming.clipData?.let { clip ->
                if (clip.itemCount > 0) {
                    clip.getItemAt(0).coerceToText(context)?.toString()
                } else {
                    null
                }
            }

        return ShareParser.parse(
            incoming.getStringExtra(Intent.EXTRA_SUBJECT),
            sharedText
        )
    }
}

sealed class ShareRouteResult {
    data class Sent(val plan: CareerOpsRoutePlan) : ShareRouteResult()
    data class Failed(
        val reason: String,
        val plan: CareerOpsRoutePlan? = null
    ) : ShareRouteResult()
    data object NotShare : ShareRouteResult()
}

object ShareRouter {
    fun route(
        activity: Activity,
        incoming: Intent?,
        preset: CareerOpsPreset
    ): ShareRouteResult {
        val intake = IncomingShareReader.read(activity, incoming)
            ?: return ShareRouteResult.NotShare
        val plan = CareerOpsRoutePlanner.plan(intake, preset)
        val transport = TransportRegistry.transportFor(plan.destination)
            ?: return ShareRouteResult.Failed(
                "${plan.destination.displayName} transport is not enabled",
                plan
            )

        return when (val result = transport.send(activity, plan.payload, plan.destination)) {
            TransportResult.Sent -> ShareRouteResult.Sent(plan)
            is TransportResult.Failed -> ShareRouteResult.Failed(result.reason, plan)
        }
    }
}
