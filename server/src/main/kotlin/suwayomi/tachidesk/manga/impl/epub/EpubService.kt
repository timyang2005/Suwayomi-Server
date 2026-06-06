package suwayomi.tachidesk.manga.impl.epub

/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import io.github.oshai.kotlinlogging.KotlinLogging
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
import suwayomi.tachidesk.manga.impl.Page
import suwayomi.tachidesk.manga.impl.util.getChapterCachePath
import suwayomi.tachidesk.manga.model.table.ChapterTable
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

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }
    private val taskProgress = ConcurrentHashMap<Int, MutableStateFlow<EpubTaskProgress>>()

    private fun getExtensionFromMime(mime: String): String = when {
        mime.contains("png") -> "png"
        mime.contains("webp") -> "webp"
        mime.contains("gif") -> "gif"
        else -> "jpg"
    }

    private fun findCachedImage(cacheDir: File, pageIndex: Int): File? {
        val indexStr = String.format("%03d", pageIndex + 1)
        val possibleNames = listOf(
            "$indexStr.jpg", "$indexStr.png", "$indexStr.webp",
            "${pageIndex + 1}.jpg", "${pageIndex + 1}.png"
        )
        return cacheDir.listFiles()?.firstOrNull { file ->
            possibleNames.any { it.equals(file.name, ignoreCase = true) }
        }
    }

    private suspend fun downloadChapterImages(
        mangaId: Int,
        chapterId: Int,
        taskId: Int,
        progress: MutableStateFlow<EpubTaskProgress>,
        totalChapters: Int,
        chapterIndex: Int
    ): Pair<String, List<ImageData>> {
        val chapterEntry = transaction {
            ChapterTable.selectAll().where { ChapterTable.id eq chapterId }.first()
        }
        val pageCount = chapterEntry[ChapterTable.pageCount]
        val chapterName = chapterEntry[ChapterTable.name]
        val isDownloaded = chapterEntry[ChapterTable.isDownloaded]
        val cacheDir = File(getChapterCachePath(mangaId, chapterId))

        logger.debug { "Chapter $chapterId: $chapterName, pages=$pageCount, downloaded=$isDownloaded" }

        val images = mutableListOf<ImageData>()

        for (pageIndex in 0 until pageCount) {
            try {
                var imageData: ByteArray? = null
                var extension = "jpg"

                // L1: 优先检查已下载章节
                if (isDownloaded) {
                    try {
                        val chapterImageHelper = suwayomi.tachidesk.manga.impl.ChapterDownloadHelper
                        val (inputStream, mime) = chapterImageHelper.getImage(mangaId, chapterId, pageIndex)
                        imageData = inputStream.readBytes()
                        inputStream.close()
                        extension = getExtensionFromMime(mime)
                        logger.debug { "L1 hit: chapter $chapterId page $pageIndex from downloads" }
                    } catch (e: Exception) {
                        logger.debug { "L1 miss: chapter $chapterId page $pageIndex - ${e.message}" }
                    }
                }

                // L2: 检查图片缓存目录
                if (imageData == null && cacheDir.exists()) {
                    val cachedFile = findCachedImage(cacheDir, pageIndex)
                    if (cachedFile != null) {
                        imageData = cachedFile.readBytes()
                        extension = cachedFile.extension.ifEmpty { "jpg" }
                        logger.debug { "L2 hit: chapter $chapterId page $pageIndex from cache" }
                    }
                }

                // L3: 从扩展源下载（Page.getPageImage 内置缓存写入）
                if (imageData == null) {
                    val (inputStream, mime) = Page.getPageImage(
                        mangaId = mangaId,
                        chapterId = chapterId,
                        index = pageIndex
                    )
                    imageData = inputStream.readBytes()
                    inputStream.close()
                    extension = getExtensionFromMime(mime)
                    logger.debug { "L3 download: chapter $chapterId page $pageIndex from source" }
                }

                images.add(ImageData("page${pageIndex + 1}.$extension", imageData))

                // 更新进度
                val chapterProgress = (chapterIndex + (pageIndex + 1).toFloat() / pageCount) / totalChapters
                val progressValue = 0.1f + 0.7f * chapterProgress
                progress.value = EpubTaskProgress(
                    taskId,
                    EpubTaskStatus.DOWNLOADING,
                    progressValue.coerceAtMost(0.9f),
                    "Chapter ${chapterIndex + 1}/$totalChapters - Page ${pageIndex + 1}/$pageCount"
                )

            } catch (e: Exception) {
                logger.error(e) { "Failed to download page $pageIndex of chapter $chapterId" }
            }
        }

        return chapterName to images
    }

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
                    val (chapterName, images) = downloadChapterImages(
                        mangaId = mangaId,
                        chapterId = chapterId,
                        taskId = taskId,
                        progress = progress,
                        totalChapters = chapterInputs.size,
                        chapterIndex = index
                    )

                    ChapterWithImages(
                        chapterId = chapterId,
                        title = chapterName,
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
