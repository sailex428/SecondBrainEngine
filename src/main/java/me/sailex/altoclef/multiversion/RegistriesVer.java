package me.sailex.altoclef.multiversion;

import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.util.Identifier;

public class RegistriesVer {

    public static <T> T get(DefaultedRegistry<T> registry, Identifier id) {
        //? if >=1.21.8 {
        /*return registry.getOptionalValue(id).orElse(null);
        *///?} else {
        return registry.getOrEmpty(id).orElse(null);
         //?}
    }

}
