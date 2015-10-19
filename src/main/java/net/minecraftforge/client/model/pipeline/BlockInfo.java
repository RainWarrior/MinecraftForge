package net.minecraftforge.client.model.pipeline;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;

public class BlockInfo
{
    private IBlockAccess world;
    private Block block;
    private BlockPos blockPos;

    private final boolean[][][] translucent = new boolean[3][3][3];
    private final float[][][] skyLight = new float[3][3][3];
    private final float[][][] blockLight = new float[3][3][3];
    private final float[][][] ao = new float[3][3][3];

    private float shx = 0, shy = 0, shz = 0;

    private int cachedTint = -1;
    private int cachedMultiplier = -1;

    public int getColorMultiplier(int tint)
    {
        if(cachedTint == tint) return cachedMultiplier;
        return block.colorMultiplier(world, blockPos, tint);
    }

    public void updateShift()
    {
        shx = shy = shz = 0;
        long rand = 0;
        // FIXME
        switch(block.getOffsetType())
        {
        case XYZ:
            rand = MathHelper.getPositionRandom(blockPos);
            shy = ((float)((rand >> 20) & 0xF) / 0xF - 1) * .2f;
        case XZ:
            shx = ((float)((rand >> 16) & 0xF) / 0xF - .5f) * .5f;
            shz = ((float)((rand >> 24) & 0xF) / 0xF - .5f) * .5f;
        default:
        }
    }

    public float getLight(float[][][] light, float[] normal, float cx, float cy, float cz, int ix, int iy, int iz)
    {
        float x = cx + 1 - ix;
        float y = cy + 1 - iy;
        float z = cz + 1 - iz;

        /*float ax = Math.abs(x);
        float ay = Math.abs(y);
        float az = Math.abs(z);*/

        float ax = Math.abs(normal[0]);
        float ay = Math.abs(normal[1]);
        float az = Math.abs(normal[2]);

        boolean tr = translucent[ix][iy][iz];
        boolean tx = translucent[1][iy][iz];
        boolean ty = translucent[ix][1][iz];
        boolean tz = translucent[ix][iy][1];

        int nx = 1, ny = 1, nz = 1;
        if(ax > ay)
        {
            if(ax > az)
            {
                nx = ix;
                if(ty || tz)
                {
                    if(tr || !tz) nz = iz;
                    if(tr || !ty) ny = iy;
                }
            }
            else
            {
                nz = iz;
                if(tx || ty)
                {
                    if(tr || !ty) ny = iy;
                    if(tr || !tx) nx = ix;
                }
            }
        }
        else
        {
            if(ay > az)
            {
                ny = iy;
                if(tx || tz)
                {
                    if(tr || !tz) nz = iz;
                    if(tr || !tx) nx = ix;
                }
            }
            else
            {
                nz = iz;
                if(tx || ty)
                {
                    if(tr || !ty) ny = iy;
                    if(tr || !tx) nx = ix;
                }
            }
        }
        return light[nx][ny][nz];
    }

    public float getSkyLight(float[] normal, float cx, float cy, float cz, int ix, int iy, int iz)
    {
        return getLight(skyLight, normal, cx, cy, cz, ix, iy, iz);
    }

    public float getBlockLight(float[] normal, float cx, float cy, float cz, int ix, int iy, int iz)
    {
        return getLight(blockLight, normal, cx, cy, cz, ix, iy, iz);
    }

    public void setWorld(IBlockAccess world)
    {
        this.world = world;
        cachedTint = -1;
    }

    public void setBlock(Block block)
    {
        this.block = block;
        cachedTint = -1;
    }

    public void setBlockPos(BlockPos blockPos)
    {
        this.blockPos = blockPos;
        cachedTint = -1;
    }

    public void updateLightMatrix()
    {
        for(int x = 0; x <= 2; x++)
        {
            for(int y = 0; y <= 2; y++)
            {
                for(int z = 0; z <= 2; z++)
                {
                    BlockPos pos = blockPos.add(x - 1, y - 1, z - 1);
                    //translucent[x][y][z] = world.getBlockState(pos).getBlock().isTranslucent();
                    translucent[x][y][z] = world.getBlockState(pos).getBlock().getLightOpacity(world, pos) == 0;
                    int brightness = block.getMixedBrightnessForBlock(world, pos);
                    skyLight[x][y][z] =   (float)((brightness >> 0x14) & 0xF) / 0xF;
                    blockLight[x][y][z] = (float)((brightness >> 0x04) & 0xF) / 0xF;
                    ao[x][y][z] = world.getBlockState(pos).getBlock().getAmbientOcclusionLightValue();
                }
            }
        }
        if(!translucent[1][1][1])
        {
            for(EnumFacing side : EnumFacing.values())
            {
                int x = side.getFrontOffsetX() + 1;
                int y = side.getFrontOffsetX() + 1;
                int z = side.getFrontOffsetX() + 1;
                skyLight[1][1][1] = Math.max(skyLight[1][1][1], skyLight[x][y][z]);
                blockLight[1][1][1] = Math.max(blockLight[1][1][1], blockLight[x][y][z]);
                //ao[1][1][1] = Math.max(ao[1][1][1], ao[x][y][z]);
            }
            translucent[1][1][1] = true;
        }
        if(blockPos.getX() == 91 && blockPos.getY() == 78 && blockPos.getZ() == 20)
        for(int x = 0; x <= 2; x++)
        {
            for(int y = 0; y <= 2; y++)
            {
                for(int z = 0; z <= 2; z++)
                {
                    System.out.print("" + skyLight[x][y][z] + ":" + translucent[x][y][z] + " ");
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    public IBlockAccess getWorld()
    {
        return world;
    }

    public Block getBlock()
    {
        return block;
    }

    public BlockPos getBlockPos()
    {
        return blockPos;
    }

    public boolean[][][] getTranslucent()
    {
        return translucent;
    }

    public float[][][] getSkyLight()
    {
        return skyLight;
    }

    public float[][][] getBlockLight()
    {
        return blockLight;
    }

    public float[][][] getAo()
    {
        return ao;
    }

    public float getShx()
    {
        return shx;
    }

    public float getShy()
    {
        return shy;
    }

    public float getShz()
    {
        return shz;
    }

    public int getCachedTint()
    {
        return cachedTint;
    }

    public int getCachedMultiplier()
    {
        return cachedMultiplier;
    }
}
