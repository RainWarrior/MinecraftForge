package net.minecraftforge.client.model.pipeline;

import net.minecraft.client.renderer.vertex.VertexFormat;

public interface IVertexProducer
{
    /**
     * @param consumer consumer to receive the vertex data this producer can provide
     * @param format the format that consumer expects; consumer.setFormat call shouldn't be necessary
     */
    void pipe(IVertexConsumer consumer, VertexFormat format);
}
