/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.graphql.subscriptions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import suwayomi.tachidesk.graphql.directives.RequireAuth
import suwayomi.tachidesk.graphql.types.EpubTaskProgressType
import suwayomi.tachidesk.manga.impl.epub.EpubService

class EpubSubscription {

    private val epubService = EpubService()

    @RequireAuth
    fun epubTaskProgress(taskId: Int): Flow<EpubTaskProgressType> {
        return epubService.getTaskProgress(taskId).map { progress ->
            EpubTaskProgressType(
                taskId = progress.taskId,
                status = progress.status.name,
                progress = progress.progress,
                message = progress.message,
                outputFiles = progress.outputFiles
            )
        }
    }
}
