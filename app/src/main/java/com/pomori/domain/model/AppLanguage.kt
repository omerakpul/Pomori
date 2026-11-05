package com.pomori.domain.model

/** Uygulama dili. [SYSTEM] telefonun dilini izler; diğerleri sistemden bağımsız sabit bir dil seçer. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    TURKISH("tr"),
    ENGLISH("en"),
}
