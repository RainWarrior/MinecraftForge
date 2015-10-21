package net.minecraftforge.client.model.pipeline;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IColoredBakedQuad;

public class LightUtil
{
    public static float diffuseLight(float x, float y, float z)
    {
        float s2 = (float)Math.pow(2, .5);
        float y1 = y + 3 - 2 * s2;
        return (x * x * 0.6f + (y1 * y1 * (3 + 2 * s2)) / 8 + z * z * 0.8f);
    }

    public static float diffuseLight(EnumFacing side)
    {
        switch(side)
        {
        case DOWN:
            return .5f;
        case UP:
            return 1f;
        case NORTH:
        case SOUTH:
            return .8f;
        default:
            return .6f;
        }
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
        consumer.setQuadOrientation(quad.getFace());
        if(quad.hasTintIndex())
        {
            consumer.setQuadTint(quad.getTintIndex());
        }
        if(quad instanceof IColoredBakedQuad)
        {
            consumer.setQuadColored();
        }
        for(int v = 0; v < 4; v++)
        {
            for(int e = 0; e < format.getElementCount(); e++)
            {
                float[] data = new float[4];
                unpack(quad.getVertexData(), data, format, v, e);
                consumer.put(e, data);
            }
        }
    }

    public static void unpack(int[] from, float[] to, VertexFormat format, int v, int e)
    {
        VertexFormatElement element = format.getElement(e);
        for(int i = 0; i < 4; i++)
        {
            if(i < element.getElementCount())
            {
                int pos = v * format.getNextOffset() + element.getOffset() + element.getType().getSize() * i;
                int index = pos >> 2;
                int offset = pos & 3;
                int bits = from[index];
                bits = bits >>> (offset * 8);
                if((pos + element.getElementCount() - 1) / 4 != index)
                {
                    bits |= from[index + 1] << ((4 - offset) * 8);
                }
                int mask = (256 << (8 * (element.getType().getSize() - 1))) - 1;
                bits &= mask;
                if(element.getType() == VertexFormatElement.EnumType.FLOAT)
                {
                    to[i] = Float.intBitsToFloat(bits);
                }
                else
                {
                    to[i] = (float)bits / mask;
                }
            }
            else
            {
                to[i] = 0;
            }
        }
    }

    public static void pack(float[] from, int[] to, VertexFormat format, int v, int e)
    {
        VertexFormatElement element = format.getElement(e);
        for(int i = 0; i < 4; i++)
        {
            if(i < element.getElementCount())
            {
                int pos = v * format.getNextOffset() + element.getOffset() + element.getType().getSize() * i;
                int index = pos >> 2;
                int offset = pos & 3;
                int bits;
                int mask = (256 << (8 * (element.getType().getSize() - 1))) - 1;
                if(element.getType() == VertexFormatElement.EnumType.FLOAT)
                {
                    bits = Float.floatToRawIntBits(from[i]);
                }
                else
                {
                    bits = (int)(from[i] * mask);
                }
                to[index] &= ~(mask << (offset * 8));
                to[index] |= (((bits & mask) << (offset * 8)));
                // TODO handle overflow into to[index + 1]
            }
        }
    }
}
