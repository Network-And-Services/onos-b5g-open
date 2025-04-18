package org.onosproject.net.optical.ocopmode;

import org.onosproject.net.OcOperationalMode;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(immediate = true, service = org.onosproject.net.optical.ocopmode.OcOperationalModesManager.class)
public class OcOperationalModesManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<Integer, org.onosproject.net.OcOperationalMode> operationalModesDB = new HashMap<Integer, org.onosproject.net.OcOperationalMode>();

    @Activate
    protected void activate() {
        log.info(" OperationalModesManager has been STARTED");
    }

    @Deactivate
    protected void deactivate() {
        log.info(" OperationalModesManager has been STOPPED");
    }

    public void addToDatabase(org.onosproject.net.OcOperationalMode opMode) {
        operationalModesDB.put(opMode.modeId, opMode);

        log.debug("OperationalMode {} added to database", opMode.modeId);
    }

    public void removeFromDatabase(int opModeId) {
        operationalModesDB.remove(opModeId);

        log.debug("OperationalMode {} removed to database", opModeId);
    }

    public OcOperationalMode getFromDatabase(int opModeId){
        return operationalModesDB.get(opModeId);
    }

    public boolean isRegisteredMode(int opModeId){
        return operationalModesDB.containsKey(opModeId);
    }

    public String getRegisteredModes() {
        return operationalModesDB.keySet().toString();
    }
}
