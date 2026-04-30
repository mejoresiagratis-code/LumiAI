# LumiAI — Documento de traspaso para nueva sesión de Claude

> Entrega este documento al inicio de una nueva conversación en Claude.ai con el mensaje:
> **"Lee este documento completo y confirma que entiendes el proyecto antes de empezar."**

---

## Quién eres y cómo trabajas

Eres el asistente de desarrollo de **LumiAI**, una app Android de linterna con IA. Llevas meses trabajando en este proyecto con el usuario (Pablo). Conoces el código en profundidad. Tu comportamiento:

- Lees el código antes de modificar nada
- Ejecutas `bash scripts/pre_push_audit.sh` antes de cada push — **0 issues = push, >0 = no push**
- Etiquetas cada versión con `git tag vX.X.X`
- Escribes mensajes de commit detallados explicando causa raíz
- Cuando el usuario sube un ZIP de logs → leerlo inmediatamente, es un log de build
- "Dale" → implementar lo acordado. "Informa primero" → solo analizar, no tocar código

---

## Repositorio

```
URL:    https://github.com/mejoresiagratis-code/LumiAI
Stack:  Kotlin + Jetpack Compose + Hilt + CameraX + DataStore
Token:  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
```

El código está en `/home/claude/LumiAI` cuando trabajas en sesión con herramientas de computador.

---

## Estado actual — v2.4.0 (versionCode 77)

### Versiones recientes
- `v2.4.0` — Free-only launch: Pro modes hidden, onboarding sin Gemini AI, auditor script
- `v2.3.7a` — Restore AutoOffChip deleted by accident + orphaned imports cleanup
- `v2.3.7` — Critical bugs: AdMob IDs, splash logo, permisos, widget sync, AmbientSmart re-read, SleepTimer double-off, Walk torch leak. Room + ML Kit eliminados. BillingRepository singleton fix.
- `v2.3.6` — 4 fixes: restorePurchases, onPaywall en grids, isPro propagado, Color L suffix Canvas
- `v2.3.5` — Iconos Canvas unificados, Pro UX completo, AdMob IDs reales, paywall sin scroll

### Arquitectura — archivos clave
```
feature/flash/FlashScreen.kt          — UI principal, 6 modos Free visibles, Screen mode tabbed
feature/flash/FlashViewModel.kt       — StateFlows, activateMode, hidden mode guard en restore
feature/pro/ProPaywallScreen.kt       — Paywall sin scroll, en español
ui/components/ModePanel.kt            — LumiModeIcon Canvas, AiModeCard filtrado por hidden
core/domain/model/FlashMode.kt        — sealed class con isPro + hidden flags
core/data/repository/FlashRepositoryImpl.kt  — activateMode, torch ordering
core/data/repository/SettingsRepository.kt   — DataStore 25 claves
core/util/StrobeController.kt         — wasActive guard, StrobePattern enum
core/util/AiModeController.kt         — wasActive guard, 8 modos IA (SleepTimer fix, Walk fix)
core/util/FirebaseManager.kt          — object singleton (sin Hilt)
core/di/AppModule.kt                  — Hilt, BillingRepository single instance
scripts/pre_push_audit.sh             — ⬅ EJECUTAR SIEMPRE antes de push (12 checks)
```

### AdMob IDs reales (producción)
```
App ID:       ca-app-pub-7644513562367479~7453103317
Banner:       ca-app-pub-7644513562367479/7748616787
Interstitial: ca-app-pub-7644513562367479/4306165418
Rewarded:     ca-app-pub-7644513562367479/3486984320
```

---

## Auditor pre-push — OBLIGATORIO

```bash
bash scripts/pre_push_audit.sh
```

El script hace 12 checks y termina con `AUDIT PASSED` o `AUDIT FAILED`.
**Nunca pushear con FAILED.** Los checks son:

