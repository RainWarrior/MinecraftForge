package net.minecraftforge.client.model.animation;

import java.io.IOException;

import net.minecraft.util.IStringSerializable;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Various implementations of ITimeValue.
 */
public final class TimeValues
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

    public static final class SimpleExprValue implements ITimeValue
    {
        private final String operators;
        private final ImmutableList<ITimeValue> args;

        public SimpleExprValue(String operators, ImmutableList<ITimeValue> args)
        {
            this.operators = operators;
            this.args = args;
        }

        public float apply(float input)
        {
            float ret = input;
            for(int i = 0; i < operators.length(); i++)
            {
                float arg = args.get(i).apply(input);
                switch(operators.charAt(i))
                {
                    case '+': ret += arg; break;
                    case '-': ret -= arg; break;
                    case '*': ret *= arg; break;
                    case '/': ret /= arg; break;
                    case 'm': ret = Math.min(ret, arg); break;
                    case 'M': ret = Math.max(ret, arg); break;
                    case 'r': ret = (float)Math.floor(ret / arg) * arg; break;
                    case 'R': ret = (float)Math.ceil(ret / arg) * arg; break;
                    case 'f': ret -= Math.floor(ret / arg) * arg; break;
                    case 'F': ret = (float)Math.ceil(ret / arg) * arg - ret; break;
                }
            }
            return ret;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(operators, args);
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
            SimpleExprValue other = (SimpleExprValue) obj;
            return Objects.equal(operators, other.operators) && Objects.equal(args, other.args);
        }
    }

    public static final class ParameterValue implements ITimeValue, IStringSerializable
    {
        private final String parameterName;
        private final Function<String, ITimeValue> valueResolver;
        private ITimeValue parameter;

        public ParameterValue(String parameterName, Function<String, ITimeValue> valueResolver)
        {
            this.parameterName = parameterName;
            this.valueResolver = valueResolver;
        }

        public String getName()
        {
            return parameterName;
        }

        private void resolve()
        {
            if(parameter == null)
            {
                if(valueResolver != null)
                {
                    parameter = valueResolver.apply(parameterName);
                }
                if(parameter == null)
                {
                    throw new IllegalArgumentException("Couldn't resolve parameter value " + parameterName);
                }
            }
        }

        public float apply(float input)
        {
            resolve();
            return parameter.apply(input);
        }

        @Override
        public int hashCode()
        {
            resolve();
            return parameter.hashCode();
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
            ParameterValue other = (ParameterValue) obj;
            resolve();
            other.resolve();
            return Objects.equal(parameter, other.parameter);
        }
    }

    public static enum CommonTimeValueTypeAdapterFactory implements TypeAdapterFactory
    {
        INSTANCE;

        private final ThreadLocal<Function<String, ITimeValue>> valueResolver = new ThreadLocal<Function<String, ITimeValue>>();

        public void setValueResolver(Function<String, ITimeValue> valueResolver)
        {
            this.valueResolver.set(valueResolver);
        }

        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
        {
            if(type.getRawType() != ITimeValue.class)
            {
                return null;
            }

            final TypeAdapter<ImmutableList<ITimeValue>> parameterListAdapter = gson.getAdapter(new TypeToken<ImmutableList<ITimeValue>>(){});

            return (TypeAdapter<T>)new TypeAdapter<ITimeValue>()
            {
                public void write(JsonWriter out, ITimeValue parameter) throws IOException
                {
                    if(parameter instanceof ConstValue)
                    {
                        out.value(((ConstValue)parameter).output);
                    }
                    else if(parameter instanceof SimpleExprValue)
                    {
                        SimpleExprValue p = (SimpleExprValue)parameter;
                        out.beginArray();
                        out.value(p.operators);
                        parameterListAdapter.write(out, p.args);
                        out.endArray();
                    }
                    else if(parameter instanceof IStringSerializable)
                    {
                        out.value("#" + ((IStringSerializable)parameter).getName());
                    }
                    // TODO custom value writing?
                }

                public ITimeValue read(JsonReader in) throws IOException
                {
                    switch(in.peek())
                    {
                    case NUMBER:
                        return new ConstValue((float)in.nextDouble());
                    case BEGIN_ARRAY:
                        in.beginArray();
                        ITimeValue p = new SimpleExprValue(in.nextString(), parameterListAdapter.read(in));
                        in.endArray();
                        return p;
                    case STRING:
                        String string = in.nextString();
                        if(string.equals("#identity"))
                        {
                            return IdentityValue.instance;
                        }
                        if(!string.startsWith("#"))
                        {
                            throw new IOException("expected TimeValue reference, got \"" + string + "\"");
                        }
                        // User Parameter TimeValue
                        return new ParameterValue(string.substring(1), valueResolver.get());
                    default:
                        throw new IOException("expected TimeValue, got " + in.peek());
                    }
                }
            };
        }
    }
}
