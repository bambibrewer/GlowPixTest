package com.birdbraintech.glowpixtest

import android.app.ActionBar
import android.content.Context
import android.graphics.Color
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
import kotlin.math.roundToInt

// The view controller that contains the block should be a delegate so it can be notified when the blocks has changed
interface BlockDelegate {
    fun updateGlowBoard()
    fun savePicture()
}

// Each block consists of an outer LinearLayout that contains a LinearLayout for the block and
// then the error flag, which may be invisible
class Block(val type: BlockType, val level: Level, context: Context): LinearLayout(context), ColorPickerDelegate, NumberPadDelegate {


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
                blockLayout.setBackgroundResource(value.blockImage)

            }
        }

    /* All of these variable control the size of the blocks, and the size of the number buttons and labels within them. */
    private val blockHeight: Float = 80.0F
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

    var blockLayout = LinearLayout(context)
    val errorFlag = TextView(context)
    var colorPickerButton = Button(context)
    var firstNumber = Button(context)
    var secondNumber = Button(context)
    var operatorLabel2: TextView? = null
    var thirdNumber: Button? = null
    var answer = Button(context)    // also stores the starting number for start blocks in levels 1 and 2

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

    var isFirstInChain: Boolean = ((parent == null) && (previousBlock == null))

    var isInactive: Boolean = false
        set(value) {
            field = value
            if (isInactive) {
                blockLayout.setBackgroundResource(R.drawable.block_gray2)

                // Have to layout the block to see the changes
                //layoutBlock()

                // Disabled block should not have errors
                displayError(EvaluationOptions.incomplete)

                // Disable buttons
                answer.isEnabled = false
                secondNumber.isEnabled = false
                colorPickerButton.isEnabled = false
            } else {
                blockLayout.setBackgroundResource(ledColor.blockImage)

                // Have to layout the block to see the changes
                //layoutBlock()

                // Enable buttons
                answer.isEnabled = true
                secondNumber.isEnabled = true
                colorPickerButton.isEnabled = true
            }
        }

    // Set up the structure - in the outer LinearLayout, we have a LinearLayout for the block and
    // then the error flag, which may be invisible
    init {

        // Set up the block first
        if (type == BlockType.start) {
            blockLayout.setBackgroundResource(R.drawable.start_block_background)
        } else if (level == Level.level5 && type != BlockType.equals) {
            blockLayout.setBackgroundResource(R.drawable.block_nested)
        } else {
            blockLayout.setBackgroundResource(R.drawable.block_white2)
        }
        this.addView(blockLayout)

        // Set up the errorflag
        errorFlag.setTextColor(Color.WHITE)
        errorFlag.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val params = LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, (blockHeight - heightOfRectangle).roundToInt(), 0, 0)
        errorFlag.layoutParams = params
        this.addView(errorFlag)
        displayError(EvaluationOptions.incomplete)

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
        this.y = y
    }

    // Position the vertical stack of blocks
    fun positionChainImages(){
        if (!isNestable) {        // This function shouldn't be called for a nestable block
            if (nextBlock != null) {
                nextBlock?.goToPosition(whenConnectingToBlock = this)
                nextBlock?.bringToFront() // Bring the image views to the front so that the last in the chain is on top
                if (nextBlock?.type == BlockType.equals) {
                    nextBlock!!.layoutEqualsBlock()
                }
                nextBlock?.positionChainImages()
            }
        }
    }


    override fun bringToFront() {
        super.bringToFront()
//        if !isNestable {
//            nextBlock?.bringToFront()
//        }
//        if canContainChildren {
//            layoutBlock()
//        }
    }

    //Attach a block chain below this one
    fun attachBlock (b: Block){
        if (nextBlock != null) {
            b.attachToChain(nextBlock!!)
        }
        nextBlock = b
        b.previousBlock = this

        // To change the first number in a chained block (Level 1 and 2) when it is attached
        if ((level == Level.level1) || (level == Level.level2)) {
            b.layoutBlock()        // To change the first number in chained blocks (Level 1 and 2) when they are detached
            // Check if block should be grayed out
            if ((b.previousBlock?.isInactive == true) || (b.previousBlock?.evaluate() != EvaluationOptions.correct)) {
                b.isInactive = true
            }
        }
        b.evaluateDisplayAndUpdate()
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
        if ((level == Level.level1) || (level == Level.level2)) {
            b.layoutBlock()        // To change the first number in chained blocks (Level 1 and 2) when they are detached

            // Check if block should be grayed out
            if ((b.previousBlock?.isInactive == true) || (b.previousBlock?.evaluate() != EvaluationOptions.correct)) {
                b.isInactive = true
            }
        }
        b.evaluateDisplayAndUpdate()
    }

    // Insert a block into this one (only for nesting blocks and the equals block)
    fun insertBlock(blockToInsert: Block, intoButton: Button){
        if (!canContainChildren) {
            Log.e("GlowPix","insertBlock should only be called on blocks that can contain nested children")
        }

        if (intoButton == firstNumber) {
            nestedChild1 = blockToInsert
        } else {
            nestedChild2 = blockToInsert
        }

        // Now I want to redraw all nested blocks in this tree
        if (type == BlockType.equals) {
            layoutEqualsBlock()
        } else {
            layoutNestedBlocksOfTree(this)
        }
    }

    // Detach a block from the previous block in a vertical chain or from its parents, if it is nestable
    fun detachBlock() {
        if (type == BlockType.start) {     // nothing to do for start blocks
            return
        }

        if (isNestable) {
            if (this == parent?.nestedChild1) {
                parent?.nestedChild1 = null
            } else {
                parent?.nestedChild2 = null
            }

            // Redraw the parent tree
            if (parent != null) {
                layoutNestedBlocksOfTree(parent!!)
            }
            parent = null
        } else {
            previousBlock?.nextBlock = null
        }
        previousBlock = null

        if ((level == Level.level1) || (level == Level.level2)) {
            firstNumber.text = ""      // To change the first number in chained blocks (Level 1 and 2) when they are detached
            isInactive = false
        }

        evaluateDisplayAndUpdate()
    }

    // Disables every block below this one
    private fun disableChain() {
        var next = nextBlock
        while (next != null) {
            next?.isInactive = true
            next = next?.nextBlock
        }
    }

    // Returns true if this block contains the block given by the parameter
    fun contains(block: Block): Boolean {
        if (!canContainChildren) {
            return false            // can't possibly contain a block.
        }

        if (!block.isNestable) {
            return false            // can't contain a block that can't next
        }

        var parent = block.parent
        while (parent != null) {
            if (this == parent) {
                return true
            }
            parent = parent?.parent
        }
        return false
    }

    private fun layoutBlock() {
        if (isStart) {
            layoutStartBlock()
        } else if (type == BlockType.equals) {
            layoutEqualsBlock()
        } else if (isNestable) {
            layoutNestedBlock()
        } else if (level == Level.level1) {
            layoutBlockLevel1()
        } else if (level == Level.level2) {
            layoutBlockLevel2()
        } else {
            layoutBlockLevels3and4()
        }
    }

    private fun layoutStartBlock() {
        // Resize the frame of the block itself
        val scale = context.resources.displayMetrics.density
        val pixelsHeight = (blockHeight * scale + 0.5f).toInt()
        blockLayout.layoutParams.height = pixelsHeight
        blockLayout.gravity = Gravity.CENTER

        // add the start label
        val startString = if ((level == Level.level1) || (level == Level.level2)) context.getString(R.string.start_chained) else context.getString(R.string.start)
        val label = TextView(context)
        val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        label.text = startString
        label.textSize = 24f
        label.setTextColor(ContextCompat.getColor(context, R.color.white))
        val typeface: Typeface? = ResourcesCompat.getFont(context, R.font.raleway_bold)
        label.typeface = typeface
        label.gravity = Gravity.CENTER
        label.setPadding(padding, 0, padding, 0)
        blockLayout.addView(label)

        // add a number button for levels 1 and 2
        if ((level == Level.level1) || (level == Level.level2)) {
            answer = addButton("",showPopupOnRight = true)

        }
    }

    private fun layoutBlockLevel1()
    {
        blockLayout.removeAllViews()
        addColorPickerPutton()
        firstNumber = addDisabledButton(getPreviousAnswer())
        addLabel(mathOperator)
        val secondNumString = if ((type == BlockType.addition1 || type == BlockType.subtraction1)) "1" else "10"
        secondNumber = addDisabledButton(secondNumString)
        addLabel("=")
        answer = addButton(answer.text.toString(),showPopupOnRight = true)
    }

    private fun layoutBlockLevel2()
    {
        blockLayout.removeAllViews()
        addColorPickerPutton()
        firstNumber = addDisabledButton(getPreviousAnswer())
        addLabel(mathOperator)
        secondNumber = addButton(secondNumber.text.toString(),showPopupOnRight = false)
        addLabel("=")
        answer = addButton(answer.text.toString(),showPopupOnRight = true)
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

    // This block finds the top of a nested tree containing a block and then lays out the entire tree
    fun layoutNestedBlocksOfTree(block: Block) {
        var outermostBlock = block
        while (outermostBlock.parent != null) {
            outermostBlock = outermostBlock.parent!!
        }
        if (outermostBlock.type == BlockType.equals) {
            outermostBlock.layoutEqualsBlock()
        } else {
            outermostBlock.layoutNestedBlock()
        }
    }

    private fun layoutNestedBlock() {
        blockLayout.removeAllViews()
        addLabel("(")

        if (nestedChild1 != null) {
            //nestedChild1?.imageView.frame.origin.x = imageView.frame.origin.x + origin.x
            //nestedChild1?.imageView.frame.origin.y = imageView.frame.origin.y
            nestedChild1?.bringToFront()
            nestedChild1?.layoutNestedBlock()
            //origin.x += nestedChild1?.imageView.frame.width ?? 0
        } else {
            firstNumber = addButton(firstNumber.text.toString(), false)
        }

        addLabel(mathOperator)

        // We don't want to remove a button if we are currently changing the number in it
        if (nestedChild2 != null) {
            //nestedChild1?.imageView.frame.origin.x = imageView.frame.origin.x + origin.x
            //nestedChild1?.imageView.frame.origin.y = imageView.frame.origin.y
            nestedChild2?.bringToFront()
            nestedChild2?.layoutNestedBlock()
            //origin.x += nestedChild1?.imageView.frame.width ?? 0
        } else {
            secondNumber = addButton(secondNumber.text.toString(), false)
        }

        // Add closing parentheses
        addLabel(")")
    }

    private fun layoutEqualsBlock() {
        blockLayout.removeAllViews()
        addColorPickerPutton()

        //firstNumber.removeFromSuperview()
        if (nestedChild1 != null) {
//            nestedChild1?.imageView.frame.origin.x = imageView.frame.origin.x + origin.x
//            nestedChild1?.imageView.frame.origin.y = imageView.frame.origin.y + 0.5*(heightOfRectangle - (nestedChild1?.imageView.frame.height ?? 0))
            nestedChild1?.bringToFront()
            nestedChild1?.layoutNestedBlock()
//            origin.x += nestedChild1?.imageView.frame.width ?? 0
        } else {
            firstNumber = addButton(context.getString(R.string.add_block),false)
            firstNumber.setTextColor(ContextCompat.getColor(context, R.color.sectionTeal))
            firstNumber.isClickable = false     // Can't add a number here
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
        blockLayout.addView(label)
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
        button.isAllCaps = false
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
        blockLayout.addView(button)
        return button
    }

    /* Some of the numbers in Levels 1 and 2 are disabled buttons. This function configures those.*/
    private fun addDisabledButton(text: String): Button {
        val button = Button(context)
        val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
        button.setBackgroundResource(R.drawable.text_box_borderless)
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
        button.isClickable = false      // make touches pass through
        blockLayout.addView(button)
        return button
    }

    private fun addColorPickerPutton() {
        // Add color picker button
        colorPickerButton = Button(this.context)
        colorPickerButton.setBackgroundResource(ledColor.colorButtonImage)
        blockLayout.addView(colorPickerButton)
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

            // If this is level 1 or 2, changing the answer changes the next block as well (remember
            // that the level 1 and 2 start blocks store the starting number in answer too)
            if ((selectedButton == answer) && (level == Level.level1 || level == Level.level2))  {
                nextBlock?.layoutBlock()
            }
        }
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

    // This function is used in Levels 1 and 2, where the answer to the previous block is the first number in the next
    // one. For the start blocks in levels 1 and 2, the starting number is also stored in the block's answer field.
    private fun getPreviousAnswer(): String {
        return if (previousBlock == null) {
            ""
        } else {
            previousBlock?.answer?.text.toString()
        }
    }

    /* This function evaluates a block and displays an error tag if necessary. It also notifies the
    blockDelegate to update the GlowBoard and save the picture, if necessary. It includes an optional
    parameter to indicate whether the function is being used when the picture is being loaded.
    In that case, you don't want to save the blocks (it messes up undo/redo) or call the function
    recursively (because it is called for every block when loading the picture. */
    private fun evaluateDisplayAndUpdate(loadingBlocks:Boolean = false) {
        if (isInactive) {
            return
        }

        val result = evaluate()
        // Don't display errors on nested blocks - want to display errors on the parent equals block
        if (isNestable) {
            var outermostBlock = this
            while (outermostBlock.parent != null) {
                outermostBlock = outermostBlock.parent!!
            }
            if (outermostBlock.type == BlockType.equals) {
                outermostBlock.displayError(result)
            }
        } else {
            displayError(result)

            if ((level == Level.level1) || (level == Level.level2)) {
                if (result != EvaluationOptions.correct) {
                    disableChain()
                } else {
                    nextBlock?.isInactive = false
                    //if !loadingBlocks {
                        nextBlock?.evaluateDisplayAndUpdate()
                    //}
                }
            }
        }

//        if !loadingBlocks {
            blockDelegate?.updateGlowBoard()
            blockDelegate?.savePicture()
//        }
    }

    private fun displayError(result:EvaluationOptions) {
        when (result) {
            EvaluationOptions.incomplete, EvaluationOptions.correct -> errorFlag.visibility = View.INVISIBLE
            EvaluationOptions.incorrect -> {
                errorFlag.setBackgroundResource(R.drawable.error_flag_red)
                errorFlag.setText(R.string.incorrect_error)
                errorFlag.visibility = View.VISIBLE
            }
            EvaluationOptions.offGlowBoard -> {
                errorFlag.setBackgroundResource(R.drawable.error_flag_yellow)
                errorFlag.setText(R.string.offgrid_error)
                errorFlag.visibility = View.VISIBLE
            }
        }
    }
}