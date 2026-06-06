/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.manga.impl.epub

import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpubIntegrationTest {

    fun createTestImage(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, height)
        g2d.color = Color.BLACK
        g2d.fillRect(10, 10, width - 20, height - 20)
        g2d.dispose()

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", baos)
        return baos.toByteArray()
    }

    @Test
    fun `full epub generation flow`() {
        val config = EpubConfig(
            title = "Test Manga",
            author = "Test Author",
            pageSize = PageSize.AUTO,
            groupBy = GroupBy.CHAPTER_RANGE,
            chaptersPerBook = 5
        )

        val chapters = listOf(
            ChapterWithImages(
                chapterId = 1,
                title = "Chapter 1",
                images = listOf(
                    ImageData("p1.jpg", createTestImage(100, 100)),
                    ImageData("p2.jpg", createTestImage(100, 100))
                )
            ),
            ChapterWithImages(
                chapterId = 2,
                title = "Chapter 2",
                images = listOf(
                    ImageData("p1.jpg", createTestImage(100, 100))
                )
            )
        )

        val outputDir = Files.createTempDirectory("epub-integration").toFile()
        try {
            val builder = EpubBuilder(config)
            val results = builder.build("Test Manga", chapters, outputDir, null)
            assertEquals(1, results.size)
            assertTrue(results[0].exists())
            assertTrue(results[0].name.endsWith(".epub"))
            assertTrue(results[0].length() > 0)
        } finally {
            outputDir.deleteRecursively()
        }
    }

    @Test
    fun `epub with cover image produces valid file`() {
        val config = EpubConfig(title = "Cover Test", author = "Author", cover = true, groupBy = GroupBy.SINGLE)
        val coverImage = createTestImage(200, 300)
        val chapters = listOf(
            ChapterWithImages(chapterId = 1, title = "Chapter 1", images = listOf(ImageData("page1.jpg", createTestImage(100, 100))))
        )
        val outputDir = Files.createTempDirectory("epub-cover-test").toFile()
        try {
            val builder = EpubBuilder(config)
            val results = builder.build("Cover Test", chapters, outputDir, coverImage)
            assertEquals(1, results.size)
            assertTrue(results[0].exists())
            assertTrue(results[0].length() > 0)
        } finally {
            outputDir.deleteRecursively()
        }
    }

    @Test
    fun `multiple chapters split correctly by CHAPTER_RANGE`() {
        val config = EpubConfig(title = "Split Test", author = "Author", groupBy = GroupBy.CHAPTER_RANGE, chaptersPerBook = 2)
        val chapters = (1..5).map { i ->
            ChapterWithImages(chapterId = i, title = "Chapter $i", images = listOf(ImageData("page1.jpg", createTestImage(100, 100))))
        }
        val outputDir = Files.createTempDirectory("epub-split-test").toFile()
        try {
            val builder = EpubBuilder(config)
            val results = builder.build("Split Test", chapters, outputDir, null)
            assertEquals(3, results.size)
            results.forEach { file ->
                assertTrue(file.exists())
                assertTrue(file.name.endsWith(".epub"))
                assertTrue(file.length() > 0)
            }
        } finally {
            outputDir.deleteRecursively()
        }
    }

    @Test
    fun `epub with Kindle page size`() {
        val config = EpubConfig(title = "Kindle Test", author = "Author", pageSize = PageSize.KINDLE_PW, groupBy = GroupBy.SINGLE)
        val chapters = listOf(
            ChapterWithImages(chapterId = 1, title = "Chapter 1", images = listOf(ImageData("page1.jpg", createTestImage(800, 1200))))
        )
        val outputDir = Files.createTempDirectory("epub-kindle-test").toFile()
        try {
            val builder = EpubBuilder(config)
            val results = builder.build("Kindle Test", chapters, outputDir, null)
            assertEquals(1, results.size)
            assertTrue(results[0].exists())
            assertTrue(results[0].length() > 0)
        } finally {
            outputDir.deleteRecursively()
        }
    }

    @Test
    fun `multiple images per chapter are all included`() {
        val config = EpubConfig(title = "Multi Image", author = "Author", groupBy = GroupBy.SINGLE)
        val chapters = listOf(
            ChapterWithImages(
                chapterId = 1,
                title = "Chapter 1",
                images = (1..10).map { i -> ImageData("page$i.jpg", createTestImage(100, 100)) }
            )
        )
        val outputDir = Files.createTempDirectory("epub-multiimg-test").toFile()
        try {
            val builder = EpubBuilder(config)
            val results = builder.build("Multi Image", chapters, outputDir, null)
            assertEquals(1, results.size)
            assertTrue(results[0].exists())
            assertTrue(results[0].length() > 0)
        } finally {
            outputDir.deleteRecursively()
        }
    }

    @Test
    fun `SINGLE groupBy keeps all chapters in one epub`() {
        val config = EpubConfig(title = "Single Group", author = "Author", groupBy = GroupBy.SINGLE, chaptersPerBook = 2)
        val chapters = (1..5).map { i ->
            ChapterWithImages(chapterId = i, title = "Chapter $i", images = listOf(ImageData("page1.jpg", createTestImage(100, 100))))
        }
        val outputDir = Files.createTempDirectory("epub-single-test").toFile()
        try {
            val builder = EpubBuilder(config)
            val results = builder.build("Single Group", chapters, outputDir, null)
            assertEquals(1, results.size)
            assertTrue(results[0].exists())
        } finally {
            outputDir.deleteRecursively()
        }
    }
}
