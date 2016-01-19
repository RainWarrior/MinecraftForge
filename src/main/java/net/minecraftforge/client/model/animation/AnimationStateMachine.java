package net.minecraftforge.client.model.animation;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.minecraft.util.IStringSerializable;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.animation.Clips.ClipReference;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.gson.annotations.SerializedName;

/**
 * Main controller for the animation logic.
 * API is a simple state machine.
 */
public class AnimationStateMachine
{
    @SerializedName("clips")
    private final ImmutableMap<String, IClip> clipsRaw;
    private transient ImmutableMap<String, IClip> clips;
    private final ImmutableList<String> states;
    private final ImmutableTable<String, String, IClipProvider> transitions;
    @SerializedName("start_state")
    private final String startState;

    private transient String currentStateName;
    private transient IClip currentState;
    private transient ClipLength currentTransition = null;
    private transient boolean transitioning = false;
    private transient float transitionStart = Float.MIN_VALUE;

    private static final LoadingCache<Pair<? extends IClip, Float>, IModelState> clipCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(100, TimeUnit.MILLISECONDS)
        .build(new CacheLoader<Pair<? extends IClip, Float>, IModelState>()
        {
            public IModelState load(Pair<? extends IClip, Float> key) throws Exception
            {
                IModelState state = Clips.apply(key.getLeft(), key.getRight());
                return state;
            }
        });

    protected AnimationStateMachine()
    {
        this(ImmutableMap.<String, IClip>of(), ImmutableList.<String>of(), ImmutableTable.<String, String, IClipProvider>of(), null);
    }

    public AnimationStateMachine(ImmutableMap<String, IClip> clipsRaw, ImmutableList<String> states, ImmutableTable<String, String, IClipProvider> transitions, String startState)
    {
        this.clipsRaw = clipsRaw;
        this.states = states;
        this.transitions = transitions;
        this.startState = startState;
    }

    /**
     * Post-loading initialization method. Resolves clip and parameter references.
     */
    public <P extends ITimeValue & IStringSerializable> void initialize(ImmutableMap<String, P> customParameters)
    {
        // FIXME: resolution of custom parameters
        // FIXME: deep resolution of clip references
        // resolving all name clip references
        ImmutableMap.Builder<String, IClip> builder = ImmutableMap.builder();
        for(Map.Entry<String, IClip> entry : clipsRaw.entrySet())
        {
            IClip clip = entry.getValue();
            while(clip instanceof ClipReference)
            {
                String name = ((ClipReference)clip).getName();
                clip = clipsRaw.get(name);
            }
            builder.put(entry.getKey(), clip);
        }
        clips = builder.build();
        // setting the starting state
        IClip state = clips.get(startState);
        if(!clips.containsKey(startState) || !states.contains(startState))
        {
            throw new IllegalStateException("unknown state: " + startState);
        }
        currentStateName = startState;
        currentState = state;
    }

    /**
     * Sample the state at the current time.
     */
    public IModelState apply(float time)
    {
        checkTransitionEnd(time);
        if(transitioning)
        {
            return clipCache.getUnchecked(Pair.of(currentTransition, time));
        }
        return clipCache.getUnchecked(Pair.of(currentState, time));
    }

    /**
     * Initiate transition to a new state.
     * If another transition is in progress, IllegalStateException is thrown.
     */
    public void transition(float currentTime, String newClip)
    {
        checkTransitionEnd(currentTime);
        System.out.println("transition " + currentTime + " " + newClip);
        if(transitioning)
        {
            throw new IllegalStateException("can't transition in a middle of another transition.");
        }
        IClip nc = clips.get(newClip);
        if(!clips.containsKey(newClip) || !states.contains(newClip))
        {
            throw new IllegalStateException("unknown state: " + newClip);
        }
        if(!transitions.contains(currentStateName, newClip))
        {
            throw new IllegalArgumentException("no transition from current clip to the clip " + newClip + " found.");
        }
        currentTransition = transitions.get(currentStateName, newClip).apply(currentTime);
        currentStateName = newClip;
        currentState = nc;
        transitionStart = currentTime;
        transitioning = true;
    }

    private void checkTransitionEnd(float time)
    {
        if(transitioning && time > transitionStart + currentTransition.getLength())
        {
            currentTransition = null;
            transitioning = false;
            System.out.println("transition end " + time);
        }
    }

    /**
     * Check if another transition is in progress.
     */
    public boolean transitioning()
    {
        return transitioning;
    }

    /**
     * Get the name of the current state.
     */
    public String currentState()
    {
        if(transitioning)
        {
            // FIXME
            return null;
        }
        return currentStateName;
    }
}
