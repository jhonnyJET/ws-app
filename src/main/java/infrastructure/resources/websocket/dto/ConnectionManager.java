package infrastructure.resources.websocket.dto;

import infrastructure.utils.Host;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ConnectionManager {

    private final Host host;
    private final Map<String, PersistentConnection> activeSessions = new ConcurrentHashMap<>();

    public ConnectionManager() {
        this.host = new Host();
    }

    public List<String> getSessionMap() {
        return  activeSessions.keySet().stream().toList();
    }

    public void add(Session session) {
        activeSessions.put(session.getId(), new PersistentConnection(session));
    }

    public void remove(Session session) {
        activeSessions.remove(session.getId());
    }

    public void recordActivity(Session session) {
        PersistentConnection conn = activeSessions.get(session.getId());
        if (conn != null) {
            conn.updateActivity();
        }
    }

    /**
     * CORE LOGIC: Drops a number of connections, prioritizing idle users.
     */
    private int findVictimsAndDropSessions(Integer targetToDrop){
        if (targetToDrop <= 0) return 0;

        // 1. Sort users by "Most Idle" (Oldest lastActive time first)
        List<PersistentConnection> victims = activeSessions.values().stream()
                                                           .sorted(Comparator.comparingLong(PersistentConnection::getLastActive))
                                                           .limit(targetToDrop)
                                                           .toList();

        // 2. Define our custom "Rebalance" Close Code (4001)
        CloseReason reason = new CloseReason(
                CloseReason.CloseCodes.getCloseCode(4001),
                "Server Rebalancing"
        );

        // 3. Execute Order 66
        int droppedCount = 0;
        for (PersistentConnection victim : victims) {
            try {
                // Async close prevents blocking the main thread
                victim.getSession().close(reason);
                droppedCount++;
            } catch (IOException e) {
                // Handle edge case where session is already closed
            }
        }

        return droppedCount;

    }

    public void shedLoad(String targetHostId, Integer targetToDrop) {
        host.getContainerHostId().subscribe().with(host -> {
            var isTargetHostIdEqualToThisHostId = host.equals(targetHostId);
            if (isTargetHostIdEqualToThisHostId) {
                var victimsDropped = findVictimsAndDropSessions(targetToDrop);
                Logger.getAnonymousLogger().log(Level.INFO, "Victims Dropped: " + victimsDropped);
            }
        });
    }

    public int getTotalConnections() {
        return activeSessions.size();
    }
}
