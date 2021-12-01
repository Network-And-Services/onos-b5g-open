/*
 * Copyright 2022-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This work was partially supported by EC H2020 project B5G-OPEN (101016663).
 */

package org.onosproject.drivers.odtn;

import com.google.common.collect.Range;
import org.apache.commons.configuration.XMLConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.DatastoreId;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_OPTICAL_CHANNEL_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of PowerConfig interface for Cassini device running Ocnos v5.
 */
public class CassiniOcnos5PowerConfig<T>
        extends AbstractHandlerBehaviour implements PowerConfig<T> {

    public static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final Logger log = getLogger(CassiniOcnos5PowerConfig.class);

    /**
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device indetifier
     * @return The netconf session or null
     */

    private NetconfSession getNetconfSession(DeviceId deviceId) {
        NetconfController controller = handler().get(NetconfController.class);
        NetconfDevice ncdev = controller.getDevicesMap().get(deviceId);
        if (ncdev == null) {
            log.trace("No netconf device, returning null session");
            return null;
        }
        return ncdev.getSession();
    }

    /**
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }

    /**
     * Get the OpenConfig component name for the OpticalChannel component.
     *
     * @param portNumber ONOS port number of the Line port ().
     * @return the channel component name or null
     */
    protected String getOpticalChannel(PortNumber portNumber) {
        Port clientPort = handler().get(DeviceService.class).getPort(did(), portNumber);
        return clientPort.annotations().value(OC_OPTICAL_CHANNEL_NAME);
    }

    /**
     * Get the target-output-power value on specific optical-channel.
     *
     * @param port      the port
     * @param component the port component. It should be 'oc-name' in the Annotations of Port.
     *                  'oc-name' could be mapped to '/component/name' in openconfig yang.
     * @return target power value
     */
    @Override
    public Optional<Double> getTargetPower(PortNumber port, T component) {
        if (checkPortComponent(port, component)) {
            return getOcnosTargetPower(port, component);
        }
        return Optional.empty();
    }

    @Override
    public void setTargetPower(PortNumber port, T component, double power) {
        if (checkPortComponent(port, component)) {
            setOcnosTargetPower(port, component, power);
        }
    }

    @Override
    public Optional<Double> currentPower(PortNumber port, T component) {
        if (checkPortComponent(port, component)) {
            return getOcnosCurrentPower(port, component);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Double> currentInputPower(PortNumber port, T component) {
        if (checkPortComponent(port, component)) {
            return getOcnosCurrentInputPower(port, component);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, T component) {
        if (checkPortComponent(port, component)) {
            return getOcnosTargetPowerRange(port, component);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber port, T component) {
        //FIXME to be implemented
        if (checkPortComponent(port, component)) {
            //return getOcnosInputPowerRange(port, component);
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public List<PortNumber> getPorts(T component) {
        return getOcnosPorts(component);
    }

    /**
     * Set the ComponentType to invoke proper methods for different template T.
     *
     * @param port the component.
     * @param component the component.
     */
    private Boolean checkPortComponent(PortNumber port, Object component) {

        //Check componenet
        String clsName = component.getClass().getName();
        switch (clsName) {
            case "org.onosproject.net.Direction":
                break;
            case "org.onosproject.net.OchSignal":
                break;
            default:
                log.error("Cannot parse the component type {}.", clsName);
                log.error("The component content is {}.", component.toString());
                return false;
        }

        //Check that port has an associated optical channel
        if (getOpticalChannel(port) == null) {
            return false;
        }

        //Checks are ok
        return true;
    }

    private String filteredEditConfigBuilder(String filterEditConfig) {
        StringBuilder rpc = new StringBuilder();
        rpc.append(RPC_TAG_NETCONF_BASE);
        rpc.append("<edit-config>");
        rpc.append("<target><" + DatastoreId.CANDIDATE + "/></target>");
        rpc.append("<config>");
        rpc.append(filterEditConfig);
        rpc.append("</config>");
        rpc.append("</edit-config>");
        rpc.append(RPC_CLOSE_TAG);

        return rpc.toString();
    }

    private String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder();
        rpc.append(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    /**
     * Parse filtering string from port and component.
     *
     * @param port Port Number
     * @param power      power value set.
     * @return filtering string in xml format
     */
    private String setTargetPowerFilter(PortNumber port, Double power) {

        //Retrieve optical-channel name
        String optChannelName = getOpticalChannel(port);

        StringBuilder sb = new StringBuilder();
        sb.append("<terminal-device xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform-terminal-device'>"
                + "<coherent-module>"
                + "  <slot-index>" + optChannelName + "</slot-index>"
                + "  <config>"
                + "    <slot-index>" + optChannelName + "</slot-index>"
                + "  </config>"
                + "  <network-interfaces>"
                + "    <interface>"
                + "      <net-index>0</net-index>"
                + "      <config>"
                + "        <net-index>0</net-index>"
                + "        <target-output-power>" + power + "</target-output-power>"
                + "      </config>"
                + "    </interface>"
                + "  </network-interfaces>"
                + "</coherent-module>"
                + "</terminal-device>");

        return sb.toString();
    }

    private String getTargetPower(String optChannelName) {
        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform-terminal-device'>"
                + "<coherent-module>"
                + "  <slot-index>" + optChannelName + "</slot-index>"
                + "<network-interfaces>"
                + "<interface>"
                + "  <net-index>0</net-index>"
                + "  <state>"
                + "    <target-output-power/>"
                + "  </state>"
                + "</interface>"
                + "</network-interfaces>"
                + "</coherent-module>"
                + "</terminal-device>");

        return sb.toString();
    }

    private String getInputPower(String optChannelName) {
        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform-terminal-device'>"
                + "<coherent-module>"
                + "  <slot-index>" + optChannelName + "</slot-index>"
                + "<network-interfaces>"
                + "<interface>"
                + "  <net-index>0</net-index>"
                + "  <input-power>"
                + "    <state>"
                + "      <instant/>"
                + "    </state>"
                + "  </input-power>"
                + "</interface>"
                + "</network-interfaces>"
                + "</coherent-module>"
                + "</terminal-device>");

        return sb.toString();
    }

    private String getOutputPower(String optChannelName) {
        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform-terminal-device'>"
                + "<coherent-module>"
                + "  <slot-index>" + optChannelName + "</slot-index>"
                + "<network-interfaces>"
                + "<interface>"
                + "  <net-index>0</net-index>"
                + "  <output-power>"
                + "    <state>"
                + "      <instant/>"
                + "    </state>"
                + "  </output-power>"
                + "</interface>"
                + "</network-interfaces>"
                + "</coherent-module>"
                + "</terminal-device>");

        return sb.toString();
    }

    /**
     * getOcnosTargetPower.
     *
     * @param port      port
     * @param component component
     * @return target power
     */
    Optional<Double> getOcnosTargetPower(PortNumber port, Object component) {
        NetconfSession session = getNetconfSession(did());
        checkNotNull(session);

        //Retrieve the optical channel name as port annotation
        String optChannelName = getOpticalChannel(port);

        //log.info("REQUEST get TargetPower to device/port: {}/{}", did(), port);

        String reply;
        try {
            reply = session.get(filteredGetBuilder(getTargetPower(optChannelName)));
        } catch (Exception e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve getTargetPower.", e));
        }

        //log.info("REPLY from device: {}", reply);

        XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
        if (xconf == null) {
            log.error("Error in executingRpc");
            return Optional.empty();
        }

        String powerString = xconf.getString(("data." +
                "terminal-device.coherent-module.network-interfaces.interface." +
                "state.target-output-power"));

        //log.info("TargetPower from device: {}", powerString);

        if (powerString == null) {
            return Optional.empty();
        }

        return Optional.of(Double.valueOf(powerString));
    }

    /**
     * setOcnosTargetPower.
     *
     * @param port      port
     * @param component component
     * @param power     target value
     */
    private void setOcnosTargetPower(PortNumber port, Object component, double power) {
        NetconfSession session = getNetconfSession(did());
        checkNotNull(session);

        if (!getOcnosTargetPowerRange(port, component).get().contains(power)) {
            log.error("Specified targetPower out of range {}",
                    getOcnosTargetPowerRange(port, component).get());
            return;
        }

        String rpcReq = filteredEditConfigBuilder(setTargetPowerFilter(port, power));

        //log.info("Setting power {}", rpcReq);

        try {
            session.rpc(rpcReq);
        } catch (NetconfException e) {
            log.error("Error wring channel power on CANDIDATE", e);
        }

        try {
            session.commit();
        } catch (NetconfException e) {
            log.error("Error committing channel power", e);
        }
    }

    /**
     * mirror method in the internal class.
     *
     * @param port      port
     * @param component the component.
     * @return current output power.
     */
    private Optional<Double> getOcnosCurrentPower(PortNumber port, Object component) {
        NetconfSession session = getNetconfSession(did());
        checkNotNull(session);

        //Compute port name, then retrieve opt channel
        String optChannelName = getOpticalChannel(port);

        //log.info("REQUEST get CurrentPower to device/port: {}/{}", did(), port);

        String reply;
        try {
            reply = session.get(filteredGetBuilder(getOutputPower(optChannelName)));
        } catch (Exception e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve getOcnosCurrentPower.", e));
        }

        //log.info("REPLY from device: {}", reply);

        XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
        if (xconf == null) {
            log.error("Error in executingRpc");
            return Optional.empty();
        }

        String powerString = xconf.getString(("data." +
                "terminal-device.coherent-module.network-interfaces.interface." +
                "output-power.state.instant"));

        //log.info("OutputPower from device: {}", powerString);

        if (powerString == null) {
            return Optional.empty();
        }

        return Optional.of(Double.valueOf(powerString));
    }

    /**
     * This function get the current input power.
     *
     * @param port      port
     * @param component the component
     * @return current input power
     */
    private Optional<Double> getOcnosCurrentInputPower(PortNumber port, Object component) {
        NetconfSession session = getNetconfSession(did());
        checkNotNull(session);

        //Compute port name, then retrieve opt channel
        String optChannelName = getOpticalChannel(port);

        //log.info("REQUEST get InputPower to device/port: {}/{}", did(), port);

        String reply;
        try {
            reply = session.get(filteredGetBuilder(getInputPower(optChannelName)));
        } catch (Exception e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve getOcnosCurrentInputPower.", e));
        }

        //log.info("REPLY from device: {}", reply);

        XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
        if (xconf == null) {
            log.error("Error in executingRpc");
            return Optional.empty();
        }

        String powerString = xconf.getString(("data." +
                "terminal-device.coherent-module.network-interfaces.interface." +
                "input-power.state.instant"));

        //log.info("InputPower from device: {}", powerString);

        if (powerString == null) {
            return Optional.empty();
        }

        return Optional.of(Double.valueOf(powerString));
    }

    private Optional<Range<Double>> getOcnosTargetPowerRange(PortNumber port, Object component) {
        double targetMin = -10;
        double targetMax = 2;
        return Optional.of(Range.open(targetMin, targetMax));
    }

    private Optional<Range<Double>> getOcnosInputPowerRange(PortNumber port, Object component) {
        double targetMin = -30;
        double targetMax = 1;
        return Optional.of(Range.open(targetMin, targetMax));
    }

    private List<PortNumber> getOcnosPorts(Object component) {
        // FIXME
        log.warn("Not Implemented Yet!");
        return new ArrayList<PortNumber>();
    }
}
