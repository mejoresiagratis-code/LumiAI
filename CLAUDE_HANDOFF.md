# LumiAI — Documento de traspaso para nueva sesión de Claude

> Entrega este documento al inicio de una nueva conversación en Claude.ai con el mensaje:
> **"Lee este documento completo y confirma que entiendes el proyecto antes de empezar."**

---

## Quién eres y cómo trabajas

Eres el asistente de desarrollo de **LumiAI**, una app Android de linterna con IA. Llevas meses trabajando en este proyecto con el usuario (Pablo). Conoces el código en profundidad. Tu comportamiento:

- Lees el código antes de modificar nada
- Ejecutas el **auditor completo** antes de cada push
- Haces push solo cuando el auditor da **0 issues**
- Etiquetas cada versión con `git tag vX.X.X`
- Escribes mensajes de commit detallados explicando causa raíz
- Si algo puede fallar en build, lo buscas antes de enviar
- Cuando el usuario sube un log de build, lo lees inmediatamente

---

## Repositorio

```
URL:    https://github.com/mejoresiagratis-code/LumiAI
Stack:  Kotlin + Jetpack Compose + Hilt + CameraX + DataStore
Token:  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
```

El código está en `/home/claude/LumiAI` cuando trabajas en sesión con herramientas de computador.

---

## Estado actual — v2.3.6 (versionCode 76)

### Versiones recientes
- `v2.3.6` — 4 fixes: restorePurchases, onPaywall en grids, isPro propagado, Color L suffix Canvas
- `v2.3.5` — Iconos Canvas unificados, Pro UX completo, AdMob IDs reales, paywall sin scroll
- `v2.3.4` — Deep scan: 5 issues corregidos, reglas de auditor mejoradas
- `v2.3.3` — FirebaseManager sin Hilt (reflexión), import correcto en MainActivity
- `v2.3.0` — Pro restriction activa, paywall handler, Billing DI fix
- `v2.2.9` — animateFloat import + Color Long suffix
- `v2.2.4` — Screen mode: tabs Solid/Hue/Temp/FX, efectos animados
- `v2.2.0` — Steady: slider intensidad inline, sin botón ⚙

### Arquitectura — archivos clave
```
feature/flash/FlashScreen.kt          — UI principal, 14 modos, Screen mode tabbed
feature/flash/FlashViewModel.kt       — ScreenEffect enum, StateFlows, activateMode
feature/pro/ProPaywallScreen.kt       — Paywall sin scroll, en español
ui/components/ModePanel.kt            — LumiModeIcon Canvas, AiModeCard, FlashModeCard
core/data/repository/FlashRepositoryImpl.kt  — activateMode, torch ordering
core/data/repository/SettingsRepository.kt   — DataStore 25 claves
core/util/StrobeController.kt         — wasActive guard, StrobePattern enum
core/util/AiModeController.kt         — wasActive guard, 8 modos IA
core/util/FirebaseManager.kt          — object (sin Hilt), reflexión
core/di/AppModule.kt                  — Hilt, BillingRepository single instance
```

### AdMob IDs reales (producción)
```
App ID:       ca-app-pub-7644513562367479~7453103317
Banner:       ca-app-pub-7644513562367479/7748616787
Interstitial: ca-app-pub-7644513562367479/4306165418
Rewarded:     ca-app-pub-7644513562367479/3486984320
```

---

## Reglas de compilación — SIEMPRE verificar antes de push

### Imports prohibidos (no importables — scope members)
```kotlin
// ❌ NUNCA importar estas — son métodos de clase, no extensiones
androidx.compose.ui.input.pointer.awaitPointerEvent
androidx.compose.ui.graphics.drawscope.drawCircle
androidx.compose.ui.graphics.drawscope.drawLine
androidx.compose.ui.graphics.drawscope.drawPath
androidx.compose.ui.graphics.drawscope.drawRoundRect
```

### Imports requeridos — verificar que existen
```kotlin
// animateFloat ES una extensión de InfiniteTransition — SÍ necesita import
import androidx.compose.animation.core.animateFloat
// NO es lo mismo que animateFloatAsState (diferente función)
import androidx.compose.animation.core.animateFloatAsState
```

### Firebase — NUNCA inyectar con Hilt
```kotlin
// ❌ NUNCA hacer esto — rompe kapt de Hilt
@Inject lateinit var firebase: FirebaseManager

// ✅ Firebase es singleton — usar directamente
FirebaseManager.init(this)  // desde MainActivity.onCreate
```

### Firebase ktx — NO existen en BOM 33+
```kotlin
// ❌ Artefactos que NO existen en BOM 33+
firebase-perf-ktx, firebase-config-ktx, firebase-messaging-ktx
Firebase.performance, Firebase.remoteConfig  // ktx extensions

// ✅ Usar APIs directas
FirebaseRemoteConfig.getInstance()
FirebasePerformance.getInstance()
```

### Color en Canvas — usar sufijo L (en TODO archivo con Canvas)
```kotlin
// ❌ Ambiguo: Int (android) vs Long (compose)
Color(0xFFFF3B30)
// ✅ Unambiguamente Compose Color
Color(0xFFFF3B30L)
// ⚠️  Aplica a FlashScreen.kt, ModePanel.kt y cualquier archivo con Canvas
```

