package net.minecraftforge.client.model.animation;

import java.io.IOException;

import net.minecraft.util.IStringSerializable;

import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Various implementations of ITimeValue.
 */
public class TimeValues
{
    public static enum IdentityValue implements ITimeValue, IStringSerializable
    {
        instance;

        public float apply(float input)
        {
            return input;
        }

        public String getName()
        {
            return "identity";
        }
    }

    public static final class ConstValue implements ITimeValue
    {
        private final float output;

        public ConstValue(float output)
        {
            this.output = output;
        }

        public float apply(float input)
        {
            return output;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(output);
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
            ConstValue other = (ConstValue) obj;
            return output == other.output;
        }
    }

    public static final class LinearValue implements ITimeValue
    {
        private final float weight;
        private final float offset;

        public LinearValue(float weight, float offset)
        {
            super();
            this.weight = weight;
            this.offset = offset;
        }

        public float apply(float input)
        {
            return input * weight + offset;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(weight, offset);
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
            LinearValue other = (LinearValue) obj;
            return weight == other.weight && offset == other.offset;
        }
    }

    public static enum CommonTimeValueTypeAdapterFactory implements TypeAdapterFactory
    {
        INSTANCE;

        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
        {
            if(type.getRawType() != ITimeValue.class)
            {
                return null;
            }

            return (TypeAdapter<T>)new TypeAdapter<ITimeValue>()
            {
                public void write(JsonWriter out, ITimeValue parameter) throws IOException
                {
                    if(parameter instanceof ConstValue)
                    {
                        out.value(((ConstValue)parameter).output);
                    }
                    // TODO linear value
                    else if(parameter instanceof IStringSerializable)
                    {
                        out.value("#" + ((IStringSerializable)parameter).getName());
                    }
                    // TODO custom value writing?
                }

                public ITimeValue read(JsonReader in) throws IOException
                {
                    // TODO Auto-generated method stub
                    return null;
                }
            };
        }
    }
}
