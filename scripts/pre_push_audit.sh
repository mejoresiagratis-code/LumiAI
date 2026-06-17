#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# LumiAI — Pre-push auditor
# Run before every git push. Exit 1 = issues found, do not push.
# Usage: bash scripts/pre_push_audit.sh
# NOTE: this is a fast heuristic gate, NOT a substitute for `./gradlew lint test`.
# ─────────────────────────────────────────────────────────────────────────────
SRC="app/src/main/java/com/lumiai/flashlight"
ISSUES=0
WARNINGS=0

red()    { printf "\033[0;31m  ✗ %s\033[0m\n" "$*"; }
orange() { printf "\033[0;33m  ⚠ %s\033[0m\n" "$*"; }
green()  { printf "\033[0;32m  ✓ %s\033[0m\n" "$*"; }
header() { printf "\n\033[1;37m[%s]\033[0m\n" "$*"; }

fail() { red "$*";    ISSUES=$((ISSUES+1)); }
warn() { orange "$*"; WARNINGS=$((WARNINGS+1)); }
pass() { green "$*"; }

# ── 1. SCOPE-MEMBER IMPORTS ───────────────────────────────────────────────────
header "1. Scope-member imports (never importable)"
SCOPE_MEMBERS="awaitPointerEvent drawCircle drawLine drawPath drawRoundRect drawArc"
found_scope=0
for sym in $SCOPE_MEMBERS; do
  hits=$(grep -rn "import androidx.compose.ui.*$sym" "$SRC" 2>/dev/null || true)
  if [ -n "$hits" ]; then fail "Scope-member import: $sym"; found_scope=1; fi
done
[ $found_scope -eq 0 ] && pass "No scope-member imports"

# ── 2. FIREBASE HILT ─────────────────────────────────────────────────────────
header "2. Firebase @Inject"
hits=$(grep -rn "@Inject.*FirebaseManager" "$SRC" 2>/dev/null || true)
[ -n "$hits" ] && fail "Firebase injected via Hilt: $hits" || pass "Firebase not Hilt-injected"

# ── 3. COLOR WITHOUT L IN CANVAS ─────────────────────────────────────────────
header "3. Color(0xFF…) without L suffix in Canvas files"
color_issues=0
for f in $(grep -rl "Canvas(" "$SRC" 2>/dev/null || true); do
  result=$(grep -n "Color(0x[0-9A-Fa-f]\{8\})" "$f" 2>/dev/null | grep -v "L)" | grep -v "^\s*//" || true)
  if [ -n "$result" ]; then
    fail "Color without L in $(basename $f): $result"
    color_issues=1
  fi
done
[ $color_issues -eq 0 ] && pass "All Canvas Colors have L suffix"

# ── 4. ADMOB IDS ─────────────────────────────────────────────────────────────
header "4. AdMob IDs in release"
hits=$(grep "XXXXXXXXXXXXXXXX" app/build.gradle.kts 2>/dev/null || true)
[ -n "$hits" ] && fail "Placeholder AdMob ID found" || pass "AdMob IDs are production"

# ── 5. DEAD DEPENDENCIES ─────────────────────────────────────────────────────
header "5. Dead dependencies"
for dep in "room.runtime" "room.ktx" "mlkit.image.labeling"; do
  hits=$(grep "implementation.*libs\.$dep" app/build.gradle.kts 2>/dev/null || true)
  [ -n "$hits" ] && warn "Dead dep still present: $dep"
done
pass "Dead dependency check done"

# ── 6. MANIFEST PERMISSIONS ──────────────────────────────────────────────────
header "6. Manifest permissions"
MANIFEST="app/src/main/AndroidManifest.xml"
grep -q "uses-permission.*WAKE_LOCK" "$MANIFEST" 2>/dev/null && warn "WAKE_LOCK still present (not needed)" || pass "WAKE_LOCK not present"

