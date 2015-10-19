package net.minecraftforge.client.model.pipeline;

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
}
