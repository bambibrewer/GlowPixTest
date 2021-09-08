package com.birdbraintech.glowpixtest

/* This enumerates all the colors that an LED can be in GlowPix. It also returns the image needed for the color picker button and the block for each color. */
enum class PixelColor {

    red, redDim, yellow, yellowDim, green, greenDim, teal, tealDim, blue, blueDim, magenta, magentaDim, white, whiteDim, off;

    val description: String
        get() = this.toString()

    val isDim: Boolean
        get() {
            val dimColors =
                listOf(redDim, yellowDim, greenDim, tealDim, blueDim, magentaDim, whiteDim)
            if (dimColors.contains(this)) {
                return true
            }
            return false
        }

    val colorButtonImage: Int
        get() = when (this) {
            red -> R.drawable.color_palette_button_red
            redDim -> R.drawable.color_palette_button_red_dim
            yellow -> R.drawable.color_palette_button_yellow
            yellowDim -> R.drawable.color_palette_button_yellow_dim
            green -> R.drawable.color_palette_button_green
            greenDim -> R.drawable.color_palette_button_green_dim
            teal -> R.drawable.color_palette_button_teal
            tealDim -> R.drawable.color_palette_button_teal_dim
            blue -> R.drawable.color_palette_button_blue
            blueDim -> R.drawable.color_palette_button_blue_dim
            magenta -> R.drawable.color_palette_button_purple
            magentaDim -> R.drawable.color_palette_button_purple_dim
            white -> R.drawable.color_palette_button_white
            whiteDim -> R.drawable.color_palette_button_white_dim
            off -> R.drawable.color_palette_button_black
        }

    val blockImage: Int
        get() = when (this) {
            red, redDim -> R.drawable.block_red2
            yellow, yellowDim -> R.drawable.block_yellow2
            green, greenDim -> R.drawable.block_green2
            teal, tealDim -> R.drawable.block_teal2
            blue, blueDim -> R.drawable.block_blue2
            magenta,magentaDim -> R.drawable.block_purple2
            white,whiteDim -> R.drawable.block_white2
            off -> R.drawable.block_black2
        }


//    func glowBoardColor() -> GlowBoard.Matrix.Color {
//        switch self {
//            case .red:
//            return .red
//            case .redDim:
//            return .darkRed
//            case .yellow:
//            return .yellow
//            case .yellowDim:
//            return .darkYellow
//            case .green:
//            return .green
//            case .greenDim:
//            return .darkGreen
//            case .teal:
//            return .cyan
//            case .tealDim:
//            return .darkCyan
//            case .blue:
//            return .blue
//            case .blueDim:
//            return .darkBlue
//            case .magenta:
//            return .magenta
//            case .magentaDim:
//            return .darkMagenta
//            case .white:
//            return .white
//            case .whiteDim:
//            return .grey
//            case .off:
//            return .black
//        }
//    }

//    func OwletLEDcolor() -> UIColor {
//        switch self {
//            case .red:
//            return UIColor(named: "OwletLEDred") ?? UIColor.red
//            case .redDim:
//            return UIColor(named: "OwletLEDredDim") ?? UIColor.red
//            case .yellow:
//            return UIColor(named: "OwletLEDyellow") ?? UIColor.yellow
//            case .yellowDim:
//            return UIColor(named: "OwletLEDyellowDim") ?? UIColor.yellow
//            case .green:
//            return UIColor(named: "OwletLEDgreen") ?? UIColor.green
//            case .greenDim:
//            return UIColor(named: "OwletLEDgreenDim") ?? UIColor.green
//            case .teal:
//            return UIColor(named: "OwletLEDteal") ?? UIColor.cyan
//            case .tealDim:
//            return UIColor(named: "OwletLEDtealDim") ?? UIColor.cyan
//            case .blue:
//            return UIColor(named: "OwletLEDblue") ?? UIColor.blue
//            case .blueDim:
//            return UIColor(named: "OwletLEDblueDim") ?? UIColor.blue
//            case .magenta:
//            return UIColor(named: "OwletLEDmagenta") ?? UIColor.magenta
//            case .magentaDim:
//            return UIColor(named: "OwletLEDmagentaDim") ?? UIColor.magenta
//            case .white:
//            return UIColor.white
//            case .whiteDim:
//            return UIColor(named: "OwletLEDwhiteDim") ?? UIColor.white
//            case .off:
//            return UIColor(named: "OwletLEDoff") ?? UIColor.gray
//        }
//    }
}
