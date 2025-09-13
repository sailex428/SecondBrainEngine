package me.sailex.altoclef.multiversion;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.tag.DamageTypeTags;

public class DamageSourceWrapper {
   private final DamageSource source;

   public static DamageSourceWrapper of(DamageSource source) {
      return source == null ? null : new DamageSourceWrapper(source);
   }

   private DamageSourceWrapper(DamageSource source) {
      this.source = source;
   }

   public DamageSource getSource() {
      return this.source;
   }

   public boolean bypassesArmor() {
      return this.source.isIn(DamageTypeTags.BYPASSES_ARMOR);
   }

   public boolean bypassesShield() {
      return this.source.isIn(DamageTypeTags.BYPASSES_SHIELD);
   }

   public boolean isOutOfWorld() {
      return this.source.isOf(DamageTypes.OUT_OF_WORLD);
   }
}
