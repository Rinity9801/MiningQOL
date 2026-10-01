package forfun.miningqol.mixin.client;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** Lets the shaft summary rewrite its own Fines line in place when a late sack message lands. */
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("allMessages")
    List<GuiMessage> miningqol$allMessages();

    @Invoker("refreshTrimmedMessages")
    void miningqol$refreshTrimmedMessages();
}
