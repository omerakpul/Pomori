# Pomori: Mimari

## Genel yaklaşım
Tek modüllü başlayın (`app`), paket bazlı katmanlama yapın. Proje büyüyünce `core`, `feature-*` modüllerine ayrılabilecek şekilde bağımlılık yönünü baştan doğru tutun: **ui → domain ← data**.

## Paket yapısı
```
com.example.pomori
├── di/                      Hilt modülleri
├── ui/
│   ├── navigation/          NavHost, rotalar
│   ├── timer/               TimerScreen, TimerViewModel, TimerUiState
│   ├── digest/              DigestScreen, DigestViewModel
│   ├── stats/               StatsScreen, StatsViewModel
│   ├── settings/            SettingsScreen (süreler, istisnalar, saklama süresi)
│   ├── onboarding/          İzin açıklama ekranı
│   └── theme/
├── domain/
│   ├── model/               Saf Kotlin veri sınıfları
│   ├── timer/               TimerStateMachine (saf Kotlin)
│   ├── digest/              NotificationGrouper, ExceptionMatcher (saf Kotlin)
│   ├── repository/          Arayüzler
│   └── usecase/             StartSession, EndSession, GetDigest, GetWeeklyStats
├── data/
│   ├── local/
│   │   ├── db/              AppDatabase, DAO'lar, Entity'ler
│   │   └── prefs/           DataStore (ayarlar, aktif seans durumu)
│   └── repository/          Arayüz uygulamaları, mapper'lar
└── platform/
    ├── service/             FocusForegroundService
    ├── notification/        FocusNotificationListener, bildirim kanalları
    └── permission/          İzin kontrol yardımcıları
```

## Katman sorumlulukları
- **domain:** Android'e bağımlı olmayan saf Kotlin. Zamanı ve veri kaynaklarını arayüzlerle dışarıdan alır, böylece JVM unit testleriyle test edilir.
- **data:** Room ve DataStore uygulamaları. Entity ↔ domain modeli dönüşümleri burada.
- **platform:** Android'e özgü her şey (Service, ListenerService, izinler). İş mantığı içermez, domain'i çağırır.
- **ui:** Compose ekranları. ViewModel'ler `StateFlow<UiState>` sunar, olaylar fonksiyon çağrısıyla gelir (tek yönlü veri akışı).

## Sayaç tasarımı (en kritik karar)
Sayaç "her saniye kalan süreyi azalt" şeklinde **çalışmaz**. Bunun yerine:
1. Seans başlarken `endAtEpochMs = now + duration` hesaplanır ve DataStore'a (aktif seans) yazılır.
2. Ekran her tikte `remaining = endAtEpochMs - clock.now()` hesaplar.
3. Süreç öldürülse bile, tekrar açıldığında DataStore'daki bitiş zamanından doğru süre hesaplanır.
4. Seansın bitişi için ayrıca `AlarmManager` ya da foreground service içinde zamanlayıcı kullanılır. Hassas alarm gerekmiyorsa `setAndAllowWhileIdle` yeterlidir.

`Clock` arayüzü enjekte edilir, testte sahte saat kullanılır.

### Durum makinesi
```
Idle ──start(FOCUS)──▶ Focus ──timeUp──▶ BreakPending ──startBreak──▶ Break ──timeUp──▶ Idle
                         │                                                  │
                         └──cancel──▶ Idle                                  └──skip──▶ Idle
```
`TimerStateMachine` saf bir fonksiyondur: `reduce(state, event, now): State`. Tüm geçişler unit testle doğrulanır.

## Bildirim yakalama akışı
1. `FocusNotificationListener` (NotificationListenerService) `onNotificationPosted` çağrısını alır.
2. Aktif bir FOCUS seansı yoksa hiçbir şey yapmaz.
3. `ExceptionMatcher` istisna listesine bakar. İstisnaysa bildirime dokunmaz.
4. Değilse bildirimden paket adı, uygulama adı, başlık, metin ve zaman damgası çıkarılır, `CapturedNotification` olarak Room'a yazılır.
5. Ardından `cancelNotification(key)` ile bildirim sistemden kaldırılır.
6. Ongoing (kalıcı) bildirimler, grup özeti bildirimleri ve kendi uygulamamızın bildirimleri yok sayılır.

Dikkat: Listener'ın ana iş parçacığını bloklamaması için Room yazımı kendi `CoroutineScope`'unda (SupervisorJob + Dispatchers.IO) yapılır.

## Foreground Service
- Seans boyunca çalışır, kalan süreyi gösteren kalıcı bir bildirim yayınlar.
- Android 14+ için manifest'te foreground service türü tanımlanmalı. Bu proje için `specialUse` düşünülebilir, gerekçe manifest özelliğinde yazılır. Güncel kuralları geliştirme sırasında resmi dokümandan doğrulayın.
- Android 13+ için `POST_NOTIFICATIONS` izni çalışma zamanında istenir.

## Bağımlılık enjeksiyonu (Hilt)
- `DatabaseModule`: AppDatabase, DAO'lar
- `DataStoreModule`: Ayar ve aktif seans DataStore'ları
- `RepositoryModule`: Arayüz → uygulama bağlama
- `ClockModule`: `Clock` (gerçek uygulama)
- Service ve Listener sınıflarında `@AndroidEntryPoint` kullanılır.

## Test stratejisi
| Katman | Araç | Ne test edilir |
|---|---|---|
| domain | JUnit, Turbine | Durum makinesi geçişleri, gruplama, istisna eşleştirme |
| data | Room in-memory, Robolectric veya instrumented | DAO sorguları, haftalık istatistik sorgusu |
| ui | Compose UI Test | Sayaç ekranı durumları, özet ekranı listeleme |
| platform | Manuel + instrumented | Listener davranışı gerçek cihazda elle doğrulanır |

## Gizlilik ilkeleri
- Bildirim verisi yalnızca cihazda kalır, internet izni manifest'e **eklenmez**.
- Yedekleme dışı bırakılır (`android:allowBackup="false"` ya da backup rules ile Room hariç tutma).
- Varsayılan 7 gün sonra eski kayıtlar otomatik silinir (WorkManager periyodik iş).
