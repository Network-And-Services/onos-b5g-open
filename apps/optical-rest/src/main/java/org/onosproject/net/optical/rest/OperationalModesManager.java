package org.onosproject.net.optical.rest;

import org.onosproject.net.OperationalMode;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(immediate = true, service = OperationalModesManager.class)
public class OperationalModesManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<Integer, OperationalMode> operationalModesDB = new HashMap<Integer, OperationalMode>();

    @Activate
    protected void activate() {
        log.info(" OperationalModesManager has been STARTED");
    }

    @Deactivate
    protected void deactivate() {
        log.info(" OperationalModesManager has been STOPPED");
    }

    public void addToDatabase(OperationalMode opMode) {
        operationalModesDB.put(opMode.modeId, opMode);

        log.info("OperationalMode {} added to database", opMode.modeId);
    }

    //Da implementare attraverso un comando specifico ad esempio RemoveMatrixEntry
    public void removeFromDatabase(int opModeId) {
        operationalModesDB.remove(opModeId);

        log.info("OperationalMode {} removed to database", opModeId);
    }

    public OperationalMode getFromDatabase(int opModeId){
        return operationalModesDB.get(opModeId);
    }

    public boolean isRegisteredMode(int opModeId){
        return operationalModesDB.containsKey(opModeId);
    }

    public String getRegisteredModes() {
        /*String modes = "";
        if (!operationalModesDB.isEmpty()) {
            for (Map.Entry<Integer, OperationalMode> entry : operationalModesDB.entrySet()) {
                modes = modes + entry.getKey() + ", ";
            }
        }
        return modes;*/
        return operationalModesDB.keySet().toString();
    }
}
