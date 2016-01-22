package net.minecraftforge.client.model.animation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.util.JsonUtils;
import net.minecraftforge.fml.common.FMLLog;

import org.apache.logging.log4j.Level;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public enum Animation implements IResourceManagerReloadListener
{
    INSTANCE;

    private IResourceManager manager;

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

    private final AnimationStateMachine missing = new AnimationStateMachine(
        ImmutableMap.of("missingno", (IClip)Clips.IdentityClip.instance),
        ImmutableList.of("missingno"),
        ImmutableTable.<String, String, IClipProvider>of(),
        "missingno");

    {
        missing.initialize();
    }

    private final Gson asmGson = new GsonBuilder()
        .registerTypeAdapter(ImmutableList.class, JsonUtils.ImmutableListTypeAdapter.INSTANCE)
        .registerTypeAdapter(ImmutableMap.class, JsonUtils.ImmutableMapTypeAdapter.INSTANCE)
        .registerTypeAdapterFactory(Clips.CommonClipTypeAdapterFactory.INSTANCE)
        .registerTypeAdapterFactory(ClipProviders.CommonClipProviderTypeAdapterFactory.INSTANCE)
        .registerTypeAdapterFactory(TimeValues.CommonTimeValueTypeAdapterFactory.INSTANCE)
        .registerTypeAdapterFactory(ClipProviderTableTypeAdapterFactory.INSTANCE)
        .setPrettyPrinting()
        .enableComplexMapKeySerialization()
        .disableHtmlEscaping()
        .create();

    private static final class ClipResolver implements Function<String, IClip>
    {
        private AnimationStateMachine asm;

        public IClip apply(String name)
        {
            return asm.getClips().get(name);
        }
    }

    /**
     * Entry point for loading animation state machines.
     */
    public AnimationStateMachine load(ResourceLocation location, ImmutableMap<String, ITimeValue> customParameters)
    {
        try
        {
            ClipResolver clipResolver = new ClipResolver();
            Clips.CommonClipTypeAdapterFactory.INSTANCE.setClipResolver(clipResolver);
            TimeValues.CommonTimeValueTypeAdapterFactory.INSTANCE.setValueResolver(Functions.forMap(customParameters));
            IResource resource = manager.getResource(location);
            AnimationStateMachine asm = asmGson.fromJson(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8), AnimationStateMachine.class);
            clipResolver.asm = asm;
            asm.initialize();
            //String json = asmGson.toJson(asm);
            //System.out.println(location + ": " + json);
            return asm;
        }
        catch(IOException e)
        {
            FMLLog.log(Level.ERROR, e, "Exception loading Animation State Machine %s, skipping", location);
            return missing;
        }
        catch(JsonParseException e)
        {
            FMLLog.log(Level.ERROR, e, "Exception loading Animation State Machine %s, skipping", location);
            return missing;
        }
        finally
        {
            Clips.CommonClipTypeAdapterFactory.INSTANCE.setClipResolver(null);
            TimeValues.CommonTimeValueTypeAdapterFactory.INSTANCE.setValueResolver(null);
        }

        /*if(location.equals(new ResourceLocation("forgedebugmodelanimation", "asms/block/chest.json")))
        {
            IClip b3d = Clips.getModelClipNode(new ResourceLocation("forgedebugmodelloaderregistry", "block/chest.b3d"), "main");
            IClip closed = new Clips.TimeClip(b3d, new TimeValues.ConstValue(0));
            IClip open = new Clips.TimeClip(b3d, new TimeValues.ConstValue(10));
            IClipProvider c2o = new ClipProviders.ClipSlerpProvider(closed, open, TimeValues.IdentityValue.instance, 1);
            IClipProvider o2c = new ClipProviders.ClipSlerpProvider(open, closed, TimeValues.IdentityValue.instance, 1);

            ImmutableTable.Builder<String, String, IClipProvider> builder = ImmutableTable.builder();
            builder.put("closed", "open", c2o);
            builder.put("open", "closed", o2c);

            AnimationStateMachine afsm = new AnimationStateMachine(
                ImmutableMap.of("closed", closed, "open", open),
                ImmutableList.of("closed", "open"),
                builder.build(),
                "closed"
            );
            afsm.initialize();
            return afsm;
        }
        else if(location.equals(new ResourceLocation("forgedebugmodelanimation", "asms/block/engine.json")))
        {
            final ITimeValue worldToCycle = customParameters.containsKey("worldToCycle") ? customParameters.get("worldToCycle") : TimeValues.IdentityValue.instance;
            final ITimeValue roundCycle = customParameters.containsKey("roundCycle") ? customParameters.get("roundCycle") : TimeValues.IdentityValue.instance;

            final IClip default_ = Clips.getModelClipNode(new ResourceLocation("forgedebugmodelanimation", "block/engine_ring"), "default");
            IClip movingTmp = Clips.getModelClipNode(new ResourceLocation("forgedebugmodelanimation", "block/engine_ring"), "moving");
            final IClip moving = new Clips.TimeClip(movingTmp, worldToCycle);

            //IClipProvider d2m = new ClipProviders.ClipLengthProvider(new Clips.ClipReference("default"), roundCycle);
            //IClipProvider m2d = new ClipProviders.ClipLengthProvider(new Clips.ClipReference("moving"), roundCycle);
            IClipProvider d2m = new ClipProviders.ClipLengthProvider(default_, roundCycle);
            IClipProvider m2d = new ClipProviders.ClipLengthProvider(moving, roundCycle);

            ImmutableTable.Builder<String, String, IClipProvider> builder = ImmutableTable.builder();
            builder.put("default", "moving", d2m);
            builder.put("moving", "default", m2d);

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
        else return missing;*/
    }

    private final Gson mbaGson = new GsonBuilder()
        .registerTypeAdapter(ImmutableList.class, JsonUtils.ImmutableListTypeAdapter.INSTANCE)
        .registerTypeAdapter(ImmutableMap.class, JsonUtils.ImmutableMapTypeAdapter.INSTANCE)
        .setPrettyPrinting()
        .enableComplexMapKeySerialization()
        .disableHtmlEscaping()
        .create();

    public final ModelBlockAnimation defaultModelBlockAnimation = new ModelBlockAnimation(ImmutableMap.<String, ImmutableMap<String, float[]>>of(), ImmutableMap.<String, ModelBlockAnimation.MBClip>of());

    /**
     * Entry point for loading animation tracks associated with vanilla json models.
     */
    public ModelBlockAnimation loadVanillaAnimation(ResourceLocation armatureLocation)
    {
        try
        {
            IResource resource = null;
            try
            {
                resource = manager.getResource(armatureLocation);
            }
            catch(FileNotFoundException e)
            {
                // this is normal. FIXME: error reporting?
                return defaultModelBlockAnimation;
            }
            ModelBlockAnimation mba = mbaGson.fromJson(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8), ModelBlockAnimation.class);
            String json = mbaGson.toJson(mba);
            System.out.println(armatureLocation + ": " + json);
            return mba;
        }
        catch(IOException e)
        {
            FMLLog.log(Level.ERROR, e, "Exception loading vanilla model aniamtion %s, skipping", armatureLocation);
            return defaultModelBlockAnimation;
        }
        catch(JsonParseException e)
        {
            FMLLog.log(Level.ERROR, e, "Exception loading vanilla model aniamtion %s, skipping", armatureLocation);
            return defaultModelBlockAnimation;
        }

        /*ImmutableMap<String, ImmutableMap<Integer, float[]>> joints = ImmutableMap.of();
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
                ),* /
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

        if(!clips.isEmpty())
        {
            ModelBlockAnimation mba = new ModelBlockAnimation(
                joints,
                clips
            );
            String json = mbaGson.toJson(mba);
            System.out.println(armatureLocation + ": " + json);
            return mba;
        }*/
    }

    public static enum ClipProviderTableTypeAdapterFactory implements TypeAdapterFactory
    {
        INSTANCE;

        private final Pattern arrow = Pattern.compile("^(.*) -> (.*)$");

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
            System.out.println("ClipProviderFactory OK");

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
                    ImmutableTable.Builder<String, String, IClipProvider> builder = ImmutableTable.builder();
                    in.beginObject();
                    while(in.peek() != JsonToken.END_OBJECT)
                    {
                        String key = in.nextName();
                        Matcher matcher = arrow.matcher(key);
                        if(!matcher.matches())
                        {
                            throw new IOException("Expected pair of clip names, got \"" + key + "\"");
                        }
                        String from = matcher.group(1);
                        String to = matcher.group(2);
                        IClipProvider provider = clipProviderAdapter.read(in);
                        builder.put(from, to, provider);
                    }
                    in.endObject();
                    return builder.build();
                }
            };
        }
    }

    public void onResourceManagerReload(IResourceManager manager)
    {
        this.manager = manager;
    }
}