1. Scope-member imports prohibidos (drawCircle, awaitPointerEvent, etc.)
2. Firebase @Inject (rompe kapt)
3. Color(0xFF…) sin sufijo L en archivos con Canvas
4. AdMob IDs de producción en release (no XXXX)
5. Dependencias muertas (Room, ML Kit)
6. Permisos en manifest (POST_NOTIFICATIONS, ACTIVITY_RECOGNITION, sin WAKE_LOCK)
7. Todos los composables de FlashScreen definidos (AutoOffChip, TopBar, etc.)
8. Imports huérfanos en FlashScreen
9. BillingRepository singleton único en AppModule
10. Sin mención a "Gemini AI" en onboarding
11. Pro modes con flag `hidden`
12. Sin parámetros duplicados en composables

---

## Reglas de compilación críticas

### Imports prohibidos (scope members — no importables)
```kotlin
// ❌ NUNCA importar — son métodos de clase, no extensiones
androidx.compose.ui.input.pointer.awaitPointerEvent
androidx.compose.ui.graphics.drawscope.drawCircle / drawLine / drawPath / drawRoundRect
```

### Firebase — NUNCA Hilt
```kotlin
// ❌ rompe kapt
@Inject lateinit var firebase: FirebaseManager
// ✅ singleton directo
FirebaseManager.init(this)
```

### Firebase ktx — NO existen en BOM 33+
```kotlin
// ❌ firebase-perf-ktx, Firebase.performance, Firebase.remoteConfig (no existen)
// ✅ FirebaseRemoteConfig.getInstance(), FirebasePerformance.getInstance()
```

### Color en Canvas
```kotlin
Color(0xFFFF3B30L)  // ✅ siempre sufijo L en archivos con Canvas
Color(0xFFFF3B30)   // ❌ ambiguo, falla en Compose
```

---

## Modos Flash — estado actual

### Free (6) — siempre visibles
| ID | Nombre | Config |
|---|---|---|
| steady | Continuo | Slider intensidad inline |
| screen | Pantalla | Slider brillo inline, 12 colores, tabs Solid/Hue/Temp/FX |
| morse_custom | Morse | TextField + velocidad ½×/1×/2×/4× |
| strobe | Estroboscopio | Slider Hz inline + burst pattern en ⚙ |
| sos | SOS | Sin ⚙ |
| disco | Disco | Slider BPM inline + tap-tempo en ⚙ |

### Pro (8) — todos `hidden = true` en v2.4.0
Para revelar un modo: cambiar `hidden = true → false` en `FlashMode.kt`, bump versionCode, push.
El tab "AI Modes" reaparece automáticamente cuando `hidden=false` en al menos un modo.

| ID | Nombre | Estado |
|---|---|---|
| smart_brightness | Inteligente | hidden |
| reading_mode | Lectura | hidden (falta fallback para dispositivos sin torch strength API 33+) |
| ambient_smart | Ambiental | hidden (falta fallback) |
| custom_rhythm | Personalizado | hidden (valor cuestionable, repensar) |
| sleep_timer | Sueño | hidden ✅ listo |
| music | Música | hidden ✅ listo — top Pro |
| walk | Caminar | hidden (ACTIVITY_RECOGNITION añadido, pero caso de uso nicho) |
| voice | Voz | hidden ✅ listo — top Pro |

---

## Screen mode — detalle
- UI oculta tras 3s de inactividad (desde última interacción, no desde aparición)
- Back físico apaga Screen mode (BackHandler)
- Botón ✕ en pantalla
- Tabs: Solid (12 colores) / Hue (0-360°) / Temp (2700K-6500K) / FX (Candela, Police, Rainbow, Strobe)
- AnimatedCandle: Canvas con 3 InfiniteTransition

---

## DataStore — 25 claves
```
LAST_MODE, STROBE_HZ, DISCO_BPM, SCREEN_BRIGHT, AUTO_OFF,
DARK_THEME, SHAKE_TOGGLE, KEEP_SCREEN, SEEN_ONBOARDING,
NOTIF_ENABLED, NOTIF_CALLS, NOTIF_MESSAGES, NOTIF_OTHER,
APP_LANGUAGE, TORCH_INTENSITY, MORSE_TEXT, MORSE_SPEED,
SLEEP_MINUTES, MIC_SENSITIVITY, SCREEN_COLOR_ID,
SCREEN_TEXT (pendiente), SCREEN_TAB (pendiente)
```

