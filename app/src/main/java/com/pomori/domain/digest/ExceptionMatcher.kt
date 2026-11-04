package com.pomori.domain.digest

import com.pomori.domain.model.ExceptionRule

/** Bir bildirimin istisna kurallarından birine takılıp takılmadığını söyler. Saf fonksiyon. */
object ExceptionMatcher {

    fun isExcepted(packageName: String, title: String?, rules: List<ExceptionRule>): Boolean =
        rules.any { matches(it, packageName, title) }

    private fun matches(rule: ExceptionRule, packageName: String, title: String?): Boolean {
        // Boş kural hiçbir şeyle eşleşmez. Özellikle "".contains("") gibi bir durum her bildirimi
        // istisna sayıp yakalamayı sessizce kapatırdı.
        val needle = rule.value.trim()
        if (needle.isEmpty()) return false
        return when (rule) {
            is ExceptionRule.Package -> needle == packageName
            // Sohbet başlıkları çoğu zaman "Ayşe", bazen "Aile grubu: Ayşe" ya da "Ayşe (3 yeni mesaj)"
            // olur; bu yüzden tam eşitlik değil, başlıkta geçmesi aranır.
            is ExceptionRule.Sender -> title != null && title.contains(needle, ignoreCase = true)
        }
    }
}
