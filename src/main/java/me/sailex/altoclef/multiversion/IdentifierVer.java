package me.sailex.altoclef.multiversion;

import net.minecraft.util.Identifier;

public class IdentifierVer {

    public static Identifier getId(String id) {
        //? if >=1.21 {
        /*return Identifier.of(id);
        *///?} else {
        return new Identifier(id);
         //?}
    }
}
