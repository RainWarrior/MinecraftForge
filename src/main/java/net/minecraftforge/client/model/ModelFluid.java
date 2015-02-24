package net.minecraftforge.client.model;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import javax.vecmath.Vector4f;

import org.lwjgl.BufferUtils;

import scala.actors.threadpool.Arrays;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fluids.BlockFluidBase;

public class ModelFluid implements IModel
{
    private final Vector4f color;
    private final ResourceLocation still, flowing;

    public ModelFluid(Vector4f color, ResourceLocation still, ResourceLocation flowing)
    {
        this.color = color;
        this.still = still;
        this.flowing = flowing;
    }

    public Collection<ResourceLocation> getDependencies()
    {
        return Collections.emptySet();
    }

    public Collection<ResourceLocation> getTextures()
    {
        return ImmutableSet.of(still, flowing);
    }

    public IFlexibleBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        return new BakedFluid(state.apply(this), format, color, bakedTextureGetter.apply(still), bakedTextureGetter.apply(flowing));
    }

    public IModelState getDefaultState()
    {
        return ModelRotation.X0_Y0;
    }

    public static enum FluidLoader implements ICustomModelLoader
    {
        instance;

        private FluidLoader()
        {
            ModelLoaderRegistry.registerLoader(this);
        }

        public void onResourceManagerReload(IResourceManager resourceManager)
        {
        }

        public boolean accepts(ResourceLocation modelLocation)
        {
            return modelLocation.getResourceDomain().equals("forgefluid");
        }

        public IModel loadModel(ResourceLocation modelLocation)
        {
            System.out.println("loadModel " + modelLocation);
            String data = modelLocation.getResourcePath();
            if(data.startsWith("models/item/")) data = data.substring("models/item/".length());
            else if(data.startsWith("models/block/")) data = data.substring("models/block/".length());
            System.out.println(data);
            String[] parts = data.split("\\|");
            System.out.println(Arrays.toString(parts));
            float r = Integer.parseInt(parts[0]) / 255f;
            float g = Integer.parseInt(parts[1]) / 255f;
            float b = Integer.parseInt(parts[2]) / 255f;
            float a = Integer.parseInt(parts[3]) / 255f;
            ResourceLocation still = new ResourceLocation(parts[4]);
            ResourceLocation floating = new ResourceLocation(parts[5]);
            return new ModelFluid(new Vector4f(r, g, b, a), still, floating);
        }
        
        public static ResourceLocation location(int r, int g, int b, int a, ResourceLocation still, ResourceLocation floating)
        {
            return new ResourceLocation("forgefluid", "" + r + "|" + g + "|" + b + "|" + a + "|" + still + "|" + floating);
        }
    }

    public static class BakedFluid implements IFlexibleBakedModel, ISmartBlockModel
    {
        private final TRSRTransformation transformation;
        private final VertexFormat format;
        private final Vector4f color;
        private final TextureAtlasSprite still, flowing;
        private final Optional<IExtendedBlockState> state;
        private final EnumMap<EnumFacing, List<BakedQuad>> faceQuads;
        private final Optional<BakedQuad> topQuad;

        public BakedFluid(TRSRTransformation transformation, VertexFormat format, Vector4f color, TextureAtlasSprite still, TextureAtlasSprite flowing)
        {
            this(transformation, format, color, still, flowing, Optional.<IExtendedBlockState>absent());
        }

        public BakedFluid(TRSRTransformation transformation, VertexFormat format, Vector4f color, TextureAtlasSprite still, TextureAtlasSprite flowing, Optional<IExtendedBlockState> stateOption)
        {
            this.transformation = transformation;
            this.format = format;
            this.color = color;
            this.still = still;
            this.flowing = flowing;
            this.state = stateOption;

            faceQuads = Maps.newEnumMap(EnumFacing.class);
            for(EnumFacing side : EnumFacing.values())
            {
                faceQuads.put(side, ImmutableList.<BakedQuad>of());
            }
            if(!state.isPresent())
            {
                topQuad = Optional.absent();
            }
            else
            {
                IExtendedBlockState state = this.state.get();
                ByteBuffer buf = BufferUtils.createByteBuffer(4 * format.getNextOffset());

                float[] levelCorners = new float[4];
                for(int i = 0; i < 4; i++)
                {
                    levelCorners[i] = state.getValue(BlockFluidBase.LEVEL_CORNERS[i]);
                }

                if((Boolean)state.getValue(BlockFluidBase.RENDER_SIDE.get(EnumFacing.UP)))
                {
                    buf.clear();
                    putVertex(buf, EnumFacing.UP, 0, levelCorners[0], 0, still.getInterpolatedU(0), still.getInterpolatedV(0));
                    putVertex(buf, EnumFacing.UP, 0, levelCorners[1], 1, still.getInterpolatedU(0), still.getInterpolatedV(16));
                    putVertex(buf, EnumFacing.UP, 1, levelCorners[3], 1, still.getInterpolatedU(16), still.getInterpolatedV(16));
                    putVertex(buf, EnumFacing.UP, 1, levelCorners[2], 0, still.getInterpolatedU(16), still.getInterpolatedV(0));
                    buf.flip();
                    int[] data = new int[4 * format.getNextOffset() / 4];
                    buf.asIntBuffer().get(data);
                    topQuad = Optional.<BakedQuad>of(new IColoredBakedQuad.ColoredBakedQuad(data, -1, EnumFacing.UP));
                }
                else
                {
                    topQuad = Optional.absent();
                }
            }
        }
        
        private void put(ByteBuffer buf, VertexFormatElement e, Float... fs)
        {
            Attributes.put(buf, e, true, 0f, fs);
        }
        private void putVertex(ByteBuffer buf, EnumFacing side, float x, float y, float z, float u, float v)
        {
            for(VertexFormatElement e : (List<VertexFormatElement>)format.getElements())
            {
                // TODO transformation
                switch(e.getUsage())
                {
                case POSITION:
                    put(buf, e, x, y, z, 1f);
                    break;
                case COLOR:
                    put(buf, e, color.x, color.y, color.z, color.w);
                    break;
                case UV:
                    put(buf, e, u, v, 0f, 1f);
                    break;
                case NORMAL:
                    put(buf, e, (float)side.getFrontOffsetX(), (float)side.getFrontOffsetX(), (float)side.getFrontOffsetX(), 0f);
                    break;
                default:
                    put(buf, e);
                    break;
                }
            }
        }

        public boolean isAmbientOcclusion()
        {
            return false; // FIXME
        }

        public boolean isGui3d()
        {
            return false;
        }

        public boolean isBuiltInRenderer()
        {
            return false;
        }

        public TextureAtlasSprite getTexture()
        {
            return still;
        }

        public ItemCameraTransforms getItemCameraTransforms()
        {
            return ItemCameraTransforms.DEFAULT;
        }

        public List<BakedQuad> getFaceQuads(EnumFacing side)
        {
            return faceQuads.get(side);
        }

        public List<BakedQuad> getGeneralQuads()
        {
            return ImmutableList.copyOf(topQuad.asSet());
        }

        public VertexFormat getFormat()
        {
            return format;
        }

        public IBakedModel handleBlockState(IBlockState state)
        {
            return new BakedFluid(transformation, format, color, still, flowing, Optional.of((IExtendedBlockState)state));
        }
    }
}
