package net.minecraftforge.client.model.animation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.client.model.animation.ModelBlockAnimation.Parameter.Interpolation;
import net.minecraftforge.client.model.animation.ModelBlockAnimation.Parameter.Type;
import net.minecraftforge.client.model.animation.ModelBlockAnimation.Parameter.Variable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Sets;
import com.google.gson.annotations.SerializedName;

public class ModelBlockAnimation
{
    private final ImmutableMap<String, ImmutableMap<Integer, float[]>> joints;
    private final ImmutableMap<String, MBClip> clips;
    private transient ImmutableMultimap<Integer, MBJointWeight> jointIndexMap;

    public ModelBlockAnimation(ImmutableMap<String, ImmutableMap<Integer, float[]>> joints, ImmutableMap<String, MBClip> clips)
    {
        this.joints = joints;
        this.clips = clips;
    }

    public ImmutableMap<String, MBClip> getClips()
    {
        return clips;
    }

    public ImmutableCollection<MBJointWeight> getJoint(int i)
    {
        if(jointIndexMap == null)
        {
            ImmutableMultimap.Builder<Integer, MBJointWeight> builder = ImmutableMultimap.builder();
            for(Map.Entry<String, ImmutableMap<Integer, float[]>> info : joints.entrySet())
            {
                for(Map.Entry<Integer, float[]> e : info.getValue().entrySet())
                {
                    builder.put(e.getKey(), new MBJointWeight(info.getKey(), info.getValue()));
                }
            }
            jointIndexMap = builder.build();
        }
        return jointIndexMap.get(i);
    }

    protected static class MBVariableClip
    {
        private final Variable variable;
        @SuppressWarnings("unused")
        private final Type type;
        private final Interpolation interpolation;
        private final float[] samples;

        public MBVariableClip(Variable variable, Type type, Interpolation interpolation, float[] samples)
        {
            this.variable = variable;
            this.type = type;
            this.interpolation = interpolation;
            this.samples = samples;
        }
    }

    protected static class MBClip implements IClip
    {
        private final boolean loop;
        @SerializedName("joint_clips")
        private final ImmutableMap<String, ImmutableList<MBVariableClip>> jointClipsFlat;
        private transient ImmutableMap<String, MBJointClip> jointClips;

        protected MBClip()
        {
            this(false, ImmutableMap.<String, ImmutableList<MBVariableClip>>of());
        }

        public MBClip(boolean loop, ImmutableMap<String, ImmutableList<MBVariableClip>> clips)
        {
            this.loop = loop;
            this.jointClipsFlat = clips;
        }

        @Override
        public IJointClip apply(IJoint joint)
        {
            if(jointClips == null)
            {
                ImmutableMap.Builder<String, MBJointClip> builder = ImmutableMap.builder();
                for(Map.Entry<String, ImmutableList<MBVariableClip>> e : jointClipsFlat.entrySet())
                {
                    builder.put(e.getKey(), new MBJointClip(loop, e.getValue()));
                }
                jointClips = builder.build();
            }
            if(joint instanceof MBJoint)
            {
                MBJoint mbJoint = (MBJoint)joint;
                //MBJointInfo = jointInfos.
                MBJointClip clip = jointClips.get(mbJoint.getName());
                if(clip != null) return clip;
            }
            return JointClips.IdentityJointClip.instance;
        }

        protected static class MBJointClip implements IJointClip
        {
            private final boolean loop;
            private final ImmutableList<MBVariableClip> variables;

            public MBJointClip(boolean loop, ImmutableList<MBVariableClip> variables)
            {
                this.loop = loop;
                this.variables = variables;
                EnumSet<Variable> hadVar = Sets.newEnumSet(Collections.<Variable>emptyList(), Variable.class);
                for(MBVariableClip var : variables)
                {
                    if(hadVar.contains(var.variable))
                    {
                        throw new IllegalArgumentException("duplicate variable: " + var);
                    }
                    hadVar.add(var.variable);
                }
            }

