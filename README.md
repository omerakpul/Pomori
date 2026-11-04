# Pomori - Pomodoro App

Odak (pomodoro) seansı sırasında gelen bildirimleri sessizce yakalayan, mola başlayınca uygulama bazında gruplanmış bir özet sunan Android uygulaması.

> Durum: geliştirme aşamasında. Yol haritası: [docs/roadmap.md](docs/roadmap.md)

## Problem ve çözüm
_(Hafta 3'te doldurulacak)_

## Ekran görüntüleri
_(Eklenecek)_

## Mimari
Katmanlı yapı: **ui → domain ← data**, Android'e özgü kod `platform` paketinde. Ayrıntılar: [docs/architecture.md](docs/architecture.md)

## Zor kararlar
- Sayaç tasarımı (bitiş-zamanı tabanlı)
- Bildirim erişimi ve izin deneyimi
- Test edilebilirlik (saf Kotlin domain katmanı)

## Bilinen kısıtlar ve gizlilik
- Bildirim verisi yalnızca cihazda tutulur, ağa gönderilmez (internet izni yok).
- Yakalanan bildirimler sistemden kaldırıldığı için özetten orijinal sohbete doğrudan gidilemez.
- Bildirim erişimi hassas bir izin olduğundan APK olarak GitHub Releases'te dağıtılır.

## Kurulum
_(APK bağlantısı eklenecek)_

## Testler
```
./gradlew test
```

## Teknoloji
Kotlin, Jetpack Compose (Material 3), Hilt, Room, DataStore, Coroutines/Flow, WorkManager, NotificationListenerService, Foreground Service.
