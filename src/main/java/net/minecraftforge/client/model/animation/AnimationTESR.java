package net.minecraftforge.client.model.animation;

import java.util.concurrent.TimeUnit;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.common.property.IExtendedBlockState;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Generic TileEntitySpecialRenderer that works with the Forge model system and animations.
 */
public class AnimationTESR<T extends TileEntity & IAnimationProvider> extends TileEntitySpecialRenderer<T>
{
    protected static BlockRendererDispatcher blockRenderer;

    protected static final LoadingCache<Pair<IExtendedBlockState, IModelState>, IBakedModel> modelCache = CacheBuilder.newBuilder().maximumSize(10).expireAfterWrite(100, TimeUnit.MILLISECONDS).build(new CacheLoader<Pair<IExtendedBlockState, IModelState>, IBakedModel>()
    {
        public IBakedModel load(Pair<IExtendedBlockState, IModelState> key) throws Exception
        {
            IBakedModel model = blockRenderer.getBlockModelShapes().getModelForState(key.getLeft().getClean());
            if(model instanceof ISmartBlockModel)
            {
                model = ((ISmartBlockModel)model).handleBlockState(key.getLeft().withProperty(Animation.AnimationProperty, key.getRight()));
            }
            return model;
        }
    });

    protected static IBakedModel getModel(IExtendedBlockState state, IModelState modelState)
    {
        return modelCache.getUnchecked(Pair.of(state, modelState));
    }

    public void renderTileEntityAt(T te, double x, double y, double z, float partialTick, int breakStage)
    {
        if(blockRenderer == null) blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
        BlockPos pos = te.getPos();
        IBlockAccess world = MinecraftForgeClient.getRegionRenderCache(te.getWorld(), pos);
        IBlockState state = world.getBlockState(pos);
        if(state.getPropertyNames().contains(Animation.StaticProperty))
        {
            state = state.withProperty(Animation.StaticProperty, false);
        }
        if(state instanceof IExtendedBlockState)
        {
            IExtendedBlockState exState = (IExtendedBlockState)state;
            if(exState.getUnlistedNames().contains(Animation.AnimationProperty))
            {
                IBakedModel model = getModel(exState, te.asm().apply(Animation.getWorldTime(getWorld(), partialTick)));

                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                this.bindTexture(TextureMap.locationBlocksTexture);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.enableBlend();
                GlStateManager.disableCull();

                if (Minecraft.isAmbientOcclusionEnabled())
                {
                    GlStateManager.shadeModel(GL11.GL_SMOOTH);
                }
                else
                {
                    GlStateManager.shadeModel(GL11.GL_FLAT);
                }

                worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                worldrenderer.setTranslation(x - pos.getX(), y - pos.getY(), z - pos.getZ());

                blockRenderer.getBlockModelRenderer().renderModel(world, model, state, pos, worldrenderer, false);

                worldrenderer.setTranslation(0, 0, 0);
                tessellator.draw();

                RenderHelper.enableStandardItemLighting();
            }
        }
    }
}