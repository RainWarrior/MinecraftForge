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
 * Various implementations of IParameter.
 */
public class Parameters
{
    public static enum NoopParameter implements IParameter, IStringSerializable
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

    public static final class ConstParameter implements IParameter
    {
        private final float output;

        public ConstParameter(float output)
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
            ConstParameter other = (ConstParameter) obj;
            return output == other.output;
        }
    }

    public static final class LinearParameter implements IParameter
    {
        private final float weight;
        private final float offset;

        public LinearParameter(float weight, float offset)
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
            LinearParameter other = (LinearParameter) obj;
            return weight == other.weight && offset == other.offset;
        }
    }

    public static enum CommonParameterTypeAdapterFactory implements TypeAdapterFactory
    {
        INSTANCE;

        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
        {
            if(type.getRawType() != IParameter.class)
            {
                return null;
            }

            return (TypeAdapter<T>)new TypeAdapter<IParameter>()
            {
                public void write(JsonWriter out, IParameter parameter) throws IOException
                {
                    if(parameter instanceof ConstParameter)
                    {
                        out.value(((ConstParameter)parameter).output);
                    }
                    // TODO linear parameter
                    else if(parameter instanceof IStringSerializable)
                    {
                        out.value("#" + ((IStringSerializable)parameter).getName());
                    }
                    // TODO custom parameter writing?
                }

                public IParameter read(JsonReader in) throws IOException
                {
                    // TODO Auto-generated method stub
                    return null;
                }
            };
        }
    }
}
