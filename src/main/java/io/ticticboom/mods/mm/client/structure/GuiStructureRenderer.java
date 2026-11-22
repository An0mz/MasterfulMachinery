package io.ticticboom.mods.mm.client.structure;

import lombok.Setter;
import org.joml.Vector4f;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Supplier;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.client.RenderUtil;
import io.ticticboom.mods.mm.client.blueprint.state.BlueprintStructureViewState;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.util.BlockStateRotation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuiStructureRenderer {
    public static boolean shouldEnsureValidated = false;

    private static GuiStructureRenderer hovered = null;

    public static void clearHovered() {
        hovered = null;
    }

    public static GuiStructureRenderer getHovered() {
        return hovered;
    }

    private final Supplier<List<PositionedCyclingBlockRenderer>> partsFactory;
    private List<PositionedCyclingBlockRenderer> parts;
    private Set<PositionedCyclingBlockRenderer> enclosedParts = Set.of();
    private final AutoTransform viewTransform;
    private final GuiRenderEnvSetup renderSetup = new GuiRenderEnvSetup();
    private final StructureRenderYSliceProcessor ySliceProcessor = new StructureRenderYSliceProcessor();
    private final GuiStructureLevel previewWorld = new GuiStructureLevel();
    private int worldGeneration = 0;
    private boolean previewWorldDirty = true;

    private float boundingRadius = 1;

    @Getter
    private Vector3i structureSize = new Vector3i(0);
    @Getter
    private Vector3i minBound = new Vector3i(0);
    @Getter
    private Vector3i maxBound = new Vector3i(0);
    private boolean isInitialized = false;
    @Setter
    @Nullable
    private Overlay overlay;
    private final Matrix4f lastMatrix = new Matrix4f();
    private boolean rendered;

    public interface Overlay {
        void render(PoseStack pose, MultiBufferSource buffers, Matrix4f modelView);
    }


    public GuiStructureRenderer(StructureModel model) {
        this(new AutoTransform(model), () -> {
            model.layout().setup(model);
            List<PositionedCyclingBlockRenderer> list = new ArrayList<>(new GuiStructureLayout(model.layout()).createBlockRenderers());
            var controller = model.controllerUiRenderer();
            if (controller != null) {
                list.add(controller);
            }
            return list;
        });
    }

    private GuiStructureRenderer(AutoTransform viewTransform, Supplier<List<PositionedCyclingBlockRenderer>> partsFactory) {
        this.viewTransform = viewTransform;
        this.partsFactory = partsFactory;
        parts = new ArrayList<>();
    }

    public static GuiStructureRenderer ofFormed(StructureModel model, BlockGetter level, BlockPos controller, Rotation rotation) {
        return new GuiStructureRenderer(new AutoTransform(model), () -> {
            model.layout().setup(model);
            var positions = new ArrayList<BlockPos>();
            positions.add(BlockPos.ZERO);
            for (PositionedLayoutPiece piece : model.layout().getPositionedPieces()) {
                positions.add(piece.pos());
            }
            List<PositionedCyclingBlockRenderer> list = new ArrayList<>(positions.size());
            for (BlockPos pos : positions) {
                BlockState state = level.getBlockState(controller.offset(pos.rotate(rotation)));
                if (state.isAir()) {
                    continue;
                }
                if (state.hasProperty(ControllerState.PROPERTY)) {
                    state = state.setValue(ControllerState.PROPERTY, ControllerState.UNFORMED);
                }
                GuiBlockRenderer renderer = new GuiBlockRenderer(BlockStateRotation.unrotate(state, rotation));
                renderer.setupAt(pos);
                list.add(new PositionedCyclingBlockRenderer(List.of(renderer), pos));
            }
            return list;
        });
    }

    public static GuiStructureRenderer ofBlocks(List<BuildableStructure.Placement> blocks) {
        return new GuiStructureRenderer(new AutoTransform(blocks.stream().map(BuildableStructure.Placement::pos).toList()), () -> {
            List<PositionedCyclingBlockRenderer> list = new ArrayList<>(blocks.size());
            for (BuildableStructure.Placement placement : blocks) {
                GuiBlockRenderer renderer = new GuiBlockRenderer(placement.state());
                renderer.setupAt(placement.pos());
                list.add(new PositionedCyclingBlockRenderer(List.of(renderer), placement.pos()));
            }
            return list;
        });
    }

    public void init() {
        if (!isInitialized) {
            parts = partsFactory.get();
            for (PositionedCyclingBlockRenderer part : parts) {
                part.part.setInterval(20);
            }
            getExtents();
            findEnclosedParts();
            isInitialized = true;
        }
    }

    private void getExtents() {
        var positions = parts.stream().map(x -> x.pos).toList();

        // min
        var minX = positions.stream().map(Vec3i::getX).min(Integer::compareTo).orElse(0);
        var minY = positions.stream().map(Vec3i::getY).min(Integer::compareTo).orElse(0);
        var minZ = positions.stream().map(Vec3i::getZ).min(Integer::compareTo).orElse(0);
        minBound = new Vector3i(minX, minY, minZ);

        // max
        var maxX = positions.stream().map(Vec3i::getX).max(Integer::compareTo).orElse(0);
        var maxY = positions.stream().map(Vec3i::getY).max(Integer::compareTo).orElse(0);
        var maxZ = positions.stream().map(Vec3i::getZ).max(Integer::compareTo).orElse(0);
        maxBound = new Vector3i(maxX, maxY, maxZ);

        var extentX = maxX - minX;
        var extentY = maxY - minY;
        var extentZ = maxZ - minZ;

        structureSize = new Vector3i(extentX, extentY, extentZ);

        boundingRadius = 0.5f * (float) Math.sqrt(sq(extentX + 1) + sq(extentY + 1) + sq(extentZ + 1));
        viewTransform.setCenter(new Vector3f((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f));
    }

    private static float sq(int v) {
        return (float) v * v;
    }

    private GuiPos hoverBounds;

    private void findEnclosedParts() {
        var opaquePositions = new HashSet<BlockPos>();
        for (PositionedCyclingBlockRenderer part : parts) {
            if (part.part.getPart().stream().allMatch(GuiBlockRenderer::isOpaqueCube)) {
                opaquePositions.add(part.pos);
            }
        }
        var enclosed = new HashSet<PositionedCyclingBlockRenderer>();
        for (PositionedCyclingBlockRenderer part : parts) {
            boolean surrounded = true;
            for (Direction dir : Direction.values()) {
                if (!opaquePositions.contains(part.pos.relative(dir))) {
                    surrounded = false;
                    break;
                }
            }
            if (surrounded) {
                enclosed.add(part);
            }
        }
        enclosedParts = enclosed;
    }

    public void setViewport(GuiPos viewport) {
        this.hoverBounds = viewport;
        renderSetup.setViewportPos(viewport);
    }

    public void setHoverBounds(GuiPos bounds) {
        this.hoverBounds = bounds;
    }

    public void scroll(double delta) {
        if (viewTransform.isStudio()) {
            viewTransform.fly((float) delta, 0, 0, false);
        } else {
            viewTransform.zoom(delta);
        }
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY) {
        render(gfx, mouseX, mouseY, true);
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY, boolean overView) {
        if (shouldEnsureValidated) {
            StructureManager.validateAllPieces();
            shouldEnsureValidated = false;
        }

        if (hoverBounds != null && hoverBounds.contains(mouseX, mouseY)) {
            hovered = this;
        } else if (hovered == this) {
            hovered = null;
        }

        viewTransform.run(mouseX, mouseY, overView);
        updatePreviewWorld();
        if (viewTransform.isStudio()) {
            viewTransform.placeCamera(boundingRadius);
            renderSetup.preRenderCamera(viewTransform.cameraView());
        } else {
            renderSetup.preRender((float) viewTransform.getYRotation(), (float) viewTransform.getXRotation(), boundingRadius, viewTransform.getViewTransform());
        }
        for (PositionedCyclingBlockRenderer part : parts) {
            if (!canRenderPart(part)) {
                continue;
            }
            GuiBlockRenderer next = part.part.next();
            next.render(gfx, mouseX, mouseY, viewTransform, previewWorld, worldGeneration);
        }
        var model = new Matrix4f(gfx.pose().last().pose()).mul(viewTransform.getModelTransform());
        var modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(model);
        lastMatrix.set(RenderSystem.getProjectionMatrix()).mul(modelView);
        rendered = true;
        if (overlay != null) {
            gfx.bufferSource().endBatch();
            gfx.pose().pushPose();
            gfx.pose().mulPose(viewTransform.getModelTransform());
            overlay.render(gfx.pose(), gfx.bufferSource(), modelView);
            gfx.pose().popPose();
        }
        gfx.bufferSource().endBatch();
        renderSetup.postRender();
        RenderUtil.resetViewport();
    }

    private void updatePreviewWorld() {
        boolean changed = previewWorldDirty;
        for (PositionedCyclingBlockRenderer part : parts) {
            int before = part.part.getIndex();
            part.part.tick();
            changed |= part.part.getIndex() != before;
        }
        if (!changed) {
            return;
        }
        previewWorldDirty = false;
        previewWorld.clear();
        for (PositionedCyclingBlockRenderer part : parts) {
            if (!ySliceProcessor.canProcess(part)) continue;
            GuiBlockRenderer block = part.part.next();
            previewWorld.put(part.pos, block.getState(), block.getBlockEntity());
        }
        worldGeneration++;
    }

    public void setStudio(boolean studio) {
        viewTransform.setStudio(studio);
    }

    public void fly(float forward, float strafe, float up, boolean level) {
        viewTransform.fly(forward, strafe, up, level);
    }

    @Nullable
    public double[] project(double x, double y, double z) {
        GuiPos viewport = renderSetup.getViewportPos();
        if (!rendered || viewport == null) {
            return null;
        }
        var clip = lastMatrix.transform(new Vector4f((float) x, (float) y, (float) z, 1));
        if (clip.w <= 0) {
            return null;
        }
        return new double[]{viewport.x() + (clip.x / clip.w + 1) / 2 * viewport.w(), viewport.y() + (1 - clip.y / clip.w) / 2 * viewport.h()};
    }

    public BlockAndTintGetter previewWorld() {
        return previewWorld;
    }

    @Nullable
    public double[] ray(double mouseX, double mouseY) {
        GuiPos viewport = renderSetup.getViewportPos();
        if (!rendered || viewport == null) {
            return null;
        }
        float nx = (float) ((mouseX - viewport.x()) / viewport.w() * 2 - 1);
        float ny = (float) (1 - (mouseY - viewport.y()) / viewport.h() * 2);
        var inverse = new Matrix4f(lastMatrix).invert();
        var near = inverse.transform(new Vector4f(nx, ny, -1, 1));
        var far = inverse.transform(new Vector4f(nx, ny, 0.99f, 1));
        var from = new Vec3(near.x / near.w, near.y / near.w, near.z / near.w);
        var dir = new Vec3(far.x / far.w, far.y / far.w, far.z / far.w).subtract(from).normalize();
        return new double[]{from.x, from.y, from.z, dir.x, dir.y, dir.z};
    }

    @Nullable
    public BlockHitResult pick(double mouseX, double mouseY) {
        GuiPos viewport = renderSetup.getViewportPos();
        if (!rendered || viewport == null) {
            return null;
        }
        float nx = (float) ((mouseX - viewport.x()) / viewport.w() * 2 - 1);
        float ny = (float) (1 - (mouseY - viewport.y()) / viewport.h() * 2);
        var inverse = new Matrix4f(lastMatrix).invert();
        var near = inverse.transform(new Vector4f(nx, ny, -1, 1));
        var far = inverse.transform(new Vector4f(nx, ny, 0.99f, 1));
        var from = new Vec3(near.x / near.w, near.y / near.w, near.z / near.w);
        var to = from.add(new Vec3(far.x / far.w, far.y / far.w, far.z / far.w).subtract(from).normalize().scale(100000));
        BlockHitResult best = null;
        double bestDistance = Double.MAX_VALUE;
        for (PositionedCyclingBlockRenderer part : parts) {
            if (!ySliceProcessor.canProcess(part)) {
                continue;
            }
            var shape = previewWorld.getBlockState(part.pos).getShape(previewWorld, part.pos);
            BlockHitResult hit = (shape.isEmpty() ? Shapes.block() : shape).clip(from, to, part.pos);
            if (hit != null && hit.getLocation().distanceToSqr(from) < bestDistance) {
                bestDistance = hit.getLocation().distanceToSqr(from);
                best = hit;
            }
        }
        return best;
    }

    public void setupViewState(BlueprintStructureViewState state) {
        setYSlice(state.isShouldSlice(), state.getYSlice());
    }

    public void setYSlice(boolean shouldSlice, int ySlice) {
        if (ySliceProcessor.isShouldSlice() != shouldSlice ||
                (shouldSlice && ySliceProcessor.getYSlice() != ySlice)) {
            previewWorldDirty = true;
        }
        ySliceProcessor.setShouldSlice(shouldSlice);
        ySliceProcessor.setYSlice(ySlice);
    }

    public void zoom(double scrollDelta) {
        viewTransform.zoom(scrollDelta);
    }

    private boolean canRenderPart(PositionedCyclingBlockRenderer part) {
        if (!ySliceProcessor.isShouldSlice() && enclosedParts.contains(part)) {
            return false;
        }
        return ySliceProcessor.canProcess(part);
    }

    public void resetTransforms() {
        viewTransform.reset();
    }

}
