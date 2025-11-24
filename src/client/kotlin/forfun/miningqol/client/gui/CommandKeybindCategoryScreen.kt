package forfun.miningqol.client.gui

import forfun.miningqol.client.CommandKeybindManager
import forfun.miningqol.client.MiningqolClient
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*

class CommandKeybindCategoryScreen(private val parentScreen: Screen) : VexelScreen("Command Keybinds") {
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
            .setSizing(600f, Size.Pixels, 500f, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        // Center the panel manually
        mainPanel.xPositionConstraint = Pos.ScreenPixels
        mainPanel.yPositionConstraint = Pos.ScreenPixels
        mainPanel.xConstraint = (mainPanel.screenWidth - 600f) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - 500f) / 2f
        mainPanel.fadeIn(500, EasingType.EASE_OUT)

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
        Text("Command Keybinds", 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        Text("Bind commands to keys (not fully implemented)", 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 50f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        // Info card
        val infoCard = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF4488FF.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(520f, Size.Pixels, 280f, Size.Pixels)
            .setPositioning((600f - 520f) / 2f, Pos.ParentPixels, 120f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                dropShadow = true
                shadowBlur = 15f
                shadowSpread = 1f
                shadowColor = 0x40000000.toInt()
            }

        // Info icon/accent
        Rectangle(
            backgroundColor = 0xFF4488FF.toInt(),
            borderRadius = 12f
        )
            .setSizing(5f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(infoCard)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        Text("Keybind Management", 0xFFFFFFFF.toInt(), 20f, true)
            .setPositioning(20f, Pos.ParentPixels, 18f, Pos.ParentPixels)
            .childOf(infoCard)

        Text("This feature is not yet fully implemented in the Vexel UI.", 0xFFAAAAAA.toInt(), 14f, false)
            .setPositioning(20f, Pos.ParentPixels, 55f, Pos.ParentPixels)
            .childOf(infoCard)

        Text("The keybind entry UI with text fields, key binding,", 0xFF888888.toInt(), 13f, false)
            .setPositioning(20f, Pos.ParentPixels, 85f, Pos.ParentPixels)
            .childOf(infoCard)

        Text("and scrolling is complex and requires more work.", 0xFF888888.toInt(), 13f, false)
            .setPositioning(20f, Pos.ParentPixels, 105f, Pos.ParentPixels)
            .childOf(infoCard)

        Text("For now, please use the Java version of this screen", 0xFFFF9944.toInt(), 14f, true)
            .setPositioning(20f, Pos.ParentPixels, 145f, Pos.ParentPixels)
            .childOf(infoCard)

        Text("to manage your command keybinds.", 0xFFFF9944.toInt(), 14f, true)
            .setPositioning(20f, Pos.ParentPixels, 165f, Pos.ParentPixels)
            .childOf(infoCard)

        Text("Current keybinds: ${CommandKeybindManager.getAllKeybinds().size}", 0xFF44FF44.toInt(), 13f, false)
            .setPositioning(20f, Pos.ParentPixels, 205f, Pos.ParentPixels)
            .childOf(infoCard)

        Text("(They will continue to work normally)", 0xFF666666.toInt(), 11f, false)
            .setPositioning(20f, Pos.ParentPixels, 230f, Pos.ParentPixels)
            .childOf(infoCard)

        infoCard.visible = false
        Thread {
            Thread.sleep(200L)
            MinecraftClient.getInstance().execute {
                infoCard.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()

        // Back button at bottom
        Button("Back", 0xFFFFFFFF.toInt(), fontSize = 15f)
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

    private fun closeWithAnimation() {
        MiningqolClient.getConfig()?.loadFromGame()
        MiningqolClient.getConfig()?.save()

        overlay.fadeOut(300, EasingType.EASE_IN)
        mainPanel.fadeOut(300, EasingType.EASE_IN) {
            MinecraftClient.getInstance().setScreen(parentScreen)
        }
    }

    override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int) {
        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
        } else {
            super.onKeyType(typedChar, keyCode, scanCode)
        }
    }

    override fun shouldCloseOnEsc(): Boolean = false
}
