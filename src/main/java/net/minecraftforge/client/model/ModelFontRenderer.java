package net.minecraftforge.client.model;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;

public class ModelFontRenderer extends FontRenderer {

    private float r, g, b, a;

    public ModelFontRenderer(GameSettings settings, ResourceLocation font, TextureManager manager, boolean isUnicode)
    {
        super(settings, font, manager, isUnicode);
    }

    @Override
    protected float renderDefaultChar(int p_78266_1_, boolean p_78266_2_)
    {
        // TODO
        return 0;
    }

    @Override
    protected float renderUnicodeChar(char p_78277_1_, boolean p_78277_2_)
    {
        // TODO
        return 0;
    }

    @Override
    protected void doDraw(float f)
    {
        // TODO
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
}
