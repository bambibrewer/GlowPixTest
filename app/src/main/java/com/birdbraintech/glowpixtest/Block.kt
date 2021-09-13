package com.birdbraintech.glowpixtest

import android.app.ActionBar
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

// The view controller that contains the block should be a delegate so it can be notified when the blocks has changed
interface BlockDelegate {
    fun displayError(result: EvaluationOptions, x: Int, y: Int)
    fun updateGlowBoard()
    fun savePicture()
}


class Block(val type: BlockType, val level: Level, context: Context): LinearLayout(context), ColorPickerDelegate, NumberPadDelegate {

    //var imageView: ImageView

    var blockDelegate: BlockDelegate? = null

    var isStart: Boolean = (type == BlockType.start)

    var isNestable: Boolean =
        ((level == Level.level5) && (type != BlockType.equals) && (type != BlockType.start))

    var canContainChildren: Boolean =
        ((level == Level.level5) && (type != BlockType.start))  // all level 5 blocks can contain children, except start

    var ledColor: PixelColor = PixelColor.white
        set(value) {
            field = value
            if (!isNestable && !isStart) {     // nestable blocks and start blocks should not have color buttons
                // reset the color of the block and the color of the pick button
                colorPickerButton.setBackgroundResource(value.colorButtonImage)
                this.setBackgroundResource(value.blockImage)

            }
        }

    /* All of these variable control the size of the blocks, and the size of the number buttons and labels within them. */
    private val blockHeight: Float = 72.0F
    var heightOfRectangle: Float = (0.804 * blockHeight).toFloat()

    var heightOfButton: Float = 2*heightOfRectangle/3

    private val originalBlockWidth: Float = 261.1F
    var widthOfButton: Float = originalBlockWidth/4

    private val colorButtonSize = 28

    val offsetToNext: Float  //The distance along the y axis to place the next block.
        get() {
            if (this.type == BlockType.start) {
                return (0.41 * blockHeight).toFloat()
            }
            return (0.42 * blockHeight).toFloat()
        }

    val offsetToPrevious: Float
        get() { //The distance along the y axis to place the previous block
            if (type == BlockType.start) {
                return 0.toFloat()
            }
            return (0.4 * blockHeight).toFloat()
        }

    // Find out where the block is to show the color picker popup
    val locationOnScreen: IntArray
    get() {
        // Find out where the block is to show the color picker popup
        val selectedLocation = IntArray(2)
        this.getLocationOnScreen(selectedLocation)
        return selectedLocation
    }

    var nextBlock: Block? = null//the block to be executed after this one
    var previousBlock: Block? = null//block before this one on chain

    val mathOperator = type.mathOperator

    var colorPickerButton = Button(context)
    var startLabel = TextView(context)
    var firstNumber = Button(context)
    var operatorLabel = TextView(context)
    var secondNumber = Button(context)
    var operatorLabel2: TextView? = null
    var thirdNumber: Button? = null
    var equalsLabel = TextView(context)
    var answer =
        Button(context)    // also stores the starting number for start blocks in levels 1 and 2
    //var errorView = ErrorView()

    // Only for blocks that nest
    var openParentheses = TextView(context)
    var closeParentheses = TextView(context)
    var nestingOffsetX = 10.0f
    var parent: Block? = null
    var nestedChild1: Block? = null
    var nestedChild2: Block? = null

    var selectedButton: Button? = null    // Button for which user is currently entering text
        set(value) {
            field?.setBackgroundResource(R.drawable.text_box)
            value?.setBackgroundResource(R.drawable.text_box_selected)
            field = value
        }

    init {

        setBackgroundResource(R.drawable.block_white2)

        // Define the optional fields for the double addition block - it is the only one that uses them
        if (type == BlockType.doubleAddition) {
            thirdNumber = Button(context)
            operatorLabel2 = TextView(context)
        }

        // Layout the block
        layoutBlock()
    }

    fun getPositionForGhost(whenConnectingToBlock: Block): Pair<Float, Float> {
        val x = whenConnectingToBlock.x
        val y = whenConnectingToBlock.y + whenConnectingToBlock.offsetToNext + this.offsetToPrevious

        return Pair(x, y)
    }

    // This function is used when we need to position blocks below the ghost block
    fun getPositionForBlocksAttachedToGhost(whenConnectingToBlock: Block): Pair<Float, Float> {
        val x = whenConnectingToBlock.x
        val y = (whenConnectingToBlock.y + whenConnectingToBlock.offsetToNext + this.offsetToPrevious + 0.82*blockHeight).toFloat()

        return Pair(x, y)
    }

