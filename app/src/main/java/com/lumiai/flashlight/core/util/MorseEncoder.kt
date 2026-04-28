package com.lumiai.flashlight.core.util

/**
 * Converts plain text to Morse code flash timing arrays.
 * Returns list of (on_ms, off_ms) pairs.
 *
 * ITU standard timing:
 *  dit  = 1 unit  (200ms)
 *  dah  = 3 units (600ms)
 *  inter-element gap = 1 unit  (200ms)
 *  inter-letter gap  = 3 units (600ms)
 *  inter-word gap    = 7 units (1400ms)
 */
object MorseEncoder {

    private val CODE: Map<Char, String> = mapOf(
        'A' to ".-",   'B' to "-...", 'C' to "-.-.", 'D' to "-..",
        'E' to ".",    'F' to "..-.", 'G' to "--.",  'H' to "....",
        'I' to "..",   'J' to ".---", 'K' to "-.-",  'L' to ".-..",
        'M' to "--",   'N' to "-.",   'O' to "---",  'P' to ".--.",
        'Q' to "--.-", 'R' to ".-.",  'S' to "...",  'T' to "-",
        'U' to "..-",  'V' to "...-", 'W' to ".--",  'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----",'1' to ".----",'2' to "..---",'3' to "...--",
        '4' to "....-",'5' to ".....",'6' to "-....",'7' to "--...",
        '8' to "---..",'9' to "----.",
        '.' to ".-.-.-",',' to "--..--",'?' to "..--..",
        '!' to "-.-.--",'/' to "-..-.", '-' to "-....-",
        '@' to ".--.-.",'(' to "-.--.", ')' to "-.--.-",
    )

    private const val DIT      = 200L
    private const val DAH      = 600L
    private const val ELEM_GAP = 200L   // between dots/dashes in same letter
    private const val CHAR_GAP = 600L   // between letters
    private const val WORD_GAP = 1400L  // between words

    /**
     * Encode text to a list of (onMs, offMs) pairs for the flash controller.
     * Unrecognized characters are skipped.
     */
    fun encode(text: String): List<Pair<Long, Long>> {
        val result = mutableListOf<Pair<Long, Long>>()
        val words  = text.uppercase().trim().split(" ")

        words.forEachIndexed word@{ wi, word ->
            word.forEachIndexed char@{ li, char ->
                val code = CODE[char] ?: return@char
                code.forEachIndexed { ei, symbol ->
                    val onMs = if (symbol == '.') DIT else DAH
                    val offMs = if (ei < code.length - 1) ELEM_GAP else 0L
                    result.add(onMs to offMs)
                }
                // Gap after letter (not after last letter in word)
                if (li < word.length - 1) {
                    result.add(0L to CHAR_GAP)
                }
            }
            // Gap after word (not after last word)
            if (wi < words.size - 1) {
                result.add(0L to WORD_GAP)
            }
        }

        // Final pause before repeat
        if (result.isNotEmpty()) {
            result.add(0L to 2000L)
        }
        return result
    }

    /** Human-readable Morse string e.g. "... --- ..." */
    fun toReadable(text: String): String =
        text.uppercase().trim()
            .split(" ")
            .joinToString(" / ") { word ->
                word.mapNotNull { CODE[it] }.joinToString("  ")
            }
}
