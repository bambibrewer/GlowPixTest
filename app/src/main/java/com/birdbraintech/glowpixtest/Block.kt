package com.birdbraintech.glowpixtest

import android.app.ActionBar
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// The view controller that contains the block should be a delegate so it can be notified when the blocks has changed
interface BlockDelegate {
    fun updateGlowBoard()
    fun savePicture()
}


class Block(val type: BlockType, val level: Level, context: Context): LinearLayout(context), ColorPickerDelegate {

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

                // Have to layout the block to see the changes
                layoutBlock()

            }
        }

    /* All of these variable control the size of the blocks, and the size of the number buttons and labels within them. */
    private val blockHeight: Float = 72.0F
    var heightOfRectangle: Float = (0.804 * blockHeight).toFloat()
//    var heightOfButton: CGFloat {
//        return 2*heightOfRectangle/3
//    }
//
//    let originalBlockWidth: CGFloat = 261.1
//    var widthOfButton: CGFloat {
//        return originalBlockWidth/4
//    }
//
//    var buttonSize: CGSize {
//        return CGSize(width: widthOfButton, height: heightOfButton)
//    }
//
//    let colorButtonSize = CGSize(width: 25, height: 25)
//
//    var labelWidth: CGFloat {
//        return originalBlockWidth/10
//    }
//    var labelSize: CGSize {
//        return CGSize(width: labelWidth, height: heightOfButton)
//    }

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
//        set(value) {
//            if (value == null) {      // if it is nil, want to unshift all the blocks
////                if let viewController = imageView.findViewController() as? LevelViewController {
////                    viewController.numberSelected = selectedButton
////                }
//            }
//            field = value
//        }
//        // Let the view controller know so that it can open/close keypads


    init {
        Log.d("Blocks","created block")
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
//        b.evaluateDisplayAndUpdate()
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
//        b.evaluateDisplayAndUpdate()
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
//        evaluateDisplayAndUpdate()
    }

    private fun layoutBlock() {
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


//        origin = CGPoint(x: originalBlockWidth/4, y: heightOfRectangle/6)
//
//        firstNumber = layoutButtonIfNotSelected(button: firstNumber, origin: origin)
//        origin.x += firstNumber.frame.width
//
//        operatorLabel.removeFromSuperview()
//        operatorLabel = setupLabel(text: mathOperator, origin: origin)
//        imageView.addSubview(operatorLabel)
//        origin.x += operatorLabel.frame.width
//
//        secondNumber = layoutButtonIfNotSelected(button: secondNumber, origin: origin)
//        origin.x += secondNumber.frame.width
//
//        if operatorLabel2 != nil {
//            operatorLabel2?.removeFromSuperview()
//            operatorLabel2 = setupLabel(text: mathOperator, origin: origin)
//            imageView.addSubview(operatorLabel2!)
//            origin.x += operatorLabel2!.frame.width
//        }
//
//        if thirdNumber != nil {
//            thirdNumber = layoutButtonIfNotSelected(button: thirdNumber!, origin: origin)
//            origin.x += thirdNumber!.frame.width
//        }
//
//        equalsLabel.removeFromSuperview()
//        equalsLabel = setupLabel(text: "=", origin: origin)
//        imageView.addSubview(equalsLabel)
//        origin.x += equalsLabel.frame.width
//
//        answer = layoutButtonIfNotSelected(button: answer, origin: origin)
//        origin.x += answer.frame.width
//
//        // Resize the frame of the block itself
//        let newFrame = CGRect(x: imageView.frame.minX, y: imageView.frame.minY, width: origin.x + 10, height: blockHeight + 5)
//        imageView.frame = newFrame
    }

    private fun addColorPickerPutton() {
        // Add color picker button
        this.removeView(colorPickerButton)
        colorPickerButton = Button(this.context)
        colorPickerButton.setBackgroundResource(ledColor.colorButtonImage)
        this.addView(colorPickerButton)
        val params = LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        val scale = context.resources.displayMetrics.density
        val pixels = (28 * scale + 0.5f).toInt()
        params.width = pixels
        params.height = pixels
        params.setMargins(0, 5, 20, 0)
        colorPickerButton.layoutParams = params
        colorPickerButton.setOnClickListener {
            val context = context as GlowPixActivity
            context.colorPickerFragment.colorPickerDelegate = this
            
            // Find out where the block is to show the color picker popup
            val selectedLocation = IntArray(2)
            this.getLocationOnScreen(selectedLocation)
            context.showColorPicker(true, x = selectedLocation[0].toFloat(), y = selectedLocation[1].toFloat(), blockHeight = heightOfRectangle)
        }
        colorPickerButton.isFocusable = false
    }

    // Required for ColorPickerDelegate
    override fun colorSelected(color: PixelColor) {
        ledColor = color
    }

}