/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.graphql.types

import suwayomi.tachidesk.manga.impl.epub.EpubConfig

data class EpubTaskType(
    val id: Int,
    val mangaId: Int,
    val status: String,
    val config: EpubConfig,
    val groupBy: String,
    val chaptersPerBook: Int,
    val outputPath: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val errorMessage: String?
)

data class EpubChapterPreviewType(
    val chapterId: Int,
    val title: String,
    val chapterNumber: Float,
    val imageCount: Int,
    val included: Boolean,
    val sortOrder: Int
)

data class EpubOutputType(
    val id: Int,
    val taskId: Int,
    val partNumber: Int,
    val title: String,
    val filePath: String,
    val chapterStart: Int,
    val chapterEnd: Int,
    val fileSize: Long,
    val status: String
)

data class EpubTaskProgressType(
    val taskId: Int,
    val status: String,
    val progress: Float,
    val message: String,
    val outputFiles: List<String>
)
