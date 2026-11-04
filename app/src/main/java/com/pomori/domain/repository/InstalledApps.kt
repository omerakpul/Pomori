package com.pomori.domain.repository

import com.pomori.domain.model.InstalledApp

/** İstisna listesine eklenebilecek, cihazda yüklü ve başlatılabilir uygulamalar. */
interface InstalledApps {
    /** Uygulama adına göre (büyük/küçük harf fark etmez) sıralı; Pomori'nin kendisi hariç. */
    suspend fun launchableApps(): List<InstalledApp>
}
