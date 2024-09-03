package org.onosproject.net.optical.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.graph.ScalarWeight;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.Direction;
import org.onosproject.net.GridType;
import org.onosproject.net.Link;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.OchSignal;
import org.onosproject.net.behaviour.ModulationConfig;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.optical.json.OchSignalCodec;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

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
import java.util.ArrayList;
import java.util.List;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.onosproject.net.optical.util.OpticalIntentUtility.createExplicitOpticalIntent;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query OpenConfig operational modes.
 */
@Path("opmodes")
public class OperationalModesWebResource extends AbstractWebResource {

    private static final Logger log = getLogger(OpticalIntentsWebResource.class);

    private static final String JSON_INVALID = "Invalid json input: ";
    private static final String INGRESS_POINT = "ingressPoint";
    private static final String EGRESS_POINT = "egressPoint";
    private static final String MISSING_MEMBER_MESSAGE = " member is required";

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
     * Configure the specified operational mode on a pair of transceivers from JSON request.
     *
     * @return ok
     * @onos.rsModel ConfigureOpMode
     */
    @POST
    @Path("opModeConfigure")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response configureOpMode(InputStream stream) {
        try {
            ObjectNode root = readTreeFromStream(mapper(), stream);
            decodeOpMode(root);

            ObjectNode response = mapper().createObjectNode();
            return Response.ok(response).build();
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    /**
     * Configure the specified output power on a pair of transceivers from JSON request.
     *
     * @return ok
     * @onos.rsModel ConfigureOutputPower
     */
    @POST
    @Path("outputPowerConfigure")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response configureOutputPower(InputStream stream) {
        try {
            ObjectNode root = readTreeFromStream(mapper(), stream);
            decodeOutputPower(root);

            ObjectNode response = mapper().createObjectNode();
            return Response.ok(response).build();
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
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

    private void decodeOpMode(ObjectNode json) {
        JsonNode ingressJson = json.get(INGRESS_POINT);
        if (!ingressJson.isObject()) {
            log.error(JSON_INVALID + "ingress node");
            throw new IllegalArgumentException(JSON_INVALID + "ingress node");
        }

        ConnectPoint ingress = codec(ConnectPoint.class).decode((ObjectNode) ingressJson, this);

        JsonNode egressJson = json.get(EGRESS_POINT);
        if (!egressJson.isObject()) {
            log.error(JSON_INVALID + "egress node");
            throw new IllegalArgumentException(JSON_INVALID + "egress node");
        }

        ConnectPoint egress = codec(ConnectPoint.class).decode((ObjectNode) egressJson, this);

        String opModeId = nullIsIllegal(json.get("opModeId"), "OpModeId" + MISSING_MEMBER_MESSAGE).asText();
        log.warn("Received operational mode configuration request {} from {} to {}", opModeId, ingress, egress);

        //Retrieve the modulation format from the specified Operational Mode ID
        OperationalModesManager manager = get(OperationalModesManager.class);
        OperationalMode opMode = manager.getFromDatabase(Integer.decode(opModeId));

        if (opMode == null) {
            log.error(JSON_INVALID + "operational mode not defined");
            throw new IllegalArgumentException(JSON_INVALID + "operational mode not defined");
        }

        ModulationScheme modulationScheme = null;
        String opModeModulation = opMode.opModeCaps.get("modulation-format").asText();
        log.warn("Received operational mode {} correspond to modulation {}", opModeId, opModeModulation);

        if (opModeModulation.equals("MODULATION_FORMAT_DP_QPSK")) {
            modulationScheme = ModulationScheme.DP_QPSK;
            log.warn("Selected modulation scheme {}", modulationScheme);
        }
        if (opModeModulation.equals("MODULATION_FORMAT_DP_8QAM")) {
            modulationScheme = ModulationScheme.DP_8QAM;
            log.warn("Selected modulation scheme {}", modulationScheme);
        }
        if (opModeModulation.equals("MODULATION_FORMAT_DP_16QAM")) {
            modulationScheme = ModulationScheme.DP_16QAM;
            log.warn("Selected modulation scheme {}", modulationScheme);
        }
        if (modulationScheme == null) {
            log.error(JSON_INVALID + "modulation format not supported");
            throw new IllegalArgumentException(JSON_INVALID + "modulation format not supported");
        }

        //Retrieve source and destination devices
        DeviceService deviceService = get(DeviceService.class);
        Device srcDevice = deviceService.getDevice(ingress.deviceId());
        Device dstDevice = deviceService.getDevice(egress.deviceId());

        if (srcDevice == null) {
            log.error("source device does not exist");
            throw new IllegalArgumentException(JSON_INVALID + "source device does not exist");
        }

        if (dstDevice == null) {
            log.error("destination device does not exist");
            throw new IllegalArgumentException(JSON_INVALID + "destination device does not exist");
        }

        //Configuring SOURCE device
        if (srcDevice.is(ModulationConfig.class)) {
            log.warn("Going to set OPERATIONAL MODE {} on SRC device {}", opModeId, srcDevice.id());
            ModulationConfig<Object> modulationConfig = srcDevice.as(ModulationConfig.class);
            modulationConfig.setModulationScheme(ingress.port(), Direction.ALL, modulationScheme);
        } else {
            log.error("SRC device is not modulation config");
            throw new IllegalArgumentException(JSON_INVALID);
        }

        //Configuring DESTINATION device
        if (dstDevice.is(ModulationConfig.class)) {
            log.warn("Going to set OPERATIONAL MODE {} on DST device {}", opModeId, dstDevice.id());
            ModulationConfig<Object> modulationConfig = dstDevice.as(ModulationConfig.class);
            modulationConfig.setModulationScheme(egress.port(), Direction.ALL, modulationScheme);
        } else {
            log.error("DST device is not modulation config");
            throw new IllegalArgumentException(JSON_INVALID);
        }
    }


    private void decodeOutputPower(ObjectNode json) {
        JsonNode ingressJson = json.get(INGRESS_POINT);
        if (!ingressJson.isObject()) {
            log.error(JSON_INVALID + "ingress node");
            throw new IllegalArgumentException(JSON_INVALID + "ingress node");
        }

        ConnectPoint ingress = codec(ConnectPoint.class).decode((ObjectNode) ingressJson, this);

        JsonNode egressJson = json.get(EGRESS_POINT);
        if (!egressJson.isObject()) {
            log.error(JSON_INVALID + "egress node");
            throw new IllegalArgumentException(JSON_INVALID + "egress node");
        }

        ConnectPoint egress = codec(ConnectPoint.class).decode((ObjectNode) egressJson, this);

        String outputPower = nullIsIllegal(json.get("outputPower"), "outputPower" + MISSING_MEMBER_MESSAGE).asText();
        log.warn("Received output power request {} dBm from {} to {}", outputPower, ingress, egress);

        Double outputTargetPower = Double.valueOf(outputPower);
        if (outputTargetPower == null) {
            log.error(JSON_INVALID + "output power");
            throw new IllegalArgumentException(JSON_INVALID + "output power");
        }

        log.warn("Received output power request {} dBm from {} to {}", outputTargetPower, ingress, egress);

        //Retrieve source and destination devices
        DeviceService deviceService = get(DeviceService.class);
        Device srcDevice = deviceService.getDevice(ingress.deviceId());
        Device dstDevice = deviceService.getDevice(egress.deviceId());

        if (srcDevice == null) {
            log.error("source device does not exist");
            throw new IllegalArgumentException(JSON_INVALID + "source device does not exist");
        }

        if (dstDevice == null) {
            log.error("destination device does not exist");
            throw new IllegalArgumentException(JSON_INVALID + "destination device does not exist");
        }

        //Configuring SOURCE device
        if (srcDevice.is(PowerConfig.class)) {
            log.warn("Going to set target output power {} on SRC device {}", outputTargetPower, srcDevice.id());
            PowerConfig<Object> powerConfig = srcDevice.as(PowerConfig.class);
            powerConfig.setTargetPower(ingress.port(), Direction.ALL, outputTargetPower);
        } else {
            log.error("SRC device is not power config");
            throw new IllegalArgumentException(JSON_INVALID);
        }

        //Configuring DESTINATION device
        if (dstDevice.is(ModulationConfig.class)) {
            log.warn("Going to set output power {} on DST device {}", outputTargetPower, dstDevice.id());
            PowerConfig<Object> powerConfig = dstDevice.as(PowerConfig.class);
            powerConfig.setTargetPower(egress.port(), Direction.ALL, outputTargetPower);
        } else {
            log.error("DST device is not power config");
            throw new IllegalArgumentException(JSON_INVALID);
        }
    }
}
