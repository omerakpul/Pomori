package com.pomori.ui.digest

import com.pomori.domain.model.AppDigest

data class DigestUiState(
    /** İlk veri gelene kadar true; ekran bu sürede boş durumu göstermemelidir. */
    val isLoading: Boolean = true,
    val total: Int = 0,
    /** Bildirim sayısına göre azalan sıralı. */
    val apps: List<AppDigest> = emptyList(),
    /** Listesi açık olan uygulamaların paket adları. */
    val expanded: Set<String> = emptySet(),
) {
    /** Yükleme bittiği halde hiç bildirim yoksa "bu seansta bildirim gelmedi" ekranı gösterilir. */
    val isEmpty: Boolean get() = !isLoading && total == 0

    fun isExpanded(packageName: String): Boolean = packageName in expanded
}
