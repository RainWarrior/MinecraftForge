package net.minecraftforge.client.model.animation;

import java.io.IOException;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelPart;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.fml.common.FMLLog;

import org.apache.commons.lang3.NotImplementedException;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Various implementations of IClip, and utility methods.
 */
public class Clips
{
    /**
     * Clip that does nothing.
     */
    public static enum IdentityClip implements IClip, IStringSerializable
    {
        instance;

        public IJointClip apply(IJoint joint)
        {
            return JointClips.IdentityJointClip.instance;
        }

        public String getName()
        {
            return "identity";
        }
    }

    /**
     * Retrieves the clip from the model.
     */
    public static IClip getModelClipNode(ResourceLocation modelLocation, String clipName)
    {
        IModel model;
        try
        {
            model = ModelLoaderRegistry.getModel(modelLocation);
            if(model instanceof IAnimatedModel)
            {
                Optional<? extends IClip> clip = ((IAnimatedModel)model).getClip(clipName);
                if(clip.isPresent())
                {
                    return new ModelClip(clip.get(), modelLocation, clipName);
                }
                FMLLog.getLogger().error("Unable to find clip " + clipName + " in the model " + modelLocation);
            }
        }
        catch (IOException e)
        {
            FMLLog.getLogger().error("Unable to load model" + modelLocation + " while loading clip " + clipName, e);
        }
        // FIXME: missing clip?
        return new ModelClip(IdentityClip.instance, modelLocation, clipName);
    }

    /**
     * Wrapper for model clips; useful for debugging and serialization;
     */
    public static final class ModelClip implements IClip
    {
        private final IClip childClip;
        private final ResourceLocation modelLocation;
        private final String clipName;

        public ModelClip(IClip childClip, ResourceLocation modelLocation, String clipName)
        {
            this.childClip = childClip;
            this.modelLocation = modelLocation;
            this.clipName = clipName;
        }

        public IJointClip apply(IJoint joint)
        {
            return childClip.apply(joint);
        }
    }

    /**
     * Clip with custom parameterization of the time.
     */
    public static final class TimeClip implements IClip
    {
        private final IClip childClip;
        private final ITimeValue time;

        public TimeClip(IClip childClip, ITimeValue time)
        {
            this.childClip = childClip;
            this.time = time;
        }

        public IJointClip apply(final IJoint joint)
        {
            return new IJointClip()
            {
                private final IJointClip parent = childClip.apply(joint);
                public TRSRTransformation apply(float time)
                {
                    return parent.apply(TimeClip.this.time.apply(time));
                }
            };
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(childClip, time);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TimeClip other = (TimeClip) obj;
            return Objects.equal(childClip, other.childClip) && Objects.equal(time, other.time);
        }
    }

    public static final class ClipLengthProvider implements IClipProvider
    {
        private final IClip clip;
        private final ITimeValue length;

        public ClipLengthProvider(IClip clip, ITimeValue length)
        {
            this.clip = clip;
            this.length = length;
        }

        public ClipLength apply(float time)
        {
            return new ClipLength(clip, length.apply(time));
        }
    }

    public static IClipProvider createClipLength(IClip clip, ITimeValue length)
    {
        return new ClipLengthProvider(clip, length);
    }

    public static final class ClipSlerpProvider implements IClipProvider
    {
        private final IClip from;
        private final IClip to;
        private final ITimeValue input;
        private final float length;

        public ClipSlerpProvider(IClip from, IClip to, ITimeValue input, float length)
        {
            this.from = from;
            this.to = to;
            this.input = input;
            this.length = length;
        }

        public ClipLength apply(float start)
        {
            ITimeValue progress = new TimeValues.LinearValue(1f / length, -start / length);
            return new ClipLength(new SlerpClip(from, to, input, progress), length);
        }
    }

    public static IClipProvider slerpFactory(final IClip from, final IClip to, final ITimeValue input, final float length)
    {
        return new ClipSlerpProvider(from, to, input, length);
    }

    /**
     * Spherical linear blend between 2 clips.
     */
    public static final class SlerpClip implements IClip
    {
        private final IClip from;
        private final IClip to;
        private final ITimeValue input;
        private final ITimeValue progress;

        public SlerpClip(IClip from, IClip to, ITimeValue input, ITimeValue progress)
        {
            this.from = from;
            this.to = to;
            this.input = input;
            this.progress = progress;
        }

        public IJointClip apply(IJoint joint)
        {
            IJointClip fromClip = from.apply(joint);
            IJointClip toClip = to.apply(joint);
            return blendClips(joint, fromClip, toClip, input, progress);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(from, to, input, progress);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SlerpClip other = (SlerpClip) obj;
            return Objects.equal(from, other.from) &&
                Objects.equal(to, other.to) &&
                Objects.equal(input, other.input) &&
                Objects.equal(progress, other.progress);
        }
    }

    /*public static class AdditiveLerpClip implements IClip
    {
        private final IClip base;
        private final IClip add;
        private final IParameter progress;

        public AdditiveLerpClip(IClip base, IClip add, IParameter progress)
        {
            this.base = base;
            this.add = add;
            this.progress = progress;
        }

        public IJointClip apply(IJoint joint)
        {
            throw new NotImplementedException("AdditiveLerpClip.apply");
        }
    }*/

