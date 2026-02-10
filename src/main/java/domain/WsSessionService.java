package domain;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class WsSessionService {

    WsSessionRepository wsSessionRepository;

    public WsSessionService(WsSessionRepository wsSessionRepository) {
        this.wsSessionRepository = wsSessionRepository;
    }

    public Map<String, Uni<PersistentSession>> findSession(String user) {
        return wsSessionRepository.findSessionMap(user);
    }

    public List<PersistentSession> findAllSessions() {
        return wsSessionRepository.findAllSessions();
    }

    public Uni<Void> save(String id, PersistentSession session) {
         return wsSessionRepository.save(id, session);
    }

    public Uni<Void> remove(String id) {
         return wsSessionRepository.remove(id);
    }
}
