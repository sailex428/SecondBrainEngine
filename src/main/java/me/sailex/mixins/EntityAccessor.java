package me.sailex.mixins;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    //? >=1.21 {

    //?} elif >= 1.20 {
    @Accessor("inNetherPortal")
    boolean isInNetherPortal();
    //?}
}
