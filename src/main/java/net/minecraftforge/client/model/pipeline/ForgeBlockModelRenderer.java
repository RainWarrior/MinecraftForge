package net.minecraftforge.client.model.pipeline;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

public class ForgeBlockModelRenderer extends BlockModelRenderer
{
    private final ThreadLocal<VertexLighterFlat> lighterFlat = new ThreadLocal<VertexLighterFlat>()
    {
        @Override
        protected VertexLighterFlat initialValue()
        {
            return new VertexLighterFlat();
        }
    };

    private final ThreadLocal<VertexLighterSmoothAo> lighterSmooth = new ThreadLocal<VertexLighterSmoothAo>()
    {
        @Override
        protected VertexLighterSmoothAo initialValue()
        {
            return new VertexLighterSmoothAo();
        }
    };

    private final ThreadLocal<WorldRenderer> lastRendererFlat = new ThreadLocal<WorldRenderer>();
    private final ThreadLocal<WorldRenderer> lastRendererSmooth = new ThreadLocal<WorldRenderer>();

    @Override
    public boolean renderModelStandard(IBlockAccess world, IBakedModel model, Block block, BlockPos pos, WorldRenderer wr, boolean checkSides)
    {
        return render(lighterFlat.get(), lastRendererFlat, world, model, block, pos, wr, checkSides);
    }

    @Override
    public boolean renderModelAmbientOcclusion(IBlockAccess world, IBakedModel model, Block block, BlockPos pos, WorldRenderer wr, boolean checkSides)
    {
        return render(lighterSmooth.get(), lastRendererSmooth, world, model, block, pos, wr, checkSides);
    }

    public static boolean render(VertexLighterFlat lighter, ThreadLocal<WorldRenderer> lastRenderer, IBlockAccess world, IBakedModel model, Block block, BlockPos pos, WorldRenderer wr, boolean checkSides)
    {
        lighter.setWorld(world);
        lighter.setBlock(block);
        lighter.setBlockPos(pos);
        if(wr != lastRenderer.get())
        {
            lighter.setParent(new WorldRendererConsumer(wr));
            lighter.setVertexFormat(wr.getVertexFormat());
            lastRenderer.set(wr);
        }
        boolean empty = true;
        List<BakedQuad> quads = model.getGeneralQuads();
        if(!quads.isEmpty())
        {
            lighter.updateBlockInfo();
            empty = false;
            for(BakedQuad quad : quads)
            {
                quad.pipe(lighter, wr.getVertexFormat());
            }
        }
        for(EnumFacing side : EnumFacing.values())
        {
            quads = model.getFaceQuads(side);
            if(!quads.isEmpty())
            {
                if(!checkSides || block.shouldSideBeRendered(world, pos.offset(side), side))
                {
                    if(empty) lighter.updateBlockInfo();
                    empty = false;
                    for(BakedQuad quad : quads)
                    {
                        quad.pipe(lighter, wr.getVertexFormat());
                    }
                }
            }
        }
        return !empty;
    }
}
