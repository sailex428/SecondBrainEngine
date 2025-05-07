/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.api.IBaritone;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * A cached list of the best tools on the hotbar for any block
 *
 * @author Avery, Brady, leijurv
 */
public class ToolSet {

    /**
     * A cache mapping a {@link Block} to how long it will take to break
     * with this toolset, given the optimum tool is used.
     */
    private final Map<Block, Double> breakStrengthCache;

    /**
     * My buddy leijurv owned me so we have this to not create a new lambda instance.
     */
    private final Function<Block, Double> backendCalculation;

    private final PlayerEntity player;
    private final IBaritone baritone;

    public ToolSet(PlayerEntity player) {
        this.breakStrengthCache = new HashMap<>();
        this.player = player;
        this.baritone = IBaritone.KEY.get(player);

        if (baritone.settings().considerPotionEffects.get()) {
            double amplifier = potionAmplifier();
            Function<Double, Double> amplify = x -> amplifier * x;
            backendCalculation = amplify.compose(this::getBestDestructionTime);
        } else {
            backendCalculation = this::getBestDestructionTime;
        }
    }

    /**
     * Using the best tool on the hotbar, how fast we can mine this block
     *
     * @param state the blockstate to be mined
     * @return the speed of how fast we'll mine it. 1/(time in ticks)
     */
    public double getStrVsBlock(BlockState state) {
        return breakStrengthCache.computeIfAbsent(state.getBlock(), backendCalculation);
    }

    /**
     * Evaluate the material cost of a possible tool. Will return 1 for tools, -1 for other
     *
     * @param itemStack a possibly empty ItemStack
     * @return Either 1 or -1
     */
    private int getMaterialCost(ItemStack itemStack) {
        return itemStack.getItem() instanceof ToolItem ? 1 : -1;
    }

    public boolean hasSilkTouch(ItemStack stack) {
        return getEnchantmentLevel(Enchantments.SILK_TOUCH, stack, player) > 0;
    }

    private static int getEnchantmentLevel(RegistryKey<Enchantment> enchantmentKey, ItemStack stack, Entity entity) {
        Enchantment enchantment = entity.getRegistryManager().get(RegistryKeys.ENCHANTMENT).get(enchantmentKey);
        ItemEnchantmentsComponent component = stack.getEnchantments();
        Optional<RegistryEntry<Enchantment>> enchantmentHolder = component.getEnchantments().stream().filter(holder -> holder.value().equals(enchantment)).findFirst();
        return enchantmentHolder.map(component::getLevel).orElse(0);
    }

    /**
     * Calculate which tool on the hotbar is best for mining, depending on an override setting,
     * related to auto tool movement cost, it will either return current selected slot, or the best slot.
     *
     * @param b the blockstate to be mined
     * @return An int containing the index in the tools array that worked best
     */

    public int getBestSlot(Block b, boolean preferSilkTouch) {
        return getBestSlot(b, preferSilkTouch, false);
    }

    public int getBestSlot(Block b, boolean preferSilkTouch, boolean pathingCalculation) {

        /*
        If we actually want know what efficiency our held item has instead of the best one
        possible, this lets us make pathing depend on the actual tool to be used (if auto tool is disabled)
        */
        if (baritone.settings().disableAutoTool.get() && pathingCalculation) {
            return player.getInventory().selectedSlot;
        }

        int best = 0;
        double highestSpeed = Double.NEGATIVE_INFINITY;
        int lowestCost = Integer.MIN_VALUE;
        boolean bestSilkTouch = false;
        BlockState blockState = b.getDefaultState();
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if (!baritone.settings().useSwordToMine.get() && itemStack.getItem() instanceof SwordItem) {
                continue;
            }

            if (baritone.settings().itemSaver.get() && itemStack.getDamage() >= itemStack.getMaxDamage() && itemStack.getMaxDamage() > 1) {
                continue;
            }
            double speed = calculateSpeedVsBlock(itemStack, blockState, player);
            boolean silkTouch = hasSilkTouch(itemStack);
            if (speed > highestSpeed) {
                highestSpeed = speed;
                best = i;
                lowestCost = getMaterialCost(itemStack);
                bestSilkTouch = silkTouch;
            } else if (speed == highestSpeed) {
                int cost = getMaterialCost(itemStack);
                if ((cost < lowestCost && (silkTouch || !bestSilkTouch)) ||
                        (preferSilkTouch && !bestSilkTouch && silkTouch)) {
                    highestSpeed = speed;
                    best = i;
                    lowestCost = cost;
                    bestSilkTouch = silkTouch;
                }
            }
        }
        return best;
    }

    /**
     * Calculate how effectively a block can be destroyed
     *
     * @param b the blockstate to be mined
     * @return A double containing the destruction ticks with the best tool
     */
    private double getBestDestructionTime(Block b) {
        ItemStack stack = player.getInventory().getStack(getBestSlot(b, false, true));
        return calculateSpeedVsBlock(stack, b.getDefaultState(), player) * avoidanceMultiplier(b);
    }

    private double avoidanceMultiplier(Block b) {
        return b.getRegistryEntry().isIn(baritone.settings().blocksToAvoidBreaking.get()) ? 0.1 : 1;
    }

    /**
     * Calculates how long would it take to mine the specified block given the best tool
     * in this toolset is used. A negative value is returned if the specified block is unbreakable.
     *
     * @param item  the item to mine it with
     * @param state the blockstate to be mined
     * @return how long it would take in ticks
     */
    public static double calculateSpeedVsBlock(ItemStack item, BlockState state, Entity entity) {
        float hardness = state.getHardness(null, null);
        if (hardness < 0) {
            return -1;
        }

        float speed = item.getMiningSpeedMultiplier(state);
        if (speed > 1) {
            int effLevel = getEnchantmentLevel(Enchantments.EFFICIENCY, item, entity);
            if (effLevel > 0 && !item.isEmpty()) {
                speed += effLevel * effLevel + 1;
            }
        }

        speed /= hardness;
        if (!state.isToolRequired() || (!item.isEmpty() && item.isSuitableFor(state))) {
            return speed / 30;
        } else {
            return speed / 100;
        }
    }

    /**
     * Calculates any modifier to breaking time based on status effects.
     *
     * @return a double to scale block breaking speed.
     */
    private double potionAmplifier() {
        double speed = 1;

        StatusEffectInstance hasteEffect = player.getStatusEffect(StatusEffects.HASTE);
        if (hasteEffect != null) {
            speed *= 1 + (hasteEffect.getAmplifier() + 1) * 0.2;
        }

        StatusEffectInstance fatigueEffect = player.getStatusEffect(StatusEffects.MINING_FATIGUE);
        if (fatigueEffect != null) {
            switch (fatigueEffect.getAmplifier()) {
                case 0 -> speed *= 0.3;
                case 1 -> speed *= 0.09;
                case 2 -> speed *= 0.0027; // you might think that 0.09*0.3 = 0.027 so that should be next, that would make too much sense. it's 0.0027.
                default -> speed *= 0.00081;
            }
        }
        return speed;
    }
}
