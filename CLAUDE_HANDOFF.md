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
Stack:  Kotlin 1.9 + Jetpack Compose + Material 3 + Hilt + CameraX + DataStore
Token:  (NUNCA commitear credenciales — usar $GITHUB_TOKEN)
```

El código está en `/home/claude/LumiAI` cuando trabajas en sesión con herramientas de computador.

---

## Estado actual — v2.5.8 (versionCode 88)

### Versiones recientes

- `v2.5.7` / versionCode 87 — **estado actual** · Music + Voice + SleepTimer activados
- `v2.5.6` / versionCode 86 — base anterior
- `v2.6.4` — último tag CI generado automáticamente (mismo código base de v2.5.x)
- `v2.5.x` — sprint Pro modes: AiModeController, MusicBeatDetector, ModeConfigScreen añadidos
- `v2.5.0` — servicios del sistema: FlashTileService, FlashNotificationService, BootReceiver
- `v2.4.x` — widget home screen: FlashWidgetReceiver, PowerArcWidget
- `v2.4.0` — Free-only launch, todos los Pro modes en hidden=true

### Arquitectura — archivos clave

```
com/lumiai/flashlight/
├── LumiAIApp.kt
├── MainActivity.kt
├── core/
│   ├── data/repository/
│   │   ├── BillingRepository.kt + Impl   Google Play Billing 7.x
│   │   ├── FlashRepository.kt + Impl     CameraX torch
│   │   ├── SettingsRepository.kt         DataStore (22 claves)
│   │   └── BatteryRepository.kt
│   ├── di/
│   │   ├── AppModule.kt
│   │   ├── AdManager.kt                  AdMob interstitial/rewarded
│   │   └── TorchControllerEntryPoint.kt
│   ├── domain/model/
│   │   ├── FlashMode.kt                  ← CLAVE: sealed class, flags isPro + hidden
│   │   ├── ProStatus.kt
│   │   └── UserSettings.kt
│   ├── torch/
│   │   ├── TorchController.kt            mutex + StateFlow del estado real del LED
│   │   ├── TorchHardware.kt
│   │   └── Camera2TorchHardware.kt
│   └── util/
│       ├── AiModeController.kt           controla los 8 modos Pro IA
│       ├── MusicBeatDetector.kt          RMS beat detection via micrófono
│       ├── EnergyEstimator.kt            estimación de batería por modo
│       ├── StrobeController.kt
│       ├── MorseEncoder.kt
│       ├── ShakeDetector.kt
│       ├── FirebaseManager.kt            singleton (NO Hilt)
│       └── LanguageManager.kt
├── feature/
│   ├── flash/
│   │   ├── FlashScreen.kt                pantalla principal
│   │   ├── FlashViewModel.kt
│   │   ├── ModeConfigScreen.kt           config full-screen por modo
│   │   ├── AutoOffTimer.kt
│   │   └── ScreenColor.kt
│   ├── onboarding/OnboardingScreen.kt
│   ├── pro/ProPaywallScreen.kt
│   └── settings/SettingsScreen.kt + ViewModel
├── service/
│   ├── FlashTileService.kt               Quick Settings tile
│   ├── FlashNotificationService.kt       flash en llamadas/mensajes
│   ├── BootReceiver.kt
│   └── NotificationFlashController.kt
├── ui/
│   ├── components/
│   │   ├── FlashButton.kt
│   │   ├── ModePanel.kt
│   │   ├── AdBanner.kt
│   │   ├── LumiIcons.kt
│   │   └── PowerArcWidget.kt
│   ├── navigation/LumiNavHost.kt + NavRoutes.kt
│   └── theme/Color.kt + Theme.kt + Type.kt
└── widget/
    └── FlashWidgetReceiver.kt
