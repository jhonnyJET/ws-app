package api;

import domain.PersistentSession;
import domain.WsSessionService;
import infrastructure.resources.websocket.dto.ConnectionManager;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class WsSessionApi {

    @Inject
    ConnectionManager connectionManager;

    WsSessionService wsSessionService;

    public WsSessionApi(WsSessionService wsSessionService) {
        this.wsSessionService = wsSessionService;
    }

    public Map<String, Uni<PersistentSession>> findSession(String user) {
        return wsSessionService.findSession(user);
    }

    public List<PersistentSession> findAllSessions() {
        return wsSessionService.findAllSessions();
    }

    public Uni<Void> save(String id, PersistentSession session) {
        return wsSessionService.save(id, session);
    }

    public void dropSessions(Map<String, Integer> sessionsToDropMap) {
        sessionsToDropMap.forEach((key, value) -> connectionManager.shedLoad(key, value));
    }

    public Uni<Void> remove(String id) {
        return wsSessionService.remove(id);
    }    
}
