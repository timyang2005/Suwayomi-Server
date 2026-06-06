package suwayomi.tachidesk.manga.impl.epub

/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageProcessorTest {

    @Test
    fun `stripWhitespace removes white borders`() {
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, 100, 100)
        g2d.color = Color.BLACK
        g2d.fillRect(25, 25, 50, 50)
        g2d.dispose()

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val imageData = baos.toByteArray()

        val config = EpubConfig(title = "test", stripWhitespace = true)
        val processor = ImageProcessor(config)
        val result = processor.process(imageData)

        assertTrue(result.image.width <= 60)
        assertTrue(result.image.height <= 60)
    }

    @Test
    fun `resize to Kindle PW dimensions`() {
        val image = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val imageData = baos.toByteArray()

        val config = EpubConfig(title = "test", pageSize = PageSize.KINDLE_PW)
        val processor = ImageProcessor(config)
        val result = processor.process(imageData)

        assertEquals(1072, result.image.width)
        assertEquals(1448, result.image.height)
    }

    @Test
    fun `preserves original dimensions in ProcessedImage`() {
        val image = BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB)
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val imageData = baos.toByteArray()

        val config = EpubConfig(title = "test", stripWhitespace = false, pageSize = PageSize.AUTO)
        val processor = ImageProcessor(config)
        val result = processor.process(imageData)

        assertEquals(300, result.originalWidth)
        assertEquals(400, result.originalHeight)
        assertEquals(300, result.image.width)
        assertEquals(400, result.image.height)
    }

    @Test
    fun `no processing when config is default AUTO`() {
        val image = BufferedImage(500, 600, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, 500, 600)
        g2d.color = Color.BLACK
        g2d.fillRect(50, 50, 400, 500)
        g2d.dispose()

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val imageData = baos.toByteArray()

        val config = EpubConfig(title = "test", stripWhitespace = false, pageSize = PageSize.AUTO)
        val processor = ImageProcessor(config)
        val result = processor.process(imageData)

        assertEquals(500, result.image.width)
        assertEquals(600, result.image.height)
    }

    @Test
    fun `toByteArray produces valid JPEG`() {
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        g2d.color = Color.RED
        g2d.fillRect(0, 0, 100, 100)
        g2d.dispose()

        val config = EpubConfig(title = "test")
        val processor = ImageProcessor(config)
        val bytes = processor.toByteArray(image, "jpg", 95)

        assertTrue(bytes.isNotEmpty())
        val reRead = ImageIO.read(bytes.inputStream())
        assertEquals(100, reRead.width)
        assertEquals(100, reRead.height)
    }
}
