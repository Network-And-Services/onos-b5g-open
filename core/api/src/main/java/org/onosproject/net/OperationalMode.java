package org.onosproject.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.slf4j.Logger;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class OperationalMode {
    public int modeId;
    public String modeType;
    public ObjectNode opModeCaps = new ObjectMapper().createObjectNode();
    public ObjectNode opModeCapsFec = new ObjectMapper().createObjectNode();
    public ArrayNode opModeCapsPenalties = new ObjectMapper().createArrayNode();
    public ObjectNode opModeCapsFilter = new ObjectMapper().createObjectNode();
    public ObjectNode opModeCapsConstraints = new ObjectMapper().createObjectNode();

    private static final Logger log = getLogger(OperationalMode.class);

    private static final Set<String> keysOpModeCaps = new HashSet<>(Arrays.asList(
            //operational-mode-capabilities.state
            "modulation-format", "bit-rate", "baud-rate", "optical-channel-spectrum-width",
            "min-tx-osnr", "min-rx-osnr", "min-input-power", "max-input-power", "max-chromatic-dispersion",
            "max-differential-group-delay", "max-polarization-dependent-loss"));

    private static final Set<String> keysOpModeCapsFec = new HashSet<>(Arrays.asList(
            //operational-mode-capabilities.fec.state
            "fec-coding", "coding-overhead", "coding-gain", "pre-fec-ber-threshold"));

    private static final Set<String> keysOpModeCapsPenalty = new HashSet<>(Arrays.asList(
            //operational-mode-capabilities.penalties.penalty.state
            "parameter-and-unit", "up-to-boundary", "penalty-value"));

    private static final Set<String> keysOpModeCapsFilter = new HashSet<>(Arrays.asList(
            //operational-mode-capabilities.filter.state
            "pulse-shaping-type"));

    private static final Set<String> keysOpModeCapsConstraints = new HashSet<>(Arrays.asList(
            //optical-channel-config-value-constraints.state
            "min-central-frequency", "max-central-frequency", "grid-type", "adjustment-granularity",
            "min-channel-spacing", "min-output-power", "max-output-power"));

    public OperationalMode(int id, String type) {
        modeId = id;
        modeType = type;

        log.info("New OperationalMode created with id {}", modeId);
    }

    public OperationalMode() {
        Random random = new Random();

        modeId = random.nextInt(9999);
        modeType = "TRANSCEIVER_MODE_TYPE_EXPLICIT";

        for (String s : keysOpModeCaps) {
            // Esegue un'azione specifica su ogni elemento di tipo stringa
            addOpModeCaps(s, String.valueOf(random.nextInt(100)));
        }

        for (String s : keysOpModeCapsFec) {
            // Esegue un'azione specifica su ogni elemento di tipo stringa
            addOpModeCaps(s, String.valueOf(random.nextInt(100)));
        }

        for (String s : keysOpModeCapsFilter) {
            // Esegue un'azione specifica su ogni elemento di tipo stringa
            addOpModeCaps(s, String.valueOf(random.nextInt(100)));
        }

        for (String s : keysOpModeCapsConstraints) {
            // Esegue un'azione specifica su ogni elemento di tipo stringa
            addOpModeCaps(s, String.valueOf(random.nextInt(100)));
        }

        addOpModeCapsPenalty("CD_PS_NM", "800.0", "10.0");
        addOpModeCapsPenalty("PMD_PS", "800.0", "10.0");
        addOpModeCapsPenalty("PDL_DB", "800.0", "10.0");

        log.info("New OperationalMode created with id {}", modeId);
    }

    public boolean addOpModeCaps(String key, String value) {

        log.info("OpMode {} adding leaf  {}-{}", modeId, key, value);

        if (keysOpModeCaps.contains(key)) {
            opModeCaps.put(key, value);
            return true;
        }
        if (keysOpModeCapsFec.contains(key)) {
            opModeCapsFec.put(key, value);
            return true;
        }
        if (keysOpModeCapsFilter.contains(key)) {
            opModeCapsFilter.put(key, value);
            return true;
        }
        if (keysOpModeCapsConstraints.contains(key)) {
            opModeCapsConstraints.put(key, value);
            return true;
        }

        log.error("OperationalMode {} added key is not supported {}", modeId, key);
        return false;
    }

    public boolean addOpModeCapsPenalty(String parameterAndUnit, String upToBoundary, String penaltyValue) {

        log.info("OpMode {} adding penalty sub-object {}-{}", modeId,
                "parameter-and-unit", parameterAndUnit,
                "up-to-boundary", upToBoundary,
                "penalty-value", penaltyValue);

        ObjectNode penalty = new ObjectMapper().createObjectNode();

        penalty.put("parameter-and-unit", parameterAndUnit);
        penalty.put("up-to-boundary", upToBoundary);
        penalty.put("penalty-value", penaltyValue);

        opModeCapsPenalties.add(penalty);

        return true;
    }

    public ObjectNode encode() {
        ObjectNode ocOperationalMode = new ObjectMapper().createObjectNode();

        ocOperationalMode.put("mode-id", modeId);
        ocOperationalMode.put("mode-type", modeType);
        ocOperationalMode.put("operational-mode-capabilities", opModeCaps);
        ocOperationalMode.put("fec", opModeCapsFec);
        ocOperationalMode.put("filter", opModeCapsFilter);
        ocOperationalMode.put("optical-channel-config-value-constraints", opModeCapsConstraints);
        ocOperationalMode.put("penalties", opModeCapsPenalties);

        return ocOperationalMode;
    }

    static public OperationalMode decodeFromJson(ObjectNode node) {

        OperationalMode opMode = new OperationalMode(node.get("mode-id").asInt(),node.get("mode-type").asText());

        JsonNode opModeCapsJson = node.get("operational-mode-capabilities");
        for (String key : keysOpModeCaps) {
            opMode.addOpModeCaps(key, opModeCapsJson.get(key).asText());
        }

        JsonNode opModeCapsFecJson = node.get("fec");
        for (String key : keysOpModeCapsFec) {
            opMode.addOpModeCaps(key, opModeCapsFecJson.get(key).asText());
        }

        JsonNode opModeCapsFilterJson = node.get("filter");
        for (String key : keysOpModeCapsFilter) {
            opMode.addOpModeCaps(key, opModeCapsFilterJson.get(key).asText());
        }

        JsonNode opModeCapsConstraintsJson = node.get("optical-channel-config-value-constraints");
        for (String key : keysOpModeCapsConstraints) {
            opMode.addOpModeCaps(key, opModeCapsConstraintsJson.get(key).asText());
        }

        JsonNode opModePenaltiesJson = node.get("penalties");
        for (JsonNode penalty : opModePenaltiesJson) {
            opMode.addOpModeCapsPenalty(
                    penalty.get("parameter-and-unit").asText(),
                    penalty.get("up-to-boundary").asText(),
                    penalty.get("penalty-value").asText());
        }

        return opMode;
    }

    static public OperationalMode decodeFromXml(HierarchicalConfiguration mode) {
        int id = Integer.decode(mode.getString("state/mode-id"));
        String type = mode.getString("state/mode-type");

        OperationalMode opMode = new OperationalMode(id, type);

        log.info("STEP 1 keysOpModeCaps");
        for (String key : keysOpModeCaps) {
            opMode.addOpModeCaps(key, mode.getString("explicit-mode/operational-mode-capabilities/state/" + key));
        }

        log.info("STEP 2 keysOpModeCapsFec");
        for (String key : keysOpModeCapsFec) {
            opMode.addOpModeCaps(key, mode.getString("explicit-mode/operational-mode-capabilities/fec/state/" + key));
        }

        log.info("STEP 3 keysOpModeCapsFilter");
        for (String key : keysOpModeCapsFilter) {
            opMode.addOpModeCaps(key, mode.getString("explicit-mode/operational-mode-capabilities/filter/state/" + key));
        }

        log.info("STEP 4 keysOpModeCapsConstraints");
        for (String key : keysOpModeCapsConstraints) {
            opMode.addOpModeCaps(key, mode.getString("explicit-mode/optical-channel-config-value-constraints/state/" + key));
        }

        //penalties
        log.info("STEP 5 penalties");
        HierarchicalConfiguration penalties = mode.configurationAt("explicit-mode/operational-mode-capabilities/penalties");
        List<HierarchicalConfiguration> listPenalties = penalties.configurationsAt("penalty");

        for (HierarchicalConfiguration penalty : listPenalties) {
            //log.info("--- added penalty subfield.");

            opMode.addOpModeCapsPenalty(
                    penalty.getString("state/parameter-and-unit"),
                    penalty.getString("state/up-to-boundary"),
                    penalty.getString("state/penalty-value"));
        }

        return opMode;
    }
}
