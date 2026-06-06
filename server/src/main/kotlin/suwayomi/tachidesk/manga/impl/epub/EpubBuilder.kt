/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.manga.impl.epub

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File
import java.nio.file.Files
import java.util.UUID

data class ChapterWithImages(
    val chapterId: Int,
    val title: String,
    val images: List<ImageData>,
)

data class ImageData(
    val fileName: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ImageData
        return fileName == other.fileName && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

class EpubBuilder(private val config: EpubConfig) {

    private val imageProcessor = ImageProcessor(config)

    fun build(
        mangaTitle: String,
        chapters: List<ChapterWithImages>,
        outputDir: File,
        coverImage: ByteArray?,
    ): List<File> {
        val groups = groupChapters(chapters)
        val outputFiles = mutableListOf<File>()

        groups.forEachIndexed { index, group ->
            val epubFile = buildSingleEpub(
                mangaTitle = mangaTitle,
                groupTitle = if (groups.size > 1) "Part ${index + 1}" else mangaTitle,
                chapters = group,
                outputDir = outputDir,
                coverImage = coverImage,
            )
            outputFiles.add(epubFile)
        }

        return outputFiles
    }

    private fun groupChapters(chapters: List<ChapterWithImages>): List<List<ChapterWithImages>> {
        return when (config.groupBy) {
            GroupBy.SINGLE -> listOf(chapters)
            GroupBy.CHAPTER_RANGE -> chapters.chunked(config.chaptersPerBook)
            GroupBy.VOLUME -> chapters.chunked(config.chaptersPerBook)
        }
    }

    private fun buildSingleEpub(
        mangaTitle: String,
        groupTitle: String,
        chapters: List<ChapterWithImages>,
        outputDir: File,
        coverImage: ByteArray?,
    ): File {
        val workDir = Files.createTempDirectory("epub-work").toFile()
        val oebpsDir = File(workDir, "OEBPS")
        val textDir = File(oebpsDir, "Text")
        val imagesDir = File(oebpsDir, "Images")
        val stylesDir = File(oebpsDir, "styles")
        val metaInfDir = File(workDir, "META-INF")

        textDir.mkdirs()
        imagesDir.mkdirs()
        stylesDir.mkdirs()
        metaInfDir.mkdirs()

        try {
            File(workDir, "mimetype").writeText("application/epub+zip")

            File(metaInfDir, "container.xml").writeText(EpubMetadata.generateContainerXml())

            var coverFileName: String? = null
            if (config.cover && coverImage != null) {
                coverFileName = "cover.jpg"
                val processedCover = imageProcessor.process(coverImage)
                File(imagesDir, coverFileName).writeBytes(
                    imageProcessor.toByteArray(processedCover.image, "jpg", config.imageQuality),
                )
            }

            val chapterFiles = mutableListOf<String>()
            chapters.forEachIndexed { chapterIndex, chapter ->
                val chapterFileName = String.format("ch%03d.xhtml", chapterIndex + 1)
                chapterFiles.add(chapterFileName)

                val imageFileNames = mutableListOf<String>()
                chapter.images.forEachIndexed { pageIndex, image ->
                    val imageFileName = String.format("ch%03d_p%03d.jpg", chapterIndex + 1, pageIndex + 1)
                    imageFileNames.add(imageFileName)

                    val processed = imageProcessor.process(image.data)
                    File(imagesDir, imageFileName).writeBytes(
                        imageProcessor.toByteArray(processed.image, "jpg", config.imageQuality),
                    )
                }

                File(textDir, chapterFileName).writeText(
                    EpubMetadata.generateChapterXhtml(chapter.title, imageFileNames),
                )
            }

            if (coverFileName != null) {
                File(textDir, "cover.xhtml").writeText(
                    EpubMetadata.generateCoverXhtml(coverFileName),
                )
            }

            File(stylesDir, "manga.css").writeText(
                EpubMetadata.generateMangaCss(config.pageSize),
            )

            val uuid = UUID.randomUUID().toString()

            File(oebpsDir, "content.opf").writeText(
                EpubMetadata.generateContentOpf(config, mangaTitle, chapterFiles, coverFileName, uuid),
            )

            File(oebpsDir, "toc.ncx").writeText(
                EpubMetadata.generateTocNcx(chapterFiles, uuid),
            )

            File(oebpsDir, "nav.xhtml").writeText(
                EpubMetadata.generateNavXhtml(chapterFiles),
            )

            val epubFileName = sanitizeFileName("$groupTitle.epub")
            val epubFile = File(outputDir, epubFileName)

            val zipFile = ZipFile(epubFile)

            val mimetypeParams = ZipParameters().apply {
                compressionMethod = CompressionMethod.STORE
                fileNameInZip = "mimetype"
            }
            zipFile.addFile(File(workDir, "mimetype"), mimetypeParams)

            listOf(metaInfDir, oebpsDir).forEach { dir ->
                dir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relativePath = workDir.toPath().relativize(file.toPath()).toString()
                        .replace("\\", "/")
                    val params = ZipParameters().apply {
                        compressionMethod = CompressionMethod.DEFLATE
                        fileNameInZip = relativePath
                    }
                    zipFile.addFile(file, params)
                }
            }

            return epubFile
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9.\\-_\\s]"), "_")
    }
}
