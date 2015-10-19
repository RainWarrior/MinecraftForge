package net.minecraftforge.client.model.pipeline;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

/**
 * Assumes VertexFormatElement is present in the WorlRenderer's vertex format.
 */
public class WorldRendererConsumer implements IVertexConsumer
{
    private final WorldRenderer renderer;
    private VertexFormat format;
    private float x, y, z;

    public WorldRendererConsumer(WorldRenderer renderer)
    {
        super();
        this.renderer = renderer;
        this.format = new VertexFormat(renderer.getVertexFormat());
    }

    public void setVertexFormat(VertexFormat format)
    {
        renderer.setVertexFormat(format);
        this.format = new VertexFormat(format);
    }

    public void put(int element, float... data)
    {
        VertexFormatElement e = format.getElement(element);
        if(e.getElementCount() > data.length) throw new IllegalArgumentException("not enough elements");
        switch(e.getUsage())
        {
        case POSITION:
            x = data[0];
            y = data[1];
            z = data[2];
            break;
        case COLOR:
            renderer.setColorRGBA_F(data[0], data[1], data[2], data[3]);
            break;
        case UV:
            if (e.getIndex() == 0)
            {
                renderer.setTextureUV(data[0], data[1]);
            }
            else if (e.getIndex() == 1)
            {
                // assume the full range is used
                int b = (int)(data[0] * 0xF0) << 16 | (int)(data[1] * 0xF0);
                renderer.setBrightness(b);
            }
            else throw new IllegalArgumentException("WorldRenderer only has 2 texture units");
            break;
        case NORMAL:
            renderer.setNormal(data[0], data[1], data[2]);
            break;
        default:
            // WorldRenderer doesn't know about anything else
            break;
        }
        if(element == format.getElementCount() - 1) // we're done
        {
            renderer.addVertex(x, y, z);
        }
    }
}