```

### AdMob IDs reales (producción)

```
App ID:       ca-app-pub-7644513562367479~7453103317
Banner:       ca-app-pub-7644513562367479/7748616787
Interstitial: ca-app-pub-7644513562367479/4306165418
Rewarded:     ca-app-pub-7644513562367479/3486984320
```

---

## Auditor pre-push — OBLIGATORIO (16 checks)

```bash
bash scripts/pre_push_audit.sh
```

16 checks. Termina con `AUDIT PASSED` o `AUDIT FAILED`. **Nunca pushear con FAILED.**

---

## Reglas de compilación críticas

```kotlin
// ❌ NUNCA importar — scope members
androidx.compose.ui.input.pointer.awaitPointerEvent
androidx.compose.ui.graphics.drawscope.drawCircle / drawLine / drawPath / drawRoundRect

// ❌ Firebase via Hilt — rompe kapt
@Inject lateinit var firebase: FirebaseManager
// ✅ singleton directo
FirebaseManager.init(this)

// ❌ firebase-perf-ktx no existe en BOM 33+
// ✅ FirebasePerformance.getInstance()

// Canvas: siempre sufijo L
Color(0xFFFF3B30L)  // ✅
Color(0xFFFF3B30)   // ❌
```

### AiModeController — reglas de ciclo de vida

1. Cada `start*()` llama a `stop()` primero
2. Todo modo llama `setTorch(true)` inmediatamente
3. `stop()` desregistra TODOS los sensor listeners
4. READ y AMBIENT son modos "steady" — sin pulso visible

---

## Modos Flash — estado actual

### Free (6) — siempre visibles

| ID | Nombre | Config |
|---|---|---|
| steady | Continuo | Slider intensidad inline |
| screen | Pantalla | Slider brillo, 12 colores, tabs Solid/Hue/Temp/FX |
| morse_custom | Morse | TextField + velocidad ½×/1×/2×/4× |
| strobe | Estroboscopio | Slider Hz + burst pattern en ⚙ |
| sos | SOS | Selector velocidad en ⚙ |
| disco | Disco | Slider BPM + tap-tempo en ⚙ |

### Pro (8)

| ID | Nombre | Estado |
|---|---|---|
| smart_brightness | Inteligente | hidden=true — falta fallback sin sensor luz |
| reading_mode | Lectura | hidden=true — falta fallback sin torch strength API 33+ |
| ambient_smart | Ambiental | hidden=true — falta fallback + UI lux |
| custom_rhythm | Personalizado | hidden=true — tap-to-record implementado ✅ |
| sleep_timer | Sueño | **✅ ACTIVO (hidden=false)** |
| music | Música | **✅ ACTIVO (hidden=false)** — top Pro |
| walk | Caminar | hidden=true — ACTIVITY_RECOGNITION pendiente en manifest |
| voice | Voz | **✅ ACTIVO (hidden=false)** |

---

## DataStore — 22 claves

```
LAST_MODE, STROBE_HZ, DISCO_BPM, SCREEN_BRIGHT, AUTO_OFF,
DARK_THEME, SHAKE_TOGGLE, KEEP_SCREEN, SEEN_ONBOARDING,
NOTIF_ENABLED, NOTIF_CALLS, NOTIF_MESSAGES, NOTIF_OTHER,
APP_LANGUAGE, TORCH_INTENSITY, MORSE_TEXT, MORSE_SPEED,
SLEEP_MINUTES, MIC_SENSITIVITY, SCREEN_COLOR_ID,
SCREEN_TEXT, CUSTOM_PATTERN
```

---

## Servicios del sistema

| Servicio | Función | Permiso |
|---|---|---|
| FlashTileService | Quick Settings tile | BIND_QUICK_SETTINGS_TILE |
| FlashNotificationService | Flash en llamadas/mensajes | BIND_NOTIFICATION_LISTENER_SERVICE |
| BootReceiver | Restaura estado al arrancar | RECEIVE_BOOT_COMPLETED |
| FlashWidgetReceiver | Widget home screen | AppWidgetProvider |

---

## Roadmap

### 🔴 Play Store — pendiente Pablo

- [ ] Subir APK release a Play Console y crear ficha
- [ ] **Privacy Policy** en `mejoresiagratis.com/lumiai-privacy` (URL ya en SettingsScreen)
- [ ] Declaración de permisos sensibles (RECORD_AUDIO, BIND_NOTIFICATION_LISTENER)
- [ ] Firebase: activar Analytics + Crashlytics en consola lumiai-54bbc
- [ ] Remote Config: crear 5 parámetros en Firebase Console
- [ ] Screenshots para Play Store
- [ ] `pro_unlock` producto creado en Play Console (€2,99)

### 🟡 Sprint v2.6.x — código

- [ ] **Touch targets**: ⚙ e ⓘ a mínimo 40×40dp en todas las tarjetas
- [ ] **ProStatus loading**: shimmer/skeleton mientras billing resuelve
- [ ] **restorePurchases()**: forzar re-query al servidor Google Billing
- [ ] **Interstitial AdMob**: cada 5 cambios de modo (AdManager.preloadInterstitial() existe)
- [ ] **Rewarded ad**: "Probar Pro 30 minutos" en paywall
- [ ] **RECORD_AUDIO + ACTIVITY_RECOGNITION**: añadir al manifest antes de activar Music/Voice/Walk en producción si Play Store lo requiere

### 🟠 Pro modes — pendientes de activar

- [ ] **CustomRhythm** (`hidden=false`) — tap-to-record implementado ✅
- [ ] **ReadingMode fallback** — pulso lento para dispositivos sin torch strength API 33+
- [ ] **AmbientSmart fallback** — + UI informativa del lux
- [ ] **Walk** — añadir ACTIVITY_RECOGNITION al manifest primero

### 🟢 Diseño (v2.8.x)

- [ ] Material 3 color tokens — refactorizar LumiColor.kt de hex hardcoded
- [ ] Light mode real — colores hardcoded Navy, tokens M3 lo arreglan solos
- [ ] Animación FlashButton: glow pulse cuando ON

### 🔵 Nuevos modos

- [ ] **Morse SOS + GPS** — feature de seguridad viral
- [ ] **Respira** — 4s on / 4s off, mindfulness
- [ ] **Cámara lenta** — strobe calibrado para slow-motion en vídeo
- [ ] **Saved Morse** — 3 mensajes guardados en DataStore

---

## CI/CD

- **Debug APK**: cada push a `main` (~6 min)
- **Release APK firmado**: solo en tags `v*` (~12 min)
- Keystore en secret `KEYSTORE_BASE64`, alias `lumiai_key`
- SDK: compileSdk 35, minSdk 23, targetSdk 35

---

*Actualizado: 17/06/2026 — v2.6.9 (versionCode 90) · fix compile paywall + slider intensidad*

## Sesión 17/06/2026 — fixes UI (v2.6.8)

- **Paywall sincronizado con la grid**: `ProPaywallScreen` ya no tiene la lista de features hardcodeada. Ahora deriva de `FlashMode.proModes().filter { IS_DEBUG || !it.hidden }` + helper `paywallFeatureFor()`. Antes el paywall mostraba modos ocultos (Brillo/Lectura/Ambiental) que no están en la grid en release. Al añadir/ocultar un modo, actualizar el `when` de `paywallFeatureFor()`.
- **Slider de intensidad reactivado**: en `ModeConfigSheet` para Steady/Strobe/Disco/Morse (`hasTorchIntensity`). El backend (`setTorchStrength` con fallback PWM, `torchIntensityProvider`, DataStore `TORCH_INTENSITY`) ya existía; solo faltaba la UI. Live-apply reactivado en `FlashViewModel.setTorchIntensity()` para Steady. El comentario "torch strength API unstable" era obsoleto — hay fallback PWM cross-device.
- Estado modos Pro: SleepTimer, Music, Voice `hidden=false`; resto `hidden=true`.
- **Privacy Policy**: `privacy-policy.html` generada (pendiente subir a hosting + rellenar nombre/correo).
- **HOTFIX v2.6.9**: el build v2.6.8 falló en CI (`compileReleaseKotlin`) — `paywallFeatureFor()` usaba `val M = FlashMode` como tipo en patrones `is M.X`, que Kotlin no permite. Corregido a `is FlashMode.X` con import directo. Añadido **check 17** al auditor para detectar `val-alias usado como tipo en is-pattern`. Recordatorio: el auditor es heurístico, NO compila — un push verde en audit puede fallar en CI.
