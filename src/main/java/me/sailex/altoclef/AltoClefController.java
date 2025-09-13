package me.sailex.altoclef;

import me.sailex.altoclef.chains.FoodChain;
import me.sailex.altoclef.chains.MLGBucketFallChain;
import me.sailex.altoclef.chains.MobDefenseChain;
import me.sailex.altoclef.chains.PlayerDefenseChain;
import me.sailex.altoclef.chains.PlayerInteractionFixChain;
import me.sailex.altoclef.chains.PreEquipItemChain;
import me.sailex.altoclef.chains.UnstuckChain;
import me.sailex.altoclef.chains.UserTaskChain;
import me.sailex.altoclef.chains.WorldSurvivalChain;
import me.sailex.altoclef.commands.BlockScanner;
import me.sailex.altoclef.commandsystem.CommandExecutor;
import me.sailex.altoclef.control.InputControls;
import me.sailex.altoclef.control.PlayerExtraController;
import me.sailex.altoclef.control.SlotHandler;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.tasksystem.TaskRunner;
import me.sailex.altoclef.trackers.CraftingRecipeTracker;
import me.sailex.altoclef.trackers.EntityStuckTracker;
import me.sailex.altoclef.trackers.EntityTracker;
import me.sailex.altoclef.trackers.MiscBlockTracker;
import me.sailex.altoclef.trackers.SimpleChunkTracker;
import me.sailex.altoclef.trackers.TrackerManager;
import me.sailex.altoclef.trackers.UserBlockRangeTracker;
import me.sailex.altoclef.trackers.storage.ContainerSubTracker;
import me.sailex.altoclef.trackers.storage.ItemStorageTracker;
import me.sailex.automatone.Baritone;
import me.sailex.automatone.api.IBaritone;
import me.sailex.automatone.api.utils.IEntityContext;
import me.sailex.automatone.api.utils.InteractionController;
import me.sailex.automatone.settings.AltoClefSettings;
import me.sailex.common.ServerTickable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AltoClefController implements ServerTickable {

   private final IBaritone baritone;
   private final IEntityContext ctx;
   private final CommandExecutor commandExecutor;
   private final TaskRunner taskRunner;
   private final TrackerManager trackerManager;
   private final BotBehaviour botBehaviour;
   private final UserTaskChain userTaskChain;
   private final FoodChain foodChain;
   private final MobDefenseChain mobDefenseChain;
   private final MLGBucketFallChain mlgBucketChain;
   private final ItemStorageTracker storageTracker;
   private ContainerSubTracker containerSubTracker;
   private final EntityTracker entityTracker;
   private final BlockScanner blockScanner;
   private final SimpleChunkTracker chunkTracker;
   private final MiscBlockTracker miscBlockTracker;
   private final CraftingRecipeTracker craftingRecipeTracker;
   private final EntityStuckTracker entityStuckTracker;
   private final UserBlockRangeTracker userBlockRangeTracker;
   private final InputControls inputControls;
   private final SlotHandler slotHandler;
   private final PlayerExtraController extraController;
   private Settings settings;
   private boolean paused = false;
   private Task storedTask;
   private PlayerEntity owner;

   public AltoClefController(IBaritone baritone) {
      this.baritone = baritone;
      this.ctx = baritone.getPlayerContext();
      this.commandExecutor = new CommandExecutor(this);
      this.taskRunner = new TaskRunner(this);
      this.trackerManager = new TrackerManager(this);
      this.userTaskChain = new UserTaskChain(this.taskRunner);
      this.mobDefenseChain = new MobDefenseChain(this.taskRunner);
      new PlayerInteractionFixChain(this.taskRunner);
      this.mlgBucketChain = new MLGBucketFallChain(this.taskRunner);
      new UnstuckChain(this.taskRunner);
      new PreEquipItemChain(this.taskRunner);
      new WorldSurvivalChain(this.taskRunner);
      this.foodChain = new FoodChain(this.taskRunner);
      new PlayerDefenseChain(this.taskRunner);
      this.storageTracker = new ItemStorageTracker(this, this.trackerManager, container -> this.containerSubTracker = container);
      this.entityTracker = new EntityTracker(this.trackerManager);
      this.blockScanner = new BlockScanner(this);
      this.chunkTracker = new SimpleChunkTracker(this);
      this.miscBlockTracker = new MiscBlockTracker(this);
      this.craftingRecipeTracker = new CraftingRecipeTracker(this.trackerManager);
      this.entityStuckTracker = new EntityStuckTracker(this.trackerManager);
      this.userBlockRangeTracker = new UserBlockRangeTracker(this.trackerManager);
      this.inputControls = new InputControls(this);
      this.slotHandler = new SlotHandler(this);
      this.extraController = new PlayerExtraController(this);
      this.initializeBaritoneSettings();
      this.botBehaviour = new BotBehaviour(this);
      this.initializeCommands();
      Settings.load(
         newSettings -> {
            this.settings = newSettings;
            List<Item> baritoneCanPlace = Arrays.stream(this.settings.getThrowawayItems(this, true)).toList();
            this.getBaritoneSettings().acceptableThrowawayItems.get().addAll(baritoneCanPlace);
            if ((!this.getUserTaskChain().isActive() || this.getUserTaskChain().isRunningIdleTask())
               && this.getModSettings().shouldRunIdleCommandWhenNotActive()) {
               this.getUserTaskChain().signalNextTaskToBeIdleTask();
               this.getCommandExecutor().executeWithPrefix(this.getModSettings().getIdleCommand());
            }

            this.getExtraBaritoneSettings().avoidBlockBreak(this.userBlockRangeTracker::isNearUserTrackedBlock);
            this.getExtraBaritoneSettings().avoidBlockPlace(this.entityStuckTracker::isBlockedByEntity);
         }
      );
      registerTickListener();
   }

   @Override
   public void onTick() {
      this.inputControls.onTickPre();
      this.storageTracker.setDirty();
      this.miscBlockTracker.tick();
      this.trackerManager.tick();
      this.blockScanner.tick();
      this.taskRunner.tick();
      this.inputControls.onTickPost();
      this.baritone.serverTick();
   }

   public void stop() {
      this.getUserTaskChain().cancel(this);
      if (this.taskRunner.getCurrentTaskChain() != null) {
         this.taskRunner.getCurrentTaskChain().stop();
      }

      this.getTaskRunner().disable();
      this.getBaritone().getPathingBehavior().forceCancel();
      this.getBaritone().getInputOverrideHandler().clearAllKeys();
   }

   private void initializeBaritoneSettings() {
      this.getExtraBaritoneSettings().canWalkOnEndPortal(false);
      this.getExtraBaritoneSettings().avoidBlockPlace(this.entityStuckTracker::isBlockedByEntity);
      this.getExtraBaritoneSettings().avoidBlockBreak(this.userBlockRangeTracker::isNearUserTrackedBlock);
      this.getBaritoneSettings().freeLook.set(false);
      this.getBaritoneSettings().overshootTraverse.set(true);
      this.getBaritoneSettings().allowOvershootDiagonalDescend.set(true);
      this.getBaritoneSettings().allowInventory.set(true);
      this.getBaritoneSettings().allowParkour.set(false);
      this.getBaritoneSettings().allowParkourAscend.set(false);
      this.getBaritoneSettings().allowParkourPlace.set(false);
      this.getBaritoneSettings().allowDiagonalDescend.set(false);
      this.getBaritoneSettings().allowDiagonalAscend.set(false);
      this.getBaritoneSettings().fadePath.set(true);
      this.getBaritoneSettings().mineScanDroppedItems.set(false);
      this.getBaritoneSettings().mineDropLoiterDurationMSThanksLouca.set(0L);
      this.getExtraBaritoneSettings().configurePlaceBucketButDontFall(true);
      this.getBaritoneSettings().randomLooking.set(0.0);
      this.getBaritoneSettings().randomLooking113.set(0.0);
      this.getBaritoneSettings().failureTimeoutMS.reset();
      this.getBaritoneSettings().planAheadFailureTimeoutMS.reset();
      this.getBaritoneSettings().movementTimeoutTicks.reset();
   }

   private void initializeCommands() {
      try {
         AltoClefCommands.init(this);
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   public void runUserTask(Task task, Runnable onFinish) {
      this.userTaskChain.runTask(this, task, onFinish);
   }

   public void runUserTask(Task task) {
      this.runUserTask(task, () -> {});
   }

   public void cancelUserTask() {
      this.userTaskChain.cancel(this);
   }

   public CommandExecutor getCommandExecutor() {
      return this.commandExecutor;
   }

   public ServerPlayerEntity getEntity() {
      return this.ctx.entity();
   }

   public ServerWorld getWorld() {
      return this.ctx.world();
   }

   public InteractionController getInteractionManager() {
      return this.ctx.interactionController();
   }

   public IBaritone getBaritone() {
      return this.baritone;
   }

   public me.sailex.automatone.api.Settings getBaritoneSettings() {
      return this.baritone.settings();
   }

   public AltoClefSettings getExtraBaritoneSettings() {
      return ((Baritone)this.baritone).getExtraBaritoneSettings();
   }

   public TaskRunner getTaskRunner() {
      return this.taskRunner;
   }

   public UserTaskChain getUserTaskChain() {
      return this.userTaskChain;
   }

   public BotBehaviour getBehaviour() {
      return this.botBehaviour;
   }

   public boolean isPaused() {
      return this.paused;
   }

   public void setPaused(boolean pausing) {
      this.paused = pausing;
   }

   public Task getStoredTask() {
      return this.storedTask;
   }

   public void setStoredTask(Task currentTask) {
      this.storedTask = currentTask;
   }

   public ItemStorageTracker getItemStorage() {
      return this.storageTracker;
   }

   public EntityTracker getEntityTracker() {
      return this.entityTracker;
   }

   public CraftingRecipeTracker getCraftingRecipeTracker() {
      return this.craftingRecipeTracker;
   }

   public BlockScanner getBlockScanner() {
      return this.blockScanner;
   }

   public SimpleChunkTracker getChunkTracker() {
      return this.chunkTracker;
   }

   public MiscBlockTracker getMiscBlockTracker() {
      return this.miscBlockTracker;
   }

   public Settings getModSettings() {
      return this.settings;
   }

   public FoodChain getFoodChain() {
      return this.foodChain;
   }

   public MobDefenseChain getMobDefenseChain() {
      return this.mobDefenseChain;
   }

   public MLGBucketFallChain getMLGBucketChain() {
      return this.mlgBucketChain;
   }

   public void log(String message) {
      Debug.logMessage(message);
   }

   public void logWarning(String message) {
      Debug.logWarning(message);
   }

   public static boolean inGame() {
      return true;
   }

   public ServerPlayerEntity getPlayer() {
      return this.ctx.entity();
   }

   public InputControls getInputControls() {
      return this.inputControls;
   }

   public SlotHandler getSlotHandler() {
      return this.slotHandler;
   }

   public PlayerInventory getInventory() {
      return this.getBaritone().getPlayerContext().inventory();
   }

   public PlayerExtraController getControllerExtras() {
      return this.extraController;
   }

   public void setEnabled(boolean enabled) {
      if (!enabled) {
         this.getUserTaskChain().cancel(this);
         this.getTaskRunner().disable();
      }
   }

   public PlayerEntity getOwner() {
      return this.owner;
   }

   public void setOwner(PlayerEntity owner) {
      this.owner = owner;
      //update sys prompt i guess
   }

   public boolean isOwner(UUID playerToCheck) {
      return playerToCheck.equals(owner.getUuid());
   }

   public String getOwnerUsername(){
      if(getOwner() == null){
         return "UNKNOWN OWNER";
      }
      return getOwner().getName().getString();
   }

   public Optional<ServerPlayerEntity> getClosestPlayer(){
      return this.getWorld().getPlayers().stream().sorted((a,b)-> {
         float adist = a.distanceTo(this.getEntity());
         float bdist = b.distanceTo(this.getEntity());
         return Float.compare(adist, bdist);
      } ).findFirst();
   }
}
