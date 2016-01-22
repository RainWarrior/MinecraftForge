package net.minecraftforge.client.model.animation;

import java.util.concurrent.TimeUnit;

import net.minecraftforge.client.model.IModelState;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.UnmodifiableIterator;
import com.google.gson.annotations.SerializedName;

/**
 * Main controller for the animation logic.
 * API is a simple state machine.
 */
public class AnimationStateMachine
{
    private final ImmutableMap<String, ITimeValue> parameters;
    private final ImmutableMap<String, IClip> clips;
    private final ImmutableList<String> states;
    private final ImmutableTable<String, String, IClipProvider> transitions;
    @SerializedName("start_state")
    private final String startState;

    private transient String currentStateName;
    private transient IClip currentState;
    private transient ClipLength currentTransition = null;
    private transient boolean transitioning = false;
    private transient float transitionStart = Float.MIN_VALUE;
    private transient float lastPollTime = Float.NEGATIVE_INFINITY;

    private static final LoadingCache<Triple<? extends IClip, Float, Float>, Pair<IModelState, UnmodifiableIterator<Event>>> clipCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(100, TimeUnit.MILLISECONDS)
        .build(new CacheLoader<Triple<? extends IClip, Float, Float>, Pair<IModelState, UnmodifiableIterator<Event>>>()
        {
            public Pair<IModelState, UnmodifiableIterator<Event>> load(Triple<? extends IClip, Float, Float> key) throws Exception
            {
                return Clips.apply(key.getLeft(), key.getMiddle(), key.getRight());
            }
        });

    protected AnimationStateMachine()
    {
        this(ImmutableMap.<String, ITimeValue>of(), ImmutableMap.<String, IClip>of(), ImmutableList.<String>of(), ImmutableTable.<String, String, IClipProvider>of(), null);
    }

    public AnimationStateMachine(ImmutableMap<String, ITimeValue> parameters, ImmutableMap<String, IClip> clips, ImmutableList<String> states, ImmutableTable<String, String, IClipProvider> transitions, String startState)
    {
        this.parameters = parameters;
        this.clips = clips;
        this.states = states;
        this.transitions = transitions;
        this.startState = startState;
    }

    /**
     * Used during resolution of parameter references.
     */
    ImmutableMap<String, ITimeValue> getParameters()
    {
        return parameters;
    }

    /**
     * Used during resolution of clip references.
     */
    ImmutableMap<String, IClip> getClips()
    {
        return clips;
    }

    /**
     * Post-loading initialization method.
     */
    public void initialize()
    {
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
    public Pair<IModelState, UnmodifiableIterator<Event>> apply(float time)
    {
        if(lastPollTime == Float.NEGATIVE_INFINITY)
        {
            lastPollTime = time;
        }
        checkTransitionEnd(time);
        Pair<IModelState, UnmodifiableIterator<Event>> pair;
        if(transitioning)
        {
            pair = clipCache.getUnchecked(Triple.of(currentTransition, lastPollTime, time));
        }
        else
        {
            pair = clipCache.getUnchecked(Triple.of(currentState, lastPollTime, time));
        }
        lastPollTime = time;
        return pair;
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
