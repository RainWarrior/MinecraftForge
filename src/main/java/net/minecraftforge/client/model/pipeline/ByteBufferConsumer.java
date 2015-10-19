package net.minecraftforge.client.model.pipeline;

import java.nio.ByteBuffer;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class ByteBufferConsumer implements IVertexConsumer
{
    private final ByteBuffer buf;
    private VertexFormat format;

    public ByteBufferConsumer(ByteBuffer buf)
    {
        this.buf = buf;
    }

    public void setVertexFormat(VertexFormat format)
    {
        this.format = new VertexFormat(format);
    }

    public void put(int element, float... data)
    {
        VertexFormatElement e = format.getElement(element);
        if(e.getElementCount() > data.length) throw new IllegalArgumentException("not enough elements");
        for(int i = 0; i < e.getElementCount(); i++)
        {
            float f = data[i];
            switch(e.getType())
            {
            case BYTE:
                buf.put((byte)(f * (Byte.MAX_VALUE - 1)));
                break;
            case UBYTE:
                buf.put((byte)(f * ((1 << Byte.SIZE) - 1)));
                break;
            case SHORT:
                buf.putShort((short)(f * (Short.MAX_VALUE - 1)));
                break;
            case USHORT:
                buf.putShort((short)(f * ((1 << Short.SIZE) - 1)));
                break;
            case INT:
                buf.putInt((int)(f * (Integer.MAX_VALUE - 1)));
                break;
            case UINT:
                buf.putInt((int)(f * ((1L << Integer.SIZE) - 1)));
                break;
            case FLOAT:
                buf.putFloat(f);
                break;
            }
        }
    }
}