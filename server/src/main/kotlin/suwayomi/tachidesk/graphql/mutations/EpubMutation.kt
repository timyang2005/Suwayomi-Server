/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.graphql.mutations

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import suwayomi.tachidesk.graphql.directives.RequireAuth
import suwayomi.tachidesk.graphql.types.EpubTaskType
import suwayomi.tachidesk.manga.impl.epub.EpubChapterInput
import suwayomi.tachidesk.manga.impl.epub.EpubConfig
import suwayomi.tachidesk.manga.impl.epub.EpubService
import suwayomi.tachidesk.manga.impl.epub.EpubTaskTable
import kotlinx.serialization.json.Json

class EpubMutation {

    private val epubService = EpubService()
    private val json = Json { ignoreUnknownKeys = true }

    data class CreateEpubTaskInput(
        val clientMutationId: String? = null,
        val mangaId: Int,
        val config: EpubConfig
    )

    data class EpubTaskPayload(
        val clientMutationId: String?,
        val epubTask: EpubTaskType
    )

    @RequireAuth
    suspend fun createEpubTask(input: CreateEpubTaskInput): EpubTaskPayload {
        val taskId = epubService.createTask(input.mangaId, input.config)
        return EpubTaskPayload(
            clientMutationId = input.clientMutationId,
            epubTask = getTask(taskId)
        )
    }

    data class UpdateEpubTaskInput(
        val clientMutationId: String? = null,
        val taskId: Int,
        val config: EpubConfig
    )

    @RequireAuth
    suspend fun updateEpubTask(input: UpdateEpubTaskInput): EpubTaskPayload {
        epubService.updateTaskConfig(input.taskId, input.config)
        return EpubTaskPayload(
            clientMutationId = input.clientMutationId,
            epubTask = getTask(input.taskId)
        )
    }

    data class SetEpubChaptersInput(
        val clientMutationId: String? = null,
        val taskId: Int,
        val chapters: List<EpubChapterInput>
    )

    @RequireAuth
    suspend fun setEpubChapters(input: SetEpubChaptersInput): EpubTaskPayload {
        epubService.setTaskChapters(input.taskId, input.chapters)
        return EpubTaskPayload(
            clientMutationId = input.clientMutationId,
            epubTask = getTask(input.taskId)
        )
    }

    data class GenerateEpubInput(
        val clientMutationId: String? = null,
        val taskId: Int
    )

    @RequireAuth
    suspend fun generateEpub(input: GenerateEpubInput): EpubTaskPayload {
        epubService.generateEpub(input.taskId)
        return EpubTaskPayload(
            clientMutationId = input.clientMutationId,
            epubTask = getTask(input.taskId)
        )
    }

    data class DeleteEpubTaskInput(
        val clientMutationId: String? = null,
        val taskId: Int
    )

    data class DeleteEpubTaskPayload(
        val clientMutationId: String?,
        val success: Boolean
    )

    @RequireAuth
    suspend fun deleteEpubTask(input: DeleteEpubTaskInput): DeleteEpubTaskPayload {
        epubService.deleteTask(input.taskId)
        return DeleteEpubTaskPayload(
            clientMutationId = input.clientMutationId,
            success = true
        )
    }

    private fun getTask(taskId: Int): EpubTaskType {
        return transaction {
            EpubTaskTable.selectAll().where { EpubTaskTable.id eq taskId }.first().let {
                EpubTaskType(
                    id = it[EpubTaskTable.id].value,
                    mangaId = it[EpubTaskTable.mangaId].value,
                    status = it[EpubTaskTable.status],
                    config = json.decodeFromString(it[EpubTaskTable.configJson]),
                    groupBy = it[EpubTaskTable.groupBy],
                    chaptersPerBook = it[EpubTaskTable.chaptersPerBook],
                    outputPath = it[EpubTaskTable.outputPath],
                    createdAt = it[EpubTaskTable.createdAt],
                    completedAt = it[EpubTaskTable.completedAt],
                    errorMessage = it[EpubTaskTable.errorMessage]
                )
            }
        }
    }
}
