package suwayomi.tachidesk.manga.impl.epub

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object EpubTaskTable : IntIdTable() {
    val mangaId = integer("manga_id")
    val status = varchar("status", 32).default("pending")
    val configJson = text("config_json")
    val groupBy = varchar("group_by", 32).default("VOLUME")
    val chaptersPerBook = integer("chapters_per_book").default(20)
    val outputPath = varchar("output_path", 1024).nullable()
    val createdAt = long("created_at")
    val completedAt = long("completed_at").nullable()
    val errorMessage = text("error_message").nullable()
}

object EpubChapterTable : IntIdTable() {
    val taskId = integer("task_id")
    val chapterId = integer("chapter_id")
    val sortOrder = integer("sort_order")
    val included = bool("included").default(true)
}

object EpubOutputTable : IntIdTable() {
    val taskId = integer("task_id")
    val partNumber = integer("part_number")
    val title = varchar("title", 512)
    val filePath = varchar("file_path", 1024)
    val chapterStart = integer("chapter_start")
    val chapterEnd = integer("chapter_end")
    val fileSize = long("file_size")
    val status = varchar("status", 32).default("pending")
}
