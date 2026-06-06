/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.manga.impl.epub

import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpubBuilderTest {

    @Test
    fun `build creates valid epub file`() {
        val config = EpubConfig(title = "Test Manga", author = "Author")
        val builder = EpubBuilder(config)

        val tempDir = Files.createTempDirectory("epub-test").toFile()
        val outputDir = File(tempDir, "output")
        outputDir.mkdirs()

        val image = createTestImage()
        val chapters = listOf(
            ChapterWithImages(
                chapterId = 1,
                title = "Chapter 1",
                images = listOf(
                    ImageData("page1.jpg", image),
                    ImageData("page2.jpg", image),
                ),
            ),
        )

        val result = builder.build(
            mangaTitle = "Test Manga",
            chapters = chapters,
            outputDir = outputDir,
            coverImage = null,
        )

        assertEquals(1, result.size)
        assertTrue(result[0].exists())
        assertTrue(result[0].name.endsWith(".epub"))
        assertTrue(result[0].length() > 0)

        tempDir.deleteRecursively()
    }

    @Test
    fun `build with cover image`() {
        val config = EpubConfig(title = "Test Manga", author = "Author", cover = true)
        val builder = EpubBuilder(config)

        val tempDir = Files.createTempDirectory("epub-test").toFile()
        val outputDir = File(tempDir, "output")
        outputDir.mkdirs()

        val image = createTestImage()
        val chapters = listOf(
            ChapterWithImages(
                chapterId = 1,
                title = "Chapter 1",
                images = listOf(ImageData("page1.jpg", image)),
            ),
        )

        val result = builder.build(
            mangaTitle = "Test Manga",
            chapters = chapters,
            outputDir = outputDir,
            coverImage = image,
        )

        assertEquals(1, result.size)
        assertTrue(result[0].exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun `build with multiple chapters`() {
        val config = EpubConfig(title = "Test Manga", author = "Author")
        val builder = EpubBuilder(config)

        val tempDir = Files.createTempDirectory("epub-test").toFile()
        val outputDir = File(tempDir, "output")
        outputDir.mkdirs()

        val image = createTestImage()
        val chapters = (1..5).map { i ->
            ChapterWithImages(
                chapterId = i,
                title = "Chapter $i",
                images = listOf(ImageData("page1.jpg", image)),
            )
        }

        val result = builder.build(
            mangaTitle = "Test Manga",
            chapters = chapters,
            outputDir = outputDir,
            coverImage = null,
        )

        assertEquals(1, result.size)
        assertTrue(result[0].exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun `groupBy SINGLE keeps all chapters in one epub`() {
        val config = EpubConfig(title = "Test Manga", groupBy = GroupBy.SINGLE, chaptersPerBook = 2)
        val builder = EpubBuilder(config)

        val tempDir = Files.createTempDirectory("epub-test").toFile()
        val outputDir = File(tempDir, "output")
        outputDir.mkdirs()

        val image = createTestImage()
        val chapters = (1..5).map { i ->
            ChapterWithImages(
                chapterId = i,
                title = "Chapter $i",
                images = listOf(ImageData("page1.jpg", image)),
            )
        }

        val result = builder.build(
            mangaTitle = "Test Manga",
            chapters = chapters,
            outputDir = outputDir,
            coverImage = null,
        )

        assertEquals(1, result.size)

        tempDir.deleteRecursively()
    }

    @Test
    fun `groupBy CHAPTER_RANGE splits into multiple epubs`() {
        val config = EpubConfig(title = "Test Manga", groupBy = GroupBy.CHAPTER_RANGE, chaptersPerBook = 2)
        val builder = EpubBuilder(config)

        val tempDir = Files.createTempDirectory("epub-test").toFile()
        val outputDir = File(tempDir, "output")
        outputDir.mkdirs()

        val image = createTestImage()
        val chapters = (1..5).map { i ->
            ChapterWithImages(
                chapterId = i,
                title = "Chapter $i",
                images = listOf(ImageData("page1.jpg", image)),
            )
        }

        val result = builder.build(
            mangaTitle = "Test Manga",
            chapters = chapters,
            outputDir = outputDir,
            coverImage = null,
        )

        assertEquals(3, result.size)
        result.forEach { assertTrue(it.exists()) }

        tempDir.deleteRecursively()
    }

    private fun createTestImage(): ByteArray {
        val image = java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        g2d.color = java.awt.Color.RED
        g2d.fillRect(0, 0, 100, 100)
        g2d.dispose()

        val baos = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(image, "jpg", baos)
        return baos.toByteArray()
    }
}
