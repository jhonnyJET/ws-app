package infrastructure.resources.websocket.dto;

import jakarta.websocket.Session;

import java.util.concurrent.atomic.AtomicLong;

public class PersistentConnection {

    private final Session session;
    private final AtomicLong lastActive;


    public PersistentConnection(Session session) {
        this.session = session;
        this.lastActive = new AtomicLong(System.currentTimeMillis());
    }

    public Session getSession() {
        return session;
    }

    public long getLastActive() {
        return lastActive.get();
    }

    public void updateActivity() {
        this.lastActive.set(System.currentTimeMillis());
    }
}
