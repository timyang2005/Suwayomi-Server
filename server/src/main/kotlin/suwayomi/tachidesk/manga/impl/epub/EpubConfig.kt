/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package suwayomi.tachidesk.manga.impl.epub

import kotlinx.serialization.Serializable

@Serializable
data class EpubConfig(
    val title: String,
    val author: String = "",
    val language: String = "zh",
    val cover: Boolean = true,
    val coverImage: String? = null,
    val pageSize: PageSize = PageSize.AUTO,
    val imageQuality: Int = 95,
    val stripWhitespace: Boolean = true,
    val flattenDirectory: Boolean = false,
    val groupBy: GroupBy = GroupBy.VOLUME,
    val chaptersPerBook: Int = 20,
)

@Serializable
enum class PageSize {
    AUTO,
    KINDLE_PW,
    KINDLE_OASIS,
    KOBO,
    CUSTOM
}

@Serializable
enum class GroupBy {
    VOLUME,
    CHAPTER_RANGE,
    SINGLE
}

fun PageSize.toDimensions(): Pair<Int, Int>? = when (this) {
    PageSize.AUTO -> null
    PageSize.KINDLE_PW -> 1072 to 1448
    PageSize.KINDLE_OASIS -> 1264 to 1680
    PageSize.KOBO -> 1174 to 1566
    PageSize.CUSTOM -> null
}