# ── 7. FLASHSCREEN — ALL COMPOSABLES DEFINED ─────────────────────────────────
header "7. FlashScreen composable definitions"
FS="$SRC/feature/flash/FlashScreen.kt"
REQUIRED="FlashScreen TopBar ScreenEffectEngine ScreenControlPanel AnimatedCandle AutoOffChip ModeConfigSheet"
for sym in $REQUIRED; do
  found=$(grep -c "fun $sym" "$FS" 2>/dev/null || true)
  [ "$found" -eq "0" ] && fail "$sym not defined in FlashScreen.kt" || pass "$sym defined"
done

# ── 8. ORPHANED IMPORTS IN FLASHSCREEN ───────────────────────────────────────
header "8. Orphaned imports in FlashScreen"
FS="$SRC/feature/flash/FlashScreen.kt"
check_orphan() {
  local label="$1" import_pat="$2" use_pat="$3"
  imported=$(grep -c "$import_pat" "$FS" 2>/dev/null || true)
  used=$(grep -c "$use_pat" "$FS" 2>/dev/null || true)
  if [ "${imported:-0}" -gt "0" ] && [ "${used:-0}" -eq "0" ]; then
    warn "Orphaned import in FlashScreen: $label"
  fi
}
check_orphan "AnimatedVisibility" "AnimatedVisibility" "AnimatedVisibility("
check_orphan "KeyboardOptions"    "KeyboardOptions"    "KeyboardOptions("
check_orphan "ImeAction"          "ImeAction"          "ImeAction\."
check_orphan "fadeIn"             "import.*fadeIn"     "fadeIn("
check_orphan "fadeOut"            "import.*fadeOut"    "fadeOut("
pass "Orphaned import check done"

# ── 9. BILLING SINGLETON ─────────────────────────────────────────────────────
header "9. Billing singleton"
count=$(grep -c "fun provideBilling" app/src/main/java/com/lumiai/flashlight/core/di/AppModule.kt 2>/dev/null || true)
[ "$count" -gt "1" ] && fail "BillingRepository provided $count times (should be 1)" || pass "BillingRepository provided once"

# ── 10. MISLEADING "GEMINI" CLAIMS (whole codebase, not just onboarding) ──────
header "10. Misleading Gemini/AI claims"
hits=$(grep -rn "Gemini AI\|Gemini Nano" "$SRC" 2>/dev/null || true)
[ -n "$hits" ] && warn "Code mentions Gemini (not used): $hits" || pass "No misleading Gemini mention"

# ── 11. PRO MODES — UI VISIBILITY ────────────────────────────────────────────
header "11. Pro modes hidden for Free launch"
FM="$SRC/core/domain/model/FlashMode.kt"
pro_count=$(grep -c "isPro = true" "$FM" 2>/dev/null || true)
hidden_count=$(grep -c "hidden = true" "$FM" 2>/dev/null || true)
if [ "${pro_count:-0}" -gt "0" ] && [ "${hidden_count:-0}" -eq "0" ]; then
  warn "$pro_count Pro modes without hidden=true — confirm they are excluded from AiModeGrid"
elif [ "${pro_count:-0}" -gt "0" ] && [ "${hidden_count:-0}" -gt "0" ]; then
  pass "$hidden_count/$pro_count Pro modes marked hidden"
else
  pass "No Pro modes (or all handled)"
fi

# ── 12. DUPLICATE PARAMS ─────────────────────────────────────────────────────
header "12. Duplicate composable parameters"
hits=$(grep -rn "onValueChange.*onValueChange" "$SRC" 2>/dev/null | grep -v "import\|//" || true)
[ -n "$hits" ] && fail "Duplicate params: $hits" || pass "No duplicate parameters"

