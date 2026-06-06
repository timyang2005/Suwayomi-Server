/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.manga.impl.epub

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

object EpubMetadata {

    fun generateContainerXml(): String = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""

    fun generateContentOpf(
        config: EpubConfig,
        mangaTitle: String,
        chapters: List<String>,
        coverImage: String?
    ): String {
        val manifestItems = buildString {
            appendLine("    <item id=\"toc\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>")
            appendLine("    <item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>")
            appendLine("    <item id=\"style\" href=\"styles/manga.css\" media-type=\"text/css\"/>")

            if (coverImage != null) {
                appendLine("    <item id=\"cover\" href=\"Text/cover.xhtml\" media-type=\"application/xhtml+xml\"/>")
                appendLine("    <item id=\"cover-img\" href=\"Images/$coverImage\" media-type=\"image/jpeg\" properties=\"cover-image\"/>")
            }

            chapters.forEachIndexed { index, chapter ->
                val id = String.format("ch%03d", index + 1)
                appendLine("    <item id=\"$id\" href=\"Text/$chapter\" media-type=\"application/xhtml+xml\"/>")
            }
        }

        val spineItems = buildString {
            if (coverImage != null) {
                appendLine("    <itemref idref=\"cover\"/>")
            }
            chapters.forEachIndexed { index, _ ->
                val id = String.format("ch%03d", index + 1)
                appendLine("    <itemref idref=\"$id\"/>")
            }
        }

        val modified = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="bookid">urn:uuid:${UUID.randomUUID()}</dc:identifier>
    <dc:title>$mangaTitle</dc:title>
    <dc:language>${config.language}</dc:language>
    <dc:creator>${config.author}</dc:creator>
    <meta property="dcterms:modified">$modified</meta>
  </metadata>
  <manifest>
$manifestItems
  </manifest>
  <spine toc="toc">
$spineItems
  </spine>
</package>"""
    }

    fun generateTocNcx(chapters: List<String>): String {
        val navPoints = chapters.mapIndexed { index, chapter ->
            val id = String.format("navPoint-%d", index + 1)
            val title = chapter.removeSuffix(".xhtml")
            """    <navPoint id="$id" playOrder="${index + 1}">
      <navLabel><text>$title</text></navLabel>
      <content src="Text/$chapter"/>
    </navPoint>"""
        }.joinToString("\n")

        return """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="urn:uuid:${UUID.randomUUID()}"/>
    <meta name="dtb:depth" content="1"/>
    <meta name="dtb:totalPageCount" content="0"/>
    <meta name="dtb:maxPageNumber" content="0"/>
  </head>
  <docTitle><text>Manga</text></docTitle>
  <navMap>
$navPoints
  </navMap>
</ncx>"""
    }

    fun generateNavXhtml(chapters: List<String>): String {
        val navItems = chapters.mapIndexed { index, chapter ->
            val title = chapter.removeSuffix(".xhtml")
            "        <li><a href=\"Text/$chapter\">$title</a></li>"
        }.joinToString("\n")

        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head>
  <title>Table of Contents</title>
</head>
<body>
  <nav epub:type="toc" id="toc">
    <h1>Table of Contents</h1>
    <ol>
$navItems
    </ol>
  </nav>
</body>
</html>"""
    }

    fun generateMangaCss(pageSize: PageSize): String {
        val dimensions = pageSize.toDimensions()
        val width = dimensions?.first ?: 1072
        val height = dimensions?.second ?: 1448

        return """@charset "UTF-8";

@page {
    width: ${width}px;
    height: ${height}px;
    margin: 0;
    padding: 0;
}

body {
    margin: 0;
    padding: 0;
    text-align: center;
    background-color: #000;
}

img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
}

.cover-page img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}
"""
    }

    fun generateChapterXhtml(chapterTitle: String, images: List<String>): String {
        val imgTags = images.joinToString("\n") { img ->
            "    <img src=\"../Images/$img\" alt=\"Page\"/>"
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>$chapterTitle</title>
  <link rel="stylesheet" type="text/css" href="../styles/manga.css"/>
</head>
<body>
$imgTags
</body>
</html>"""
    }

    fun generateCoverXhtml(coverImage: String): String = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Cover</title>
  <link rel="stylesheet" type="text/css" href="../styles/manga.css"/>
</head>
<body class="cover-page">
  <img src="../Images/$coverImage" alt="Cover"/>
</body>
</html>"""
}
