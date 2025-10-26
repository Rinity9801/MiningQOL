package forfun.miningqol.mixin.client;

import forfun.miningqol.client.NameHider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to intercept and modify entity name rendering
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @ModifyVariable(
        method = "renderLabelIfPresent",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Text modifyEntityName(Text original) {
        return NameHider.processName(original);
    }
}
