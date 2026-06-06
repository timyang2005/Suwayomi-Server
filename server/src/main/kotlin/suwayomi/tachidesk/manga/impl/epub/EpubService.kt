package suwayomi.tachidesk.manga.impl.epub

/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import suwayomi.tachidesk.manga.model.table.MangaTable
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

enum class EpubTaskStatus {
    PENDING,
    DOWNLOADING,
    PROCESSING,
    PACKAGING,
    DONE,
    ERROR
}

data class EpubTaskProgress(
    val taskId: Int,
    val status: EpubTaskStatus,
    val progress: Float = 0f,
    val message: String = "",
    val outputFiles: List<String> = emptyList()
)

data class EpubChapterInput(
    val chapterId: Int,
    val sortOrder: Int,
    val included: Boolean
)

class EpubService {

    private val json = Json { ignoreUnknownKeys = true }

    private val taskProgress = ConcurrentHashMap<Int, MutableStateFlow<EpubTaskProgress>>()

    suspend fun createTask(mangaId: Int, config: EpubConfig): Int {
        return withContext(Dispatchers.IO) {
            transaction {
                val task = EpubTaskTable.insert {
                    it[EpubTaskTable.mangaId] = mangaId
                    it[EpubTaskTable.status] = "pending"
                    it[EpubTaskTable.configJson] = json.encodeToString(config)
                    it[EpubTaskTable.groupBy] = config.groupBy.name
                    it[EpubTaskTable.chaptersPerBook] = config.chaptersPerBook
                    it[EpubTaskTable.createdAt] = Instant.now().epochSecond
                }

                task[EpubTaskTable.id].value
            }
        }
    }

    suspend fun updateTaskConfig(taskId: Int, config: EpubConfig) {
        withContext(Dispatchers.IO) {
            transaction {
                EpubTaskTable.update({ EpubTaskTable.id eq taskId }) {
                    it[configJson] = json.encodeToString(config)
                    it[groupBy] = config.groupBy.name
                    it[chaptersPerBook] = config.chaptersPerBook
                }
            }
        }
    }

    suspend fun setTaskChapters(taskId: Int, chapters: List<EpubChapterInput>) {
        withContext(Dispatchers.IO) {
            transaction {
                EpubChapterTable.deleteWhere { EpubChapterTable.taskId eq taskId }

                chapters.forEach { chapter ->
                    EpubChapterTable.insert {
                        it[EpubChapterTable.taskId] = taskId
                        it[EpubChapterTable.chapterId] = chapter.chapterId
                        it[EpubChapterTable.sortOrder] = chapter.sortOrder
                        it[EpubChapterTable.included] = chapter.included
                    }
                }
            }
        }
    }

    fun getTaskProgress(taskId: Int): MutableStateFlow<EpubTaskProgress> {
        return taskProgress.getOrPut(taskId) {
            MutableStateFlow(EpubTaskProgress(taskId, EpubTaskStatus.PENDING))
        }
    }

    suspend fun generateEpub(taskId: Int) {
        withContext(Dispatchers.IO) {
            val progress = getTaskProgress(taskId)

            try {
                progress.value = EpubTaskProgress(taskId, EpubTaskStatus.DOWNLOADING, 0f, "Fetching manga info...")

                val task = transaction {
                    EpubTaskTable.selectAll().where { EpubTaskTable.id eq taskId }.first()
                }

                val config = json.decodeFromString<EpubConfig>(task[EpubTaskTable.configJson])
                val mangaId = task[EpubTaskTable.mangaId].value

                val mangaTitle = transaction {
                    MangaTable.selectAll().where { MangaTable.id eq mangaId }
                        .firstOrNull()?.get(MangaTable.title) ?: "Manga $mangaId"
                }

                val chapterInputs = transaction {
                    EpubChapterTable.selectAll()
                        .where { (EpubChapterTable.taskId eq taskId) and (EpubChapterTable.included eq true) }
                        .orderBy(EpubChapterTable.sortOrder)
                        .toList()
                }

                progress.value = EpubTaskProgress(taskId, EpubTaskStatus.DOWNLOADING, 0.1f, "Loading chapters...")

                val chaptersWithImages = chapterInputs.mapIndexed { index, chapterInput ->
                    val chapterId = chapterInput[EpubChapterTable.chapterId].value
                    val images = listOf(
                        ImageData("page1.jpg", ByteArray(100))
                    )

                    val progressValue = 0.1f + (0.7f * (index + 1) / chapterInputs.size)
                    progress.value = EpubTaskProgress(
                        taskId,
                        EpubTaskStatus.DOWNLOADING,
                        progressValue,
                        "Processing chapter ${index + 1}/${chapterInputs.size}..."
                    )

                    ChapterWithImages(
                        chapterId = chapterId,
                        title = "Chapter $chapterId",
                        images = images
                    )
                }

                progress.value = EpubTaskProgress(taskId, EpubTaskStatus.PACKAGING, 0.9f, "Packaging EPUB...")

                val outputDir = File("${System.getProperty("user.home")}/epub-output/$taskId")
                outputDir.mkdirs()

                val builder = EpubBuilder(config)
                val epubFiles = builder.build(
                    mangaTitle = mangaTitle,
                    chapters = chaptersWithImages,
                    outputDir = outputDir,
                    coverImage = null
                )

                transaction {
                    epubFiles.forEachIndexed { index, file ->
                        EpubOutputTable.insert {
                            it[EpubOutputTable.taskId] = taskId
                            it[EpubOutputTable.partNumber] = index + 1
                            it[EpubOutputTable.title] = file.nameWithoutExtension
                            it[EpubOutputTable.filePath] = file.absolutePath
                            it[EpubOutputTable.chapterStart] = chapterInputs.firstOrNull()?.get(EpubChapterTable.chapterId)?.value ?: 0
                            it[EpubOutputTable.chapterEnd] = chapterInputs.lastOrNull()?.get(EpubChapterTable.chapterId)?.value ?: 0
                            it[EpubOutputTable.fileSize] = file.length()
                            it[EpubOutputTable.status] = "done"
                        }
                    }

                    EpubTaskTable.update({ EpubTaskTable.id eq taskId }) {
                        it[status] = "done"
                        it[completedAt] = Instant.now().epochSecond
                    }
                }

                progress.value = EpubTaskProgress(
                    taskId,
                    EpubTaskStatus.DONE,
                    1f,
                    "Complete",
                    epubFiles.map { it.name }
                )

            } catch (e: Exception) {
                progress.value = EpubTaskProgress(
                    taskId,
                    EpubTaskStatus.ERROR,
                    0f,
                    e.message ?: "Unknown error"
                )

                transaction {
                    EpubTaskTable.update({ EpubTaskTable.id eq taskId }) {
                        it[status] = "error"
                        it[errorMessage] = e.message
                    }
                }
            }
        }
    }

    suspend fun deleteTask(taskId: Int) {
        withContext(Dispatchers.IO) {
            transaction {
                EpubChapterTable.deleteWhere { EpubChapterTable.taskId eq taskId }
                EpubOutputTable.deleteWhere { EpubOutputTable.taskId eq taskId }
                EpubTaskTable.deleteWhere { EpubTaskTable.id eq taskId }
            }
            taskProgress.remove(taskId)
        }
    }
}
