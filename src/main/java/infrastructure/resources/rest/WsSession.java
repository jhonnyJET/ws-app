package infrastructure.resources.rest;

import api.WsSessionApi;
import domain.PersistentSession;
import infrastructure.resources.websocket.dto.ConnectionManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/ws-session")
public class WsSession {

    @Inject
    private WsSessionApi api;

    @Inject
    ConnectionManager connectionManager;


    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PersistentSession> getAllSessions() {
        return api.findAllSessions();
    }

    @GET
    @Path("all/runtime")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getRuntimeSessions() {
        return connectionManager.getSessionMap();
    }

    @POST
    @Path("{hostId}/drop")
    @Produces(MediaType.APPLICATION_JSON)
    public void dropSessions(@PathParam("hostId") String hostId) {
        connectionManager.shedLoad(hostId, 1);
    }


}
