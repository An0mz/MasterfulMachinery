package io.ticticboom.mods.mm.controller.machine.register;

import java.util.TreeMap;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.PortTiers;
import net.minecraft.server.level.ServerLevel;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import io.ticticboom.mods.mm.util.ColorUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import io.ticticboom.mods.mm.structure.StructureDiagnosis;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.oritech.BlackHoleShield;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.IControllerBlockEntity;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.port.IControllerAwareStorage;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IRecipeDemandListener;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.ITickSpreadIngredient;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.entity.EntityPortStorage;
import io.ticticboom.mods.mm.port.entity.EntityPortStorageModel;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeBlockReason;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.NearbyMachines;
import io.ticticboom.mods.mm.structure.StructureBoost;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.StructureTier;
import io.ticticboom.mods.mm.structure.attachment.visuals.FluidVolumeState;
import io.ticticboom.mods.mm.structure.attachment.visuals.VisualsStructureAttachment;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.ticticboom.mods.mm.config.MMConfigSetup.COMMON;

public class MachineControllerBlockEntity extends BlockEntity implements IControllerBlockEntity, IControllerPart {

    private static final long REQUEST_TIMEOUT = 40;
    private static final int SYNC_INTERVAL = 5;
    private static final int IDLE_SCAN_INTERVAL = 5;
    private static final int SKIP_COOLDOWN = 20;
    private static final int OUTPUT_BLOCKED_COOLDOWN = 100;
    private static final int SEARCH_CHECKS_PER_TICK = 5;
    private static final int RECENT_RECIPE_TICKS = 20;
    public static final int MAX_NAME_LENGTH = 50;

    private enum RedstoneMode { IGNORED, WITH_REDSTONE, WITHOUT_REDSTONE }

    private static final int BLOCK_REASON_TICKS = 100;
    private static final int BLOCK_REASON_INTERVAL = 20;
    private static final int OTHER_BLOCK_REASONS = 5;
    private static final int NEAR_MISS_CHECKS = 8;

    private static final class BlockNote {
        private RecipeBlockReason reason;
        private long time;

        private BlockNote(RecipeBlockReason reason, long time) {
            this.reason = reason;
            this.time = time;
        }
    }

    private record Candidate(RecipeModel recipe, @Nullable ResourceLocation inputKey, RecipeStateModel state, long lastUse) {
    }

    private final ControllerModel model;
    private final RegistryGroupHolder groupHolder;
    private final ResourceLocation controllerId;
    private final int validationOffset;

    @Getter
    private StructureModel structure = null;
    private Rotation formedRotation = null;
    private int[] structureChunks = null;
    private StructureModel structureChunksOf = null;
    private Rotation structureChunksRotation = null;
    private boolean isFormed = false;
    private boolean pendingValidation = true;
    private long lastTick = Long.MIN_VALUE;

    private final List<ActiveRun> activeRecipes = new ArrayList<>();
    private final Object2IntOpenHashMap<ResourceLocation> runCounts = new Object2IntOpenHashMap<>();
    private int syncedRunCount = 0;
    private double tickSpeedMultiplier = 1;
    @Getter
    private RecipeModel currentRecipe;
    private RecipeStorages portStorages = null;
    private List<EntityPortStorage> speedStorages = List.of();
    private NearbyMachines.Lookup nearby = null;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private final Set<ResourceLocation> failedRecipes = new HashSet<>();

    private final Set<ResourceLocation> cachedItemIds = new HashSet<>();
    private final Set<ResourceLocation> cachedFluidIds = new HashSet<>();
    private boolean cachedHasEnergy = false;
    private boolean storageContentCacheValid = false;
    private long lastResourceScanTime = -1;
    private long lastStorageChangeCount = -1;
    private final Map<ResourceLocation, Long> recipeNextCheckTime = new HashMap<>();

    private RecipeCandidateIndex cachedStructureRecipes = null;
    private final IntArrayList candidatePositions = new IntArrayList();
    private final IntArrayList nearMissPositions = new IntArrayList();
    private final Map<ResourceLocation, BlockNote> blockReasons = new HashMap<>();
    @Nullable
    private RecipeBlockReason shownBlockReason = null;
    private List<RecipeBlockReason> otherBlockReasons = List.of();
    private boolean candidatesValid = false;
    private final Set<ResourceLocation> candidateItems = new HashSet<>();
    private final Set<ResourceLocation> candidateFluids = new HashSet<>();
    private boolean candidateEnergy = false;
    private int cachedRecipeVersion = -1;
    private int nextRecipeCheckIndex = 0;
    private ResourceLocation lastStartedRecipeId = null;
    private long lastRecipeStartTime = Long.MIN_VALUE;
    private long lastProgressTime = Long.MIN_VALUE;
    private RecipeSelectionMode recipeModeOverride = null;
    private String customName = null;
    private long recipeSelectionSequence = 0L;
    private final Map<ResourceLocation, Long> inputItemLastStartedSequence = new HashMap<>();
    private final Map<ResourceLocation, Long> recipeLastStartedSequence = new HashMap<>();

    @Getter
    private ResourceLocation selectedRecipeId = null;
    private ResourceLocation requestedRecipeId = null;
    private Object requestOwner = null;
    private long requestExpiresAt = 0L;
    private int requestedCrafts = 0;
    private int requestStarts = 0;
    private Object reservationOwner = null;
    private ItemStack reservedOutput = ItemStack.EMPTY;
    private int reservedCount = 0;
    private long reservationExpiresAt = 0L;

    private boolean syncPending = false;
    private RecipeStorages linkedStorages = null;
    private ControllerState linkedState = null;
    private int comparatorSignal = 0;
    private boolean soundMuted = false;
    private ModelDisplay modelDisplay = ModelDisplay.ON;
    private LinkData networkLink = null;
    private boolean linkExportRequested = false;
    private long lastLinkExport = -1L;
    private final TierPrefs assemblyTiers = new TierPrefs();
    private ResourceLocation assemblyStructureId = null;
    private boolean wasActive = false;
    private long lastSync = Long.MIN_VALUE / 2;
    @Getter
    private List<FluidVolumeState> fluidVolumes = List.of();
    @Getter
    private AABB visualBounds = null;
    private RecipeStorages visualStorages = null;
    private long lastVisualChangeCount = -1;
    @Nullable
    private Object geoAnimatable;

    public MachineControllerBlockEntity(ControllerModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.controllerId = Ref.id(model.id());
        this.validationOffset = Math.floorMod(pos.hashCode(), 100);
    }

