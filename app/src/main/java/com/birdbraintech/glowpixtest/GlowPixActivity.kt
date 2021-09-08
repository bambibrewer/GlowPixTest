package com.birdbraintech.glowpixtest

import android.content.ClipData
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.round


class GlowPixActivity : AppCompatActivity(), BlockDelegate, ColorPickerDelegate {

    private lateinit var colorPaletteFragment: ColorPaletteFragment

    private var blockBeingDragged: Block? = null

    //Keep track of all the active blocks
    private val workspaceBlocks = mutableListOf<Block>()   // moveable blocks that can make chains

    private var selectedBox: TextView? = null
    var selectedBlock: Block? = null

    // This variable is used to keep track of a block that has been shifted by a ghost image
    private var shiftedBlock: Block? = null
        set(value)  {
            if (value == null) {      // if it is nil, want to unshift all the blocks
                // unshift before changing the value
                if (field?.previousBlock != null) {
                    field?.goToPosition(field?.previousBlock!!)
                }
                field?.positionChainImages()
            }
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* This section of code creates the scrolling area and then positions
        the start block and script within it. */
        val padding = 5000  // Determines the amount of scrolling to the edge - not unlimited so kids don't lose scripts
        workspace.setPadding(padding, padding, padding, padding)

        // Scroll to where the main script is - we need an observer for this because the workspace doesn't have the
        // script in it at initialization. The observer will fire once and remove itself.
        scroll.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                scroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                scroll.scrollTo((workspace.width/2),padding)
            }
        })

        // Set up the blocks on the left menu for this level
        setupMenu(Level.level3)

        val dragListener = View.OnDragListener { _, dragEvent ->
            handleBlockMove(dragEvent)
        }
        workspace.setOnDragListener(dragListener)

        val dragListenerMenu = View.OnDragListener { _, dragEvent ->
            deleteBlocks(dragEvent)
        }
        block_menu.setOnDragListener(dragListenerMenu)

        colorPaletteFragment = supportFragmentManager.findFragmentById(R.id.fragmentColorPalette) as ColorPaletteFragment
        colorPaletteFragment.colorPickerDelegate = this
        //colorPaletteFragment.setExistingColor(PixelColor.white)


    }

    // This function sets up the available blocks for each level
    private fun setupMenu(level: Level) {
        val blocks = blocksByLevel[level]

        // Set up all the blocks in the menu
        if (blocks != null) {
            for (i in 0 until block_menu.childCount) {
                val v: View = block_menu.getChildAt(i)
                if (i < blocks.size) {
                    val blockPair = blocks[i]
                    v.visibility = View.VISIBLE
                    setupMenuBlock(v, level = level, type = blockPair.first, drawableInt = blockPair.second)
                } else {
                    v.visibility = View.INVISIBLE
                }
            }
        }
    }

    // This function sets up a menu block to add a new block when it is dragged into the workspace
    private fun setupMenuBlock(menuBlockView: View, level: Level, type: BlockType, drawableInt: Int) {
        (menuBlockView as ImageView).setImageResource(drawableInt)
        menuBlockView.scaleX = 1.2f
        menuBlockView.scaleY = 1.2f
        // If you make a long click, start to drag the block
        val onLongClickListener = View.OnLongClickListener { view ->
            // A menu block creates a new block in the workspace
            val newBlock = Block(type,level,this)
            addBlockToWorkspace(newBlock)
            blockBeingDragged = newBlock
            ViewCompat.startDragAndDrop(view, ClipData.newPlainText("", ""),
                View.DragShadowBuilder(view), view, 0)
        }
        menuBlockView.setOnLongClickListener(onLongClickListener)
    }

    private fun addBlockToWorkspace(block: Block) {

        block.blockDelegate = this

        // If you make a long click, start to drag the block
        val onLongClickListener = View.OnLongClickListener { view ->
            startBlockDrag(view)
        }
        block.setOnLongClickListener(onLongClickListener)


        //All this just to add a shadow
//        view.layer.shadowColor = UIColor.lightGray.cgColor
//        view.layer.shadowOpacity = 1
//        view.layer.shadowOffset = CGSize(width: 0.0, height: 2.0)
//        view.layer.shadowRadius = 1

        workspace.addView(block)
        workspaceBlocks.add(block)

        // block is invisible until it is dropped in the workspace
        block.visibility = View.INVISIBLE
    }

    private fun handleBlockMove(dragEvent: DragEvent): Boolean {
        return when (dragEvent.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                if (blockBeingDragged != null) {
                    // Move each block in the chain by the translation amount

                    blockBeingDragged?.x = dragEvent.x - (blockBeingDragged?.width?.div(2) ?: 0)
                    blockBeingDragged?.y = dragEvent.y - (blockBeingDragged?.height?.div(2) ?: 0)

                    // We also need to update the position of any children below this block

                    if (blockBeingDragged!!.canContainChildren) {
//                        gestureBlock.layoutNestedBlocksOfTree(containing: gestureBlock)
//                        if gestureBlock.type == .equals {
//                            gestureBlock.positionChainImages()
//                        }
                    } else  {
                        blockBeingDragged!!.positionChainImages()
                    }

                    // Look for a block that could be connected to and produce a ghost image
                    addGhostImageOrNestTarget(blockBeingDragged!!)
                }
                true
            }
            DragEvent.ACTION_DROP -> {
                removeGhostImage()

                if (blockBeingDragged != null) {
                    nestOrAttach(blockBeingDragged!!)
                }
                hideBlocks(false)
                blockBeingDragged = null
                true
            }
            else -> true
        }
    }

    private fun deleteBlocks(dragEvent: DragEvent): Boolean {
        return when (dragEvent.action) {
            DragEvent.ACTION_DROP -> {
                removeGhostImage()

                if (blockBeingDragged != null) {
                    deleteBlockChain(blockBeingDragged!!)
                }

                blockBeingDragged = null
                false
            }
            else -> true
        }
    }

    // This function recursively deletes blocks
    private fun deleteBlockChain(startingWithBlock: Block?, shouldAutoCreateNewStartBlock:Boolean = true) {
        if (startingWithBlock != null) {
            // Delete the next blocks in the sequence
            deleteBlockChain(startingWithBlock = startingWithBlock.nextBlock, shouldAutoCreateNewStartBlock = shouldAutoCreateNewStartBlock)

            // Delete any children that exist
//            if let child1 = block.nestedChild1 {
//                deleteBlockChain(startingWith: child1, shouldAutoCreateNewStartBlock: shouldAutoCreateNewStartBlock)
//            }
//            if let child2 = block.nestedChild2 {
//                deleteBlockChain(startingWith: child2, shouldAutoCreateNewStartBlock: shouldAutoCreateNewStartBlock)
//            }

            // Delete this block
            deleteBlock(block = startingWithBlock, shouldAutoCreateNewStartBlock = shouldAutoCreateNewStartBlock)
        }
    }

    // This function removes a single block
    private fun deleteBlock(block: Block, shouldAutoCreateNewStartBlock:Boolean = true){
        // if we delete a start block, add a new one if auto-creation is enabled
        if (shouldAutoCreateNewStartBlock && (block.isStart)) {
            //addStartBlock()
        }

        // Remove the view from our workspace and from our list of blocks
        workspace.removeView(block)
        workspaceBlocks.remove(block)
    }

    private fun startBlockDrag(view: View): Boolean {
        blockBeingDragged = view as? Block
        hideBlocks(true)

        //if there is a block ahead of us on the chain, moving this block will change that
        blockBeingDragged?.detachBlock()
//        if (blockBeingDragged?.isNestable) {
//            block.imageView.center = gesture.location(in: self.view)
//        }
//        block.bringToFront()

        // Need to create a shadow of all the blocks here
        //val data = Pair(touchX.toInt(), touchY.toInt())
        val shadowBuilder: View.DragShadowBuilder = object : View.DragShadowBuilder(view) {
            /* This function determines the size of the shadow */
            override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
                super.onProvideShadowMetrics(outShadowSize, outShadowTouchPoint)

                var numExtraBlocks = 1
                var maxWidth = blockBeingDragged?.width ?: 0
                var nextBlock = blockBeingDragged?.nextBlock
                while (nextBlock != null) {
                    numExtraBlocks += 1
                    if (nextBlock.width > maxWidth) {
                        maxWidth = nextBlock.width
                    }
                    nextBlock = nextBlock?.nextBlock
                }
                numExtraBlocks = kotlin.math.max(numExtraBlocks - 1, 0)
                val yBlockOffset = (round(blockBeingDragged?.heightOfRectangle ?: 0f)).toInt()
                outShadowSize.y = outShadowSize.y + numExtraBlocks*yBlockOffset
                outShadowSize.x = maxWidth
            }

            /* This function draws a drag shadow that contains all the blocks that will move */
            override fun onDrawShadow(canvas: Canvas) {
                var nextBlock = blockBeingDragged
                val yBlockOffset = (blockBeingDragged?.heightOfRectangle ?: 0f)
                while (nextBlock != null) {
                    nextBlock!!.draw(canvas)
                    canvas.translate(0.0F, yBlockOffset)
                    nextBlock = nextBlock?.nextBlock
                }
            }
        }
        return ViewCompat.startDragAndDrop(view, ClipData.newPlainText("", ""), shadowBuilder, view, 0)
    }

    // This function is used to hide or show the blocks being dragged
    private fun hideBlocks(wantToHide: Boolean) {
        var nextBlock = blockBeingDragged
        while (nextBlock != null) {
            nextBlock.visibility = if (wantToHide) View.INVISIBLE else View.VISIBLE
            nextBlock = nextBlock?.nextBlock
        }
    }

    //This function returns the block in the workspace that the block
    //should attach to if dropped (or nil if not close enough to any)
    //Should probably find the closest, but this isn't usually an issue
    private fun blockAttachable(toBlock: Block): Block? {
//        let offset = canvasOffset(of: block)
//
        var attachBlock: Block? = null
        if ((toBlock.type != BlockType.start) && !toBlock.isNestable) { // start block and nestable blocks do not snap to anything
            for (workspaceBlock in workspaceBlocks) {
                if ((workspaceBlock != toBlock) && !workspaceBlock.isNestable) { // Don't want to snap to itself or a nestable block

                    val minX = workspaceBlock.x + workspaceBlock.width/2 - toBlock.width/2  //+ offset.x
                    val maxX = workspaceBlock.x + workspaceBlock.width/2 + toBlock.width/2  //+ offset.x
                    val minY = workspaceBlock.y + workspaceBlock.height/2 + workspaceBlock.offsetToNext + toBlock.offsetToPrevious - 10.0  //+ offset.y
                    val maxY = workspaceBlock.y + workspaceBlock.height/2 + workspaceBlock.offsetToNext + toBlock.offsetToPrevious + toBlock.height / 2.0 //+ offset.y

                    val toBlockCenterX = toBlock.x + toBlock.width/2
                    val toBlockCenterY = toBlock.y + toBlock.height/2
                    if (toBlockCenterX > minX && toBlockCenterX < maxX &&
                            toBlockCenterY > minY && toBlockCenterY < maxY) {
                        attachBlock = workspaceBlock
                    }
                }
            }
        }
        return attachBlock
    }

    private fun addGhostImageOrNestTarget(block: Block/*, gesture: UIPanGestureRecognizer*/) {
        shiftedBlock = null // unshift any blocks

        //Look for a block that could be connected to and produce a ghost image
        val attachableBlock = blockAttachable(block)
        if (block.isNestable) {
//            removeShadedButtons()
//            let (_, targetButton) = targetForNestedBlock(block: block)
//            if let buttonToReplace = targetButton {
//                buttonToReplace.backgroundColor = UIColor.lightGray
//            }
        } else if (attachableBlock != null) {
            val ghostPosition = block.getPositionForGhost(whenConnectingToBlock = attachableBlock)
            addGhostImage(ofBlock = block, atPosition = ghostPosition)
            block.bringToFront()

            // now we need to draw the rest of the blocks that were in the chain below the drop target
            shiftedBlock = attachableBlock.nextBlock
            val blocksAfterGhostPosition = block.getPositionForBlocksAttachedToGhost(whenConnectingToBlock = attachableBlock)
            attachableBlock.nextBlock?.x = blocksAfterGhostPosition.first
            attachableBlock.nextBlock?.y = blocksAfterGhostPosition.second
            attachableBlock.nextBlock?.positionChainImages()

        } else {
            removeGhostImage()
        }
    }

    /* This function nests or attaches a block if that is currently possible. */
    private fun nestOrAttach(block: Block) {
        shiftedBlock = null   // unshift any blocks

        if (block.isNestable) {
//            let (targetBlock, targetButton) = targetForNestedBlock(block: block)
//            if let parentBlock = targetBlock, let buttonToReplace = targetButton {
//                block.parent = parentBlock
//                parentBlock.insertBlock(blockToInsert: block, intoButton: buttonToReplace)
//            }
        } else {
            blockAttachable(block)?.attachBlock(block)
        }
    }

    private fun addGhostImage(ofBlock: Block, atPosition: Pair<Float,Float>) {
        ghostBlock.x = atPosition.first
        ghostBlock.y = atPosition.second
        ghostBlock.width = ofBlock.width
        ghostBlock.visibility = View.VISIBLE
    }

    private fun removeGhostImage() {
        ghostBlock.visibility = View.INVISIBLE
    }

    fun showNumberPad(isVisible: Boolean, showOnRight: Boolean, selectedBox: TextView? = this.selectedBox) {
//        if (isVisible) {
//            pictureName.clearFocus()        // Don't want text box to have focus when the numberpad is visible
//        }
//        val numberPad = findViewById<View>(R.id.fragmentNumberPad)
//        if (!isVisible) {
//            numberPad.visibility = View.INVISIBLE
//            if (selectedBox != null) {
//                selectedBox.setBackgroundResource(R.drawable.text_box)
//                selectedBlock?.updateErrorFlag()
//                // TO REMOVE - This is just to record problems they got wrong for the pilots
//                //if (selectedBlock != null) {recordAnswerIfWrong(selectedBlock!!)}
//
//                // End of TO REMOVE
//            }
//            return
//        }
//
//        showColorPalette(false)
//        val numberBackground = ContextCompat.getDrawable(this, R.drawable.number_pad_popup)!!
//
//        if (selectedBox == null) {
//            throw AssertionError("showNumberPad called to show on null selected box")
//        }
//        selectedBox.getLocationOnScreen(selectedLocation)
//
//        val arrow: Drawable
//        val left: Int
//        val right: Int
//        if (showOnRight) {
//            arrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_left)!!
//
//            left = 0
//            right = numberBackground.intrinsicWidth
//
//            numberPad.x = (selectedLocation[0] + selectedBox.width).toFloat()
//        } else {
//            arrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_right)!!
//
//            left = numberBackground.intrinsicWidth
//            right = 0
//
//            numberPad.x = (selectedLocation[0] - numberPad.width).toFloat()
//        }
//
//        val screenPercentage = (selectedLocation[1] + selectedBox.height / 2 - menuBar.height).toDouble() / (window.decorView.height - menuBar.height)
//        val distanceFromTop = (arrow.intrinsicHeight + (numberBackground.intrinsicHeight - arrow.intrinsicHeight * 2) * screenPercentage).toInt()
//
//        // Put the arrow and the white background of the numberpad together.
//        val together = LayerDrawable(arrayOf(numberBackground, arrow))
//        // Compress the white number background on both sides by the width of the arrow (both sides for symmetry)
//        together.setLayerInset(0, arrow.intrinsicWidth - 4, 0, arrow.intrinsicWidth - 4, 0)
//        // Put the arrow in the middle and to the side of the number background
//        together.setLayerInset(1, left, distanceFromTop - arrow.intrinsicHeight / 2, right, numberBackground.intrinsicHeight - distanceFromTop - arrow.intrinsicHeight / 2)
//        numberPad.background = together
//
//        numberPad.y = (selectedLocation[1] - distanceFromTop + selectedBox.height / 2).toFloat()
//        selectedBox.setBackgroundResource(R.drawable.text_box_selected)
//
//        clearBlockSpace = true
//        numberPad.visibility = View.VISIBLE
    }


    fun showColorPalette(isVisible: Boolean) {
        val colorPalette = findViewById<View>(R.id.fragmentColorPalette)
        if (!isVisible) {
            colorPalette.visibility = View.INVISIBLE
            return
        }

        showNumberPad(false, false)

//        if (selectedBlock != null) {
//            selectedBlock!!.getLocationOnScreen(selectedLocation)
//            colorPalette.x = (selectedLocation[0] - colorPalette.width).toFloat()
//            colorPalette.y = (selectedLocation[1] - colorPalette.height / 2 + blockHeight / 2).toFloat()
//
//            val brightness = selectedBlock!!.brightness
//            updateBrightnessButtons(brightness)
//        }


        colorPalette.visibility = View.VISIBLE
    }
    override fun updateGlowBoard() {}
    override fun savePicture() {}

    // Required for ColorPickerDelegate
    override fun colorSelected(color: PixelColor) {
        Log.d("Blocks",color.toString())
    }

    companion object {

        // This dictionary defines the blocks for each level
        val blocksByLevel: Map<Level,List<Pair<BlockType,Int>>> = mapOf(
            Level.level1 to listOf(
                Pair(BlockType.addition1, R.drawable.menu_operator_add_one),
                Pair(BlockType.addition10, R.drawable.menu_operator_add_ten),
                Pair(BlockType.subtraction1, R.drawable.menu_operator_subtract_one),
                Pair(BlockType.addition10, R.drawable.menu_operator_subtract_ten)
            ),
            Level.level2 to listOf(
                Pair(BlockType.addition, R.drawable.menu_operator_addition),
                Pair(BlockType.subtraction, R.drawable.menu_operator_subtraction)
            ),
            Level.level3 to listOf(
                Pair(BlockType.addition, R.drawable.menu_operator_addition),
                Pair(BlockType.subtraction, R.drawable.menu_operator_subtraction),
                Pair(BlockType.doubleAddition, R.drawable.menu_operator_plusplus)
            ),
            Level.level4 to listOf(
                Pair(BlockType.addition, R.drawable.menu_operator_addition),
                Pair(BlockType.subtraction, R.drawable.menu_operator_subtraction),
                Pair(BlockType.multiplication, R.drawable.menu_operator_multiplication),
                Pair(BlockType.division, R.drawable.menu_operator_division)
            ),
            Level.level5 to listOf(
                Pair(BlockType.equals, R.drawable.menu_operator_equals),
                Pair(BlockType.addition, R.drawable.menu_operator_addition_nested),
                Pair(BlockType.subtraction, R.drawable.menu_operator_subtraction_nested),
                Pair(BlockType.multiplication, R.drawable.menu_operator_multiplication_nested),
                Pair(BlockType.division, R.drawable.menu_operator_division_nested)
            )
        )
    }


}