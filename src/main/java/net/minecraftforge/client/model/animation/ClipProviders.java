package net.minecraftforge.client.model.animation;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ClipProviders
{
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

    public static final class ClipSlerpProvider implements IClipProvider
    {
        private final IClip from;
        private final IClip to;
        private final ITimeValue input;
        private final ITimeValue length;

        public ClipSlerpProvider(IClip from, IClip to, ITimeValue input, ITimeValue length)
        {
            this.from = from;
            this.to = to;
            this.input = input;
            this.length = length;
        }

        public ClipLength apply(float start)
        {
            float length = this.length.apply(start);
            ITimeValue progress = new TimeValues.LinearValue(1f / length, -start / length);
            return new ClipLength(new Clips.SlerpClip(from, to, input, progress), length);
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
                        out.value("length");
                        clipAdapter.write(out, provider.clip);
                        parameterAdapter.write(out, provider.length);
                        out.endArray();
                    }
                    else if(clipProvider instanceof ClipSlerpProvider)
                    {
                        ClipSlerpProvider provider = (ClipSlerpProvider)clipProvider;
                        out.beginArray();
                        out.value("slerp");
                        clipAdapter.write(out, provider.from);
                        clipAdapter.write(out, provider.to);
                        parameterAdapter.write(out, provider.input);
                        parameterAdapter.write(out, provider.length);
                        out.endArray();
                    }
                }

                public IClipProvider read(JsonReader in) throws IOException
                {
                    IClipProvider provider;
                    in.beginArray();
                    String type = in.nextString();
                    if("length".equals(type))
                    {
                        provider = new ClipLengthProvider(clipAdapter.read(in), parameterAdapter.read(in));
                    }
                    else if("slerp".equals(type))
                    {
                        provider = new ClipSlerpProvider(
                            clipAdapter.read(in),
                            clipAdapter.read(in),
                            parameterAdapter.read(in),
                            parameterAdapter.read(in)
                        );
                    }
                    else
                    {
                        throw new IOException(new IllegalArgumentException("Unknown parameter type: " + type));
                    }
                    in.endArray();
                    return provider;
                }
            };
        }
    }
}
