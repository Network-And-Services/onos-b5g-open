package org.onosproject.drivers.odtn.openconfig;

import org.onosproject.driver.optical.query.MultiBandLambdaQuery;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class MultiBandCplusObands extends MultiBandLambdaQuery {

    protected static final Logger log = getLogger(MultiBandLambdaQuery.class);

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {

        channelSpacing = ChannelSpacing.CHL_50GHZ;
        slotGranularity = 4;

        if (port.toString().equals("30") || port.toString().equals("31") || port.toString().equals("32")) {
            //log.info("OPENCONFIG: queried lambdas O for port {}", port);
            lBandLambdaCount = 0;
            cBandLambdaCount = 0;
            sBandLambdaCount = 0;
            oBandLambdaCount = 80;

            return super.queryLambdas(port);
        }

        if (port.toString().equals("10") || port.toString().equals("11") || port.toString().equals("12")) {
            //log.info("OPENCONFIG: queried lambdas C for port {}", port);
            lBandLambdaCount = 0;
            cBandLambdaCount = 88;
            sBandLambdaCount = 0;
            oBandLambdaCount = 0;

            return super.queryLambdas(port);
        }

        //log.info("OPENCONFIG: queried lambdas O+C for port {}", port);
        lBandLambdaCount = 0;
        cBandLambdaCount = 88;
        sBandLambdaCount = 0;
        oBandLambdaCount = 80;

        return super.queryLambdas(port);
    }
}
