package com.swordfish.lemuroid.app.tv.games

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.swordfish.lemuroid.R
import java.text.Normalizer

/**
 * Vertical "#, A-Z" bar of the games of a console. Up and down move between the letters that have games
 * ([onLetterSelected] jumps to the first one of them); OK or left go back to the grid ([onExit]).
 * The selected letter is only highlighted while the bar has the focus.
 */
class TVAlphabetBar(context: Context) : View(context) {
    var onLetterSelected: ((Char) -> Unit)? = null
    var onExit: (() -> Unit)? = null

    private var availableLetters: Set<Char> = emptySet()
    private var selectedLetter: Char? = null

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = resources.getDimension(R.dimen.tv_alphabet_text_size)
        }
    private val highlightPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.tv_tab_focused_background)
        }
    private val availableColor = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
    private val unavailableColor = ContextCompat.getColor(context, R.color.tv_alphabet_unavailable)
    private val highlightTextColor = ContextCompat.getColor(context, R.color.tv_tab_focused_text)
    private val highlightRect = RectF()
    private val textBounds = Rect()

    // No background: only the letters are drawn.
    init {
        isFocusable = true
    }

    fun setAvailableLetters(letters: Set<Char>) {
        availableLetters = letters
        if (selectedLetter !in letters) selectedLetter = null
        invalidate()
    }

    /** Moves the focus to the bar, on [letter] or on the closest letter with games. */
    fun focusLetter(letter: Char) {
        selectedLetter =
            LETTERS.filter { it in availableLetters }
                .minByOrNull { Math.abs(LETTERS.indexOf(it) - LETTERS.indexOf(letter)) }
        requestFocus()
        invalidate()
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> moveSelection(-1)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveSelection(1)
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
            -> onExit?.invoke()
            // Nothing on the right of the bar.
            KeyEvent.KEYCODE_DPAD_RIGHT -> Unit
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    private fun moveSelection(direction: Int) {
        val current = LETTERS.indexOf(selectedLetter ?: return)
        val next =
            generateSequence(current + direction) { it + direction }
                .takeWhile { it in LETTERS.indices }
                .firstOrNull { LETTERS[it] in availableLetters }
                ?: return
        selectedLetter = LETTERS[next]
        invalidate()
        onLetterSelected?.invoke(LETTERS[next])
    }

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?,
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val verticalPadding = resources.getDimension(R.dimen.tv_alphabet_vertical_padding)
        val slotHeight = (height - 2 * verticalPadding) / LETTERS.size
        val centerX = width / 2f
        val highlightHalfWidth = resources.getDimension(R.dimen.tv_alphabet_highlight_width) / 2

        LETTERS.forEachIndexed { index, letter ->
            val centerY = verticalPadding + slotHeight * (index + 0.5f)
            val highlighted = hasFocus() && letter == selectedLetter
            if (highlighted) {
                highlightRect.set(
                    centerX - highlightHalfWidth,
                    centerY - slotHeight / 2,
                    centerX + highlightHalfWidth,
                    centerY + slotHeight / 2,
                )
                canvas.drawRoundRect(highlightRect, slotHeight / 2, slotHeight / 2, highlightPaint)
            }
            textPaint.color =
                when {
                    highlighted -> highlightTextColor
                    letter in availableLetters -> availableColor
                    else -> unavailableColor
                }
            textPaint.typeface = if (highlighted) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            val text = letter.toString()
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            canvas.drawText(text, centerX, centerY - textBounds.exactCenterY(), textPaint)
        }
    }

    companion object {
        /** "#" groups the names that start with a number or a symbol. */
        val LETTERS: List<Char> = listOf('#') + ('A'..'Z')

        /** Letter of the bar for a game name, ignoring accents ("Ético" is in "E"). */
        fun letterOf(name: String): Char {
            val first =
                Normalizer.normalize(name.trim().take(1), Normalizer.Form.NFD)
                    .firstOrNull()
                    ?.uppercaseChar()
            return if (first != null && first in 'A'..'Z') first else '#'
        }
    }
}
