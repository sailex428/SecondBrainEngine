package me.sailex.altoclef.multiversion;

import net.minecraft.util.ActionResult;

public class ActionResultVer {

    public static boolean shouldSwingHand(ActionResult actionResult) {
        return /*? >=1.21.8 {*/ /*ActionResult.SUCCESS_SERVER == actionResult; *//*?} else {*/ actionResult.shouldSwingHand(); /*?}*/
    }

}