# ── 13. CRITICAL IMPORT PATTERN — imports not replaced when adding new ones ────
header "13. Import replacement safety (FlashScreen + LumiNavHost)"
for f in \
  "app/src/main/java/com/lumiai/flashlight/ui/navigation/LumiNavHost.kt:FlashScreen" \
  "app/src/main/java/com/lumiai/flashlight/ui/navigation/LumiNavHost.kt:ModeConfigScreen" \
  "app/src/main/java/com/lumiai/flashlight/feature/flash/ModeConfigScreen.kt:FlashButton"; do
  file="${f%%:*}"; sym="${f##*:}"
  used=$(grep -c "$sym" "$file" 2>/dev/null || true)
  imported=$(grep -c "^import.*$sym\b" "$file" 2>/dev/null || true)
  if [ "$used" -gt "0" ] && [ "$imported" -eq "0" ]; then
    fail "$sym used but NOT imported in $(basename $file)"
  fi
done
pass "Critical import check done"

# ── 14. CANVAS DRAWING TYPES — StrokeCap, StrokeJoin need explicit imports ────
header "14. Canvas drawing type imports"
FS="$SRC/feature/flash/FlashScreen.kt"
for sym in "StrokeCap" "StrokeJoin" "BlendMode" "FastOutSlowInEasing"; do
  used=$(grep -c "\b$sym\b" "$FS" 2>/dev/null || true)
  imported=$(grep -c "^import.*\b$sym\b" "$FS" 2>/dev/null || true)
  if [ "${used:-0}" -gt "1" ] && [ "${imported:-0}" -eq "0" ]; then
    fail "$sym used in FlashScreen but not imported"
  fi
done
pass "Canvas drawing type imports OK"

# ── 15. MODIFIER.ALIGN SCOPE CHECK ────────────────────────────────────────────
header "15. Modifier.align() not in Column/Row context (heuristic)"
align_hits=$(grep -c "\.align(Alignment\." "$FS" 2>/dev/null || true)
if [ "${align_hits:-0}" -gt "0" ]; then
  pass "Modifier.align() found $align_hits times — verify each is inside BoxScope"
else
  pass "No Modifier.align() calls"
fi

# ── 16. COMPOSABLES IN VAL ASSIGNMENT (debug passes, release fails) ────────────
header "16. Composable calls inside val = when(){} blocks"
FS="$SRC/feature/flash/FlashScreen.kt"
if grep -q "val title = when" "$FS" 2>/dev/null; then
  awk '/val title = when/,/^        \)/' "$FS" | grep -q "remember\|LocalContext\|checkSelfPermission" \
    && fail "Composable call (remember/LocalContext) inside 'val title = when()' block in FlashScreen" \
    || pass "No composable calls in title=when() block"
else
  pass "No title=when() block found"
fi

# ── 17. VAL-ALIAS USED AS TYPE IN is-PATTERN (compiles never, audit missed it) ─
header "17. val alias used as type in is-pattern"
alias_bug=0
for f in $(grep -rl "val [A-Z] *= .*FlashMode$" "$SRC" 2>/dev/null || true); do
  # a 'val X = SomeType' followed later by 'is X.' is a compile error in Kotlin
  aliases=$(grep -oE "val [A-Z] *=" "$f" | grep -oE "[A-Z]" || true)
  for a in $aliases; do
    if grep -qE "is $a\\." "$f" 2>/dev/null; then
      fail "val-alias '$a' used as type in is-pattern in $(basename $f) — won't compile"
      alias_bug=1
    fi
  done
done
[ $alias_bug -eq 0 ] && pass "No val-alias used as is-pattern type"

# ── SUMMARY (must stay LAST so every check above runs) ───────────────────────
echo ""
echo "────────────────────────────────────────"
if [ "$ISSUES" -gt "0" ]; then
  printf "\033[0;31m  AUDIT FAILED — %d issue(s), %d warning(s). Do NOT push.\033[0m\n" "$ISSUES" "$WARNINGS"
  exit 1
else
  printf "\033[0;32m  AUDIT PASSED — 0 issues, %d warning(s). Safe to push.\033[0m\n" "$WARNINGS"
  exit 0
fi
