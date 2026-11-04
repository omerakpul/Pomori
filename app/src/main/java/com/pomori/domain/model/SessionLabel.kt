package com.pomori.domain.model

const val MAX_LABEL_LENGTH = 40

/** Etiketi kırpar, boşsa null yapar, uzunsa [MAX_LABEL_LENGTH] ile keser. */
fun normalizeLabel(raw: String?): String? =
    raw?.trim()?.take(MAX_LABEL_LENGTH)?.trim()?.takeIf { it.isNotEmpty() }