### Tipos geometry en Canvas — usar nombre importado
```kotlin
// ❌ Causa overload ambiguity
topLeft = androidx.compose.ui.geometry.Offset(x, y)
// ✅ Usar el importado
topLeft = Offset(x, y)
```

### Parámetros duplicados — verificar en Slider y funciones
```kotlin
// ❌ Error: mismo parámetro dos veces
Slider(onValueChange = { a = it }, onValueChange = { b = it })
```

### Declaraciones antes de imports
```kotlin
// ❌ enum class antes de todos los imports → error
import X
enum class Foo  // ← mal
import Y
// ✅ todos los imports primero, luego declaraciones
```

---

## Flujo de trabajo estándar

### Antes de cada push
```python
# El auditor verifica:
# 1. Scope-member imports (no importables)
# 2. Firebase Hilt injection
# 3. Firebase ktx patterns
# 4. Color(0xFF) sin L en Canvas
# 5. Tipos FQ dentro de Canvas
# 6. Parámetros duplicados
# 7. Imports duplicados
# 8. Symbols usados sin importar
```

### Comandos típicos de push
```bash
git add -A
git commit -m "feat/fix: descripción detallada vX.X.X"
git tag vX.X.X
git push origin main
git push origin vX.X.X
```

### Cuando llega un log de build
1. Leer el archivo de log inmediatamente
2. Identificar **todas** las líneas de error (no solo la primera)
3. Buscar causas similares en otros archivos
4. Arreglar todo antes del siguiente push
5. Verificar que el auditor da 0 issues

---

## Modos Flash — 14 en total

### Free (6)
| ID | Nombre | Config |
|---|---|---|
| steady | Continuo | Slider intensidad inline |
| screen | Pantalla | Slider brillo inline, 12 colores, tabs FX |
| morse_custom | Morse | TextField + velocidad ½×/1×/2×/4× |
| strobe | Estroboscopio | Slider Hz inline + burst en ⚙ |
| sos | SOS | Sin ⚙ |
| disco | Disco | Slider BPM inline + tap-tempo en ⚙ |

### Pro (8)
| ID | Nombre |
|---|---|
| smart_brightness | Inteligente |
| reading_mode | Lectura |
| ambient_smart | Ambiental |
| custom_rhythm | Personalizado |
| sleep_timer | Sueño |
| music | Música |
| walk | Caminar |
| voice | Voz |

**Pro restriction**: activa. Un usuario Free que toque un modo Pro → paywall directo.
**Badge PRO**: centrado en borde derecho de la tarjeta, fondo ámbar.
**Todos los taps en tarjeta Pro** (cuerpo + ⚙ + ⓘ) → `viewModel.showPaywall()`.
**Cadena de params**: `ModePanel(onPaywall)` → `AiModeGrid(isPro, onPaywall)` → `AiModeCard(isPro, onPaywall)`.
`FlashViewModel.restorePurchases()` disponible para "Restaurar compra" en paywall.

---

## Screen mode — detalle

Cuando Screen está ON:
- UI se oculta tras **3 segundos de inactividad** (no de aparición)
- Cualquier interacción resetea el timer
- Back físico apaga Screen mode (BackHandler)
- Botón ✕ en pantalla para apagar
- Panel inferior con 4 tabs: **Solid** (12 colores), **Hue** (slider 0-360°), **Temp** (2700K-6500K), **FX** (Candela, Police, Rainbow)
- Efectos FX: primer frame pre-aplicado antes de cancelar el anterior (sin frame en blanco)
- AnimatedCandle: Canvas con 3 InfiniteTransition simultáneas

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

## Pendientes del roadmap

### Inmediato (código — yo lo hago)
- [ ] LED Scroller como modo propio Free (`FlashMode.LedScroller`)
- [ ] SOS speed selector inline (4 botones ½×/1×/2×/4×)
- [ ] Saved Morse messages (3 mensajes en DataStore)
- [ ] Interstitial AdMob cada 5 cambios de modo
- [ ] Rewarded ad "Probar Pro 30 min"

### Tú (cuentas externas)
- [ ] Firebase: activar Analytics + Crashlytics en consola lumiai-54bbc
- [ ] Remote Config: crear los 5 parámetros en consola Firebase
- [ ] Privacy Policy URL online (Play Store la requiere)
- [ ] `pro_unlock` producto creado en Play Console

---

## Frases clave del usuario

- "Revisa antes de push" → ejecutar auditor completo
- "Busca similares" → escanear todos los archivos por el mismo patrón
- "Informa primero" → no modificar código, solo analizar y explicar
- "Dale" → implementar lo que se discutió antes
- Cuando sube un ZIP de logs → leerlo inmediatamente, es un log de build

---

## CI/CD

- **Debug APK**: en cada push a `main` (~6 min con caché)
- **Release APK firmado**: solo en tags `v*`
- Keystore en secret `KEYSTORE_BASE64`, alias `lumiai_key`
- Retención: 14 días debug, 90 días release

---

*Generado automáticamente desde el estado del proyecto el 30/04/2026 — v2.3.6*
