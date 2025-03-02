package dev.sora.relay.game.event

import dev.sora.relay.utils.logError

class EventManager {

    private val registry = mutableMapOf<Class<out GameEvent>, ArrayList<EventHook<in GameEvent>>>()

    fun register(hook: EventHook<out GameEvent>) {
        val handlers = registry.computeIfAbsent(hook.eventClass) { ArrayList() }

		handlers.add(hook as EventHook<in GameEvent>)
    }

//    fun register(listenable: Listenable) {
//        listenable.listeners.forEach(this::register)
//    }

    inline fun <reified T : GameEvent> listenNoCondition(noinline handler: Handler<T>) {
        register(EventHook(T::class.java, handler) as EventHook<in GameEvent>)
    }

    fun emit(event: GameEvent) {
        for (handler in (registry[event.javaClass] ?: return)) {
            try {
				if (handler.condition()) {
					handler.handler(event)
				}
            } catch (t: Throwable) {
                logError("event ${event.friendlyName}", t)
            }
        }
    }
}
