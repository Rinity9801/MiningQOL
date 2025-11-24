package forfun.miningqol.client.gui

import net.minecraft.client.MinecraftClient
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*

class VexelMainScreen : VexelScreen("MiningQOL Settings") {
    private lateinit var overlay: Rectangle
    private lateinit var mainPanel: Rectangle

    override fun afterInitialization() {
        // Semi-transparent dark overlay background
        overlay = Rectangle(
            backgroundColor = 0x80000000.toInt(),
            borderColor = 0x00000000,
            borderRadius = 0f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(window)
            .fadeIn(400, EasingType.EASE_OUT)

        // Main panel - darker and more modern
        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(750f, Size.Pixels, 580f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        // Animate main panel entrance
        mainPanel.yConstraint = -100f
        mainPanel.fadeIn(500, EasingType.EASE_OUT)
        mainPanel.moveTo(0f, 0f, 600, EasingType.EASE_OUT)

        // Title bar background
        Rectangle(
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 80f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                borderRadiusBottomLeft = 0f
                borderRadiusBottomRight = 0f
            }
            .fadeIn(600, EasingType.EASE_OUT)

        // Title
        Text("MiningQOL", 0xFFFFFFFF.toInt(), 36f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        Text("Enhanced mining experience for Hypixel Skyblock", 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 55f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        // Category cards container with grid layout
        val cardStartY = 110f
        val cardSpacing = 12f
        val cardHeight = 70f
        val leftX = 25f
        val rightX = 390f
        val cardWidth = 335f

        // Left column cards
        val leftCards = listOf(
            Triple("Mining Profit", "Track earnings and optimize gains", 0xFF5B7CFF.toInt()),
            Triple("Efficient Miner", "Overlay for max mining efficiency", 0xFFFF7C5B.toInt()),
            Triple("Corpse ESP", "Highlight corpses in Crystal Hollows", 0xFF5BFF7C.toInt()),
            Triple("Block Outline", "Custom mining block outlines", 0xFFFFEB5B.toInt()),
            Triple("Pickaxe Cooldown", "HUD for ability cooldown tracking", 0xFF5BFFFF.toInt())
        )

        val rightCards = listOf(
            Triple("Name Hider", "Hide or customize your name display", 0xFFFF5BFF.toInt()),
            Triple("Auto Clicker", "Automated clicking for mining", 0xFFFFA05B.toInt()),
            Triple("Command Keybinds", "Bind commands to keys", 0xFFA05BFF.toInt()),
            Triple("Misc", "Miscellaneous features and utilities", 0xFF888888.toInt())
        )

        val leftScreens = listOf(
            { MiningProfitCategoryScreen(this) },
            { MinerOverlayCategoryScreen(this) },
            { CorpseESPCategoryScreen(this) },
            { BlockOutlineCategoryScreen(this) },
            { PickaxeCooldownCategoryScreen(this) }
        )

        val rightScreens = listOf(
            { NameHiderCategoryScreen(this) },
            { AutoClickerCategoryScreen(this) },
            { CommandKeybindCategoryScreen(this) },
            { MiscCategoryScreen(this) }
        )

        // Create left column cards
        leftCards.forEachIndexed { index, (title, desc, color) ->
            val delay = 200L + (index * 100L)
            createCategoryCard(
                title, desc, color,
                leftX, cardStartY + (index * (cardHeight + cardSpacing)),
                cardWidth, cardHeight,
                mainPanel,
                delay
            ) {
                MinecraftClient.getInstance().setScreen(leftScreens[index]())
            }
        }

        // Create right column cards
        rightCards.forEachIndexed { index, (title, desc, color) ->
            val delay = 200L + (index * 100L)
            createCategoryCard(
                title, desc, color,
                rightX, cardStartY + (index * (cardHeight + cardSpacing)),
                cardWidth, cardHeight,
                mainPanel,
                delay
            ) {
                MinecraftClient.getInstance().setScreen(rightScreens[index]())
            }
        }

        // Close button at bottom
        Button("Close", 0xFFFFFFFF.toInt(), fontSize = 15f)
            .setSizing(140f, Size.Pixels, 42f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -25f)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                closeWithAnimation()
                true
            }
            .childOf(mainPanel)
            .fadeIn(1000, EasingType.EASE_OUT)
    }

    private fun createCategoryCard(
        title: String,
        description: String,
        accentColor: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        parent: Rectangle,
        animDelay: Long,
        onClick: () -> Unit
    ) {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f,
            hoverColor = 0xF0252525.toInt()
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .onClick { mouseX, mouseY, button ->
                onClick()
                true
            }
            .childOf(parent)
            .apply {
                dropShadow = true
                shadowBlur = 20f
                shadowSpread = 1f
                shadowColor = 0x60000000.toInt()
            }

        // Animate card entrance
        card.xConstraint = if (x < 200f) -50f else 800f
        Thread {
            Thread.sleep(animDelay)
            card.fadeIn(400, EasingType.EASE_OUT)
            card.moveTo(x, y, 500, EasingType.EASE_OUT)
        }.start()

        // Glowing accent bar on left
        Rectangle(
            backgroundColor = accentColor,
            borderRadius = 12f
        )
            .setSizing(5f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(card)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        // Title
        Text(title, 0xFFFFFFFF.toInt(), 18f, true)
            .setPositioning(20f, Pos.ParentPixels, 16f, Pos.ParentPixels)
            .childOf(card)

        // Description
        Text(description, 0xFF888888.toInt(), 13f, false)
            .setPositioning(20f, Pos.ParentPixels, 42f, Pos.ParentPixels)
            .childOf(card)

        // Hover animation
        var isHovered = false
        card.onMouseEnter { _, _ ->
            if (!isHovered) {
                isHovered = true
                card.animateSize(width + 8f, height + 4f, 200, EasingType.EASE_OUT)
            }
        }
        card.onMouseExit { _, _ ->
            if (isHovered) {
                isHovered = false
                card.animateSize(width, height, 200, EasingType.EASE_IN)
            }
        }
    }

    private fun closeWithAnimation() {
        overlay.fadeOut(300, EasingType.EASE_IN)
        mainPanel.fadeOut(300, EasingType.EASE_IN) {
            close()
        }
    }

    override fun shouldCloseOnEsc(): Boolean = true
}