    fun goToPosition(whenConnectingToBlock: Block) {
        val x = whenConnectingToBlock.x
        val y = whenConnectingToBlock.y + whenConnectingToBlock.offsetToNext + this.offsetToPrevious

        this.x = x
        this.y = y //- this.height/2
    }

    // Position the vertical stack of blocks
    fun positionChainImages(){
        if (!isNestable) {        // This function shouldn't be called for a nestable block
            if (nextBlock != null) {
                nextBlock?.goToPosition(whenConnectingToBlock = this)
                // STILL NEED TO HANDLE THESE
//                nextBlock?.bringToFront()
//                if (nextBlock?.type == BlockType.equals) {
//                    nextBlock.layoutEqualsBlock()
//                }
                nextBlock?.positionChainImages()
            }
        }
    }

    //Attach a block chain below this one
    fun attachBlock (b: Block){
        if (nextBlock != null) {
            b.attachToChain(nextBlock!!)
        }
        nextBlock = b
        b.previousBlock = this

        // To change the first number in a chained block (Level 1 and 2) when it is attached
//        if (level == .level1) || (level == .level2) {
//            b.layoutBlock()        // To change the first number in chained blocks (Level 1 and 2) when they are detached
//            // Check if block should be grayed out
//            if b.previousBlock?.isInactive == true || b.previousBlock?.evaluate() != .correct {
//                b.isInactive = true
//            }
//        }
        b.evaluateDisplayAndUpdate()
        Log.d("Blocks","attaching")
        positionChainImages()
    }

    //attach block b to the end of this chain
    fun attachToChain(b: Block){
        if (nextBlock == null) {
            nextBlock = b
            b.previousBlock = this
        } else {
            nextBlock?.attachToChain(b)
        }

        // To change the first number in a chained block (Level 1 and 2) when it is attached
//        if (level == Level.level1) || (level == Level.level2) {
//            b.layoutBlock()        // To change the first number in chained blocks (Level 1 and 2) when they are detached
//
//            // Check if block should be grayed out
//            if b.previousBlock?.isInactive == true || b.previousBlock?.evaluate() != .correct {
//                b.isInactive = true
//            }
//        }
        b.evaluateDisplayAndUpdate()
    }

    // Detach a block from the previous block in a vertical chain or from its parents, if it is nestable
    fun detachBlock() {
        if (type == BlockType.start) {     // nothing to do for start blocks
            return
        }

        if (isNestable) {
//            if self == parent?.nestedChild1 {
//                parent?.nestedChild1 = nil
//            } else {
//                parent?.nestedChild2 = nil
//            }
//
//            // Redraw the parent tree
//            if let parentExists = parent {
//                layoutNestedBlocksOfTree(containing: parentExists)
//            }
//            parent = nil
        } else {
            previousBlock?.nextBlock = null
        }
        previousBlock = null

//        if (level == .level1) || (level == .level2) {
//            layoutBlock()        // To change the first number in chained blocks (Level 1 and 2) when they are detached
//            isInactive = false
//        }
//
        evaluateDisplayAndUpdate()
    }

    private fun layoutBlock() {

        // Should we remove all views here?
        if (isStart) {
            //layoutStartBlock()
        } else if (type == BlockType.equals) {
            //layoutEqualsBlock()
        } else if (isNestable) {
            //layoutNestedBlock()
        } else if (level == Level.level1) {
            //layoutBlockLevel1()
        } else if (level == Level.level2) {
            //layoutBlockLevel2()
        } else {
            layoutBlockLevels3and4()
        }
    }

    private fun layoutBlockLevels3and4()
    {
        addColorPickerPutton()
        firstNumber = addButton("",showPopupOnRight = false)
        addLabel(mathOperator)
        secondNumber = addButton("",showPopupOnRight = false)
        if (type == BlockType.doubleAddition) {
            addLabel(mathOperator)
            thirdNumber = addButton("",showPopupOnRight = false)
        }
        addLabel("=")
        answer = addButton("",showPopupOnRight = true)
    }

    /* This function configures a button in the block. The buttons are where the user enters numbers.*/
    private fun addLabel(text: String) {
        val label = TextView(context)
        val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        label.text = text
        label.textSize = 24f
        label.setTextColor(ContextCompat.getColor(context, R.color.darkGray))
        val typeface: Typeface? = ResourcesCompat.getFont(context, R.font.raleway)
        label.typeface = typeface
        label.gravity = Gravity.CENTER
        label.setPadding(padding, 0, padding, 0)
        this.addView(label)
    }