---

## Roadmap completo

### 🔴 Play Store — pendiente TÚ (sin código)
- [ ] Subir APK release a Play Console y crear ficha de la app
- [ ] Privacy Policy URL pública (Pablo la tiene — subirla a hosting)
- [ ] Declaración de permisos sensibles en Play Console (RECORD_AUDIO, BIND_NOTIFICATION_LISTENER)
- [ ] Firebase: activar Analytics + Crashlytics en consola lumiai-54bbc
- [ ] Remote Config: crear 5 parámetros en Firebase Console
- [ ] Screenshots para Play Store (mínimo 2 por tamaño: teléfono + tablet)
- [ ] `pro_unlock` producto creado en Play Console (para cuando se active Pro)

### 🟡 Sprint siguiente — código (v2.4.x)
- [ ] **Touch targets**: ampliar área táctil de ⚙ e ⓘ a mínimo 40×40dp en todas las tarjetas
- [ ] **ProStatus loading**: shimmer/skeleton mientras billing resuelve (evita flash de UI)
- [ ] **SOS speed selector**: 4 botones inline ½×/1×/2×/4× (ya en StrobeController, solo UI)
- [ ] **restorePurchases()**: forzar re-query al servidor Google Billing (ahora solo re-subscribe al flow)
- [ ] **Interstitial AdMob**: cada 5 cambios de modo (AdManager.preloadInterstitial() ya existe)
- [ ] **Rewarded ad**: "Probar Pro 30 minutos" en paywall

### 🟠 Pro modes — antes de activar (v2.5.x)
- [ ] **ReadingMode fallback**: pulso muy lento (~3s on/off) para dispositivos sin torch strength API 33+
- [ ] **AmbientSmart fallback**: igual + UI informativa del lux detectado
- [ ] **CustomRhythm**: añadir visualización del patrón activo + posiblemente fusionar con otro modo
- [ ] Activar Music (hidden=false) — ya listo, es el mejor modo Pro
- [ ] Activar Voice (hidden=false) — ya listo, segundo mejor
- [ ] Activar SleepTimer (hidden=false) — ya listo

### 🟢 Diseño — rediseño UX/UI (v2.6.x o cuando haya tracción)
- [ ] **Material 3 color tokens**: refactorizar `LumiColor.kt` de hex hardcoded a tokens semánticos
  (`MaterialTheme.colorScheme.surface`, etc.) para que dark/light funcione automáticamente
- [ ] **Light mode real**: actualmente el toggle dark/light existe en DataStore y Settings,
  pero los colores son todos hardcoded Navy. Con tokens M3, el switch funciona solo.
- [ ] **Minimalismo**: reducir densidad de la grid de modos, más espacio, tipografía más grande
- [ ] **Animación FlashButton**: glow pulse cuando está ON, transición más expresiva al ON/OFF
- [ ] **Onboarding visual**: reemplazar emojis por LumiModeIcon Canvas en las páginas

### 🔵 Nuevos modos — ideas validadas (v2.7.x+)
- [ ] **Morse SOS + GPS**: transmite SOS + coordenadas via pantalla. Feature de seguridad viral
- [ ] **Respira**: linterna sigue ritmo de respiración (4s on / 4s off). Mindfulness, único en linternas
- [ ] **Cámara lenta**: strobe calibrado para efecto slow-motion en vídeo (24fps → 12Hz)
- [ ] **LED Scroller**: texto desplazándose en pantalla con la linterna parpadeando en sync
- [ ] **Saved Morse**: 3 mensajes guardados en DataStore para morse rápido

---

## CI/CD

- **Debug APK**: en cada push a `main` (~6 min)
- **Release APK firmado**: solo en tags `v*` (~12 min)
- Keystore en secret `KEYSTORE_BASE64`, alias `lumiai_key`
- Retención: 14 días debug, 90 días release
- Build debug: ~17MB | Build release: ~4.5MB (R8 + minify + shrink)

---

*Actualizado: 30/04/2026 — v2.4.0 (versionCode 77)*
