package com.birdbraintech.glowpixtest

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout

// The view controller that contains the block should be a delegate so it can be notified when the blocks has changed
interface BlockDelegate {
    fun updateGlowBoard()
    fun savePicture()
}


class Block(val type: BlockType, val level: Level, context: Context): LinearLayout(context) {

    //var imageView: ImageView

    var blockDelegate: BlockDelegate? = null

    var isStart: Boolean = (type == BlockType.start)

    var isNestable: Boolean =
        ((level == Level.level5) && (type != BlockType.equals) && (type != BlockType.start))

    var canContainChildren: Boolean  =
        ((level == Level.level5) && (type != BlockType.start))  // all level 5 blocks can contain children, except start


    /* All of these variable control the size of the blocks, and the size of the number buttons and labels within them. */
    private val blockHeight: Float = 72.0F
    var heightOfRectangle: Float = (0.804*blockHeight).toFloat()
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

    init {
        setBackgroundResource(R.drawable.block_white2)
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
        Log.d("Blocks","position chain images")
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
}