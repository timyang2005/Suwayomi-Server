package suwayomi.tachidesk.manga.impl.epub

/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import suwayomi.tachidesk.manga.model.table.ChapterTable
import suwayomi.tachidesk.manga.model.table.MangaTable

object EpubTaskTable : IntIdTable() {
    val mangaId = reference("manga_id", MangaTable, ReferenceOption.CASCADE).index()
    val status = varchar("status", 32).default("pending")
    val configJson = text("config_json")
    val groupBy = varchar("group_by", 32).default("VOLUME")
    val chaptersPerBook = integer("chapters_per_book").default(20)
    val outputPath = varchar("output_path", 1024).nullable()
    val createdAt = long("created_at").default(0)
    val completedAt = long("completed_at").nullable()
    val errorMessage = text("error_message").nullable()
}

object EpubChapterTable : IntIdTable() {
    val taskId = reference("task_id", EpubTaskTable, ReferenceOption.CASCADE).index()
    val chapterId = reference("chapter_id", ChapterTable, ReferenceOption.CASCADE).index()
    val sortOrder = integer("sort_order")
    val included = bool("included").default(true)
}

object EpubOutputTable : IntIdTable() {
    val taskId = reference("task_id", EpubTaskTable, ReferenceOption.CASCADE).index()
    val partNumber = integer("part_number")
    val title = varchar("title", 512)
    val filePath = varchar("file_path", 1024)
    val chapterStart = integer("chapter_start")
    val chapterEnd = integer("chapter_end")
    val fileSize = long("file_size")
    val status = varchar("status", 32).default("pending")
}