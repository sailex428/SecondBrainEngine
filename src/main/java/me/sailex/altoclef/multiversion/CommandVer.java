package me.sailex.altoclef.multiversion;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

public class CommandVer {

    public static ClickEvent getRunCommand(String value) {
        //? >=1.21.8 {
        /*return new ClickEvent.RunCommand(value);
        *///?} else {
        
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, value);
        //?}
    }

    public static ClickEvent getSuggestCommand(String value) {
        //? >=1.21.8 {
        /*return new ClickEvent.RunCommand(value);
        *///?} else {
        
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, value);
        //?}
    }

    public static HoverEvent getShowText(Text value) {
        //? >=1.21.8 {
        /*return new HoverEvent.ShowText(value);
        *///?} else {
        
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, value);
        //?}
    }

}
