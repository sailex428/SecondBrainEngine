package me.sailex.altoclef.tasks;

import me.sailex.altoclef.Playground;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.helpers.LookHelper;

public class LookAtOwnerTask extends Task {
    public static String name = "LookAtOwnerTask";
    @Override
    protected void onStart() {
        
    }

    @Override
    protected Task onTick() {
        if (this.controller.getOwner() != null) {
            LookHelper.lookAt(this.controller, this.controller.getOwner().getEyePos());
        } else {
            this.controller.getClosestPlayer().ifPresent(
                    (player) -> {
                        LookHelper.lookAt(controller, player.getEyePos());
                    });
        }
        Playground.IDLE_TEST_TICK_FUNCTION(this.controller);
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LookAtOwnerTask;
    }

    @Override
    protected String toDebugString() {
        return "LookAtOwnerTask";
    }
}
