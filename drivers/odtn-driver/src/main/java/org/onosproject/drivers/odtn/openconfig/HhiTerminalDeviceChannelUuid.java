package org.onosproject.drivers.odtn.openconfig;

import org.apache.commons.configuration.XMLConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ModulationConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_OPTICAL_CHANNEL_NAME;

public class HhiTerminalDeviceChannelUuid<T> extends AbstractHandlerBehaviour
        implements ModulationConfig<T> {

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final Logger log = LoggerFactory.getLogger(org.onosproject.drivers.odtn.CassiniOcnos5Modulation.class);

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
     * Get the OpenConfig component name for the OpticalChannel component.
     *
     * @param portNumber ONOS port number of the Line port ().
     * @return the channel component name or null
     */
    protected String getOpticalChannel(PortNumber portNumber) {
        Port clientPort = handler().get(DeviceService.class).getPort(did(), portNumber);
        return clientPort.annotations().value(OC_OPTICAL_CHANNEL_NAME);
    }

    /*
     *
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }

    private String getOpModeFilter(String uuid) {
        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>"
                + "  <logical-channels>"
                + "    <channel>"
                + "      <index>1</index>"
                + "    </channel>"
                + "  </logical-channels>"
                + "</terminal-device>");

        return sb.toString();
    }

    /*Parse filtering string from port and component.
     *
     * @param portNumber Port Number
     * @param modulation
     * @return filtering string in xml format
     */
    private String setOpModeFilter(PortNumber portNumber, String uuid) {

        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>"
                + "  <logical-channels>"
                + "    <channel>"
                + "      <index>1</index>"
                + "      <config>"
                + "        <index>1</index>"
                + "        <description>" + uuid + "</description>"
                + "      </config>"
                + "    </channel>"
                + "  </logical-channels>"
                + "</terminal-device>");

        return sb.toString();
    }

    /**
     * Get the target Modulation Scheme on the component.
     *
     * @param port      the port
     * @param component the port component
     * @return ModulationScheme as per bitRate value
     **/
    @Override
    public Optional<ModulationScheme> getModulationScheme(PortNumber port, T component) {
        if (checkPortComponent(port, component)) {
            return Optional.of(ModulationScheme.DP_16QAM);
        }
        return Optional.empty();
    }

    /**
     * Set the target Modulation Scheme on the component.
     *
     * @param port      the port
     * @param component the port component
     * @param bitRate   bit rate in bps
     **/
    @Override
    public void setModulationScheme(PortNumber port, T component, long bitRate) {
        if (checkPortComponent(port, component)) {
            setOcnosModulationScheme(port, component, bitRate);
        }
    }

    /**
     * Set the target Modulation Scheme on the component.
     *
     * @param port      the port
     * @param component the port component
     * @param modulationScheme   selecetd modulation
     **/
    @Override
    public void setModulationScheme(PortNumber port, T component, ModulationScheme modulationScheme) {
        if (checkPortComponent(port, component)) {
            setOcnosModulationScheme(port, component);
        }
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
     * Set the ComponentType to invoke proper methods for different template T.
     *
     * @param port the component.
     * @param component the component.
     */
    private Boolean checkPortComponent(PortNumber port, Object component) {

        //Check componenet
        String clsName = component.getClass().getName();

        if (component instanceof String) {
            log.warn("Set telemetry id with string {}.", component);
            return true;
        } else {
            log.error("Cannot parse the component type {}.", clsName);
            log.error("The component content is {}.", component.toString());
            return false;
        }
    }

    /*
     * Set modulation scheme.
     *
     * @param port port
     * @param component component
     * @param power target value
     */
    void setOcnosModulationScheme(PortNumber port, Object component) {
        NetconfSession session = getNetconfSession(did());
        checkNotNull(session);

        log.info("Setting telemetry uuid {}", component);

        String filter = setOpModeFilter(port, (String) component);
        String rpcReq = filteredEditConfigBuilder(filter);

        try {
            session.rpc(rpcReq);
        } catch (Exception e) {
            log.error("Error writing operational mode on CANDIDATE", e);
        }

        //log.info("Modulation config sent {}", rpcReq);

        try {
            session.commit();
        } catch (NetconfException e) {
            log.error("Error committing operational mode", e);
        }
    }

    /*
     * Get modulation scheme.
     *
     * @param port port
     * @param component component
     * @return target modulation
     */
    Optional<ModulationScheme> getOcnosModulationScheme(PortNumber port, Object component) {
        return Optional.of(ModulationScheme.DP_16QAM);
    }

    /*
     * Set modulation scheme using bitrate.
     *
     * @param port port
     * @param component component
     * @param power target value
     */
    void setOcnosModulationScheme(PortNumber port, Object component, long bitRate) {
        NetconfSession session = getNetconfSession(did());
        checkNotNull(session);

        String filter = setOpModeFilter(port, (String) component);
        String rpcReq = filteredEditConfigBuilder(filter);

        try {
            session.rpc(rpcReq);
        } catch (Exception e) {
            log.error("Error writing operational mode on CANDIDATE", e);
        }

        log.info("Modulation config sent {}", rpcReq);

        try {
            session.commit();
        } catch (NetconfException e) {
            log.error("Error committing channel power", e);
        }
    }

    private String getOperationalMode(ModulationScheme modulation) {
        if (modulation.equals(ModulationScheme.DP_QPSK)) {
            return "dp-qpsk";
        }
        if (modulation.equals(ModulationScheme.DP_16QAM)) {
            return "dp-16-qam";
        }
        if (modulation.equals(ModulationScheme.DP_8QAM)) {
            return "dp-8-qam";
        }
        log.error("Modulation scheme is not supported.");
        return null;
    }
}