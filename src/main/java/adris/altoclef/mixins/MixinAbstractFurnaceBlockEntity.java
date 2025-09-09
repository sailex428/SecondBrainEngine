package adris.altoclef.mixins;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.screen.PropertyDelegate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AbstractFurnaceBlockEntity.class})
public interface MixinAbstractFurnaceBlockEntity {
   @Accessor("propertyDelegate")
   PropertyDelegate getPropertyDelegate();
}