    /* This function configures a button in the block. The buttons are where the user enters numbers.*/
    private fun addButton(text: String,showPopupOnRight: Boolean): Button {
        val button = Button(context)
        val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
        button.setBackgroundResource(R.drawable.text_box)
        val params = LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        val scale = context.resources.displayMetrics.density
        val pixelsHeight = (heightOfButton * scale + 0.5f).toInt()
        val pixelsWidth = (widthOfButton * scale + 0.5f).toInt()
        params.height = pixelsHeight
        button.minWidth = pixelsWidth
        button.minimumWidth = pixelsWidth
        //params.setMargins(0, 5, 20, 0)
        button.layoutParams = params
        button.text = text
        button.textSize = 24f
        button.setTextColor(ContextCompat.getColor(context, R.color.darkGray))
        val typeface: Typeface? = ResourcesCompat.getFont(context, R.font.raleway)
        button.typeface = typeface
        button.gravity = Gravity.CENTER
        button.setPadding(padding, 0, padding, 0)
        button.setOnClickListener { v: View ->
            val context = context as GlowPixActivity
            selectedButton = button
            context.numberPadFragment.numberPadDelegate = this
            context.numberPadFragment.resetNumber()

            context.showNumberPad(true, showPopupOnRight, x = locationOnScreen[0].toFloat() + button.x, y = locationOnScreen[1].toFloat() + button.y,boxWidth = button.width.toFloat(), boxHeight = button.height.toFloat())
        }
        this.addView(button)
        return button
    }

    private fun addColorPickerPutton() {
        // Add color picker button
        //this.removeView(colorPickerButton)
        colorPickerButton = Button(this.context)
        colorPickerButton.setBackgroundResource(ledColor.colorButtonImage)
        this.addView(colorPickerButton)
        val params = LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        val scale = context.resources.displayMetrics.density
        val pixels = (colorButtonSize * scale + 0.5f).toInt()
        params.width = pixels
        params.height = pixels
        params.setMargins(0, 5, 20, 0)
        colorPickerButton.layoutParams = params
        colorPickerButton.setOnClickListener {
            val context = context as GlowPixActivity
            context.colorPickerFragment.colorPickerDelegate = this
            context.colorPickerFragment.setExistingColor(ledColor)

            // Find out where the block is to show the color picker popup
            val selectedLocation = IntArray(2)
            this.getLocationOnScreen(selectedLocation)
            context.showColorPicker(true, x = locationOnScreen[0].toFloat(), y = locationOnScreen[1].toFloat(), blockHeight = heightOfRectangle)
        }
        colorPickerButton.isFocusable = false
    }

    // Required for ColorPickerDelegate
    override fun colorSelected(color: PixelColor) {
        ledColor = color
        blockDelegate?.updateGlowBoard()
        blockDelegate?.savePicture()
    }

    // These two functions required for NumberPadDelegate
    /* This is the function that is called when a user taps a number on the pop-up number pad. */
    override fun numberChanged(number: Int?) {
        if (selectedButton != null) {
            if (number != null) {
                selectedButton!!.text = number.toString()
            } else {
                selectedButton!!.text = ""
            }
        }
//            if (button.titleLabel?.intrinsicContentSize.width ?? 0 > widthOfButton) {
//            button.sizeToFit()
//            addBorder(button: button)
            // Since button has gotten bigger, we need to layout the block again to adjust
            //layoutBlock()
        //}

            // If this is level 1 or 2, changing the answer changes the next block as well (remember
            // that the level 1 and 2 start blocks store the starting number in answer too)
//            if button == answer && (level == .level1 || level == .level2)  {
//                nextBlock?.layoutBlock()
//            }
        //}
    }

    /* This function is called when the keypad is dismissed */
    override fun keypadDismissed() {
        selectedButton = null
        // Evaluate the blocks, update the error tags, and notify the block delegate that the blocks have changed.
        evaluateDisplayAndUpdate()
    }

    private fun checkExpression(expressionValue: Int, answerValue: Int?): EvaluationOptions {

        val maxValue = if (level == Level.level4 || level == Level.level5) 144 else 120

        return if ((1..maxValue).contains(expressionValue)) {
            if (answerValue != null) {
                if (expressionValue == answerValue) EvaluationOptions.correct else EvaluationOptions.incorrect
            } else {
                EvaluationOptions.incomplete
            }
        } else {
            EvaluationOptions.offGlowBoard
        }
    }

    // Extension to the Button class to help us get the numbers from the buttons
    private fun Button.number(): Int? {
        return text.toString().toIntOrNull()
    }

