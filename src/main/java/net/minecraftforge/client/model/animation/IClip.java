package net.minecraftforge.client.model.animation;

import com.google.common.collect.UnmodifiableIterator;

/**
 * Clip for a rigged model.
 */
public interface IClip
{
    IJointClip apply(IJoint joint);

    UnmodifiableIterator<Event> pastEvents(float lastPollTime, float time);
}
