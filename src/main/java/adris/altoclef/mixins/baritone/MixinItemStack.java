package adris.altoclef.mixins.baritone;

//@Mixin({ItemStack.class})
//public abstract class MixinItemStack implements IItemStack {
//   @Shadow
//   @Final
//   private Item item;
//   @Unique
//   private int baritoneHash;
//
//   @Shadow
//   public abstract int getDamageValue();
//
//   private void recalculateHash() {
//      this.baritoneHash = this.item == null ? -1 : this.item.hashCode() + this.getDamageValue();
//   }
//
//   @Inject(
//      method = {"<init>*"},
//      at = {@At("RETURN")}
//   )
//   private void onInit(CallbackInfo ci) {
//      this.recalculateHash();
//   }
//
//   @Inject(
//      method = {"setDamageValue"},
//      at = {@At("TAIL")}
//   )
//   private void onItemDamageSet(CallbackInfo ci) {
//      this.recalculateHash();
//   }
//
//   @Override
//   public int getBaritoneHash() {
//      return this.baritoneHash;
//   }
//}