    private fun evaluate(): EvaluationOptions {
        // If this function is called for the nesting operation blocks in level 5, we want to find the outermost block and evaluate it
        if (type == BlockType.start) {
            return EvaluationOptions.correct
        }
        else if (isNestable) {
            var outermostBlock = this
            while (outermostBlock.parent != null) {
                outermostBlock = outermostBlock.parent!!
            }
            if (outermostBlock.type == BlockType.equals) {
                return outermostBlock.evaluate()
            } else {
                return EvaluationOptions.incomplete   // nesting blocks that are not in an equals block are incomplete
            }
        } else if (type == BlockType.equals) {    // evaluate the equals blocks for level 5
            if (nestedChild1 != null) {
                if (nestedChild1!!.evaluateNested() != null) {
                    return checkExpression(nestedChild1!!.evaluateNested()!!, answer.number())
                } else {
                    return EvaluationOptions.incomplete   // Either the answer or a button within the expression is blank
                }
            } else {    // There is no block nested inside the left side of equals block
                return EvaluationOptions.incomplete
            }
        } else if (type == BlockType.doubleAddition) {  // this is the only one with three numbers
            val num1 = firstNumber.number()
            val num2 = secondNumber.number()
            val num3 = thirdNumber?.number()
            if ((num1 != null) && (num2 != null) && (num3 != null)) {
                val expressionValue = num1 + num2 + num3
                return checkExpression(expressionValue, answer.number())
            } else {
                return EvaluationOptions.incomplete
            }
        } else {       // Levels 1-4 all blocks except the double addition one
            val num1 = firstNumber.number()
            val num2 = secondNumber.number()
            if ((num1 != null) && (num2 != null)) {
                val expressionValue: Int = {
                    when (mathOperator) {
                        "+" -> num1 + num2
                        "−" -> num1 - num2
                        "×" -> num1 * num2
                        else -> num1 / num2
                    }
                }()
                return checkExpression( expressionValue, answer.number())
            } else {
                return EvaluationOptions.incomplete
            }
        }
    }

    // This function evaluates blocks that are only nested inside other blocks. It calls itself
    // recursively because blocks may be nested inside other blocks. It returns either the
    // value of the nested expression, or nil if one of the buttons in the nested expression does not contain a number
    private fun evaluateNested(): Int? {
        if (!isNestable) {
            Log.e("GlowPix","Call to evaluateNested for a block that is not nested")
        }

        var num1: Int? = null
        var num2: Int? = null
        if (nestedChild1 != null) {
            num1 = nestedChild1!!.evaluateNested()
        } else if (firstNumber.number() != null) {
            num1 = firstNumber.number()
        }

        if (nestedChild2 != null) {
            num2 = nestedChild2!!.evaluateNested()
        } else if (secondNumber.number() != null) {
            num2 = secondNumber.number()
        }

        if ((num1 != null) && (num2 != null)) {
            when (mathOperator) {
                "+" -> return num1 + num2
                "−" -> return num1 - num2
                "×" -> return num1 * num2
                else ->  return num1 / num2
            }
        } else {
            return null
        }
    }

    /* This function evaluates a block and displays an error tag if necessary. It also notifies the
    blockDelegate to update the GlowBoard and save the picture, if necessary. It includes an optional
    parameter to indicate whether the function is being used when the picture is being loaded.
    In that case, you don't want to save the blocks (it messes up undo/redo) or call the function
    recursively (because it is called for every block when loading the picture. */
    private fun evaluateDisplayAndUpdate(loadingBlocks:Boolean = false) {
//        if (isInactive) {
//            return
//        }

        val result = evaluate()
        // Don't display errors on nested blocks - want to display errors on the parent equals block
        if (isNestable) {
            var outermostBlock = this
            while (outermostBlock.parent != null) {
                outermostBlock = outermostBlock.parent!!
            }
            if (outermostBlock.type == BlockType.equals) {
                outermostBlock.blockDelegate?.displayError(result,locationOnScreen[0],locationOnScreen[1])
            }
        } else {
            blockDelegate?.displayError(result,locationOnScreen[0],locationOnScreen[1])

            if ((level == Level.level1) || (level == Level.level2)) {
                if (result != EvaluationOptions.correct) {
                    //disableChain()
                } else {
//                    nextBlock?.isInactive = false
//                    if !loadingBlocks {
//                        nextBlock?.evaluateDisplayAndUpdate()
//                    }
                }
            }
        }

//        if !loadingBlocks {
//            blockDelegate?.updateGlowBoard()
//            blockDelegate?.savePicture()
//        }
    }
}