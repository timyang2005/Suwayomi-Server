/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.graphql.queries

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import suwayomi.tachidesk.graphql.directives.RequireAuth
import suwayomi.tachidesk.graphql.types.EpubOutputType
import suwayomi.tachidesk.graphql.types.EpubTaskType
import suwayomi.tachidesk.manga.impl.epub.EpubOutputTable
import suwayomi.tachidesk.manga.impl.epub.EpubTaskTable
import kotlinx.serialization.json.Json

class EpubQuery {

    private val json = Json { ignoreUnknownKeys = true }

    @RequireAuth
    fun epubTask(taskId: Int): EpubTaskType? {
        return transaction {
            EpubTaskTable.selectAll().where { EpubTaskTable.id eq taskId }.firstOrNull()?.let {
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

    @RequireAuth
    fun epubTasks(limit: Int = 50): List<EpubTaskType> {
        return transaction {
            EpubTaskTable.selectAll()
                .orderBy(EpubTaskTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .map {
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

    @RequireAuth
    fun epubOutputs(taskId: Int): List<EpubOutputType> {
        return transaction {
            EpubOutputTable.selectAll()
                .where { EpubOutputTable.taskId eq taskId }
                .orderBy(EpubOutputTable.partNumber)
                .map {
                    EpubOutputType(
                        id = it[EpubOutputTable.id].value,
                        taskId = it[EpubOutputTable.taskId].value,
                        partNumber = it[EpubOutputTable.partNumber],
                        title = it[EpubOutputTable.title],
                        filePath = it[EpubOutputTable.filePath],
                        chapterStart = it[EpubOutputTable.chapterStart],
                        chapterEnd = it[EpubOutputTable.chapterEnd],
                        fileSize = it[EpubOutputTable.fileSize],
                        status = it[EpubOutputTable.status]
                    )
                }
        }
    }
}
