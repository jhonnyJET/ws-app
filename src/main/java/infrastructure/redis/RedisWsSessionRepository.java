package infrastructure.redis;

import api.WsSessionApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.PersistentSession;
import domain.WsSessionRepository;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class RedisWsSessionRepository implements WsSessionRepository {
   private static final TypeReference<PersistentSession> dsValueType = new TypeReference<>() {};
    private static final TypeReference<Map<String, Integer>> dsPubSubValueType = new TypeReference<>() {};
    private final ValueCommands<String, PersistentSession> valueCommands;
    private final ReactiveValueCommands<String, PersistentSession> reactiveValueCommands;
    private final ReactiveKeyCommands<String> reactiveWsSessionKeyCommands;
    private final KeyCommands<String> wsSessionKeyCommands;
    private final ReactivePubSubCommands<Map<String, Integer>> pubSubCommands;
    private final ObjectMapper objectMapper;

    @Inject
    WsSessionApi wsSessionApi;

    public RedisWsSessionRepository(RedisDataSource ds, ReactiveRedisDataSource reactiveDS, ObjectMapper objectMapper) {
        this.valueCommands = ds.value(dsValueType);
        this.reactiveValueCommands = reactiveDS.value(dsValueType);
        this.reactiveWsSessionKeyCommands = reactiveDS.key();
        this.wsSessionKeyCommands = ds.key();
        this.objectMapper = objectMapper;
        this.pubSubCommands = reactiveDS.pubsub(dsPubSubValueType);
    }

    @Override
    public Uni<Void> save(String id, PersistentSession session) {
        Logger.getAnonymousLogger().log(Level.WARNING, "Storing redis session");

        try {
            Logger.getAnonymousLogger().log(Level.WARNING, "Storing redis session : " + formattedKey(id) + " / " );
            Logger.getAnonymousLogger().log(Level.WARNING, objectMapper.writeValueAsString(session));
            return reactiveValueCommands.set(formattedKey(id), session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Uni<Void> remove(String id) {
        Logger.getAnonymousLogger().log(Level.WARNING, "Removing redis session: " + formattedKey(id));
        return reactiveWsSessionKeyCommands.del(formattedKey(id)).replaceWithVoid();
    }

    @Override
    public Map<String, Uni<PersistentSession>> findSessionMap(String user) {
//        return Map.of(user, reactiveValueCommands.get(formattedKey(user)));
        return Map.of();
    }

    @Override
    public List<PersistentSession> findAllSessions() {
        // Use the imperative API's key commands
        Iterable<String> keys = wsSessionKeyCommands.keys("WsSession#*"); // Caution: Use with care in production
        Map<String, PersistentSession> result = new HashMap<>();
        
        try {
            for (String key : keys) {
                var value = valueCommands.get(key);
                result.put(key, value);
                Logger.getAnonymousLogger().log(Level.WARNING, "Found session key: " + key + " with value: " + objectMapper.writeValueAsString(value));
            }
            Logger.getAnonymousLogger().log(Level.WARNING, "All sessions: " + result.toString());
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error processing JSON: " + e.getMessage(), e);
        }

        return List.copyOf(result.values());
    }

    void onStart(@Observes StartupEvent ev) {
        // subscribe(channel) returns a Multi<Message>
        Multi<Map<String, Integer>> messages = pubSubCommands.subscribe("drop-persistent-sessions");

        messages.subscribe().with(
                notification -> {
                    // This callback runs on an I/O thread.
                    // Do NOT perform blocking operations (like DB calls) here directly.
                    try {
                        Logger.getAnonymousLogger().log(Level.WARNING, "Received notification for user %s" +
                                  objectMapper.writeValueAsString(notification));
                        wsSessionApi.dropSessions(notification);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                },
                failure -> Logger.getAnonymousLogger().log(Level.SEVERE, "Subscriber failed", failure)
        );

        Logger.getAnonymousLogger().log(Level.INFO,"Redis Pub/Sub subscriber started.");
    }

    private String formattedKey(String key) {
        return String.format("%s#%s", "WsSession", key);
    }


}
