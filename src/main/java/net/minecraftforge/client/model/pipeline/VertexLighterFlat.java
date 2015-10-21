package net.minecraftforge.client.model.pipeline;

import javax.vecmath.Vector3f;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

public class VertexLighterFlat extends QuadGatheringTransformer
{
    protected final BlockInfo blockInfo = new BlockInfo();
    private int tint = -1;

    protected int posIndex = -1;
    protected int normalIndex = -1;
    protected int colorIndex = -1;
    protected int lightmapIndex = -1;

    @Override
    public void setVertexFormat(VertexFormat format)
    {
        super.setVertexFormat(format);
        for(int i = 0; i < format.getElementCount(); i++)
        {
            switch(format.getElement(i).getUsage())
            {
            case POSITION:
                posIndex = i;
                break;
            case NORMAL:
                normalIndex = i;
                break;
            case COLOR:
                colorIndex = i;
                break;
            case UV:
                if(format.getElement(i).getIndex() == 1)
                {
                    lightmapIndex = i;
                }
                break;
            default:
            }
        }
        if(posIndex == -1)
        {
            throw new IllegalArgumentException("vertex lighter needs format with position");
        }
        if(lightmapIndex == -1)
        {
            throw new IllegalArgumentException("vertex lighter needs format with lightmap");
        }
        if(colorIndex == -1)
        {
            throw new IllegalArgumentException("vertex lighter needs format with color");
        }
    }

    @Override
    protected void processQuad()
    {
        float[][] position = quadData[posIndex];
        float[][] normal = null;
        float[][] lightmap = quadData[lightmapIndex];
        float[][] color = quadData[colorIndex];

        if(normalIndex != -1)
        {
            normal = quadData[normalIndex];
        }
        else
        {
            normal = new float[4][];
            normal[0] = normal[1] = normal[2] = normal[3] = new float[4];
            Vector3f x = new Vector3f(position[0]);
            Vector3f y = new Vector3f(position[1]);
            Vector3f z = new Vector3f(position[2]);
            z.sub(y);
            y.sub(x);
            y.cross(y, z);
            y.normalize();
            normal[0][0] = y.x;
            normal[0][1] = y.y;
            normal[0][2] = y.z;
            normal[0][3] = 0;
        }

        int multiplier = -1;
        if(tint != -1)
        {
            multiplier = blockInfo.getColorMultiplier(tint);
        }

        for(int v = 0; v < 4; v++)
        {
            position[v][0] += blockInfo.getShx();
            position[v][1] += blockInfo.getShy();
            position[v][2] += blockInfo.getShz();

            float x = position[v][0] - .5f;
            float y = position[v][1] - .5f;
            float z = position[v][2] - .5f;

            //if(blockInfo.getBlock().isFullCube())
            {
                x += normal[v][0] * .5f;
                y += normal[v][1] * .5f;
                z += normal[v][2] * .5f;
            }

            updateLightmap(normal[v], lightmap[v], x, y, z);
            updateColor(normal[v], color[v], x, y, z, tint, multiplier);

            for(int e = 0; e < format.getElementCount(); e++)
            {
                switch(format.getElement(e).getUsage())
                {
                case POSITION:
                    float[] pos = new float[4];
                    System.arraycopy(position[v], 0, pos, 0, position[v].length);
                    pos[0] += blockInfo.getBlockPos().getX();
                    pos[1] += blockInfo.getBlockPos().getY();
                    pos[2] += blockInfo.getBlockPos().getZ();
                    parent.put(e, pos);
                    break;
                case NORMAL: if(normalIndex != -1)
                {
                    parent.put(e, normal[v]);
                    break;
                }
                case COLOR:
                    parent.put(e, color[v]);
                    break;
                case UV: if(format.getElement(e).getIndex() == 1)
                {
                    parent.put(e, lightmap[v]);
                    break;
                }
                default:
                    parent.put(e, quadData[e][v]);
                }
            }
        }
    }

    protected void updateLightmap(float[] normal, float[] lightmap, float x, float y, float z)
    {
        float e1 = .5f - 1e4f;
        float e2 = 1 - 1e4f;
        BlockPos pos = blockInfo.getBlockPos();

        if(y < -e1 && normal[1] < -e2) pos = pos.down();
        if(y >  e1 && normal[1] >  e2) pos = pos.up();
        if(z < -e1 && normal[2] < -e2) pos = pos.north();
        if(z >  e1 && normal[2] >  e2) pos = pos.south();
        if(x < -e1 && normal[0] < -e2) pos = pos.west();
        if(x >  e1 && normal[0] >  e2) pos = pos.east();

        int brightness = blockInfo.getBlock().getMixedBrightnessForBlock(blockInfo.getWorld(), pos);

        lightmap[0] = (float)((brightness >> 0x14) & 0xF) / 0xF;
        lightmap[1] = (float)((brightness >> 0x04) & 0xF) / 0xF;
    }

    protected void updateColor(float[] normal, float[] color, float x, float y, float z, float tint, int multiplier)
    {
        if(tint != -1)
        {
            color[0] *= (float)(multiplier >> 0x10 & 0xFF) / 0xFF;
            color[1] *= (float)(multiplier >> 0x8 & 0xFF) / 0xFF;
            color[2] *= (float)(multiplier & 0xFF) / 0xFF;
        }
    }

    public void setQuadTint(int tint)
    {
        this.tint = tint;
    }
    public void setQuadOrientation(EnumFacing orientation) {}
    public void setQuadCulled() {}
    public void setQuadColored() {}

    public void setParent(IVertexConsumer parent)
    {
        this.parent = parent;
    }

    public void setWorld(IBlockAccess world)
    {
        blockInfo.setWorld(world);
    }

    public void setBlock(Block block)
    {
        blockInfo.setBlock(block);
    }

    public void setBlockPos(BlockPos blockPos)
    {
        blockInfo.setBlockPos(blockPos);
    }

    public void updateBlockInfo()
    {
        blockInfo.updateShift();
    }
}
