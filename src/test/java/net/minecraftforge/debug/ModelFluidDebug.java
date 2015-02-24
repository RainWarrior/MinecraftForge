package net.minecraftforge.debug;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelFluid;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = ModelFluidDebug.MODID, version = ModelFluidDebug.VERSION)
public class ModelFluidDebug
{
    public static final String MODID = "ForgeDebugModelFluid";
    public static final String VERSION = "1.0";

    private static String blockName = MODID.toLowerCase() + ":" + TestFluidBlock.name;


    @SidedProxy(serverSide = "net.minecraftforge.debug.ModelFluidDebug$CommonProxy", clientSide = "net.minecraftforge.debug.ModelFluidDebug$ClientProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) { proxy.preInit(event); }

    public static class CommonProxy
    {
        public void preInit(FMLPreInitializationEvent event)
        {
            FluidRegistry.registerFluid(TestFluid.instance);
            GameRegistry.registerBlock(TestFluidBlock.instance, TestFluidBlock.name);
        }
    }

    public static class ClientProxy extends CommonProxy
    {
        private static String model = ModelFluid.FluidLoader.location(255, 0, 0, 255, new ResourceLocation("blocks/water_still"), new ResourceLocation("blocks/water_flow")).toString();
        private static ModelResourceLocation modelLocation = new ModelResourceLocation(model, "inventory");

        @Override
        public void preInit(FMLPreInitializationEvent event)
        {
            super.preInit(event);
            Item item = Item.getItemFromBlock(TestFluidBlock.instance);
            ModelBakery.addVariantName(Item.getItemFromBlock(TestFluidBlock.instance), model);
            ModelLoader.setCustomMeshDefinition(item, new ItemMeshDefinition()
            {
                public ModelResourceLocation getModelLocation(ItemStack stack)
                {
                    return modelLocation;
                }
            });
            ModelLoader.setCustomStateMapper(TestFluidBlock.instance, new StateMapperBase()
            {
                protected ModelResourceLocation getModelResourceLocation(IBlockState state)
                {
                    return modelLocation;
                }
            });
        }
    }
    
    public static final class TestFluid extends Fluid
    {
        public static final String name = "TestFluid";
        public static final TestFluid instance = new TestFluid();
        
        private TestFluid()
        {
            super(name);
        }
    }

    public static final class TestFluidBlock extends BlockFluidClassic
    {
        public static final TestFluidBlock instance = new TestFluidBlock();
        public static final String name = "TestFluidBlock";

        private TestFluidBlock()
        {
            super(TestFluid.instance, Material.water);
            setCreativeTab(CreativeTabs.tabBlock);
            setUnlocalizedName(MODID + ":" + name);
        }

        /*@Override
        public boolean isOpaqueCube() { return false; }

        @Override
        public boolean isFullCube() { return false; }

        @Override
        public boolean isVisuallyOpaque() { return false; }*/

        @Override
        protected BlockState createBlockState()
        {
            return new ExtendedBlockState(this, PROPERTIES, LEVEL_CORNERS);
        }
    }
}
