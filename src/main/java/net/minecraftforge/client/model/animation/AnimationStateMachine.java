package net.minecraftforge.client.model.animation;

import java.util.concurrent.TimeUnit;

import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.gson.annotations.SerializedName;

class AnimationStateMachine implements IAnimationStateMachine
{
    private final ImmutableMap<String, ITimeValue> parameters;
    private final ImmutableMap<String, IClip> clips;
    private final ImmutableList<String> states;
    private final ImmutableMap<String, String> transitions;
    @SerializedName("start_state")
    private final String startState;

    private transient String currentStateName;
    private transient IClip currentState;
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

    public AnimationStateMachine(ImmutableMap<String, ITimeValue> parameters, ImmutableMap<String, IClip> clips, ImmutableList<String> states, ImmutableMap<String, String> transitions, String startState)
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
     * post-loading initialization hook.
     */
    void initialize()
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

    public Pair<IModelState, UnmodifiableIterator<Event>> apply(float time)
    {
        if(lastPollTime == Float.NEGATIVE_INFINITY)
        {
            lastPollTime = time;
        }
        Pair<IModelState, UnmodifiableIterator<Event>> pair = clipCache.getUnchecked(Triple.of(currentState, lastPollTime, time));
        lastPollTime = time;
        ImmutableList<Event> events = ImmutableList.copyOf(pair.getRight());
        boolean shouldFilter = false;
        for(Event event : events.reverse())
        {
            if(event.event().startsWith("!"))
            {
                shouldFilter = true;
                if(event.event().startsWith("!transition:"))
                {
                    String newState = event.event().substring("!transition:".length());
                    transition(newState);
                }
                else
                {
                    System.out.println("Unknown special event \"" + event.event() + "\", ignoring");
                }
            }
        }
        if(!shouldFilter)
        {
            return Pair.of(pair.getLeft(), events.reverse().iterator());
        }
        return Pair.of(pair.getLeft(), Iterators.filter(pair.getRight(), new Predicate<Event>()
        {
            public boolean apply(Event event)
            {
                return !event.event().startsWith("!");
            }
        }));
    }

    public void transition(String newState)
    {
        System.out.println("transition " + newState);
        IClip nc = clips.get(newState);
        if(!clips.containsKey(newState) || !states.contains(newState))
        {
            throw new IllegalStateException("unknown state: " + newState);
        }
        if(!transitions.get(currentStateName).equals(newState))
        {
            throw new IllegalArgumentException("no transition from current clip to the clip " + newState + " found.");
        }
        currentStateName = newState;
        currentState = nc;
    }

    public String currentState()
    {
        return currentStateName;
    }
}
