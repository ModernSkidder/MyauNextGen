package laoqi123.event.impl;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class Render3DEvent implements Event {
    private final MatrixStack matrices;
    private final VertexConsumerProvider consumers;
    private final float partialTicks;

    public Render3DEvent(MatrixStack matrices, VertexConsumerProvider consumers, float partialTicks) {
        this.matrices = matrices;
        this.consumers = consumers;
        this.partialTicks = partialTicks;
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }

    public VertexConsumerProvider getConsumers() {
        return this.consumers;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }
}
