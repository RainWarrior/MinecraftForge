package net.minecraftforge.client.model.pipeline;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;

public class LightUtil
{
    public static float diffuseLight(float x, float y, float z)
    {
        float s2 = (float)Math.pow(2, .5);
        float y1 = y + 3 - 2 * s2;
        return (x * x * 0.6f + (y1 * y1 * (3 + 2 * s2)) / 8 + z * z * 0.8f);
    }

    public static EnumFacing toSide(float x, float y, float z)
    {
        if(Math.abs(x) > Math.abs(y))
        {
            if(Math.abs(x) > Math.abs(z))
            {
                if(x < 0) return EnumFacing.WEST;
                return EnumFacing.EAST;
            }
            else
            {
                if(z < 0) return EnumFacing.NORTH;
                return EnumFacing.SOUTH;
            }
        }
        else
        {
            if(Math.abs(y) > Math.abs(z))
            {
                if(y < 0) return EnumFacing.DOWN;
                return EnumFacing.UP;
            }
            else
            {
                if(z < 0) return EnumFacing.NORTH;
                return EnumFacing.SOUTH;
            }
        }
    }

    public static void putBakedQuad(IVertexConsumer consumer, BakedQuad quad, VertexFormat format)
    {
        float[] data = new float[4];
        for(int v = 0; v < 4; v++)
        {
            for(int i = 0; i < format.getElementCount(); i++)
            {
                VertexFormatElement e = format.getElement(i);
                for(int j = 0; j < 4; j++)
                {
                    if(j < e.getElementCount())
                    {
                        int pos = v * format.getNextOffset() + e.getOffset() + e.getType().getSize() * j;
                        int index = pos >> 2;
                        int offset = pos & 3;
                        int bits = quad.getVertexData()[index];
                        bits = bits >>> (offset * 8);
                        if((pos + e.getElementCount() - 1) / 4 != index)
                        {
                            bits |= quad.getVertexData()[index + 1] << ((4 - offset) * 8);
                        }
                        int mask = (256 << (8 * (e.getType().getSize() - 1))) - 1;
                        bits &= mask;
                        if(e.getType() == VertexFormatElement.EnumType.FLOAT)
                        {
                            data[j] = Float.intBitsToFloat(bits);
                        }
                        else
                        {
                            data[j] = (float)bits / mask;
                        }
                    }
                    else
                    {
                        data[j] = 0;
                    }
                }
                consumer.put(i, data);
            }
        }
    }
}
