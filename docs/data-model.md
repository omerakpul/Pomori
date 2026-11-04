# Pomori: Veri Modeli

## Room varlıkları

### FocusSessionEntity
| Alan | Tür | Not |
|---|---|---|
| id | Long (PK, autoGenerate) | |
| type | String (FOCUS / BREAK) | Room 2.3+ enum'u adıyla saklar, TypeConverter gerekmez |
| startedAt | Long | epoch ms |
| plannedDurationMs | Long | |
| endedAt | Long? | Bitmediyse null |
| status | String (RUNNING / COMPLETED / CANCELLED) | |

### CapturedNotificationEntity
| Alan | Tür | Not |
|---|---|---|
| id | Long (PK, autoGenerate) | |
| sessionId | Long (FK → FocusSessionEntity.id, onDelete CASCADE) | index ekleyin |
| packageName | String | |
| appLabel | String | Yakalama anında çözülür |
| title | String? | |
| text | String? | |
| postedAt | Long | epoch ms |

### Şema sürümleri
| Sürüm | Değişiklik |
|---|---|
| 1 | `focus_sessions` |
| 2 | `captured_notifications` eklendi (`MIGRATION_1_2`, `MigrationTest` ile doğrulanır) |

Her şema değişikliğinde sürümü artırın, `Migration` yazın ve `schemas/` altındaki yeni JSON'u commit edin.

### ExceptionRuleEntity
| Alan | Tür | Not |
|---|---|---|
| id | Long (PK, autoGenerate) | |
| type | String (PACKAGE / SENDER) | |
| value | String | Paket adı ya da gönderen adı (başlıkta eşleşir) |

Not: (type, value) için unique index koyun.

## Domain modelleri (saf Kotlin)
```kotlin
enum class SessionType { FOCUS, BREAK }
enum class SessionStatus { RUNNING, COMPLETED, CANCELLED }

data class FocusSession(
    val id: Long = 0,
    val type: SessionType,
    val startedAt: Long,
    val plannedDurationMs: Long,
    val endedAt: Long? = null,
    val status: SessionStatus
)

data class CapturedNotification(
    val id: Long = 0,
    val sessionId: Long,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postedAt: Long
)

data class AppDigest(
    val packageName: String,
    val appLabel: String,
    val count: Int,
    val items: List<CapturedNotification>
)

data class SessionDigest(
    val sessionId: Long,
    val total: Int,
    val apps: List<AppDigest>   // count'a göre azalan sıralı
)

sealed interface ExceptionRule {
    val value: String
    data class Package(override val value: String) : ExceptionRule
    data class Sender(override val value: String) : ExceptionRule
}
```

## DAO'lar (özet)
```kotlin
@Dao
interface SessionDao {
    @Insert suspend fun insert(session: FocusSessionEntity): Long
    @Update suspend fun update(session: FocusSessionEntity)
    @Query("SELECT * FROM focus_sessions WHERE id = :id") suspend fun getById(id: Long): FocusSessionEntity?
    @Query("""
        SELECT * FROM focus_sessions
        WHERE type = 'FOCUS' AND status = 'COMPLETED' AND startedAt >= :from
        ORDER BY startedAt DESC
    """)
    fun observeCompletedSince(from: Long): Flow<List<FocusSessionEntity>>
}

@Dao
interface NotificationDao {
    @Insert suspend fun insert(n: CapturedNotificationEntity)
    @Query("SELECT * FROM captured_notifications WHERE sessionId = :sessionId ORDER BY postedAt DESC")
    fun observeBySession(sessionId: Long): Flow<List<CapturedNotificationEntity>>
    @Query("""
        SELECT packageName, appLabel, COUNT(*) AS cnt
        FROM captured_notifications
        WHERE postedAt >= :from
        GROUP BY packageName
        ORDER BY cnt DESC
        LIMIT :limit
    """)
    fun observeTopDistractors(from: Long, limit: Int): Flow<List<DistractorRow>>
    @Query("DELETE FROM captured_notifications WHERE postedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long): Int
}

@Dao
interface ExceptionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(rule: ExceptionRuleEntity)
    @Delete suspend fun delete(rule: ExceptionRuleEntity)
    @Query("SELECT * FROM exception_rules") fun observeAll(): Flow<List<ExceptionRuleEntity>>
}
```

## DataStore anahtarları

### Ayarlar (Preferences DataStore)
| Anahtar | Tür | Varsayılan |
|---|---|---|
| focus_minutes | Int | 25 |
| break_minutes | Int | 5 |
| retention_days | Int | 7 |
| onboarding_done | Boolean | false |

### Aktif seans (ayrı bir DataStore)
| Anahtar | Tür | Not |
|---|---|---|
| active_session_id | Long | -1 = yok |
| active_end_at | Long | Bitiş zamanı epoch ms |
| active_type | String | FOCUS / BREAK |
| active_planned_ms | Long | Planlanan süre (ilerleme halkası için; Hafta 1'de eklendi) |

## Saf iş mantığı sınıfları

### NotificationGrouper
`fun group(sessionId: Long, items: List<CapturedNotification>): SessionDigest`
- Paket adına göre gruplar, grup içinde en yeniden eskiye sıralar, grupları sayıya göre azalan sıralar. Eşitlikte uygulama adına göre alfabetik.

### ExceptionMatcher
`fun isExcepted(packageName: String, title: String?, rules: List<ExceptionRule>): Boolean`
- Paket kuralı: tam eşleşme.
- Gönderen kuralı: başlıkta büyük/küçük harf duyarsız içerir kontrolü.

## Test edilecek sınır durumları
- Boş bildirim listesi → `total = 0`, boş `apps`.
- Aynı sayıda bildirimi olan iki uygulama → deterministik sıralama.
- Başlığı null olan bildirim ve gönderen kuralı → eşleşmez, hata fırlatmaz.
- Seans iptal edilirse yakalanan bildirimler yine de silinmeden özette gösterilebilir olmalı (karar: iptalde de özet göster).
- `deleteOlderThan` sınır zamanı: tam eşit zaman silinmez.
