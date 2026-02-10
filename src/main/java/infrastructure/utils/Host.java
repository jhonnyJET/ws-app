package infrastructure.utils;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Host {
    public Uni<String> getContainerHostname() {
        try {
            return Uni.createFrom().item(InetAddress.getLocalHost().getHostAddress()).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        } catch (UnknownHostException e) {
            return Uni.createFrom().item("unknown");
        }
    }

    public Uni<String> getContainerHostId() {
        try {
            return Uni.createFrom().item(InetAddress.getLocalHost().getHostAddress()).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        } catch (UnknownHostException e) {
            return Uni.createFrom().item("unknown");
        }
    }
}
