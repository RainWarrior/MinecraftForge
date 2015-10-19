package net.minecraftforge.client.model.pipeline;

import net.minecraft.client.renderer.vertex.VertexFormat;

/**
 * Assumes that the data length is not less than e.getElementCount().
 * Also assumes that element index passed will increment from 0 to format.getElementCount() - 1.
 * Normal, Color and UV are assumed to be in 0-1 range.
 */
public interface IVertexConsumer
{
    void setVertexFormat(VertexFormat format);
    void put(int element, float... data);
}
