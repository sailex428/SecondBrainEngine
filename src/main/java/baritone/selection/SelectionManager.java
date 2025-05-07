package baritone.selection;

import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.Direction;

import java.util.LinkedList;
import java.util.ListIterator;

public class SelectionManager implements ISelectionManager {

    private final LinkedList<ISelection> selections = new LinkedList<>();
    private ISelection[] selectionsArr = new ISelection[0];

    public SelectionManager(Entity holder) {}

    private void resetSelectionsArr() {
        selectionsArr = selections.toArray(new ISelection[0]);
    }

    @Override
    public synchronized ISelection addSelection(ISelection selection) {
        selections.add(selection);
        resetSelectionsArr();
        return selection;
    }

    @Override
    public ISelection addSelection(BetterBlockPos pos1, BetterBlockPos pos2) {
        return addSelection(new Selection(pos1, pos2));
    }

    @Override
    public synchronized ISelection removeSelection(ISelection selection) {
        selections.remove(selection);
        resetSelectionsArr();
        return selection;
    }

    @Override
    public synchronized ISelection[] removeAllSelections() {
        ISelection[] selectionsArr = getSelections();
        selections.clear();
        resetSelectionsArr();
        return selectionsArr;
    }

    @Override
    public ISelection[] getSelections() {
        return selectionsArr;
    }

    @Override
    public synchronized ISelection getOnlySelection() {
        if (selections.size() == 1) {
            return selections.peekFirst();
        }

        return null;
    }

    @Override
    public ISelection getLastSelection() {
        return selections.peekLast();
    }

    @Override
    public synchronized ISelection expand(ISelection selection, Direction direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.expand(direction, blocks));
                resetSelectionsArr();
                return it.previous();
            }
        }

        return null;
    }

    @Override
    public synchronized ISelection contract(ISelection selection, Direction direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.contract(direction, blocks));
                resetSelectionsArr();
                return it.previous();
            }
        }

        return null;
    }

    @Override
    public synchronized ISelection shift(ISelection selection, Direction direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.shift(direction, blocks));
                resetSelectionsArr();
                return it.previous();
            }
        }

        return null;
    }

    @Override
    public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup provider) {
        // NO-OP
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup provider) {
        // NO-OP
    }

//    @Override
//    public boolean shouldSyncWith(ServerPlayerEntity player) {
//        return player == this.holder || (
//                IBaritone.KEY.maybeGet(this.holder)
//                        .map(IBaritone::settings)
//                        .orElseGet(BaritoneAPI::getGlobalSettings)
//                        .syncWithOps.get()
//                        && player.server.getPermissionLevel(player.getGameProfile()) >= 2
//        );
//    }

//    @Override
//    public void writeSyncPacket(PacketByteBuf buf, ServerPlayerEntity recipient) {
//        buf.writeVarInt(this.selectionsArr.length);
//
//        for (ISelection sel : this.selectionsArr) {
//            buf.writeBlockPos(sel.pos1());
//            buf.writeBlockPos(sel.pos2());
//        }
//    }
//
//    @Override
//    public void applySyncPacket(PacketByteBuf buf) {
//        this.removeAllSelections();
//
//        int length = buf.readVarInt();
//
//        for (int i = 0; i < length; i++) {
//            BlockPos pos1 = buf.readBlockPos();
//            BlockPos pos2 = buf.readBlockPos();
//            this.addSelection(new BetterBlockPos(pos1), new BetterBlockPos(pos2));
//        }
//
//        if (this.selections.isEmpty()) {
//            AutomatoneClient.selectionRenderList.remove(this);
//        } else {
//            AutomatoneClient.selectionRenderList.add(this);
//        }
//    }
}
