package forfun.miningqol.client.gui

import net.minecraft.client.MinecraftClient
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size

class VexelMainScreen : VexelScreen("MiningQOL Settings") {

    override fun afterInitialization() {
        // Background panel
        val mainPanel = Rectangle(
            backgroundColor = 0xCC000000.toInt(),
            borderColor = 0xFF88AAFF.toInt(),
            borderRadius = 10f,
            borderThickness = 2f
        )
            .setSizing(700f, Size.Pixels, 550f, Size.Pixels)
            .setPositioning(Pos.ParentCenter, Pos.ParentCenter)
            .childOf(window)

        // Title
        Text("MiningQOL Settings", 0xFFFFFFFF.toInt(), 32f, true)
            .setPositioning(0f, Pos.ParentCenter, 20f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Subtitle
        Text("Enhanced mining experience for Hypixel Skyblock", 0xFFAAAAAA.toInt(), 14f, false)
            .setPositioning(0f, Pos.ParentCenter, 60f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Category buttons - Left column
        val buttonY = 100f
        val buttonSpacing = 65f
        val leftX = 30f
        val rightX = 370f

        createCategoryButton(
            "Mining Profit",
            "Track earnings and optimize gains",
            0xFF88AAFF.toInt(),
            leftX, buttonY,
            mainPanel
        ) {
            MinecraftClient.getInstance().setScreen(MiningProfitCategoryScreen(this@VexelMainScreen))
        }

        createCategoryButton(
            "Efficient Miner",
            "Overlay for max mining efficiency",
            0xFFFFAA88.toInt(),
            leftX, buttonY + buttonSpacing,
            mainPanel
        ) {
            MinecraftClient.getInstance().setScreen(MinerOverlayCategoryScreen(this@VexelMainScreen))
        }

        createCategoryButton(
            "Corpse ESP",
            "Highlight corpses in Crystal Hollows",
            0xFFAAFF88.toInt(),
            leftX, buttonY + buttonSpacing * 2,
            mainPanel
        ) {
            MinecraftClient.getInstance().setScreen(CorpseESPCategoryScreen(this@VexelMainScreen))
        }

        createCategoryButton(
            "Block Outline",
            "Custom mining block outlines",
            0xFFFFFF88.toInt(),
            leftX, buttonY + buttonSpacing * 3,
            mainPanel
        ) {
            MinecraftClient.getInstance().setScreen(BlockOutlineCategoryScreen(this@VexelMainScreen))
        }

        createCategoryButton(
            "Pickaxe Cooldown",
            "HUD for ability cooldown tracking",
            0xFF88FFFF.toInt(),
            leftX, buttonY + buttonSpacing * 4,
            mainPanel
        ) {
            MinecraftClient.getInstance().setScreen(PickaxeCooldownCategoryScreen(this@VexelMainScreen))
        }

        // Right column
        createCategoryButton(
            "Name Hider",
            "Hide or customize your name display",
            0xFFFF88FF.toInt(),
            rightX, buttonY,
            mainPanel
        ) {
            MinecraftClient.getInstance().setScreen(NameHiderCategoryScreen(this@VexelMainScreen))
        }

        createCategoryButton(
            "Auto Clicker",
            "Automated clicking for mining",
            0xFFFFC888.toInt(),
            rightX, buttonY + buttonSpacing,
            mainPanel
        ) {
            MinecraftClient.getInstance().setScreen(AutoClickerCategoryScreen(this@VexelMainScreen))
        }

        createCategoryButton(
            "Command Keybinds",
            "Bind commands to keys",
            0xFFC888FF.toInt(),
            rightX, buttonY + buttonSpacing * 2,
            mainPanel
        ) {
            MinecraftClient.getInstance().setScreen(CommandKeybindCategoryScreen(this@VexelMainScreen))
        }

        createCategoryButton(
            "Misc",
            "Miscellaneous features and utilities",
            0xFFAAAAAA.toInt(),
            rightX, buttonY + buttonSpacing * 3,
            mainPanel
        ) {
            MinecraftClient.getInstance().setScreen(MiscCategoryScreen(this@VexelMainScreen))
        }

        // Close button
        Button("Close", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(120f, Size.Pixels, 35f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 490f, Pos.ParentPixels)
            .backgroundColor(0xFF3A3A3A.toInt())
            .hoverColor(0xFF4A4A4A.toInt())
            .onClick { _, _, _ ->
                close()
                true
            }
            .childOf(mainPanel)
    }

    private fun createCategoryButton(
        title: String,
        description: String,
        accentColor: Int,
        x: Float,
        y: Float,
        parent: Rectangle,
        onClick: () -> Unit
    ) {
        val panel = Rectangle(
            backgroundColor = 0xE62A2A2A.toInt(),
            borderColor = 0xFF404040.toInt(),
            borderRadius = 8f,
            borderThickness = 1f,
            hoverColor = 0xE6353535.toInt()
        )
            .setSizing(310f, Size.Pixels, 50f, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .onClick { _, _, _ ->
                onClick()
                true
            }
            .childOf(parent)

        // Accent bar
        Rectangle(
            backgroundColor = accentColor,
            borderRadius = 8f,
        )
            .setSizing(4f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(panel)

        // Title text
        Text(title, 0xFFFFFFFF.toInt(), 16f, true)
            .setPositioning(15f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(panel)

        // Description text
        Text(description, 0xFF969696.toInt(), 12f, false)
            .setPositioning(15f, Pos.ParentPixels, 30f, Pos.ParentPixels)
            .childOf(panel)
    }

    override fun shouldCloseOnEsc(): Boolean = true
}
