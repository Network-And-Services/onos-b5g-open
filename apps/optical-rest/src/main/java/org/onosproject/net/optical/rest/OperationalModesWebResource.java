package org.onosproject.net.optical.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Query OpenConfig operational modes.
 */
@Path("opmodes")
public class OperationalModesWebResource extends AbstractWebResource {
    /**
     * Submits a new operational mode from JSON.
     * Creates and submits a new operational mode from the JSON request.
     *
     * @param stream input JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel CreateOpMode
     */
    @POST
    @Path("opMode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIntent(InputStream stream) {
        OperationalModesManager manager = get(OperationalModesManager.class);

        try {
            ObjectNode root = readTreeFromStream(mapper(), stream);
            OperationalMode opmode = OperationalMode.decodeFromJson(root);

            manager.addToDatabase(opmode);

            return Response.ok().build();
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    /**
     * Submits a new operational mode empty.
     * Creates and submits a new operational mode from the JSON request.
     *
     * @param id integer id key of mode
     * @param type string see model
     * @return ok
     */
    @POST
    @Path("opModeEmpty")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postOpMode(@QueryParam("id") String id,
                               @QueryParam("type") String type) {
        OperationalModesManager manager = get(OperationalModesManager.class);

        OperationalMode mode = new OperationalMode(Integer.decode(id), type);

        manager.addToDatabase(mode);

        ObjectNode root = mapper().createObjectNode();
        return Response.ok(root).build();
    }

    /**
     * Submits a new operational mode random generation.
     * Creates and submits a new operational mode from the JSON request.
     *
     * @return ok and JSON of built mode
     */
    @POST
    @Path("opModeRandom")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postOpModeRandom() {
        OperationalModesManager manager = get(OperationalModesManager.class);

        OperationalMode mode = new OperationalMode();

        manager.addToDatabase(mode);

        return Response.ok(mode.encode()).build();
    }

    /**
     * Get indexes of registered operational modes.
     *
     * @return ok
     */
    @GET
    @Path("opModes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableOperationalModes() {
        OperationalModesManager manager = get(OperationalModesManager.class);

        ObjectNode objectNode = mapper().createObjectNode();
        objectNode.put("available-op-modes", manager.getRegisteredModes());

        return ok(objectNode).build();
    }

    /**
     * Get description of specified operational mode.
     *
     * @param id integer id key of mode
     * @return ok and JSON description of operational mode.
     */
    @GET
    @Path("opMode")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperationalMode(@QueryParam("id") String id) {
        OperationalModesManager manager = get(OperationalModesManager.class);

        OperationalMode opMode = manager.getFromDatabase(Integer.decode(id));

        return ok(opMode.encode()).build();
    }

    /**
     * Delete the specified operational mode.
     *
     * @param id integer id key of mode
     * @return ok
     */
    @DELETE
    @Path("opMode")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteOperationalMode(@QueryParam("id") String id) {
        OperationalModesManager manager = get(OperationalModesManager.class);

        manager.removeFromDatabase(Integer.valueOf(id));

        return Response.ok().build();
    }
}
