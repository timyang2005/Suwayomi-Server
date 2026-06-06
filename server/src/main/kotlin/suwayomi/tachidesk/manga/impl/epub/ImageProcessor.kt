/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.manga.impl.epub

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

data class ProcessedImage(
    val image: BufferedImage,
    val originalWidth: Int,
    val originalHeight: Int
)

class ImageProcessor(private val config: EpubConfig) {

    fun process(imageData: ByteArray): ProcessedImage {
        val original = ImageIO.read(ByteArrayInputStream(imageData))
        val originalWidth = original.width
        val originalHeight = original.height

        var processed = original

        if (config.stripWhitespace) {
            processed = stripWhitespace(processed)
        }

        val targetSize = config.pageSize.toDimensions()
        if (targetSize != null) {
            processed = resize(processed, targetSize.first, targetSize.second)
        }

        return ProcessedImage(
            image = processed,
            originalWidth = originalWidth,
            originalHeight = originalHeight
        )
    }

    private fun stripWhitespace(image: BufferedImage): BufferedImage {
        val width = image.width
        val height = image.height

        var top = 0
        var bottom = height - 1
        var left = 0
        var right = width - 1

        while (top < height && isWhiteRow(image, top)) top++
        while (bottom > top && isWhiteRow(image, bottom)) bottom--
        while (left < width && isWhiteColumn(image, left)) left++
        while (right > left && isWhiteColumn(image, right)) right--

        val margin = 5
        top = maxOf(0, top - margin)
        bottom = minOf(height - 1, bottom + margin)
        left = maxOf(0, left - margin)
        right = minOf(width - 1, right + margin)

        val newWidth = right - left + 1
        val newHeight = bottom - top + 1

        if (newWidth <= 0 || newHeight <= 0) {
            return image
        }

        return image.getSubimage(left, top, newWidth, newHeight)
    }

    private fun isWhiteRow(image: BufferedImage, y: Int): Boolean {
        for (x in 0 until image.width) {
            if (!isWhite(image.getRGB(x, y))) return false
        }
        return true
    }

    private fun isWhiteColumn(image: BufferedImage, x: Int): Boolean {
        for (y in 0 until image.height) {
            if (!isWhite(image.getRGB(x, y))) return false
        }
        return true
    }

    private fun isWhite(rgb: Int): Boolean {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return r > 240 && g > 240 && b > 240
    }

    private fun resize(image: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
        val resized = BufferedImage(targetWidth, targetHeight, image.type)
        val g2d: Graphics2D = resized.createGraphics()

        val scale = minOf(
            targetWidth.toDouble() / image.width,
            targetHeight.toDouble() / image.height
        )
        val scaledWidth = (image.width * scale).toInt()
        val scaledHeight = (image.height * scale).toInt()
        val x = (targetWidth - scaledWidth) / 2
        val y = (targetHeight - scaledHeight) / 2

        g2d.color = Color.BLACK
        g2d.fillRect(0, 0, targetWidth, targetHeight)

        g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null)
        g2d.dispose()

        return resized
    }

    fun toByteArray(image: BufferedImage, format: String = "jpg", quality: Int = 95): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = ImageIO.getImageWritersByFormatName(format).next()

        val params = writer.defaultWriteParam
        if (format == "jpg" || format == "jpeg") {
            params.compressionMode = ImageWriteParam.MODE_EXPLICIT
            params.compressionQuality = quality / 100f
        }

        writer.output = ImageIO.createImageOutputStream(baos)
        writer.write(null, javax.imageio.IIOImage(image, null, null), params)
        writer.dispose()

        return baos.toByteArray()
    }
}
