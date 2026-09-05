package com.zenith.udl.renderblender.api.client.render.buffer;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class ColoredVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float red, green, blue;

    public ColoredVertexConsumer(VertexConsumer delegate, float red, float green, float blue) {
        this.delegate = delegate;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        return delegate.vertex(x, y, z);
    }

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        // 使用自定义颜色替换原始颜色
        return delegate.color((int)(red * 255), (int)(green * 255), (int)(blue * 255), a);
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        return delegate.uv(u, v);
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return delegate.overlayCoords(u, v);
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        return delegate.uv2(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return delegate.normal(x, y, z);
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int i, int i1, int i2, int i3) {

    }

    @Override
    public void unsetDefaultColor() {

    }
}
