package me.sailex.mixins;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({PersistentProjectileEntity.class})
public interface PersistentProjectileEntityAccessor {

    //? >=1.21.8 {
    /*@Invoker("isInGround")
    boolean isInGround();
    *///?} else {
    
    @Accessor("inGround")
    boolean isInGround();
    
    //?}
}
