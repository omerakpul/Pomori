# Pomori: Yol Haritası

Tahmini süre: yaklaşık 3 hafta (günde 2-3 saat). Her adım küçük ve ayrı bir commit olacak şekilde tasarlandı. Bir adımı bitirmeden sonrakine geçmeyin.

## Hafta 0: Hazırlık (1 gün)
- [x] GitHub reposu oluştur, `README.md` iskeleti ve `.gitignore` ekle
- [x] Android Studio'da yeni proje: Empty Activity (Compose), min SDK 26, Kotlin DSL
- [x] Version catalog (`libs.versions.toml`) ile bağımlılıkları tanımla: Compose BOM, Hilt, Room, DataStore, Navigation, Coroutines, test kütüphaneleri
- [x] Hilt'i kur, `@HiltAndroidApp` Application sınıfını ekle
- [x] GitHub Actions: `./gradlew test` ve `assembleDebug` çalıştıran temel CI

## Hafta 1: Sayaç ve foreground service
**Hedef:** Seans başlat/iptal et, kalan süre doğru göster, süreç ölse bile bozulmasın.

- [x] `Clock` arayüzü ve gerçek/sahte uygulaması
- [x] `TimerStateMachine` (saf Kotlin) + kapsamlı unit testler
- [x] Aktif seans DataStore'u (bitiş zamanı)
- [x] `FocusSession` Room varlığı, DAO, repository
- [x] `StartSession`, `EndSession` use case'leri
- [x] `TimerViewModel` + `TimerUiState` (StateFlow)
- [x] `TimerScreen` (Compose): dairesel ilerleme, başlat/iptal düğmeleri
- [x] `FocusForegroundService` + kalıcı bildirim (kalan süre)
- [x] `POST_NOTIFICATIONS` izin akışı (Android 13+)
- [x] Manuel test: uygulamayı son uygulamalardan kapat, tekrar aç, süre doğru mu?
- [x] Seans bitişinde bildirim/ses

**Bitti sayılır:** Unit testler yeşil, gerçek cihazda süreç öldürme testi geçti.

## Hafta 2: Bildirim yakalama ve özet
**Hedef:** Seans sırasında bildirimleri tut, mola başlayınca özet göster.

- [x] Onboarding ekranı: neden bildirim erişimi istiyoruz, hangi veriyi saklıyoruz
- [x] Bildirim erişim iznini kontrol eden yardımcı ve ayarlara yönlendirme
- [x] `CapturedNotificationEntity`, DAO, repository
- [x] `FocusNotificationListener`: filtreleme (ongoing, grup özeti, kendi uygulaması), kaydetme, `cancelNotification`
- [x] `NotificationGrouper` + unit testler
- [x] `GetDigest` use case, `DigestViewModel`
- [x] `DigestScreen`: uygulama bazlı genişleyen liste, boş durum ekranı
- [x] Seans bitişinde özet ekranına yönlendirme
- [x] İzin verilmediğinde zarif davranış (uygulama çalışır, özet boş, uyarı kartı görünür)

**Bitti sayılır:** WhatsApp/Gmail gibi gerçek uygulamalardan gelen bildirimler özette doğru gruplanıyor.

## Hafta 3: İstisnalar, istatistik, cila
**Hedef:** Kullanılabilir ve paylaşılabilir hale getir.

- [x] `ExceptionRuleEntity`, DAO, `ExceptionMatcher` + unit testler
- [x] Ayarlar ekranı: süreler, istisna listesi (yüklü uygulamalardan seçme), saklama süresi
- [x] Listener'a istisna kontrolünü bağla
- [x] `StatsScreen`: haftalık odak süresi, tamamlanan seans, en çok dikkat dağıtan uygulamalar
- [x] WorkManager ile eski bildirimleri silme işi (periyodik)
- [x] Compose UI testleri (sayaç ve özet ekranları)
- [x] Koyu tema, erişilebilirlik (içerik açıklamaları, yeterli kontrast)
- [x] Uygulama ikonu ve adaptive icon
- [ ] Release APK imzalama ve GitHub Releases'e yükleme

## README kontrol listesi (CV için en önemli kısım)
- [ ] Ekran görüntüleri ya da kısa GIF
- [ ] "Problem ve çözüm" bölümü (3-4 cümle)
- [ ] Mimari diyagramı (katmanlar ve veri akışı)
- [ ] "Zor kararlar" bölümü: sayaç tasarımı, listener ve izin deneyimi, test edilebilirlik
- [ ] Bilinen kısıtlar ve gizlilik notu
- [ ] Kurulum ve APK indirme bağlantısı
- [ ] Test çalıştırma komutu ve kapsama rozeti

## CV'ye yazılacak örnek madde
> **Pomori** (Kotlin, Jetpack Compose, Hilt, Room, Coroutines/Flow): Odak seansı sırasında bildirimleri yakalayıp mola özeti sunan Android uygulaması. Süreç ölümüne dayanıklı sayaç (bitiş-zamanı tabanlı durum makinesi), NotificationListenerService ile bildirim yakalama, cihaz içi gizlilik odaklı veri saklama ve %80+ kapsama ile unit testler. GitHub Actions ile CI.

## Claude Project'te nasıl çalışmalı
1. Bu dosyaları Project'in bilgi (Knowledge) bölümüne yükleyin, `project-instructions.md` içeriğini Instructions alanına yapıştırın.
2. Her sohbette tek bir yol haritası adımını yapın. Örnek istek: "Hafta 1, `TimerStateMachine` adımını yapalım. Önce durum ve olay sınıflarını yaz, sonra testleri."
3. Yazılan kodu kendi projenize kopyalayıp çalıştırın, hata çıkarsa hata çıktısını yapıştırın.
4. Adım bitince bu dosyadaki kutuyu işaretleyin ve güncel halini Project'e tekrar yükleyin.