            public TRSRTransformation apply(float time)
            {
                time -= Math.floor(time);
                Vector3f translation = new Vector3f(0, 0, 0);
                Vector3f scale = new Vector3f(1, 1, 1);
                AxisAngle4f rotation = new AxisAngle4f(0, 0, 0, 0);
                for(MBVariableClip var : variables)
                {
                    int length = loop ? var.samples.length : (var.samples.length - 1);
                    float timeScaled = time * length;
                    int s1 = MathHelper.clamp_int((int)Math.round(Math.floor(timeScaled)), 0, length - 1);
                    float progress = timeScaled - s1;
                    int s2 = s1 + 1;
                    if(s2 == length && loop) s2 = 0;
                    float value = 0;
                    switch(var.interpolation)
                    {
                        case LINEAR:
                            if(var.variable == Variable.ANGLE)
                            {
                                float v1 = var.samples[s1];
                                float v2 = var.samples[s2];
                                float diff = ((v2 - v1) % 360 + 540) % 360 - 180;
                                value = v1 + diff * progress;
                            }
                            else
                            {
                                value = var.samples[s1] * (1 - progress) + var.samples[s2] * progress;
                            }
                            break;
                        case NEAREST:
                            value = var.samples[progress < .5f ? s1 : s2];
                            break;
                    }
                    switch(var.variable)
                    {
                        case X:
                            translation.x = value;
                            break;
                        case Y:
                            translation.y = value;
                            break;
                        case Z:
                            translation.z = value;
                            break;
                        case XROT:
                            rotation.x = value;
                            break;
                        case YROT:
                            rotation.y = value;
                            break;
                        case ZROT:
                            rotation.z = value;
                            break;
                        case ANGLE:
                            rotation.angle = (float)Math.toRadians(value);
                            break;
                        case SCALE:
                            scale.x = scale.y = scale.z = value;
                            break;
                        case XS:
                            scale.x = value;
                            break;
                        case YS:
                            scale.y = value;
                            break;
                        case ZS:
                            scale.z = value;
                            break;
                    }
                }
                Quat4f rot = new Quat4f();
                rot.set(rotation);
                return TRSRTransformation.blockCenterToCorner(new TRSRTransformation(translation, rot, scale, null));
            }
        }
    }

    protected static class MBJoint implements IJoint
    {
        private final String name;
        private final TRSRTransformation invBindPose;

        public MBJoint(String name, BlockPart part)
        {
            this.name = name;
            if(part.partRotation != null)
            {
                float x = 0, y = 0, z = 0;
                switch(part.partRotation.axis)
                {
                    case X:
                        x = 1;
                    case Y:
                        y = 1;
                    case Z:
                        z = 1;
                }
                Quat4f rotation = new Quat4f();
                rotation.set(new AxisAngle4f(x, y, z, 0));
                Matrix4f m = new TRSRTransformation(
                    TRSRTransformation.toVecmath(part.partRotation.origin),
                    rotation,
                    null,
                    null).getMatrix();
                m.invert();
                invBindPose = new TRSRTransformation(m);
            }
            else
            {
                invBindPose = TRSRTransformation.identity();
            }
        }

        public TRSRTransformation getInvBindPose()
        {
            return invBindPose;
        }

        public Optional<? extends IJoint> getParent()
        {
            return Optional.absent();
        }

        public String getName()
        {
            return name;
        }
    }

    protected static class MBJointWeight
    {
        private final String name;
        private final ImmutableMap<Integer, float[]> weights;

        public MBJointWeight(String name, ImmutableMap<Integer, float[]> weights)
        {
            this.name = name;
            this.weights = weights;
        }

        public String getName()
        {
            return name;
        }

        public ImmutableMap<Integer, float[]> getWeights()
        {
            return weights;
        }
    }

    protected static class Parameter
    {
        public static enum Variable
        {
            @SerializedName("offset_x")
            X,
            @SerializedName("offset_y")
            Y,
            @SerializedName("offset_z")
            Z,
            @SerializedName("axis_x")
            XROT,
            @SerializedName("axis_y")
            YROT,
            @SerializedName("axis_z")
            ZROT,
            @SerializedName("angle")
            ANGLE,
            @SerializedName("scale")
            SCALE,
            @SerializedName("scale_x")
            XS,
            @SerializedName("scale_y")
            YS,
            @SerializedName("scale_z")
            ZS;
        }

        public static enum Type
        {
            @SerializedName("uniform")
            UNIFORM;
        }

        public static enum Interpolation
        {
            @SerializedName("linear")
            LINEAR,
            @SerializedName("nearest")
            NEAREST;
        }
    }

    public TRSRTransformation getPartTransform(IModelState state, BlockPart part, int i)
    {
        ImmutableCollection<MBJointWeight> infos = getJoint(i);
        if(!infos.isEmpty())
        {
            Matrix4f m = new Matrix4f(), tmp;
            float weight = 0;
            for(MBJointWeight info : infos)
            {
                if(info.getWeights().containsKey(i))
                {
                    ModelBlockAnimation.MBJoint joint = new ModelBlockAnimation.MBJoint(info.getName(), part);
                    Optional<TRSRTransformation> trOp = state.apply(Optional.of(joint));
                    if(trOp.isPresent() && trOp.get() != TRSRTransformation.identity())
                    {
                        float w = info.getWeights().get(i)[0];
                        tmp = trOp.get().getMatrix();
                        tmp.mul(w);
                        m.add(tmp);
                        weight += w;
                    }
                }
            }
            if(weight > 1e-5)
            {
                m.mul(1f / weight);
                return new TRSRTransformation(m);
            }
        }
        return null;
    }
}
