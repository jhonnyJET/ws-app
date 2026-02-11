package infrastructure.resources.websocket;


import api.WsSessionApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.PersistentSession;
import infrastructure.resources.websocket.dto.ConnectionManager;
import infrastructure.utils.Host;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@jakarta.websocket.server.ServerEndpoint("/api/v1/websocket/{userId}")
@ApplicationScoped
public class LocationWs {


    @ConfigProperty(name = "max.connections.per.host")
    Integer MAX_CONNECTIONS_PER_HOST;

    @Inject
    WsSessionApi wsSessionApi;

    @Inject
    ConnectionManager connectionManager;

    private ObjectMapper objectMapper;

    private final Host host;


    public LocationWs(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.host = new Host();
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        Logger.getAnonymousLogger().log(Level.WARNING, "Ws OnOpen : " + session.getId());
        if(isMaxConnectionPerHostReached()) {
            throw new RuntimeException("Max Connections Limit Exceeded Exception");
        }
        host.getContainerHostname().subscribe().with(hostname -> {
            Logger.getAnonymousLogger().log(Level.WARNING, "Ws OnOpen Hostname : " + hostname);
            try {
                PersistentSession persistentSession = new PersistentSession(userId, session.getId(), hostname);            
                
                Logger.getAnonymousLogger().log(Level.WARNING, "Attempting to save WebSocket session to Redis: " + session.getId());
                wsSessionApi.save(session.getId(), persistentSession).subscribe().with(
                    unused -> Logger.getAnonymousLogger().log(Level.WARNING, "Successfully saved WebSocket session to Redis: " + session.getId()),
                    error -> Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to save WebSocket session to Redis", error)
                );
                connectionManager.add(session);
                Logger.getAnonymousLogger().log(Level.WARNING, "Successfully saved WebSocket session to Redis: " + session.getId());
                
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to save WebSocket session to Redis", e);
                throw e;
            }
        });
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        wsSessionApi.remove(session.getId()).subscribe().with( 
            unused -> Logger.getAnonymousLogger().log(Level.WARNING, "Successfully removed WebSocket session from Redis: " + session.getId()),
            error -> Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to remove WebSocket session from Redis", error)
        );
        connectionManager.remove(session);
    }

    @OnError
    public void onError(Session session, @PathParam("userId") String userId, Throwable throwable) {
        wsSessionApi.remove(session.getId()).subscribe().with(
                unused -> Logger.getAnonymousLogger().log(Level.WARNING, "Successfully removed WebSocket session from Redis: " + session.getId()),
                error -> Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to remove WebSocket session from Redis", error)
        );
        connectionManager.remove(session);
    }

    private Boolean isMaxConnectionPerHostReached() {
        return connectionManager.getTotalConnections() >= MAX_CONNECTIONS_PER_HOST;
    }
}
