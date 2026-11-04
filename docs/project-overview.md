# Pomori: Proje Özeti

## Tek cümlelik tanım
Odak (pomodoro) seansı sırasında gelen bildirimleri sessizce yakalayan, mola başlayınca uygulama bazında gruplanmış bir özet sunan Android uygulaması.

## Çözdüğü problem
Odaklanırken telefon bildirimleri dikkati dağıtır. Bildirimleri tamamen kapatmak ise önemli bir şeyi kaçırma kaygısı yaratır. Pomori seans boyunca bildirimleri tutar, mola gelince "neyi kaçırdım" sorusuna tek ekranda cevap verir.

## Hedef kullanıcı
Çalışırken ya da ders çalışırken telefonuna dikkatini kaptıran öğrenciler, yazılımcılar ve uzaktan çalışanlar.

## Temel özellikler (MVP)
1. **Odak seansı:** Varsayılan 25 dk odak / 5 dk mola. Süreler ayarlanabilir.
2. **Bildirim yakalama:** Seans sırasında gelen bildirimler yakalanır ve Room'da saklanır.
3. **Mola özeti:** Seans bitince uygulama bazında sayılar ve detay listesi: "14 bildirim: WhatsApp (6), Gmail (5), Slack (3)".
4. **İstisna listesi:** Seçilen uygulamalar ve gönderenler bildirimi yakalamaz, normal şekilde gelir.
5. **İstatistikler:** Haftalık odak süresi, tamamlanan seans sayısı, en çok dikkat dağıtan uygulamalar.

## MVP dışı (sonra)
- Odak sırasında uygulama bazlı sessize alma kuralları
- Widget ve Quick Settings tile
- Dışa aktarma (CSV)
- Wear OS eşlikçisi
- Bulut yedekleme

## Bilinen kısıtlar (README'de açıkça yazılacak)
- Yakalanan bildirimler sistemden kaldırıldığı için özetteki öğeye dokununca orijinal uygulamadaki sohbete doğrudan gidilemez. Dokununca ilgili uygulama açılır.
- Bildirim erişim izni hassas bir izindir. Uygulama Play Store politikaları nedeniyle APK olarak GitHub'da dağıtılacak.
- Bildirim içeriği yalnızca cihazda tutulur, ağa gönderilmez, belirlenen süre sonunda silinir (varsayılan 7 gün).

## Başarı ölçütleri (CV için)
- Uygulama arka plana atılsa ve süreç öldürülse bile sayaç doğru süre gösterir.
- Gruplama ve istisna mantığı için en az %80 kapsama sahip unit testler.
- README'de mimari diyagramı, ekran görüntüleri ve "zor kararlar" bölümü.
- Temiz commit geçmişi ve CI (GitHub Actions ile build + test).

## Teknoloji yığını
Kotlin, Jetpack Compose (Material 3), Navigation Compose, Hilt, Coroutines/Flow, Room, DataStore, NotificationListenerService, Foreground Service, JUnit, MockK, Turbine, Compose UI Test.

## Mülakatta anlatılacak 3 hikâye
1. **Sayaç dayanıklılığı:** Sayacı "kalan süreyi azalt" yerine "bitiş zamanını sakla ve ona göre hesapla" mantığıyla kurdum, böylece süreç ölse bile doğru çalışıyor.
2. **İzin deneyimi:** Bildirim erişim iznini kullanıcıya neden istediğimi anlatan bir onboarding akışı yaptım ve izin verilmediğinde uygulamanın bozulmadan çalışmasını sağladım.
3. **Test edilebilir iş mantığı:** Gruplama ve istisna kurallarını Android'den bağımsız saf Kotlin sınıflarında yazdım, böylece hızlıca unit test edebildim.
