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

class ColorPaletteFragment : Fragment() {

    //var view: View? = null

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
        Log.d("Blocks","created fragment")
        brightnessButtons = arrayOf(bright, dim, off)
        colorButtons = arrayOf(color_red, color_yellow, color_green, color_lightblue, color_darkblue, color_purple, color_white)

        setButtonListeners()
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
                //colorClicked(button)
            }
        }
    }

    private fun brightnessClicked(button: Button) {
        Log.d("Blocks","brightness clicked")
        val oldBrightness = brightnessLevel
        //val buttonColor = getButtonColor()

        if (button == bright) {
            brightnessLevel = BrightnessLevel.bright
            if (oldBrightness == BrightnessLevel.off) {
                color = PixelColor.white
            } else {
                //color = getLEDColor(brightnessLevel, buttonColor)
            }
        } else if (button == dim) {
            brightnessLevel = BrightnessLevel.dim
            if (oldBrightness == BrightnessLevel.off) {
                color = PixelColor.whiteDim
            } else {
                //color = getLEDColor(brightnessLevel, buttonColor)
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