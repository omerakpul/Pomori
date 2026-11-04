# Pomori - Pomodoro App

Odak (pomodoro) seansı sırasında gelen bildirimleri sessizce yakalayan, mola başlayınca uygulama bazında gruplanmış bir özet sunan Android uygulaması.

> Durum: geliştirme aşamasında. Yol haritası: [docs/roadmap.md](docs/roadmap.md)

## Problem ve çözüm
Odaklanmaya çalışırken telefona düşen her bildirim dikkati böler; bildirimleri tamamen kapatmak ise önemli bir şeyi kaçırma kaygısı yaratır. Pomori, odak seansı boyunca gelen bildirimleri sessizce yakalar ve sistemden kaldırır, sen çalışmaya devam edersin. Mola başladığında kaçırdıkların uygulama bazında gruplanmış tek bir özette karşına çıkar. Acil olması gereken uygulamaları istisna listesine ekleyerek onların bildirimlerinin hiç engellenmemesini sağlayabilirsin.

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
1. [Releases](https://github.com/omerakpul/pomori/releases) sayfasından en son `pomori-vX.Y.apk` dosyasını indir (`debug-latest` ön sürümü imzasızdır, yalnızca test içindir).
2. Telefonda APK'yı aç; istenirse tarayıcın veya dosya yöneticin için "bilinmeyen kaynaklardan yükleme" iznini ver.
3. İlk açılışta bildirim erişimi izni istenir: Ayarlar > Bildirim erişimi ekranında Pomori'yi aç. İzin vermezsen uygulama çalışır ama özet boş kalır.

Gereksinim: Android 8.0 (API 26) ve üzeri.

### Kaynaktan imzalı sürüm üretme
Repo içinde imza anahtarı yoktur. Kendi anahtarınla:

```
keytool -genkeypair -keystore pomori-release.jks -alias pomori -keyalg RSA -keysize 2048 -validity 10000
```

Kök dizine `keystore.properties` dosyası ekle (git tarafından yok sayılır):

```
storeFile=pomori-release.jks
storePassword=...
keyAlias=pomori
keyPassword=...
```

Sonra `./gradlew assembleRelease` imzalı APK'yı `app/build/outputs/apk/release/` altına yazar. Dosya yoksa release derlemesi imzasız çıkar. CI'da aynı değerler `POMORI_KEYSTORE_FILE`, `POMORI_KEYSTORE_PASSWORD`, `POMORI_KEY_ALIAS`, `POMORI_KEY_PASSWORD` ortam değişkenleriyle verilir.

## Testler
```
./gradlew test
```

## Teknoloji
Kotlin, Jetpack Compose (Material 3), Hilt, Room, DataStore, Coroutines/Flow, WorkManager, NotificationListenerService, Foreground Service.