    public void tick() {
        if (level == null || isRemoved()) {
            return;
        }
        if (level.isClientSide()) {
            if (getBlockState().getBlock() instanceof MachineControllerBlock block) {
                block.tickWorkingEffects(getBlockState(), level, getBlockPos(), soundMuted);
            }
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime == lastTick) {
            return;
        }
        lastTick = gameTime;
        boolean loaded = !isFormed || structureChunksLoaded();
        if (loaded && (pendingValidation || (gameTime + validationOffset) % COMMON.structureValidationRate.get() == 0)) {
            pendingValidation = false;
            validateStructure();
        }
        if (isFormed && loaded) {
            if (portsReplaced()) {
                formed(formedRotation);
            }
            runRecipe(gameTime);
        }
        if ((gameTime + validationOffset) % BLOCK_REASON_INTERVAL == 0) {
            updateShownBlockReason(gameTime);
        }
        updateVisuals();
        updateControllerState();
        NetworkLink.tickController((ServerLevel) level, this);
        boolean active = !activeRecipes.isEmpty();
        if (active || wasActive) {
            markChanged();
        }
        wasActive = active;
        if (syncPending && gameTime - lastSync >= SYNC_INTERVAL) {
            syncPending = false;
            lastSync = gameTime;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void updateVisuals() {
        var visuals = structure == null ? null : structure.getAttachments().get(Ref.StructureAttachments.VISUALS, VisualsStructureAttachment.class);
        if (!isFormed || portStorages == null || formedRotation == null || visuals == null) {
            visualStorages = null;
            if (!fluidVolumes.isEmpty()) {
                fluidVolumes = List.of();
                markChanged();
            }
            return;
        }
        long changeCount = portStorages.changeCount();
        if (portStorages == visualStorages && changeCount == lastVisualChangeCount) {
            return;
        }
        visualStorages = portStorages;
        lastVisualChangeCount = changeCount;
        var next = visuals.snapshot(level, getBlockPos(), structure, formedRotation, portStorages);
        if (!FluidVolumeState.same(fluidVolumes, next)) {
            fluidVolumes = next;
            markChanged();
        }
    }

    private void loadFluidVolumes(CompoundTag tag, HolderLookup.Provider registries) {
        fluidVolumes = tag.contains("FluidVolumes") ? FluidVolumeState.loadAll(tag.getList("FluidVolumes", Tag.TAG_COMPOUND), registries) : List.of();
        AABB bounds = null;
        for (FluidVolumeState volume : fluidVolumes) {
            if (volume.getBounds() != null) {
                var box = volume.getBounds().move(getBlockPos());
                bounds = bounds == null ? box : bounds.minmax(box);
            }
        }
        visualBounds = bounds == null ? null : bounds.minmax(new AABB(getBlockPos()));
    }

    public void scheduleValidation() {
        pendingValidation = true;
    }

    protected void markChanged() {
        setChanged();
        syncPending = true;
    }

    private void validateStructure() {
        var pos = getBlockPos();
        Rotation current = structure == null ? null : structure.formedRotation(level, pos, formedRotation);
        double currentTier = current == null ? Double.NEGATIVE_INFINITY : StructureTier.of(structure);
        for (StructureModel candidate : StructureManager.getStructuresForController(controllerId)) {
            if (candidate == structure) {
                continue;
            }
            if (current != null && StructureTier.of(candidate) <= currentTier) {
                break;
            }
            var rotation = candidate.formedRotation(level, pos, null);
            if (rotation != null) {
                switchStructure(candidate);
                formed(rotation);
                return;
            }
        }
        if (current != null) {
            formed(current);
            return;
        }
        unformed();
    }

    private void formed(Rotation rotation) {
        formedRotation = rotation;
        BlackHoleShield.shield(level, getBlockPos(), structure, rotation);
        portStorages = collectStorages(rotation);
        speedStorages = resolveSpeedStorages();
        NearbyMachines.formed(level, this);
        if (structure != structureChunksOf || rotation != structureChunksRotation) {
            structureChunks = structure.layout().chunkBounds(getBlockPos(), rotation);
            structureChunksOf = structure;
            structureChunksRotation = rotation;
        }
        if (!isFormed) {
            isFormed = true;
            markChanged();
        }
    }

    private boolean structureChunksLoaded() {
        if (structureChunks == null) {
            return true;
        }
        for (int x = structureChunks[0]; x <= structureChunks[2]; x++) {
            for (int z = structureChunks[1]; z <= structureChunks[3]; z++) {
                if (!level.hasChunk(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean portsReplaced() {
        if (portStorages == null || formedRotation == null) {
            return false;
        }
        for (var source : portStorages.sources()) {
            if (source instanceof BlockEntity port && port.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    protected RecipeStorages collectStorages(Rotation rotation) {
        return structure.getStorages(level, getBlockPos(), rotation);
    }

    private void unformed() {
        BlackHoleShield.unshield(level, getBlockPos());
        portStorages = null;
        speedStorages = List.of();
        NearbyMachines.unformed(level, this);
        if (isFormed) {
            isFormed = false;
            markChanged();
        }
    }

    private void updateControllerState() {
        var blockState = getBlockState();
        ControllerState next;
        if (!isFormed || portStorages == null) {
            next = ControllerState.UNFORMED;
        } else if (isAllowedByRedstone() && isWorking()) {
            next = ControllerState.WORKING;
        } else {
            next = ControllerState.IDLE;
        }
        if (blockState.hasProperty(ControllerState.PROPERTY) && blockState.getValue(ControllerState.PROPERTY) != next) {
            level.setBlock(getBlockPos(), blockState.setValue(ControllerState.PROPERTY, next), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        int signal = computeComparatorSignal(next);
        if (signal != comparatorSignal) {
            comparatorSignal = signal;
            level.updateNeighbourForOutputSignal(getBlockPos(), blockState.getBlock());
        }
        if (next != linkedState || portStorages != linkedStorages) {
            linkPorts(linkedStorages, portStorages, next);
            linkedStorages = portStorages;
            linkedState = next;
        }
    }

    private int computeComparatorSignal(ControllerState state) {
        if (state != ControllerState.WORKING) {
            return 0;
        }
        double percent = 100;
        if (!activeRecipes.isEmpty()) {
            percent = 0;
            for (int i = 0; i < activeRecipes.size(); i++) {
                percent = Math.max(percent, activeRecipes.get(i).state().getTickPercentage());
            }
        }
        return 1 + (int) Math.round(Math.max(0, Math.min(100, percent)) / 100 * 14);
    }

    public int getComparatorSignal() {
        return comparatorSignal;
    }

    private void linkPorts(@Nullable RecipeStorages previous, @Nullable RecipeStorages current, ControllerState state) {
        if (previous != null && previous != current) {
            Set<IPortBlockEntity> kept = Collections.newSetFromMap(new IdentityHashMap<>());
            if (current != null) {
                kept.addAll(current.sources());
            }
            for (var source : previous.sources()) {
                if (source instanceof AbstractPortBlockEntity port && !kept.contains(source) && !port.isRemoved()) {
                    port.setMachineInfo(null, null, ControllerState.UNFORMED, null);
                }
            }
        }
        if (current != null) {
            Direction front = getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite();
            int[] colors = machineColors();
            for (var source : current.sources()) {
                if (source instanceof AbstractPortBlockEntity port) {
                    port.setMachineInfo(getBlockPos(), front, state, colors);
                }
            }
        }
    }

    private @Nullable int[] machineColors() {
        int[] colors = new int[ControllerState.values().length];
        boolean any = false;
        for (ControllerState state : ControllerState.values()) {
            Integer color = ColorUtil.parse(model.screenColor(state.getSerializedName()));
            colors[state.ordinal()] = color == null ? -1 : color;
            any |= color != null;
        }
        return any ? colors : null;
    }

    private void switchStructure(StructureModel next) {
        activeRecipes.removeIf(run -> {
            var recipe = MachineRecipeManager.RECIPES.get(run.id());
            return recipe == null || !recipe.runsIn(next.id());
        });
        recountRuns();
        structure = next;
        blockReasons.clear();
        formedRotation = null;
        cachedStructureRecipes = null;
        storageContentCacheValid = false;
        recipeNextCheckTime.clear();
        updateCurrentRecipe();
        markChanged();
    }

    public boolean isAllowedByRedstone() {
        return switch (redstoneMode) {
            case IGNORED -> true;
            case WITH_REDSTONE -> level.hasNeighborSignal(getBlockPos());
            case WITHOUT_REDSTONE -> !level.hasNeighborSignal(getBlockPos());
        };
    }

    private void runRecipe(long gameTime) {
        if (portStorages == null) {
            return;
        }
        long changeCount = portStorages.changeCount();
        if (changeCount != lastStorageChangeCount) {
            lastStorageChangeCount = changeCount;
            storageContentCacheValid = false;
            recipeNextCheckTime.clear();
        }
        attachStorages(gameTime);
        broadcastDemand(gameTime);
        if (!storageContentCacheValid) {
            rebuildStorageCache(gameTime);
        }
        boolean allowed = isAllowedByRedstone();
        if (allowed) {
            processActiveRecipeOutputs();
            scanAndStartRecipes(gameTime);
        }
        performRecipeTick(gameTime, allowed);
    }

    private void attachStorages(long gameTime) {
        for (var storage : portStorages.inputStorages()) {
            if (storage instanceof IControllerAwareStorage aware) {
                aware.attachController(this, gameTime);
            }
        }
        for (var storage : portStorages.outputStorages()) {
            if (storage instanceof IControllerAwareStorage aware) {
                aware.attachController(this, gameTime);
            }
        }
    }

    private void broadcastDemand(long gameTime) {
        var requested = activeRequest(gameTime);
        if (requested == null && !isManualSelection()) {
            return;
        }
        var demandedId = requested != null ? requested : selectedRecipeId;
        var demanded = demandedId == null ? null : MachineRecipeManager.RECIPES.get(demandedId);
        for (var storage : portStorages.inputStorages()) {
            if (storage instanceof IRecipeDemandListener listener) {
                listener.setRecipeDemand(gameTime, demanded);
            }
        }
    }

    private void rebuildStorageCache(long gameTime) {
        if (activeRecipes.isEmpty() && lastResourceScanTime >= 0 && gameTime - lastResourceScanTime < IDLE_SCAN_INTERVAL) {
            return;
        }
        lastResourceScanTime = gameTime;
        cachedItemIds.clear();
        cachedFluidIds.clear();
        cachedHasEnergy = false;
        for (ItemPortStorage storage : portStorages.getInputStorages(ItemPortStorage.class)) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                var stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && handler.getActualCount(slot) > 0) {
                    cachedItemIds.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                }
            }
        }
        for (FluidPortStorage storage : portStorages.getInputStorages(FluidPortStorage.class)) {
            var handler = storage.getHandler();
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                var fluid = handler.getFluidInTank(tank);
                if (fluid.getAmount() > 0) {
                    cachedFluidIds.add(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
                }
            }
        }
        for (EnergyPortStorage storage : portStorages.getInputStorages(EnergyPortStorage.class)) {
            if (storage.getStoredEnergy() > 0) {
                cachedHasEnergy = true;
                break;
            }
        }
        storageContentCacheValid = true;
        if (cachedHasEnergy != candidateEnergy || !cachedItemIds.equals(candidateItems) || !cachedFluidIds.equals(candidateFluids)) {
            candidateItems.clear();
            candidateItems.addAll(cachedItemIds);
            candidateFluids.clear();
            candidateFluids.addAll(cachedFluidIds);
            candidateEnergy = cachedHasEnergy;
            candidatesValid = false;
        }
        recipeNextCheckTime.clear();
    }

    private void processActiveRecipeOutputs() {
        activeRecipes.removeIf(run -> {
            var recipe = MachineRecipeManager.RECIPES.get(run.id());
            var state = run.state();
            if (recipe == null || !state.isCanFinish() || recipeNextCheckTime.getOrDefault(run.id(), 0L) > level.getGameTime()) {
                return false;
            }
            if (!recipe.outputs().canProcess(level, portStorages, state)) {
                noteBlocked(run.id(), state, level.getGameTime());
                recipeNextCheckTime.put(run.id(), level.getGameTime() + OUTPUT_BLOCKED_COOLDOWN);
                return false;
            }
            recipe.outputs().process(level, portStorages, state);
            recipeFinished(recipe);
            blockReasons.remove(run.id());
            storageContentCacheValid = false;
            lastProgressTime = level.getGameTime();
            return true;
        });
        recountRuns();
    }

    private void scanAndStartRecipes(long gameTime) {
        if (cachedStructureRecipes == null || cachedRecipeVersion != MachineRecipeManager.RECIPE_VERSION) {
            cachedStructureRecipes = new RecipeCandidateIndex(new ArrayList<>(MachineRecipeManager.getRecipesByStrucutreId(structure.id())));
            cachedRecipeVersion = MachineRecipeManager.RECIPE_VERSION;
            candidatesValid = false;
            failedRecipes.clear();
            recipeNextCheckTime.clear();
        }
        boolean filtered = storageContentCacheValid;
        if (filtered && !candidatesValid) {
            cachedStructureRecipes.collect(cachedItemIds, cachedFluidIds, cachedHasEnergy, candidatePositions);
            candidatesValid = true;
        }
        int total = filtered ? candidatePositions.size() : cachedStructureRecipes.size();
        if (total == 0) {
            return;
        }
        var mode = getRecipeSelectionMode();
        int checks = mode.fairScheduling() ? total : Math.min(SEARCH_CHECKS_PER_TICK, total);
        int index = nextRecipeCheckIndex % total;
        var requested = activeRequest(gameTime);
        Candidate roundRobin = null;
        Candidate deferred = null;
        boolean started = false;
        for (int performed = 0; performed < checks; performed++) {
            int position = filtered ? candidatePositions.getInt(index) : index;
            RecipeModel recipe = cachedStructureRecipes.recipe(position);
            index = (index + 1) % total;
            if (!isEligible(recipe, requested, gameTime)) {
                continue;
            }
            var needs = cachedStructureRecipes.requirements(position);
            boolean mayHave = mayHaveInputs(needs);
            if (!recipe.conditions().isEmpty()) {
                int failedCondition = recipe.conditions().firstFailingStart(conditionContext());
                if (failedCondition >= 0) {
                    if (filtered && mayHave) {
                        noteBlocked(recipe.id(), RecipeBlockReason.Kind.CONDITION, failedCondition, gameTime);
                    }
                    continue;
                }
            }
            if (!mayHave) {
                recipeNextCheckTime.put(recipe.id(), gameTime + SKIP_COOLDOWN);
                continue;
            }
            var state = newRecipeProbe();
            if (!canStart(recipe, state)) {
                if (state.getBlockedKind() != null) {
                    noteBlocked(recipe.id(), state, gameTime);
                    recipeNextCheckTime.put(recipe.id(), gameTime + SKIP_COOLDOWN);
                }
                continue;
            }
            var primary = needs.primaryItem();
            if (mode == RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM && primary != null) {
                if (cachedItemIds.contains(primary)) {
                    var candidate = new Candidate(recipe, primary, state, inputItemLastStartedSequence.getOrDefault(primary, Long.MIN_VALUE));
                    if (roundRobin == null || prefers(candidate, roundRobin)) {
                        roundRobin = candidate;
                    }
                }
                continue;
            }
            if (mode == RecipeSelectionMode.AVOID_SAME_RECIPE && recipe.id().equals(lastStartedRecipeId)) {
                if (deferred == null) {
                    deferred = new Candidate(recipe, primary, state, 0);
                }
                continue;
            }
            if (startRecipe(recipe, gameTime, primary, state)) {
                started = true;
                if (!mode.fairScheduling()) {
                    startCopies(recipe, requested, gameTime, primary);
                }
            }
        }
        nextRecipeCheckIndex = index;
        if (!started && roundRobin != null && canStart(roundRobin.recipe(), roundRobin.state())) {
            started = startRecipe(roundRobin.recipe(), gameTime, roundRobin.inputKey(), roundRobin.state());
        }
        if (!started && deferred != null && canStart(deferred.recipe(), deferred.state())) {
            startRecipe(deferred.recipe(), gameTime, deferred.inputKey(), deferred.state());
        }
    }

    private void startCopies(RecipeModel recipe, @Nullable ResourceLocation requested, long gameTime, @Nullable ResourceLocation inputKey) {
        while (isEligible(recipe, requested, gameTime)) {
            if (!recipe.conditions().isEmpty() && recipe.conditions().firstFailingStart(conditionContext()) >= 0) {
                return;
            }
            var state = newRecipeProbe();
            if (!canStart(recipe, state) || !startRecipe(recipe, gameTime, inputKey, state)) {
                return;
            }
        }
    }

    private boolean isEligible(RecipeModel recipe, @Nullable ResourceLocation requested, long gameTime) {
        if (runningCopies(recipe.id()) >= maxRecipeCopies()) {
            return false;
        }
        if (requested != null) {
            if (!recipe.id().equals(requested) || requestedCrafts <= 0) {
                return false;
            }
        } else {
            boolean picked = isManualSelection() && recipe.id().equals(selectedRecipeId);
            if ((isManualSelection() || recipe.requestOnly()) && !picked) {
                return false;
            }
        }
        return recipeNextCheckTime.getOrDefault(recipe.id(), 0L) <= gameTime;
    }

    private RecipeConditionContext conditionContext() {
        return new RecipeConditionContext(level, getBlockPos(), structure, nearby());
    }

    private NearbyMachines.Lookup nearby() {
        if (nearby == null) {
            nearby = new NearbyMachines.Lookup(level, getBlockPos());
        }
        return nearby;
    }

    private boolean mayHaveInputs(RecipeRequirements needs) {
        if (!storageContentCacheValid) {
            return true;
        }
        return RecipeCandidateIndex.matches(needs, cachedItemIds, cachedFluidIds, cachedHasEnergy);
    }

    private boolean canStart(RecipeModel recipe, RecipeStateModel state) {
        return canRunAlongside(recipe)
                && recipe.inputs().canProcess(level, portStorages, state)
                && recipe.outputs().canProcess(level, portStorages, state);
    }

    private boolean prefers(Candidate candidate, Candidate current) {
        if (candidate.lastUse() != current.lastUse()) {
            return candidate.lastUse() < current.lastUse();
        }
        long a = recipeLastStartedSequence.getOrDefault(candidate.recipe().id(), Long.MIN_VALUE);
        long b = recipeLastStartedSequence.getOrDefault(current.recipe().id(), Long.MIN_VALUE);
        return a < b;
    }

    private boolean canRunAlongside(RecipeModel recipe) {
        int copies = runningCopies(recipe.id());
        if (copies > 0) {
            return copies < maxRecipeCopies();
        }
        int running = distinctRunning();
        boolean allowParallel = recipe.parallelProcessing();
        if (recipe.parallelProcessing() == MMConfig.PARALLEL_PROCESSING_DEFAULT) {
            allowParallel = model.parallelProcessingDefault();
        }
        int limit = parallelLimit();
        boolean underLimit = limit == 0 ? running == 0 : running < limit;
        return (allowParallel || running == 0) && underLimit;
    }

    public int runningCopies(ResourceLocation recipeId) {
        return runCounts.getInt(recipeId);
    }

    private int distinctRunning() {
        return runCounts.size();
    }

    private void recountRuns() {
        runCounts.clear();
        for (int i = 0; i < activeRecipes.size(); i++) {
            runCounts.addTo(activeRecipes.get(i).id(), 1);
        }
    }

    private int maxRecipeCopies() {
        return structure == null ? 1 : structure.maxRecipeCopies();
    }

    private boolean startRecipe(RecipeModel recipe, long gameTime, @Nullable ResourceLocation inputKey, RecipeStateModel state) {
        if (MMInteropManager.KUBEJS.isPresent() && !MMInteropManager.KUBEJS.get().postRecipeStarted(this, recipe.id())) {
            recipeNextCheckTime.put(recipe.id(), gameTime + SKIP_COOLDOWN);
            return false;
        }
        if (recipe.outputs().usesConsumedItems()) {
            state.trackConsumedItems();
        }
        recipe.inputs().process(level, portStorages, state);
        recipe.conditions().start(conditionContext());
        if (recipe.id().equals(requestedRecipeId) && requestedCrafts > 0) {
            requestedCrafts--;
            requestStarts++;
        }
        storageContentCacheValid = false;
        state.setCanProcess(true);
        activeRecipes.add(new ActiveRun(recipe.id(), state));
        runCounts.addTo(recipe.id(), 1);
        blockReasons.remove(recipe.id());
        lastStartedRecipeId = recipe.id();
        lastRecipeStartTime = gameTime;
        lastProgressTime = gameTime;
        recipeSelectionSequence++;
        recipeLastStartedSequence.put(recipe.id(), recipeSelectionSequence);
        if (inputKey != null) {
            inputItemLastStartedSequence.put(inputKey, recipeSelectionSequence);
        }
        if (!state.getCaptures().isEmpty()) {
            for (var kinds : state.getCaptures().values()) {
                for (ItemStack kind : kinds) {
                    inputItemLastStartedSequence.put(BuiltInRegistries.ITEM.getKey(kind.getItem()), recipeSelectionSequence);
                }
            }
        }
        state.setFlexInputOrder(null);
        markChanged();
        return true;
    }

    private RecipeStateModel newRecipeProbe() {
        var state = new RecipeStateModel();
        if (getRecipeSelectionMode() == RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM) {
            state.setFlexInputOrder(Comparator.comparingLong(stack ->
                    inputItemLastStartedSequence.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()), Long.MIN_VALUE)));
        }
        return state;
    }

    private void recipeFinished(RecipeModel recipe) {
        linkExportRequested = true;
        recipe.conditions().finish(conditionContext());
        MMInteropManager.KUBEJS.ifPresent(kubejs -> kubejs.postRecipeFinished(this, recipe.id()));
    }

    private List<EntityPortStorage> resolveSpeedStorages() {
        if (portStorages == null) {
            return List.of();
        }
        var found = new ArrayList<EntityPortStorage>();
        for (var storage : portStorages.getInputStorages(EntityPortStorage.class)) {
            if (((EntityPortStorageModel) storage.getStorageModel()).speedPerEntity() > 0) {
                found.add(storage);
            }
        }
        return found.isEmpty() ? List.of() : found;
    }

    private double recipeSpeedMultiplier() {
        double multiplier = 1;
        for (var storage : speedStorages) {
            multiplier += storage.speedBonus();
        }
        if (structure != null) {
            for (StructureBoost boost : structure.boosts()) {
                multiplier += boost.bonus(nearby());
            }
        }
        return multiplier;
    }

    private void performRecipeTick(long gameTime, boolean allowed) {
        if (allowed) {
            tickSpeedMultiplier = recipeSpeedMultiplier();
            activeRecipes.removeIf(run -> {
                var recipe = MachineRecipeManager.RECIPES.get(run.id());
                return recipe == null || tickRecipe(recipe, run.state(), gameTime);
            });
            recountRuns();
        }
        updateCurrentRecipe();
    }

    private boolean tickRecipe(RecipeModel recipe, RecipeStateModel state, long gameTime) {
        if (!state.isCanFinish() && !recipe.conditions().isEmpty()) {
            int failedCondition = recipe.conditions().firstFailingRun(conditionContext());
            if (failedCondition >= 0) {
                noteBlocked(recipe.id(), RecipeBlockReason.Kind.CONDITION, failedCondition, gameTime);
                return false;
            }
        }
        if (!state.isCanFinish()) {
            boolean fed;
            state.markBlocked(null, -1);
            try {
                fed = recipe.outputs().canProcessTick(level, portStorages, state) && processTickInputs(recipe, state);
                if (fed) {
                    recipe.outputs().processTick(level, portStorages, state);
                }
            } catch (RuntimeException e) {
                if (failedRecipes.add(recipe.id())) {
                    Ref.LOG.error("Recipe {} failed while running in the machine at {}", recipe.id(), getBlockPos(), e);
                }
                fed = false;
            }
            storageContentCacheValid = false;
            if (fed) {
                state.proceedTick(tickSpeedMultiplier);
                lastProgressTime = gameTime;
                blockReasons.remove(recipe.id());
            } else {
                noteBlocked(recipe.id(), state, gameTime);
            }
        }
        state.setTickPercentage(((double) state.getTickProgress() / recipe.ticks()) * 100);
        if (state.getTickProgress() < recipe.ticks()) {
            return false;
        }
        state.setCanFinish(true);
        if (recipeNextCheckTime.getOrDefault(recipe.id(), 0L) > gameTime) {
            return false;
        }
        if (recipe.outputs().canProcess(level, portStorages, state)) {
            blockReasons.remove(recipe.id());
            lastProgressTime = gameTime;
            recipe.outputs().process(level, portStorages, state);
            recipeFinished(recipe);
            storageContentCacheValid = false;
            return true;
        }
        noteBlocked(recipe.id(), state, gameTime);
        recipeNextCheckTime.put(recipe.id(), gameTime + OUTPUT_BLOCKED_COOLDOWN);
        return false;
    }

    private boolean processTickInputs(RecipeModel recipe, RecipeStateModel state) {
        var inputs = recipe.inputs().inputs();
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            if (input instanceof ConsumeRecipeIngredientEntry entry && entry.isPerTick()) {
                var ingredient = entry.ingredientFor(state);
                if (ingredient instanceof ITickSpreadIngredient spread) {
                    if (!drawTickSpread(recipe, state, spread)) {
                        state.markBlocked(RecipeBlockReason.Kind.INPUT, i);
                        return false;
                    }
                    continue;
                }
                if (!ingredient.canProcess(level, portStorages, state)) {
                    state.markBlocked(RecipeBlockReason.Kind.INPUT, i);
                    return false;
                }
            }
            input.processTick(level, portStorages, state);
        }
        return true;
    }

    private boolean drawTickSpread(RecipeModel recipe, RecipeStateModel state, ITickSpreadIngredient spread) {
        long total = spread.resolveAmount(state);
        int ticks = Math.max(1, recipe.ticks());
        long toExtract = total / ticks + (state.getTickProgress() == ticks - 1 ? total % ticks : 0);
        if (toExtract <= 0) {
            return true;
        }
        if (spread.extractFromInputs(portStorages, toExtract, true) < toExtract) {
            return false;
        }
        spread.extractFromInputs(portStorages, toExtract, false);
        return true;
    }

    private void noteBlocked(ResourceLocation recipeId, RecipeStateModel state, long gameTime) {
        if (state.getBlockedKind() != null) {
            noteBlocked(recipeId, state.getBlockedKind(), state.getBlockedIndex(), gameTime);
        }
    }

    private void noteBlocked(ResourceLocation recipeId, RecipeBlockReason.Kind kind, int index, long gameTime) {
        var note = blockReasons.get(recipeId);
        if (note == null) {
            blockReasons.put(recipeId, new BlockNote(new RecipeBlockReason(recipeId, kind, index), gameTime));
            return;
        }
        if (note.reason.kind() != kind || note.reason.index() != index) {
            note.reason = new RecipeBlockReason(recipeId, kind, index);
        }
        note.time = gameTime;
    }

    private void noteNearMisses(long gameTime) {
        cachedStructureRecipes.nearMisses(cachedItemIds, cachedFluidIds, cachedHasEnergy, NEAR_MISS_CHECKS, nearMissPositions);
        for (int i = 0; i < nearMissPositions.size(); i++) {
            RecipeModel recipe = cachedStructureRecipes.recipe(nearMissPositions.getInt(i));
            if (!recipe.conditions().isEmpty()) {
                int failedCondition = recipe.conditions().firstFailingStart(conditionContext());
                if (failedCondition >= 0) {
                    noteBlocked(recipe.id(), RecipeBlockReason.Kind.CONDITION, failedCondition, gameTime);
                    continue;
                }
            }
            var probe = new RecipeStateModel();
            if (!recipe.inputs().canProcess(level, portStorages, probe) || !recipe.outputs().canProcess(level, portStorages, probe)) {
                noteBlocked(recipe.id(), probe, gameTime);
            }
        }
    }

    private void updateShownBlockReason(long gameTime) {
        if (isFormed && portStorages != null && activeRecipes.isEmpty() && storageContentCacheValid && cachedStructureRecipes != null) {
            noteNearMisses(gameTime);
        }
        blockReasons.values().removeIf(note -> gameTime - note.time > BLOCK_REASON_TICKS
                || MachineRecipeManager.RECIPES.get(note.reason.recipeId()) == null);
        RecipeBlockReason shown = null;
        List<RecipeBlockReason> others = List.of();
        if (isFormed && structure != null && isAllowedByRedstone()) {
            for (int i = 0; i < activeRecipes.size(); i++) {
                var note = blockReasons.get(activeRecipes.get(i).id());
                if (note != null) {
                    shown = note.reason;
                    break;
                }
            }
            if (shown == null && activeRecipes.isEmpty() && !blockReasons.isEmpty()) {
                var ranked = rankBlockReasons();
                shown = ranked.get(0);
                others = List.copyOf(ranked.subList(1, Math.min(ranked.size(), OTHER_BLOCK_REASONS + 1)));
            }
        }
        if (!Objects.equals(shown, shownBlockReason) || !others.equals(otherBlockReasons)) {
            shownBlockReason = shown;
            otherBlockReasons = others;
            syncPending = true;
        }
    }

    private List<RecipeBlockReason> rankBlockReasons() {
        var ready = new HashMap<ResourceLocation, Double>();
        for (var note : blockReasons.values()) {
            ready.put(note.reason.recipeId(), readyInputs(note.reason.recipe()));
        }
        var reasons = new ArrayList<RecipeBlockReason>(blockReasons.size());
        for (var note : blockReasons.values()) {
            reasons.add(note.reason);
        }
        reasons.sort(Comparator.<RecipeBlockReason>comparingDouble(reason -> ready.get(reason.recipeId()))
                .thenComparingInt(RecipeBlockReason::progress)
                .reversed()
                .thenComparing(RecipeBlockReason::recipeId));
        return reasons;
    }

    private double readyInputs(@Nullable RecipeModel recipe) {
        if (recipe == null || portStorages == null) {
            return 0;
        }
        var inputs = recipe.inputs().inputs();
        if (inputs.isEmpty()) {
            return 1;
        }
        int ready = 0;
        for (var input : inputs) {
            if (input.canProcess(level, portStorages, new RecipeStateModel())) {
                ready++;
            }
        }
        return (double) ready / inputs.size();
    }

    @Nullable
    public RecipeBlockReason getBlockReason() {
        return shownBlockReason;
    }

    public List<RecipeBlockReason> getOtherBlockReasons() {
        return otherBlockReasons;
    }

    private void updateCurrentRecipe() {
        currentRecipe = activeRecipes.isEmpty() ? null : MachineRecipeManager.RECIPES.get(activeRecipes.get(0).id());
    }

    public void invalidateProgress() {
        linkPorts(linkedStorages, null, ControllerState.UNFORMED);
        linkedStorages = null;
        linkedState = null;
        activeRecipes.clear();
        runCounts.clear();
        blockReasons.clear();
        currentRecipe = null;
        structure = null;
        formedRotation = null;
        isFormed = false;
        BlackHoleShield.unshield(level, getBlockPos());
        portStorages = null;
        speedStorages = List.of();
        NearbyMachines.unformed(level, this);
        cachedStructureRecipes = null;
        storageContentCacheValid = false;
        recipeNextCheckTime.clear();
        markChanged();
    }

    public void reformTo(StructureModel newStructure) {
        if (newStructure != structure) {
            switchStructure(newStructure);
        }
        scheduleValidation();
    }

    @Nullable
    public GeoModelConfig geoModel() {
        return GeoModelConfig.resolve(model == null ? null : model.config(), structure == null ? null : structure.getConfig());
    }

    @Nullable
    public Object getGeoAnimatable() {
        return geoAnimatable;
    }

    public void setGeoAnimatable(@Nullable Object geoAnimatable) {
        this.geoAnimatable = geoAnimatable;
    }

    @Override
    public ControllerModel getModel() {
        return model;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getName();
    }

    public Component getName() {
        return customName != null ? Component.literal(customName) : model.displayName();
    }

    public @Nullable String getCustomName() {
        return customName;
    }

    public void setCustomName(@Nullable String name) {
        customName = name == null || name.isBlank() ? null : name.strip();
        if (customName != null && customName.length() > MAX_NAME_LENGTH) {
            customName = customName.substring(0, MAX_NAME_LENGTH);
        }
        markChanged();
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        var name = input.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            setCustomName(name.getString());
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (customName != null) {
            components.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("CustomName");
    }

    public @Nullable LinkData getNetworkLink() {
        return networkLink;
    }

    public void setNetworkLink(@Nullable LinkData link) {
        networkLink = link;
        markChanged();
    }

    public TierPrefs getAssemblyTiers() {
        return assemblyTiers;
    }

    public void setAssemblyTier(String key, int rank) {
        int valid = TierPrefs.validate(PortTiers.maxTiers(getAssemblyCandidates()), key, rank);
        if (valid != TierPrefs.REJECTED && assemblyTiers.set(key, valid)) {
            markChanged();
        }
    }

    public void setAssemblyStructureId(ResourceLocation id) {
        if (findAssemblyCandidate(id) == null || id.equals(assemblyStructureId)) {
            return;
        }
        assemblyStructureId = id;
        markChanged();
    }

    public List<StructureModel> getAssemblyCandidates() {
        var byId = new TreeMap<ResourceLocation, StructureModel>();
        for (StructureModel candidate : StructureManager.getStructuresForController(controllerId)) {
            byId.put(candidate.id(), candidate);
        }
        return List.copyOf(byId.values());
    }

    public @Nullable StructureModel getAssemblyStructure() {
        StructureModel chosen = findAssemblyCandidate(assemblyStructureId);
        if (chosen != null) {
            return chosen;
        }
        StructureModel current = structure == null ? null : findAssemblyCandidate(structure.id());
        if (current != null) {
            return current;
        }
        List<StructureModel> candidates = getAssemblyCandidates();
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    public @Nullable StructureModel findAssemblyCandidate(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        for (StructureModel candidate : getAssemblyCandidates()) {
            if (candidate.id().equals(id)) {
                return candidate;
            }
        }
        return null;
    }

    public boolean isSoundMuted() {
        return soundMuted;
    }

    public void setSoundMuted(boolean muted) {
        soundMuted = muted;
        markChanged();
    }

    public ModelDisplay getModelDisplay() {
        return modelDisplay;
    }

    public void setModelDisplay(@Nullable ModelDisplay display) {
        modelDisplay = display == null ? ModelDisplay.ON : display;
        markChanged();
    }

    public RecipeSelectionMode getRecipeSelectionMode() {
        return recipeModeOverride != null ? recipeModeOverride : model.recipeSelectionMode();
    }

    public void setRecipeSelectionMode(RecipeSelectionMode mode) {
        recipeModeOverride = mode == model.recipeSelectionMode() ? null : mode;
        recipeNextCheckTime.clear();
        markChanged();
    }

    public @Nullable RecipeModel getDisplayedRecipe() {
        if (currentRecipe != null) {
            return currentRecipe;
        }
        if (level == null || lastStartedRecipeId == null || lastRecipeStartTime == Long.MIN_VALUE || level.getGameTime() - lastRecipeStartTime > RECENT_RECIPE_TICKS) {
            return null;
        }
        return MachineRecipeManager.RECIPES.get(lastStartedRecipeId);
    }

    public boolean isWorking() {
        return level != null && lastProgressTime != Long.MIN_VALUE && level.getGameTime() - lastProgressTime <= RECENT_RECIPE_TICKS;
    }

    public String statusKey() {
        if (!isFormed || portStorages == null) return "not_formed";
        if (!isAllowedByRedstone()) return "paused";
        if (isWorking()) return "running";
        if (!activeRecipes.isEmpty()) return "stalled";
        return "idle";
    }

    public int getActiveRecipeCount() {
        return level != null && level.isClientSide ? syncedRunCount : activeRecipes.size();
    }

    public int getDisplayedParallelLimit() {
        int recipes = model.parallelProcessingDefault() ? Math.max(1, parallelLimit()) : 1;
        return recipes * maxRecipeCopies();
    }

    private int parallelLimit() {
        int limit = model.maxParallelRecipes();
        if (structure != null && structure.maxParallelRecipes() >= 0) {
            limit = structure.maxParallelRecipes();
        }
        return limit < 0 ? MMConfig.MAX_PARALLEL_RECIPES : limit;
    }

    public StructureDiagnosis diagnoseStructure() {
        if (level == null || isFormed) {
            return StructureDiagnosis.NONE;
        }
        try {
            return StructureDiagnosis.diagnose(level, getBlockPos(), StructureManager.getStructuresForController(controllerId));
        } catch (RuntimeException e) {
            Ref.LOG.error("Failed to diagnose the structure of the controller at {}", getBlockPos(), e);
            return StructureDiagnosis.NONE;
        }
    }

    public List<BlockPos> getPortPositions() {
        if (portStorages == null) {
            return List.of();
        }
        var inputs = new ArrayList<BlockPos>();
        var outputs = new ArrayList<BlockPos>();
        for (var source : portStorages.sources()) {
            if (source instanceof BlockEntity port) {
                (source.isInput() ? inputs : outputs).add(port.getBlockPos());
            }
        }
        inputs.addAll(outputs);
        return inputs;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new MachineControllerMenu(model, groupHolder, windowId, inv, this);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        ListTag runsTag = new ListTag();
        for (int i = 0; i < activeRecipes.size(); i++) {
            var run = activeRecipes.get(i);
            CompoundTag runTag = new CompoundTag();
            runTag.putString("id", run.id().toString());
            runTag.put("state", run.state().save(new CompoundTag(), registries));
            runsTag.add(runTag);
        }
        tag.put("activeRuns", runsTag);
        if (structure != null) {
            tag.putString("structureId", structure.id().toString());
        }
        tag.putBoolean("isFormed", isFormed);
        if (lastStartedRecipeId != null) {
            tag.putString("lastStartedRecipeId", lastStartedRecipeId.toString());
            tag.putLong("lastRecipeStartTime", lastRecipeStartTime);
        }
        tag.putLong("lastProgressTime", lastProgressTime);
        if (recipeModeOverride != null) {
            tag.putString("RecipeSelectionMode", recipeModeOverride.serializedName());
        }
        if (customName != null) {
            tag.putString("CustomName", customName);
        }
        if (soundMuted) {
            tag.putBoolean("SoundMuted", true);
        }
        if (modelDisplay != ModelDisplay.ON) {
            tag.putString("ModelDisplay", modelDisplay.serializedName());
        }
        if (networkLink != null) {
            tag.put("NetworkLink", networkLink.save());
        }
        if (lastLinkExport >= 0) {
            tag.putLong("LastLinkExport", lastLinkExport);
        }
        if (!assemblyTiers.asMap().isEmpty()) {
            tag.put("AssemblyTiers", assemblyTiers.save());
        }
        if (assemblyStructureId != null) {
            tag.putString("AssemblyStructure", assemblyStructureId.toString());
        }
        tag.putLong("recipeSelectionSequence", recipeSelectionSequence);
        if (!inputItemLastStartedSequence.isEmpty()) {
            CompoundTag inputHistoryTag = new CompoundTag();
            for (Map.Entry<ResourceLocation, Long> entry : inputItemLastStartedSequence.entrySet()) {
                inputHistoryTag.putLong(entry.getKey().toString(), entry.getValue());
            }
            tag.put("inputItemLastStartedSequence", inputHistoryTag);
        }
        if (selectedRecipeId != null) {
            tag.putString("selectedRecipe", selectedRecipeId.toString());
        }
        tag.putBoolean("filler", true);
        tag.putInt("redstoneMode", redstoneMode.ordinal());
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadFluidVolumes(tag, registries);
        activeRecipes.clear();
        if (tag.contains("activeRuns")) {
            ListTag runsTag = tag.getList("activeRuns", Tag.TAG_COMPOUND);
            for (int i = 0; i < runsTag.size(); i++) {
                CompoundTag runTag = runsTag.getCompound(i);
                ResourceLocation recipeId = ResourceLocation.tryParse(runTag.getString("id"));
                if (recipeId != null) {
                    activeRecipes.add(new ActiveRun(recipeId, RecipeStateModel.load(runTag.getCompound("state"), registries)));
                }
            }
        } else {
            CompoundTag recipesTag = tag.getCompound("activeRecipes");
            for (String key : recipesTag.getAllKeys()) {
                ResourceLocation recipeId = ResourceLocation.tryParse(key);
                if (recipeId != null) {
                    activeRecipes.add(new ActiveRun(recipeId, RecipeStateModel.load(recipesTag.getCompound(key), registries)));
                }
            }
        }
        recountRuns();
        syncedRunCount = tag.contains("activeRunCount") ? tag.getInt("activeRunCount") : activeRecipes.size();
        structure = tag.contains("structureId") ? StructureManager.STRUCTURES.get(ResourceLocation.tryParse(tag.getString("structureId"))) : null;
        isFormed = tag.contains("isFormed") ? tag.getBoolean("isFormed") : structure != null;
        lastStartedRecipeId = tag.contains("lastStartedRecipeId") ? ResourceLocation.tryParse(tag.getString("lastStartedRecipeId")) : null;
        lastRecipeStartTime = tag.contains("lastRecipeStartTime") ? tag.getLong("lastRecipeStartTime") : Long.MIN_VALUE;
        lastProgressTime = tag.contains("lastProgressTime") ? tag.getLong("lastProgressTime") : Long.MIN_VALUE;
        recipeModeOverride = tag.contains("RecipeSelectionMode") ? RecipeSelectionMode.parse(tag.getString("RecipeSelectionMode")) : null;
        customName = tag.contains("CustomName") ? tag.getString("CustomName") : null;
        soundMuted = tag.getBoolean("SoundMuted");
        var display = ModelDisplay.parse(tag.getString("ModelDisplay"));
        modelDisplay = display == null ? ModelDisplay.ON : display;
        networkLink = tag.contains("NetworkLink") ? LinkData.load(tag.getCompound("NetworkLink")) : null;
        lastLinkExport = tag.contains("LastLinkExport") ? tag.getLong("LastLinkExport") : -1L;
        assemblyTiers.copyFrom(TierPrefs.load(tag.getCompound("AssemblyTiers")));
        assemblyStructureId = tag.contains("AssemblyStructure") ? ResourceLocation.tryParse(tag.getString("AssemblyStructure")) : null;
        selectedRecipeId = tag.contains("selectedRecipe") ? ResourceLocation.tryParse(tag.getString("selectedRecipe")) : null;
        recipeSelectionSequence = tag.getLong("recipeSelectionSequence");
        inputItemLastStartedSequence.clear();
        CompoundTag inputHistoryTag = tag.getCompound("inputItemLastStartedSequence");
        for (String key : inputHistoryTag.getAllKeys()) {
            ResourceLocation itemId = ResourceLocation.tryParse(key);
            if (itemId != null) {
                inputItemLastStartedSequence.put(itemId, inputHistoryTag.getLong(key));
            }
        }
        shownBlockReason = tag.contains("BlockReason") ? RecipeBlockReason.load(tag.getCompound("BlockReason")) : null;
        var others = new ArrayList<RecipeBlockReason>();
        var othersTag = tag.getList("OtherBlockReasons", Tag.TAG_COMPOUND);
        for (int i = 0; i < othersTag.size(); i++) {
            var reason = RecipeBlockReason.load(othersTag.getCompound(i));
            if (reason != null) {
                others.add(reason);
            }
        }
        otherBlockReasons = List.copyOf(others);
        updateCurrentRecipe();
        int mode = tag.getInt("redstoneMode");
        var modes = RedstoneMode.values();
        redstoneMode = mode >= 0 && mode < modes.length ? modes[mode] : RedstoneMode.IGNORED;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveAdditional(tag, registries);
        if (activeRecipes.size() > 1) {
            ListTag runsTag = tag.getList("activeRuns", Tag.TAG_COMPOUND);
            while (runsTag.size() > 1) {
                runsTag.remove(runsTag.size() - 1);
            }
            tag.putInt("activeRunCount", activeRecipes.size());
        }
        if (shownBlockReason != null) {
            tag.put("BlockReason", shownBlockReason.save());
        }
        if (!otherBlockReasons.isEmpty()) {
            var others = new ListTag();
            for (RecipeBlockReason reason : otherBlockReasons) {
                others.add(reason.save());
            }
            tag.put("OtherBlockReasons", others);
        }
        if (!fluidVolumes.isEmpty()) {
            tag.put("FluidVolumes", FluidVolumeState.saveAll(fluidVolumes, registries));
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public RecipeStateModel getRecipeState() {
        if (activeRecipes.isEmpty()) {
            return null;
        }
        return activeRecipes.get(0).state();
    }

    public int getRedstoneModeOrdinal() {
        return redstoneMode.ordinal();
    }

    public void setRedstoneModeOrdinal(int ordinal) {
        var modes = RedstoneMode.values();
        if (ordinal >= 0 && ordinal < modes.length) {
            redstoneMode = modes[ordinal];
            markChanged();
        }
    }

    public String getRedstoneModeName() {
        return redstoneMode.name();
    }

    public boolean isManualSelection() {
        return getRecipeSelectionMode() == RecipeSelectionMode.MANUAL;
    }

    public void selectRecipe(@Nullable ResourceLocation recipeId) {
        if (recipeId != null) {
            var recipe = MachineRecipeManager.RECIPES.get(recipeId);
            if (recipe == null || structure == null || !recipe.runsIn(structure.id())) {
                return;
            }
        }
        selectedRecipeId = recipeId;
        recipeNextCheckTime.clear();
        markChanged();
    }

    public void requestRecipe(Object owner, ResourceLocation recipeId, long gameTime, int crafts) {
        if (!recipeId.equals(requestedRecipeId)) {
            var recipe = MachineRecipeManager.RECIPES.get(recipeId);
            if (recipe == null || structure == null || !recipe.runsIn(structure.id())) {
                return;
            }
            requestedRecipeId = recipeId;
            recipeNextCheckTime.remove(recipeId);
        }
        requestOwner = owner;
        requestedCrafts = Math.max(0, crafts);
        requestExpiresAt = gameTime + REQUEST_TIMEOUT;
    }

    public void clearRequest(Object owner, ResourceLocation recipeId) {
        if (owner == requestOwner && recipeId.equals(requestedRecipeId)) {
            requestedRecipeId = null;
            requestOwner = null;
            requestedCrafts = 0;
            requestStarts = 0;
        }
    }

    public int takeRequestStarts(Object owner) {
        if (owner != requestOwner) {
            return 0;
        }
        int starts = requestStarts;
        requestStarts = 0;
        return starts;
    }

    public void reserveOutput(Object owner, ItemStack stack, int count, long gameTime) {
        reservationOwner = owner;
        reservedOutput = stack.copyWithCount(1);
        reservedCount = Math.max(0, count);
        reservationExpiresAt = gameTime + REQUEST_TIMEOUT;
    }

    public void releaseOutput(Object owner) {
        if (owner == reservationOwner) {
            reservationOwner = null;
            reservedOutput = ItemStack.EMPTY;
            reservedCount = 0;
        }
    }

    public int reservedOutputCount(ItemStack stack) {
        if (reservedCount <= 0 || reservedOutput.isEmpty() || level == null || level.getGameTime() > reservationExpiresAt) {
            return 0;
        }
        return ItemStack.isSameItemSameComponents(reservedOutput, stack) ? reservedCount : 0;
    }

    public boolean takeLinkExportRequest() {
        boolean requested = linkExportRequested;
        linkExportRequested = false;
        return requested;
    }

    public void markLinkExported(long gameTime) {
        lastLinkExport = gameTime;
        syncPending = true;
    }

    public long getLastLinkExport() {
        return lastLinkExport;
    }

    public boolean isRecipeRunning(ResourceLocation recipeId) {
        return runningCopies(recipeId) > 0;
    }

    private record ActiveRun(ResourceLocation id, RecipeStateModel state) {
    }

    private @Nullable ResourceLocation activeRequest(long gameTime) {
        if (requestedRecipeId != null && gameTime > requestExpiresAt) {
            requestedRecipeId = null;
            requestOwner = null;
            requestedCrafts = 0;
        }
        return requestedRecipeId;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        BlackHoleShield.watch(level, this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        BlackHoleShield.unwatch(level, this);
        BlackHoleShield.unshield(level, getBlockPos());
        NearbyMachines.unformed(level, this);
    }

    public void validateNow() {
        if (level != null && !level.isClientSide() && !isRemoved()) {
            validateStructure();
        }
    }

    public boolean isFormed() {
        return isFormed;
    }

    public @Nullable Rotation getFormedRotation() {
        return isFormed ? formedRotation : null;
    }

    public @Nullable RecipeStorages getPortStorages() {
        return portStorages;
    }
}
