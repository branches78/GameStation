package com.swordfish.lemuroid.app.shared.covers

import coil.decode.DataSource
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.transition.TransitionTarget

/**
 * Crossfades only images that were just downloaded. Covers read from the memory or disk cache
 * are shown immediately, so browsing doesn't look like everything is being downloaded again.
 */
object NetworkOnlyCrossfadeFactory : Transition.Factory {
    private val crossfadeFactory = CrossfadeTransition.Factory()

    override fun create(
        target: TransitionTarget,
        result: ImageResult,
    ): Transition {
        if (result is SuccessResult && result.dataSource != DataSource.NETWORK) {
            return Transition.Factory.NONE.create(target, result)
        }
        return crossfadeFactory.create(target, result)
    }
}