    private static IJointClip blendClips(final IJoint joint, final IJointClip fromClip, final IJointClip toClip, final ITimeValue input, final ITimeValue progress)
    {
        return new IJointClip()
        {
            public TRSRTransformation apply(float time)
            {
                float clipTime = input.apply(time);
                return fromClip.apply(clipTime).slerp(toClip.apply(clipTime), MathHelper.clamp_float(progress.apply(time), 0, 1));
            }
        };
    }

    /**
     * IModelState wrapper for a Clip, sampled at specified time.
     */
    public static IModelState apply(final IClip clip, final float time)
    {
        return new IModelState()
        {
            public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part)
            {
                if(!part.isPresent() || !(part.get() instanceof IJoint))
                {
                    return Optional.absent();
                }
                IJoint joint = (IJoint)part.get();
                if(!joint.getParent().isPresent())
                {
                    return Optional.of(clip.apply(joint).apply(time).compose(joint.getInvBindPose()));
                }
                return Optional.of(clip.apply(joint.getParent().get()).apply(time).compose(clip.apply(joint).apply(time)).compose(joint.getInvBindPose()));
            }
        };
    }

    /**
     * Reference to another clip.
     * Should only exist during loading.
     */
    public static final class ClipReference implements IClip, IStringSerializable
    {
        private final String clipName;

        public ClipReference(String clipName)
        {
            this.clipName = clipName;
        }

        public IJointClip apply(final IJoint joint)
        {
            throw new NotImplementedException("ClipReference shouldn't exist outside the loading phase.");
        }

        public String getName()
        {
            return clipName;
        }
    }

    public static enum CommonClipTypeAdapterFactory implements TypeAdapterFactory
    {
        INSTANCE;

        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
        {
            if(type.getRawType() != IClip.class)
            {
                return null;
            }

            final TypeAdapter<ITimeValue> parameterAdapter = gson.getAdapter(ITimeValue.class);

            return (TypeAdapter<T>)new TypeAdapter<IClip>()
            {
                public void write(JsonWriter out, IClip clip) throws IOException
                {
                    if(clip instanceof IStringSerializable)
                    {
                        out.value("#" + ((IStringSerializable)clip).getName());
                        return;
                    }
                    else if(clip instanceof TimeClip)
                    {
                        out.beginArray();
                        TimeClip timeClip = (TimeClip)clip;
                        write(out, timeClip.childClip);
                        parameterAdapter.write(out, timeClip.time);
                        out.endArray();
                        return;
                    }
                    else if(clip instanceof ModelClip)
                    {
                        ModelClip modelClip = (ModelClip)clip;
                        out.value(modelClip.modelLocation + "@" + modelClip.clipName);
                        return;
                    }
                    // TODO custom clip writing?
                    throw new NotImplementedException("Clip to json: " + clip);
                }

                public IClip read(JsonReader in) throws IOException
                {
                    switch(in.peek())
                    {
                        case BEGIN_ARRAY:
                            in.beginArray();
                            IClip childClip = read(in);
                            ITimeValue time = parameterAdapter.read(in);
                            in.endArray();
                            return new TimeClip(childClip, time);
                        case STRING:
                            String name = in.nextString();
                            if(name.equals("#identity"))
                            {
                                return IdentityClip.instance;
                            }
                            if(name.startsWith("#"))
                            {
                                return new ClipReference(name.substring(1));
                            }
                            else
                            {
                                int at = name.lastIndexOf('@');
                                String location = name.substring(0, at);
                                String clipName = name.substring(at + 1, name.length());
                                ResourceLocation model;
                                if(location.indexOf('#') != -1)
                                {
                                    model = new ModelResourceLocation(location);
                                }
                                else
                                {
                                    model = new ResourceLocation(location);
                                }
                                return getModelClipNode(model, clipName);
                            }
                        default:
                            throw new IOException("expected Clip, got " + in.peek());
                    }
                }
            };
        }
    }

    public static enum CommonClipProviderTypeAdapterFactory implements TypeAdapterFactory
    {
        INSTANCE;

        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
        {
            if(type.getRawType() != IClipProvider.class)
            {
                return null;
            }

            final TypeAdapter<IClip> clipAdapter = gson.getAdapter(IClip.class);
            final TypeAdapter<ITimeValue> parameterAdapter = gson.getAdapter(ITimeValue.class);

            return (TypeAdapter<T>)new TypeAdapter<IClipProvider>()
            {
                public void write(JsonWriter out, IClipProvider clipProvider) throws IOException
                {
                    if(clipProvider instanceof ClipLengthProvider)
                    {
                        ClipLengthProvider provider = (ClipLengthProvider)clipProvider;
                        out.beginArray();
                        clipAdapter.write(out, provider.clip);
                        parameterAdapter.write(out, provider.length);
                        out.endArray();
                    }
                    else if(clipProvider instanceof ClipSlerpProvider)
                    {
                        ClipSlerpProvider provider = (ClipSlerpProvider)clipProvider;
                        out.beginArray();
                        clipAdapter.write(out, provider.from);
                        clipAdapter.write(out, provider.to);
                        parameterAdapter.write(out, provider.input);
                        out.value(provider.length);
                        out.endArray();
                    }
                }

                public IClipProvider read(JsonReader in) throws IOException
                {
                    // TODO Auto-generated method stub
                    return null;
                }
            };
        }
    }
}