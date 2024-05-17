package org.onosproject.drivers.odtn.openconfig;


import org.onosproject.driver.optical.query.MultiBandLambdaQuery;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Example lambda query for multi-band optical devices.
 */
public class MultiBandBerlinLambdaQuery extends MultiBandLambdaQuery {

    protected static final Logger log = getLogger(MultiBandLambdaQuery.class);

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        log.debug("OPENCONFIG: queried lambdas for port {}", port);

        channelSpacing = ChannelSpacing.CHL_50GHZ;
        slotGranularity = 4;

        //Generates 10 channels on each band, each channel is 50 GHz width
        lBandLambdaCount = 120;
        cBandLambdaCount = 80;
        sBandLambdaCount = 160;
        oBandLambdaCount = 0;

        return super.queryLambdas(port);
    }
}

