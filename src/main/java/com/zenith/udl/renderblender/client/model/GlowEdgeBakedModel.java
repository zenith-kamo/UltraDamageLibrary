package com.zenith.udl.renderblender.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zenith.udl.renderblender.api.client.model.PerspectiveModelState;
import com.zenith.udl.renderblender.api.client.model.bakedmodels.WrappedItemModel;
import com.zenith.udl.renderblender.api.client.render.item.IItemRenderer;
import com.zenith.udl.renderblender.api.client.util.TransformUtils;
import com.zenith.udl.renderblender.api.iface.IBowTransform;
import com.zenith.udl.renderblender.api.iface.IToolTransform;
import com.zenith.udl.renderblender.client.shader.RBRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GlowEdgeBakedModel extends WrappedItemModel implements IItemRenderer {
    private final BakedModel parentModel;
    private final int glowColor;
    private final float glowWidth;
    private final float glowOffset;

    public GlowEdgeBakedModel(BakedModel wrapped, int glowColor, float glowWidth, float glowOffset) {
        super(wrapped);
        this.parentModel = wrapped;
        this.glowColor = glowColor;
        this.glowWidth = Math.max(0.5f, glowWidth);
        this.glowOffset = glowOffset;
    }

    public GlowEdgeBakedModel(BakedModel wrapped, int glowColor, float glowWidth) {
        this(wrapped, glowColor, glowWidth, 0.02f);
    }

    @Override
    public @Nullable PerspectiveModelState getModelState() {
        return (PerspectiveModelState) this.parentState;
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pStack, MultiBufferSource source, int light, int overlay) {
        if (stack.getItem() instanceof IToolTransform) {
            this.parentState = TransformUtils.DEFAULT_TOOL;
        } else if (stack.getItem() instanceof IBowTransform) {
            this.parentState = TransformUtils.DEFAULT_BOW;
        } else{
            this.parentState = TransformUtils.DEFAULT_ITEM;
        }
        this.renderWrapped(stack, pStack, source, light, overlay, true);

        renderGlowEdge(stack, ctx, pStack, source, light, overlay);
    }

    private void renderGlowEdge(ItemStack stack, ItemDisplayContext ctx,
                                PoseStack pStack, MultiBufferSource source,
                                int light, int overlay) {

        VertexConsumer consumer =
                source.getBuffer(RBRenderTypes.GLOWING_OUTLINE);

        PoseStack tempStack = new PoseStack();
        tempStack.mulPoseMatrix(pStack.last().pose());

        tempStack.translate(0.0f, glowOffset, 0.0f);

        float expansionFactor = 1.0f + (glowWidth * 0.05f);
        tempStack.scale(expansionFactor, expansionFactor, expansionFactor);

        float r = ((glowColor >> 16) & 0xFF) / 255.0f;
        float g = ((glowColor >> 8) & 0xFF) / 255.0f;
        float b = (glowColor & 0xFF) / 255.0f;

        RandomSource random = RandomSource.create(42);

        for (Direction direction : Direction.values()) {

            random.setSeed(42);

            List<BakedQuad> quads =
                    parentModel.getQuads(null, direction, random);

            for (BakedQuad quad : quads) {

                consumer.putBulkData(
                        tempStack.last(),
                        quad,
                        r,
                        g,
                        b,
                        1.0F,
                        light,
                        overlay,
                        false
                );
            }
        }

        random.setSeed(42);

        List<BakedQuad> quads =
                parentModel.getQuads(null, null, random);

        for (BakedQuad quad : quads) {

            consumer.putBulkData(
                    tempStack.last(),
                    quad,
                    r,
                    g,
                    b,
                    1.0F,
                    light,
                    overlay,
                    false
            );
        }
    }
}
