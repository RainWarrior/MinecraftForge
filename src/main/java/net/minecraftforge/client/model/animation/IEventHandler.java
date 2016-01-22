package net.minecraftforge.client.model.animation;

import com.google.common.collect.UnmodifiableIterator;

/**
 * Handler for animation events;
 */
public interface IEventHandler<T>
{
    void handleEvents(T instance, float time, UnmodifiableIterator<Event> pastEvents);
}
