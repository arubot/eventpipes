package pw.aru.lib.eventpipes.internal;

import pw.aru.lib.eventpipes.api.*;
import pw.aru.lib.eventpipes.api.keyed.KeyedEventPipe;
import pw.aru.lib.eventpipes.api.keyed.KeyedEventPublisher;
import pw.aru.lib.eventpipes.api.keyed.KeyedEventSubscriber;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static pw.aru.lib.eventpipes.internal.Wrapper.wrapPublisher;
import static pw.aru.lib.eventpipes.internal.Wrapper.wrapSubscriber;

public class DefaultKeyedEventPipe<K, V> implements KeyedEventPipe<K, V> {
    private final EventExecutor executor;
    private final Map<K, EventPipe<V>> pipes;

    public DefaultKeyedEventPipe(EventExecutor executor) {
        this.executor = executor;
        this.pipes = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableFuture<Void> publish(K key, V value) {
        return pipeOf(key).publish(value);
    }

    @Override
    public EventSubscription<V> subscribe(K key, EventConsumer<V> consumer) {
        return pipeOf(key).subscribe(consumer);
    }

    private EventPipe<V> pipeOf(K key) {
        return pipes.computeIfAbsent(key, ignored -> new DefaultEventPipe<V>(executor) {
            @Override
            protected void onEmpty() {
                pipes.remove(key);
            }
        });
    }

    @Override
    public KeyedEventSubscriber<K, V> subscriber() {
        return wrapSubscriber(this);
    }

    @Override
    public KeyedEventPublisher<K, V> publisher() {
        return wrapPublisher(this);
    }

    @Override
    public EventPipe<V> pipe(K key) {
        return new Pipe(key);
    }

    @Override
    public EventSubscriber<V> subscriber(K key) {
        return wrapSubscriber(pipe(key));
    }

    @Override
    public EventPublisher<V> publisher(K key) {
        return wrapPublisher(pipe(key));
    }

    class Pipe implements EventPipe<V> {
        private final K key;

        Pipe(K key) {
            this.key = key;
        }

        @Override
        public CompletableFuture<Void> publish(V event) {
            return DefaultKeyedEventPipe.this.publish(key, event);
        }

        @Override
        public EventSubscription<V> subscribe(EventConsumer<V> consumer) {
            return DefaultKeyedEventPipe.this.subscribe(key, consumer);
        }

        @Override
        public EventSubscriber<V> subscriber() {
            return wrapSubscriber(this);
        }

        @Override
        public EventPublisher<V> publisher() {
            return wrapPublisher(this);
        }
    }
}
