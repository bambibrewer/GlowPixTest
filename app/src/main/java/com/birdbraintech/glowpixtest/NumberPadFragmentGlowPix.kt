package com.birdbraintech.glowpixtest

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_number_pad.*

interface NumberPadDelegate {
    fun numberChanged(number: Int?)
    fun keypadDismissed()
}

class NumberPadFragmentGlowPix : Fragment() {

    var numberPadDelegate: NumberPadDelegate? = null
    set(value) {
        // If a button on a different block has been selected, we want to let the old delegate know it no longer
        // has a keypad
        if (field != value) {
            field?.keypadDismissed()
        }
        field = value
    }


    private val inflatedLayoutResource = R.layout.fragment_number_pad

    // Use this variable if you want to constrain the number to a particular number of digits
    var maxNumberOfDigits: Int? = 9

    // This is a string representation of the number that the user is entering.
    private var numberString = ""

    lateinit var numberButtons: Array<Button>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(inflatedLayoutResource, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        numberButtons = arrayOf(button0, button1, button2, button3, button4, button5, button6, button7, button8, button9)

        setButtonListeners()
    }

    private fun setButtonListeners() {
        for (button in numberButtons) {
            button.setOnClickListener {
                numberButtonPressed(button)
            }
        }

        button_backspace.setOnClickListener {
            backspacePressed()
        }

    }

    // This function is called when the user taps one of the number buttons
    private fun numberButtonPressed(sender: Button) {
        // Don't want to do anything if this button press would make us exceed the max number of digits
        if (maxNumberOfDigits != null) {
            if (numberString.length >= maxNumberOfDigits!!) {
                return
            }
        }

        // Otherwise, add the number
        numberString += sender.text
        numberPadDelegate?.numberChanged(numberString.toIntOrNull())
    }

    // This function is called when the user taps the backspace button
    private fun backspacePressed() {
        // Remove the last digit
        numberString = numberString.dropLast(1)
        numberPadDelegate?.numberChanged(numberString.toIntOrNull())
    }

    // The view controller can use this function to reset the number
    public fun resetNumber() {
        numberString = ""
    }
}