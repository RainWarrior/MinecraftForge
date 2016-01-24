package net.minecraftforge.common.model.animation;

import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.animation.Event;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.UnmodifiableIterator;

/**
 * State machine representing the model animation.
 */
public interface IAnimationStateMachine
{
    /**
     * Sample the state at the current time.
     */
    Pair<IModelState, UnmodifiableIterator<Event>> apply(float time);

    /**
     * Transition to a new state.
     */
    void transition(String newState);

    /**
     * Get current state name.
     */
    String currentState();
}
