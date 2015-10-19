package net.minecraftforge.client.model.pipeline;


public class VertexLighterSmoothAo extends VertexLighterFlat
{
    @Override
    protected void updateLightmap(float[] normal, float[] lightmap, float x, float y, float z)
    {
        lightmap[0] = calcLightmap(blockInfo.getSkyLight(), normal, x, y, z);
        lightmap[1] = calcLightmap(blockInfo.getBlockLight(), normal, x, y, z);
    }

    @Override
    protected void updateColor(float[] normal, float[] color, float x, float y, float z, float tint, int multiplier)
    {
        if(tint != -1)
        {
            color[0] *= (float)(multiplier >> 0x10 & 0xFF) / 0xFF;
            color[1] *= (float)(multiplier >> 0x8 & 0xFF) / 0xFF;
            color[2] *= (float)(multiplier & 0xFF) / 0xFF;
        }
        float a = getAo(x, y, z);
        color[0] *= a;
        color[1] *= a;
        color[2] *= a;
    }

    protected float calcLightmap(float[][][] light, float[] normal, float x, float y, float z)
    {
        float ox = x, oy = y, oz = z;

        int sx = x < 0 ? 1 : 2;
        int sy = y < 0 ? 1 : 2;
        int sz = z < 0 ? 1 : 2;

        if(x < 0) x++;
        if(y < 0) y++;
        if(z < 0) z++;

        float l = 0;
        l += blockInfo.getLight(light, normal, ox, oy, oz, sx - 1, sy - 1, sz - 1) * (1 - x) * (1 - y) * (1 - z);
        l += blockInfo.getLight(light, normal, ox, oy, oz, sx - 1, sy - 1, sz - 0) * (1 - x) * (1 - y) * (0 + z);
        l += blockInfo.getLight(light, normal, ox, oy, oz, sx - 1, sy - 0, sz - 1) * (1 - x) * (0 + y) * (1 - z);
        l += blockInfo.getLight(light, normal, ox, oy, oz, sx - 1, sy - 0, sz - 0) * (1 - x) * (0 + y) * (0 + z);
        l += blockInfo.getLight(light, normal, ox, oy, oz, sx - 0, sy - 1, sz - 1) * (0 + x) * (1 - y) * (1 - z);
        l += blockInfo.getLight(light, normal, ox, oy, oz, sx - 0, sy - 1, sz - 0) * (0 + x) * (1 - y) * (0 + z);
        l += blockInfo.getLight(light, normal, ox, oy, oz, sx - 0, sy - 0, sz - 1) * (0 + x) * (0 + y) * (1 - z);
        l += blockInfo.getLight(light, normal, ox, oy, oz, sx - 0, sy - 0, sz - 0) * (0 + x) * (0 + y) * (0 + z);

        if(l > 1) l = 1;
        if(l < 0) l = 0;

        return l;
    }

    protected float getAo(float x, float y, float z)
    {
        int sx = x < 0 ? 1 : 2;
        int sy = y < 0 ? 1 : 2;
        int sz = z < 0 ? 1 : 2;

        if(x < 0) x++;
        if(y < 0) y++;
        if(z < 0) z++;

        float a = 0;
        a += blockInfo.getAo()[sx - 1][sy - 1][sz - 1] * (1 - x) * (1 - y) * (1 - z);
        a += blockInfo.getAo()[sx - 1][sy - 1][sz - 0] * (1 - x) * (1 - y) * (0 + z);
        a += blockInfo.getAo()[sx - 1][sy - 0][sz - 1] * (1 - x) * (0 + y) * (1 - z);
        a += blockInfo.getAo()[sx - 1][sy - 0][sz - 0] * (1 - x) * (0 + y) * (0 + z);
        a += blockInfo.getAo()[sx - 0][sy - 1][sz - 1] * (0 + x) * (1 - y) * (1 - z);
        a += blockInfo.getAo()[sx - 0][sy - 1][sz - 0] * (0 + x) * (1 - y) * (0 + z);
        a += blockInfo.getAo()[sx - 0][sy - 0][sz - 1] * (0 + x) * (0 + y) * (1 - z);
        a += blockInfo.getAo()[sx - 0][sy - 0][sz - 0] * (0 + x) * (0 + y) * (0 + z);

        return a;
    }

    @Override
    public void updateBlockInfo()
    {
        super.updateBlockInfo();
        blockInfo.updateLightMatrix();
    }
}
