package net.minecraftforge.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.Attributes;
import net.minecraftforge.client.model.ModelFontRenderer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = ModelFontRendererDebug.MODID, version = ModelFontRendererDebug.VERSION)
public class ModelFontRendererDebug
{
    public static final String MODID = "ForgeDebugModelFontRenderer";
    public static final String VERSION = "1.0";

    private static String blockName = MODID.toLowerCase() + ":" + CustomBlock.name;
    private static final ResourceLocation font = new ResourceLocation("minecraft", "textures/font/ascii.png");
    private static final ResourceLocation font2 = new ResourceLocation("minecraft", "font/ascii");

    @SidedProxy(serverSide = "net.minecraftforge.debug.ModelFontRendererDebug$CommonProxy", clientSide = "net.minecraftforge.debug.ModelFontRendererDebug$ClientProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void init(FMLInitializationEvent event) { proxy.init(event); }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) { proxy.postInit(event); }

    public static class CommonProxy
    {
        public void init(FMLInitializationEvent event)
        {
            GameRegistry.registerBlock(CustomBlock.instance, CustomBlock.name);
        }

        public void postInit(FMLPostInitializationEvent event) {}
    }

    public static class ClientProxy extends CommonProxy
    {
        private static ModelResourceLocation modelLocation = new ModelResourceLocation(blockName, null);
        static ModelFontRenderer fontRenderer;

        @Override
        public void init(FMLInitializationEvent event)
        {
            super.init(event);
            MinecraftForge.EVENT_BUS.register(EHandler.instance);
        }

        @Override
        public void postInit(FMLPostInitializationEvent event) {
            super.postInit(event);
            Item item = Item.getItemFromBlock(CustomBlock.instance);
            RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
            if(renderItem != null)
            {
                renderItem.getItemModelMesher().register(item, new ItemMeshDefinition() {
                    public ModelResourceLocation getModelLocation(ItemStack stack)
                    {
                        return modelLocation;
                    }
                });
            }
            Matrix4f m = new Matrix4f();
            m.m22 = 1f / 32f;
            m.m00 = m.m11 = -m.m22;
            m.m33 = 1;
            m.setTranslation(new Vector3f(1, 1, 0));
            fontRenderer = new ModelFontRenderer(Minecraft.getMinecraft().gameSettings, font, Minecraft.getMinecraft().getTextureManager(), false, m, Attributes.DEFAULT_BAKED_FORMAT);
            ((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(fontRenderer);
        }
    }

    public static class EHandler
    {
        public static final EHandler instance = new EHandler();

        private EHandler() {};

        @SubscribeEvent
        public void onModelBakeEvent(ModelBakeEvent event)
        {
            TextureAtlasSprite base = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/slime");
            event.modelRegistry.putObject(ClientProxy.modelLocation, new CustomModel(base));
        }

        @SubscribeEvent
        public void onTextureStitchPre(TextureStitchEvent.Pre event)
        {
            if(event.map == Minecraft.getMinecraft().getTextureMapBlocks())
            {
                event.map.registerSprite(font2);
            }
        }
    }

    public static class CustomBlock extends Block
    {
        public static final CustomBlock instance = new CustomBlock();
        public static final String name = "custom_model_block";

        private CustomBlock()
        {
            super(Material.iron);
            setCreativeTab(CreativeTabs.tabBlock);
            setUnlocalizedName(MODID + ":" + name);
        }

        @Override
        public int getRenderType() { return 3; }

        @Override
        public boolean isOpaqueCube() { return false; }

        @Override
        public boolean isFullCube() { return false; }

        @Override
        public boolean isVisuallyOpaque() { return false; }
    }

    public static class CustomModel implements IBakedModel
    {
        private final TextureAtlasSprite base;

        public CustomModel(TextureAtlasSprite base)
        {
            this.base = base;
       }

        @Override
        public List<BakedQuad> getFaceQuads(EnumFacing side)
        {
            return Collections.emptyList();
        }

        @Override
        public List<BakedQuad> getGeneralQuads()
        {
            ClientProxy.fontRenderer.drawString("Test", 0, 0, 0xFFFFFFFF);
            return ClientProxy.fontRenderer.build();
        }

        @Override
        public boolean isGui3d() { return true; }

        @Override
        public boolean isAmbientOcclusion() { return true; }

        @Override
        public boolean isBuiltInRenderer() { return false; }

        @Override
        public TextureAtlasSprite getTexture() { return this.base; }

        @Override
        public ItemCameraTransforms getItemCameraTransforms()
        {
            return ItemCameraTransforms.DEFAULT;
        }
    }
}
