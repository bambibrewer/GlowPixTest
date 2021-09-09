package com.birdbraintech.glowpixtest

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_color_palette.*

interface ColorPickerDelegate {
    fun colorSelected(color: PixelColor)
}

class ColorPickerFragment : Fragment() {


    var colorPickerDelegate: ColorPickerDelegate? = null

    lateinit var colorButtons: Array<Button>
    lateinit var brightnessButtons: Array<Button>

    // The color of the pixel. This notifies the delegate that it has changed.
    private var color: PixelColor? = null
    set(value) {
        field = value
        if (value != null) {
            colorPickerDelegate?.colorSelected(value)
        }
    }

    // The current brightness level. When the user selects that the led should be off, turn all the color buttons black to make sure they know what will happen
    private var brightnessLevel = BrightnessLevel.bright
        set(value) {
            field = value
            setButtonBackgrounds(value)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(inflatedLayoutResource, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        brightnessButtons = arrayOf(bright, dim, off)
        colorButtons = arrayOf(color_red, color_yellow, color_green, color_lightblue, color_darkblue, color_purple, color_white)

        setButtonListeners()
    }

    /* If a color was already selected, this function can be used to restore it. Also sets the corresponding brightness level. */
    fun setExistingColor(color: PixelColor) {
        this.color = color

        brightnessLevel = when (color) {
            PixelColor.white,PixelColor.magenta,PixelColor.blue,PixelColor.teal,PixelColor.green,PixelColor.yellow, PixelColor.red -> BrightnessLevel.bright
            PixelColor.off -> BrightnessLevel.off
            else -> BrightnessLevel.dim
        }
    }

    fun setButtonListeners()
    {
        for (button in brightnessButtons) {
            button.setOnClickListener {
                brightnessClicked(button)
          }
        }

        for (button in colorButtons) {
            button.setOnClickListener {
                colorClicked(button)
            }
        }
    }

    private fun colorClicked(button: Button) {
        color = when(button) {
            color_red -> getLEDColor(brightnessLevel, ButtonColor.red)
            color_yellow -> getLEDColor(brightnessLevel, ButtonColor.yellow)
            color_green -> getLEDColor(brightnessLevel, ButtonColor.green)
            color_lightblue -> getLEDColor(brightnessLevel, ButtonColor.teal)
            color_darkblue -> getLEDColor(brightnessLevel, ButtonColor.blue)
            color_purple -> getLEDColor(brightnessLevel, ButtonColor.magenta)
            else -> getLEDColor(brightnessLevel, ButtonColor.white)
        }
    }

    private fun brightnessClicked(button: Button) {
        val oldBrightness = brightnessLevel
        val buttonColor = getButtonColor()

        if (button == bright) {
            brightnessLevel = BrightnessLevel.bright
            if (oldBrightness == BrightnessLevel.off) {
                color = PixelColor.white
            } else {
                color = getLEDColor(brightnessLevel, buttonColor)
            }
        } else if (button == dim) {
            brightnessLevel = BrightnessLevel.dim
            if (oldBrightness == BrightnessLevel.off) {
                color = PixelColor.whiteDim
            } else {
                color = getLEDColor(brightnessLevel, buttonColor)
            }
        } else {
            brightnessLevel = BrightnessLevel.off
            color = PixelColor.off
        }
    }

    private fun setButtonBackgrounds(brightness: BrightnessLevel) {
        // First set the colors
        if (brightness == BrightnessLevel.off) {
            for (colorButton in colorButtons) {
               colorButton.setBackgroundResource(R.drawable.color_palette_selection_black)
            }
        } else {
            for (i in colorButtons.indices) {
                colorButtons[i].setBackgroundResource(colorBackgroundIds[i])
            }
        }

        // Now set which brightness level is selected
        val dictionary = mapOf(bright to BrightnessLevel.bright, dim to BrightnessLevel.dim, off to BrightnessLevel.off)
        for (i in brightnessButtons.indices) {
            val button = brightnessButtons[i]
            if (brightness == dictionary[button]) {
                button.setBackgroundResource(brightnessBackgroundIds[1][i])
            } else {
                button.setBackgroundResource(brightnessBackgroundIds[0][i])
            }
        }
    }

    /* This function returns the button color corresponding to the current pixel color. The default is white. */
    private fun getButtonColor(): ButtonColor {
        if (color != null) {
            return when(color) {
                PixelColor.red, PixelColor.redDim -> ButtonColor.red
                PixelColor.yellow, PixelColor.yellowDim -> ButtonColor.yellow
                PixelColor.green, PixelColor.greenDim -> ButtonColor.green
                PixelColor.teal, PixelColor.tealDim -> ButtonColor.teal
                PixelColor.blue, PixelColor.blueDim -> ButtonColor.blue
                PixelColor.magenta, PixelColor.magentaDim -> ButtonColor.magenta
                PixelColor.white, PixelColor.whiteDim -> ButtonColor.white
                PixelColor.off -> ButtonColor.white
                else -> ButtonColor.white
            }
        }

        return ButtonColor.white
    }

    /* This function takes a brightness and the color of a button and generates the color that the pixel should be */
    private fun getLEDColor(bright: BrightnessLevel, buttoncolor: ButtonColor): PixelColor {
        return when(bright) {
            BrightnessLevel.off -> PixelColor.off
            BrightnessLevel.bright ->
                when(buttoncolor) {
                    ButtonColor.red -> PixelColor.red
                    ButtonColor.yellow -> PixelColor.yellow
                    ButtonColor.green -> PixelColor.green
                    ButtonColor.teal -> PixelColor.teal
                    ButtonColor.blue -> PixelColor.blue
                    ButtonColor.magenta -> PixelColor.magenta
                    else -> PixelColor.white
                }
            BrightnessLevel.dim->
                when(buttoncolor) {
                    ButtonColor.red -> PixelColor.redDim
                    ButtonColor.yellow -> PixelColor.yellowDim
                    ButtonColor.green -> PixelColor.greenDim
                    ButtonColor.teal -> PixelColor.tealDim
                    ButtonColor.blue -> PixelColor.blueDim
                    ButtonColor.magenta -> PixelColor.magentaDim
                    else -> PixelColor.white
                }
        }
    }

    private val inflatedLayoutResource: Int
        get() = R.layout.fragment_color_palette

    internal enum class BrightnessLevel {
        bright, dim, off
    }

    internal enum class ButtonColor {
        red, yellow, green, teal, blue, magenta, white
    }

    companion object {

        val brightnessBackgroundIds = arrayOf(
            intArrayOf(
                R.drawable.button_bright_inactive,
                R.drawable.button_dim_inactive,
                R.drawable.button_off_inactive
            ),
            intArrayOf(
                R.drawable.button_bright_active,
                R.drawable.button_dim_active,
                R.drawable.button_off_active
            )
        )

        private val colorBackgroundIds = intArrayOf(
            R.drawable.color_palette_selection_red,
            R.drawable.color_palette_selection_yellow,
            R.drawable.color_palette_selection_green,
            R.drawable.color_palette_selection_teal,
            R.drawable.color_palette_selection_blue,
            R.drawable.color_palette_selection_purple,
            R.drawable.color_palette_button_white
        )
    }

}