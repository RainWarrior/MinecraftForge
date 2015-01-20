package net.minecraftforge.client.model;

import java.nio.ByteBuffer;
import java.util.List;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class ModelFontRenderer extends FontRenderer {

    private float r, g, b, a;
    private final Matrix4f matrix;
    private ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
    private final VertexFormat format;
    private final ByteBuffer buf;
    private final EnumFacing facing;

    private TextureAtlasSprite sprite;

    public ModelFontRenderer(GameSettings settings, ResourceLocation font, TextureManager manager, boolean isUnicode, Matrix4f matrix, VertexFormat format)
    {
        super(settings, font, manager, isUnicode);
        manager.bindTexture(TextureMap.locationBlocksTexture);
        this.matrix = matrix;
        this.format = format;
        buf = BufferUtils.createByteBuffer(4 * format.getNextOffset());
        Vector4f f = new Vector4f(0, 0, 1, 1);
        matrix.transform(f);
        facing = EnumFacing.getFacingFromVector(f.x, f.y, f.z);
    }

    @Override
    protected float renderDefaultChar(int pos, boolean italic)
    {
        float x = (pos % 16) / 16f;
        float y = (pos / 16) / 16f;
        float sh = italic ? 1f : 0f;
        float w = charWidth[pos] - 1.01f;
        float h = FONT_HEIGHT - 1.01f;
        float wt = w  / 128f;
        float ht = h  / 128f;

        buf.clear();
        Vector4f v = new Vector4f(0, 0, 0, 1);

        v.x = posX + sh;
        v.y = posY;
        v.z = 0;
        v.w = 1;
        matrix.transform(v);
        addVertex(v, x, y);

        v.x = posX + sh;
        v.y = posY + h;
        v.z = 0;
        v.w = 1;
        matrix.transform(v);
        addVertex(v, x, y + ht);

        v.x = posX + w + sh;
        v.y = posY + h;
        v.z = 0;
        v.w = 1;
        matrix.transform(v);
        addVertex(v, x + wt, y + ht);

        v.x = posX + w + sh;
        v.y = posY;
        v.z = 0;
        v.w = 1;
        matrix.transform(v);
        addVertex(v, x + wt, y);

        buf.flip();
        int[] data = new int[format.getNextOffset()];
        buf.asIntBuffer().get(data);
        builder.add(new BakedQuad(data, -1, facing));
        return charWidth[pos];
    }

    private void put(VertexFormatElement e, Number... ns)
    {
        Attributes.put(buf, e, true, 0, ns);
    }

    private void addVertex(Vector4f vec, float u, float v)
    {
        for(VertexFormatElement e : (List<VertexFormatElement>)format.getElements())
        {
            switch(e.getUsage())
            {
            case POSITION:
                put(e, vec.x, vec.y, vec.z, vec.w);
                break;
            case UV:
                put(e, sprite.getInterpolatedU(u * 16), sprite.getInterpolatedV(v * 16), 0, 1);
                break;
            case COLOR:
                put(e, r, g, b, a);
                break;
            case NORMAL:
                put(e, 0, 0, 1, 1);
                break;
            case PADDING:
                put(e);
                break;
            default:
                break;
            }
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager)
    {
        super.onResourceManagerReload(resourceManager);
        String p = locationFontTexture.getResourcePath();
        if(p.startsWith("textures/")) p = p.substring("textures/".length(), p.length());
        if(p.endsWith(".png")) p = p.substring(0, p.length() - ".png".length());
        String f = locationFontTexture.getResourceDomain() + ":" + p;
        sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(f);
    }

    @Override
    protected float renderUnicodeChar(char p_78277_1_, boolean p_78277_2_)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDraw(float shift)
    {
        posX += (int)shift;
    }

    @Override
    protected void setColor(float r, float g, float b, float a)
    {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override public void enableAlpha()
    {
    }

    public ImmutableList<BakedQuad> build()
    {
        ImmutableList<BakedQuad> ret = builder.build();
        builder = ImmutableList.builder();
        return ret;
    }
}
