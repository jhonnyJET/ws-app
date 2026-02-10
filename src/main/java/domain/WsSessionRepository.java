package domain;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;

public interface WsSessionRepository {
    Uni<Void> save(String id, PersistentSession session);
    Uni<Void> remove(String id);
    Map<String, Uni<PersistentSession>> findSessionMap(String user);
    List<PersistentSession> findAllSessions();
}
