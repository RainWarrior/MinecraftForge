package net.minecraftforge.client.model.pipeline;

import net.minecraft.client.renderer.vertex.VertexFormat;

import com.google.common.base.Objects;

public abstract class QuadGatheringTransformer implements IVertexConsumer
{
    protected IVertexConsumer parent;
    protected VertexFormat format;
    protected int vertices = 0;

    protected float[][][] quadData = null;

    @Override
    public void setVertexFormat(VertexFormat format)
    {
        if(Objects.equal(this.format, format)) return;
        parent.setVertexFormat(format);
        this.format = new VertexFormat(format);
        quadData = new float [format.getElementCount()][4][4];
    }

    @Override
    public void put(int element, float... data)
    {
        System.arraycopy(data, 0, quadData[element][vertices], 0, data.length);
        if(element == format.getElementCount() - 1) vertices++;
        if(vertices == 4)
        {
            vertices = 0;
            processQuad();
        }
    }

    protected abstract void processQuad();
}
