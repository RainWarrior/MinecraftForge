package net.minecraftforge.client.model.animation;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.util.JsonUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class Animation
{
    public static final PropertyBool StaticProperty = PropertyBool.create("static");
    public static final IUnlistedProperty<IModelState> AnimationProperty = new IUnlistedProperty<IModelState>()
    {
        public String getName() { return "forge_animation"; }
        public boolean isValid(IModelState state) { return true; }
        public Class<IModelState> getType() { return IModelState.class; }
        public String valueToString(IModelState state) { return state.toString(); }
    };

    public static float getWorldTime(World world)
    {
        return getWorldTime(world, 0);
    }

    public static float getWorldTime(World world, float tickProgress)
    {
        return (world.getTotalWorldTime() + tickProgress) / 20;
    }

    private static final AnimationStateMachine missing = new AnimationStateMachine(
        ImmutableMap.of("missingno", (IClip)Clips.IdentityClip.instance),
        ImmutableList.of("missingno"),
        ImmutableTable.<String, String, IClipProvider>of(),
        "missingno");

    private static final Gson asmGson = new GsonBuilder()
        .registerTypeAdapterFactory(Clips.CommonClipTypeAdapterFactory.INSTANCE)
        .registerTypeAdapterFactory(Clips.CommonClipProviderTypeAdapterFactory.INSTANCE)
        .registerTypeAdapterFactory(Parameters.CommonParameterTypeAdapterFactory.INSTANCE)
        .registerTypeAdapterFactory(ClipProviderTableTypeAdapterFactory.INSTANCE)
        .setPrettyPrinting()
        .enableComplexMapKeySerialization()
        .disableHtmlEscaping()
        .create();

    /**
     * Entry point for loading animation state machines.
     */
    public static <P extends IParameter & IStringSerializable> AnimationStateMachine load(ResourceLocation location, ImmutableMap<String, P> customParameters)
    {
        // hardcoded test case for now, JSON later

        if(location.equals(new ResourceLocation("forgedebugmodelanimation", "afsm/block/chest")))
        {
            IClip b3d = Clips.getModelClipNode(new ResourceLocation("forgedebugmodelloaderregistry", "block/chest.b3d"), "main");
            IClip closed = new Clips.TimeClip(b3d, new Parameters.ConstParameter(0));
            IClip open = new Clips.TimeClip(b3d, new Parameters.ConstParameter(10));
            IClipProvider c2o = Clips.slerpFactory(closed, open, Parameters.NoopParameter.instance, 1);
            IClipProvider o2c = Clips.slerpFactory(open, closed, Parameters.NoopParameter.instance, 1);

            ImmutableTable.Builder<String, String, IClipProvider> builder = ImmutableTable.builder();
            builder.put("closed", "open", c2o);
            builder.put("open", "closed", o2c);

            return new AnimationStateMachine(
                ImmutableMap.of("closed", closed, "open", open),
                ImmutableList.of("closed", "open"),
                builder.build(),
                "closed"
            );
        }
        else if(location.equals(new ResourceLocation("forgedebugmodelanimation", "afsm/block/engine")))
        {
            final IParameter worldToCycle = customParameters.containsKey("worldToCycle") ? customParameters.get("worldToCycle") : Parameters.NoopParameter.instance;
            final IParameter roundCycle = customParameters.containsKey("roundCycle") ? customParameters.get("roundCycle") : Parameters.NoopParameter.instance;

            final IClip default_ = Clips.getModelClipNode(new ResourceLocation("forgedebugmodelanimation", "block/engine_ring"), "default");
            IClip movingTmp = Clips.getModelClipNode(new ResourceLocation("forgedebugmodelanimation", "block/engine_ring"), "moving");
            final IClip moving = new Clips.TimeClip(movingTmp, worldToCycle);

            IClipProvider d2m = Clips.createClipLength(new Clips.ClipReference("default"), roundCycle);
            IClipProvider m2d = Clips.createClipLength(new Clips.ClipReference("moving"), roundCycle);

            ImmutableTable.Builder<String, String, IClipProvider> builder = ImmutableTable.builder();
            builder.put("default", "moving", d2m);
            builder.put("moving", "default", m2d);

            /*System.out.println("default: " + asmGson.toJson(default_, IClip.class));
            System.out.println("moving: " + asmGson.toJson(moving, IClip.class));
            System.out.println("d2m: " + asmGson.toJson(d2m, IClipProvider.class));
            System.out.println("m2d: " + asmGson.toJson(m2d, IClipProvider.class));*/
            AnimationStateMachine afsm = new AnimationStateMachine(
                ImmutableMap.of("default", default_, "moving", moving),
                ImmutableList.of("default", "moving"),
                builder.build(),
                "moving"
            );
            System.out.println("afsm: " + asmGson.toJson(afsm));
            afsm.initialize();
            return afsm;
        }
        else return missing;
    }

    private static final Gson mbaGson = new GsonBuilder()
        .registerTypeAdapter(ImmutableList.class, JsonUtils.ImmutableListTypeAdapter.INSTANCE)
        .registerTypeAdapter(ImmutableMap.class, JsonUtils.ImmutableMapTypeAdapter.INSTANCE)
        .setPrettyPrinting()
        .enableComplexMapKeySerialization()
        .disableHtmlEscaping()
        .create();

    /**
     * Entry point for loading animation tracks associated with vanilla json models.
     */
    public static ModelBlockAnimation loadVanillaAnimation(ResourceLocation armatureLocation)
    {
        // TODO json

        ImmutableMap<String, ImmutableMap<Integer, float[]>> joints = ImmutableMap.of();
        ImmutableMap<String, ModelBlockAnimation.MBClip> clips = ImmutableMap.of();

        if(armatureLocation.getResourcePath().endsWith("engine_ring"))
        {
            joints = ImmutableMap.of(
                "ring", ImmutableMap.of(
                    0, new float[]{ 1 }
                ),
                "chamber", ImmutableMap.of(
                    1, new float[]{ 1 }
                ),
                "trunk", ImmutableMap.of(
                    2, new float[]{ 1 }
                )
            );
            ModelBlockAnimation.MBClip moving = new ModelBlockAnimation.MBClip(true, ImmutableMap.of(
                "ring", ImmutableList.of(
                    new ModelBlockAnimation.MBVariableClip(
                        ModelBlockAnimation.Parameter.Variable.Y,
                        ModelBlockAnimation.Parameter.Type.UNIFORM,
                        ModelBlockAnimation.Parameter.Interpolation.LINEAR,
                        new float[]{ .00f, .08f, .25f, .42f, .50f, .42f, .25f, .08f }
                    ),
                    new ModelBlockAnimation.MBVariableClip(
                        ModelBlockAnimation.Parameter.Variable.YROT,
                        ModelBlockAnimation.Parameter.Type.UNIFORM,
                        ModelBlockAnimation.Parameter.Interpolation.NEAREST,
                        new float[]{ 1 }
                    ),
                    new ModelBlockAnimation.MBVariableClip(
                        ModelBlockAnimation.Parameter.Variable.ANGLE,
                        ModelBlockAnimation.Parameter.Type.UNIFORM,
                        ModelBlockAnimation.Parameter.Interpolation.LINEAR,
                        new float[]{ 0, 120, 240, 0, 120, 240, 0, 120, 240, 0, 120, 240 }
                    )
                ),
                /*ImmutableList.of(
                    new ModelBlockAnimation.MBVariableClip(
                        ModelBlockAnimation.Parameter.Variable.Y,
                        ModelBlockAnimation.Parameter.Type.Uniform,
                        ModelBlockAnimation.Parameter.Interpolation.Nearest,
                        new float[]{ .0f, .5f }
                    )
                ),
                ImmutableList.of(
                    new ModelBlockAnimation.MBVariableClip(
                        ModelBlockAnimation.Parameter.Variable.Y,
                        ModelBlockAnimation.Parameter.Type.Uniform,
                        ModelBlockAnimation.Parameter.Interpolation.Nearest,
                        new float[]{ .0f, -.5f }
                    )
                ),*/
                "chamber", ImmutableList.of(
                    new ModelBlockAnimation.MBVariableClip(
                        ModelBlockAnimation.Parameter.Variable.SCALE,
                        ModelBlockAnimation.Parameter.Type.UNIFORM,
                        ModelBlockAnimation.Parameter.Interpolation.NEAREST,
                        new float[]{ 0, 1 }
                    )
                ),
                "trunk", ImmutableList.of(
                    new ModelBlockAnimation.MBVariableClip(
                        ModelBlockAnimation.Parameter.Variable.SCALE,
                        ModelBlockAnimation.Parameter.Type.UNIFORM,
                        ModelBlockAnimation.Parameter.Interpolation.NEAREST,
                        new float[]{ 1, 0 }
                    )
                )
            ));

            clips = ImmutableMap.<String, ModelBlockAnimation.MBClip>of(
                "default", new ModelBlockAnimation.MBClip(false, ImmutableMap.<String, ImmutableList<ModelBlockAnimation.MBVariableClip>>of()),
                "moving", moving
            );
        }
        ModelBlockAnimation mba = new ModelBlockAnimation(
            joints,
            clips
        );

        if(!clips.isEmpty())
        {
            String json = mbaGson.toJson(mba);
            System.out.println(json);
        }
        return mba;
    }

    public static enum ClipProviderTableTypeAdapterFactory implements TypeAdapterFactory
    {
        INSTANCE;

        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken)
        {
            if(typeToken.getRawType() != ImmutableTable.class || !(typeToken.getType() instanceof ParameterizedType))
            {
                return null;
            }
            ParameterizedType type = (ParameterizedType)typeToken.getType();
            if(
                type.getActualTypeArguments()[0] != String.class ||
                type.getActualTypeArguments()[1] != String.class ||
                type.getActualTypeArguments()[2] != IClipProvider.class) {
                return null;
            }

            final TypeAdapter<IClipProvider> clipProviderAdapter = gson.getAdapter(IClipProvider.class);

            return (TypeAdapter<T>)new TypeAdapter<ImmutableTable<String, String, IClipProvider>>()
            {
                public void write(JsonWriter out, ImmutableTable<String, String, IClipProvider> table) throws IOException
                {
                    out.beginObject();
                    for(Table.Cell<String, String, IClipProvider> cell : table.cellSet())
                    {
                        out.name(cell.getRowKey() + " -> " + cell.getColumnKey());
                        clipProviderAdapter.write(out, cell.getValue());
                    }
                    out.endObject();
                }

                public ImmutableTable<String, String, IClipProvider> read(JsonReader in) throws IOException
                {
                    // TODO Auto-generated method stub
                    return null;
                }
            };
        }
    }
}
