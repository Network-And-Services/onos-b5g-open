package org.onosproject.drivers.odtn.openconfig;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.drivers.odtn.impl.FlowRuleParser;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.odtn.openconfig.PhoenixTerminalDeviceDiscovery.phoenixPortName;
import static org.onosproject.drivers.odtn.openconfig.PhoenixTerminalDeviceDiscovery.phoenixPortNumber;
import static org.onosproject.drivers.odtn.openconfig.PhoenixTerminalDeviceDiscovery.phoenixTransceiverName;
import static org.onosproject.drivers.odtn.openconfig.PhoenixTerminalDeviceDiscovery.phoenixOpticalChannelName;

public class PhoenixTerminalDeviceFlowRuleProgrammable
        extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(PhoenixTerminalDeviceFlowRuleProgrammable.class);

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final String PREFIX_PORT = "port-";
    private static final String PREFIX_CHANNEL = "channel-";
    private static final String DEFAULT_OPERATIONAL_MODE = "4308";
    private static final String DEFAULT_TARGET_POWER = "0";
    private static final String DEFAULT_ASSIGNMENT_INDEX = "1";
    private static final String DEFAULT_ALLOCATION_INDEX = "10";
    private static final int DEFAULT_RULE_PRIORITY = 10;
    private static final long DEFAULT_RULE_COOKIE = 1234L;
    private static final String OPERATION_DISABLED = "DISABLED";
    private static final String OPERATION_ENABLED = "ENABLED";
    private static final String OPERATION_TRUE = "TRUE";
    private static final String OPERATION_FALSE = "FALSE";
    private static final String OC_TYPE_PROT_OTN = "oc-opt-types:PROT_OTN";
    private static final String OC_TYPE_PROT_ETH = "oc-opt-types:PROT_ETHERNET";


    /**
     * Apply the flow entries specified in the collection rules.
     *
     * @param rules A collection of Flow Rules to be applied
     * @return The collection of added Flow Entries
     */
    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            openConfigError("null session");
            return ImmutableList.of();
        }

        // Apply the  rules on the device
        Collection<FlowRule> added = rules.stream()
                .map(r -> new TerminalDeviceFlowRule(r, getLinePorts()))
                .filter(xc -> applyFlowRule(session, xc))
                .collect(Collectors.toList());

        for (FlowRule flowRule : added) {
            log.info("OpenConfig added flowrule {}", flowRule);
            getConnectionCache().add(did(), ((TerminalDeviceFlowRule) flowRule).connectionName(), flowRule);
        }

        //Print out number of rules sent to the device (without receiving errors)
        openConfigLog("applyFlowRules added {}", added.size());
        return added;
    }

    /**
     * Get the flow entries that are present on the device.
     *
     * @return A collection of Flow Entries
     */
    @Override
    public Collection<FlowEntry> getFlowEntries() {
        Collection<FlowEntry> fetched = new HashSet<>();

        log.info("getFlowEntries device {} cache size {}", did(), getConnectionCache().size(did()));
        if (getConnectionCache().size(did())==0) {
            return fetched;
        }

        //Get all flow entry in the cache - quick but not double-checked on device
        fetched = getConnectionCache().get(did()).stream()
                .map(fr -> new DefaultFlowEntry(fr, FlowEntry.FlowEntryState.ADDED, 0, 0, 0))
                .collect(Collectors.toList());

        //Print out number of rules actually found on the device that are also included in the cache
        log.info("getFlowEntries fetched connections {}", fetched.size());

        return fetched;
    }

    /**
     * Remove the specified flow rules.
     *
     * @param rules A collection of Flow Rules to be removed
     * @return The collection of removed Flow Entries
     */
    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            openConfigError("null session");
            return ImmutableList.of();
        }
        List<FlowRule> removed = new ArrayList<>();
        for (FlowRule r : rules) {
            try {
                TerminalDeviceFlowRule termFlowRule = new TerminalDeviceFlowRule(r, getLinePorts());
                removeFlowRule(session, termFlowRule);
                getConnectionCache().remove(did(), termFlowRule.connectionName());
                removed.add(r);
            } catch (Exception e) {
                openConfigError("Error {}", e);
                continue;
            }
        }

        //Print out number of removed rules from the device (without receiving errors)
        openConfigLog("removeFlowRules removed {}", removed.size());

        return removed;
    }

    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }

    // Context so XPath expressions are aware of XML namespaces
    private static final NamespaceContext NS_CONTEXT = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix.equals("oc-platform-types")) {
                return "http://openconfig.net/yang/platform-types";
            }
            if (prefix.equals("oc-opt-term")) {
                return "http://openconfig.net/yang/terminal-device";
            }
            return null;
        }

        @Override
        public Iterator getPrefixes(String val) {
            return null;
        }

        @Override
        public String getPrefix(String uri) {
            return null;
        }
    };


    /**
     * Helper method to get the device id.
     */
    private DeviceId did() {
        return data().deviceId();
    }

    /**
     * Helper method to log from this class adding DeviceId.
     */
    private void openConfigLog(String format, Object... arguments) {
        log.info("OPENCONFIG {}: " + format, did(), arguments);
    }

    /**
     * Helper method to log an error from this class adding DeviceId.
     */
    private void openConfigError(String format, Object... arguments) {
        log.error("OPENCONFIG {}: " + format, did(), arguments);
    }


    /**
     * Helper method to get the Netconf Session.
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(did()).getSession();
    }


    /**
     * Construct a String with a Netconf filtered get RPC Message.
     *
     * @param filter A valid XML tree with the filter to apply in the get
     * @return a String containing the RPC XML Document
     */
    private String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    /**
     * Construct a get request to retrieve Components and their
     * properties (for the ONOS port, index).
     *
     * @return The filt content to send to the device.
     */
    private String getComponents() {
        StringBuilder filt = new StringBuilder();
        filt.append("<components xmlns='http://openconfig.net/yang/platform'>");
        filt.append(" <component>");
        filt.append("  <name/>");
        filt.append("  <properties/>");
        filt.append(" </component>");
        filt.append("</components>");
        return filteredGetBuilder(filt.toString());
    }

    /**
     * Construct a get request to retrieve Optical Channels and
     * the line port they are using.
     * <p>
     * This method is used to query the device so we can find the
     * OpticalChannel component name that used a given line port.
     *
     * @return The filt content to send to the device.
     */
    private String getOpticalChannels() {
        StringBuilder filt = new StringBuilder();
        filt.append("<components xmlns='http://openconfig.net/yang/platform'>");
        filt.append(" <component>");
        filt.append("  <name/>");
        filt.append("  <state/>");
        filt.append("  <oc-opt-term:optical-channel xmlns:oc-opt-term"
                + " = 'http://openconfig.net/yang/terminal-device'>");
        filt.append("    <oc-opt-term:config>");
        filt.append("     <oc-opt-term:line-port/>");
        filt.append("    </oc-opt-term:config>");
        filt.append("  </oc-opt-term:optical-channel>");
        filt.append(" </component>");
        filt.append("</components>");
        return filteredGetBuilder(filt.toString());
    }

    /**
     * Get the OpenConfig component name for the OpticalChannel component
     * associated to the passed port number (typically a line side port, already
     * mapped to ONOS port).
     *
     * @param session    The netconf session to the device.
     * @param portNumber ONOS port number of the Line port ().
     * @return the channel component name or null
     */
    private String getOpticalChannel(NetconfSession session,
                                     PortNumber portNumber) {
        try {
            checkNotNull(session);
            checkNotNull(portNumber);
            XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(NS_CONTEXT);

            // Get the port name for a given port number
            // We could iterate the port annotations too, no need to
            // interact with device.
            String xpGetPortName =
                    "/rpc-reply/data/components/"
                            +
                            "component[./properties/property[name='onos-index']/config/value ='" +
                            portNumber.toLong() + "']/"
                            + "name/text()";

            // Get all the components and their properties
            String compReply = session.rpc(getComponents()).get();
            DocumentBuilderFactory builderFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document =
                    builder.parse(new InputSource(new StringReader(compReply)));
            String portName = xp.evaluate(xpGetPortName, document);
            String xpGetOptChannelName =
                    "/rpc-reply/data/components/"
                            + "component[./optical-channel/config/line-port='" + portName +
                            "']/name/text()";

            String optChannelReply = session.rpc(getOpticalChannels()).get();
            document =
                    builder.parse(new InputSource(new StringReader(optChannelReply)));
            return xp.evaluate(xpGetOptChannelName, document);
        } catch (Exception e) {
            openConfigError("Exception {}", e);
            return null;
        }
    }

    private void setLogicalChannel(NetconfSession session, String operation, String logChannel)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>");
        sb.append("<logical-channels>");
        sb.append("<channel>");
        sb.append("<index>" + logChannel + "</index>");
        sb.append("<config>");
        sb.append("<admin-state>" + operation + "</admin-state>");
        sb.append("</config>");
        sb.append("</channel>");
        sb.append("</logical-channels>");
        sb.append("</terminal-device>");

        boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing the logical channel");
        }
    }

    private void setPort(NetconfSession session, String operation, String portName)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<components xmlns='http://openconfig.net/yang/platform'>");
        sb.append("<component>");
        sb.append("<name>" + portName + "</name>");
        sb.append("<port>");
        sb.append("<optical-port xmlns='http://openconfig.net/yang/transport-line-common'>");
        sb.append("<config>");
        sb.append("<admin-state>" + operation + "</admin-state>");
        sb.append("</config>");
        sb.append("</optical-port>");
        sb.append("</port>");
        sb.append("</component>");
        sb.append("</components>");

        log.info("SENT EDIT-CONFIG PORT {}", sb.toString());

        boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing the logical channel");
        }
    }

    private void setOpticalChannels(NetconfSession session, String operation, String opticalChannelName)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<components xmlns='http://openconfig.net/yang/platform'>");
        sb.append("<component>");
        sb.append("<name>" + opticalChannelName + "</name>");
        sb.append("<properties>");
        sb.append("<property>");
        sb.append("<name>" + "tx-dis" + "</name>");
        sb.append("<config>");
        sb.append("<name>" + "tx-dis" + "</name>");
        sb.append("<value>" + operation + "</value>");
        sb.append("</config>");
        sb.append("</property>");
        sb.append("</properties>");
        sb.append("</component>");
        sb.append("</components>");

        boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing the component optical channel");
        }
    }

    private void setInterface(NetconfSession session, String operation, String interfaceName)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<interfaces xmlns='http://openconfig.net/yang/interfaces'>");
        sb.append("<interface>");
        sb.append("<name>" + interfaceName + "</name>");
        sb.append("<config>");
        sb.append("<enabled>" + operation + "</enabled>");
        sb.append("</config>");
        sb.append("</interface>");
        sb.append("</interfaces>");

        boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing the interface");
        }
    }

    private void setOpticalChannelFrequency(NetconfSession session, String optChannel, Frequency freq)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<components xmlns='http://openconfig.net/yang/platform'>");
        sb.append("<component>");
        sb.append("<name>" + optChannel + "</name>");
        sb.append("<optical-channel xmlns='http://openconfig.net/yang/terminal-device'>");
        sb.append("<config>");
        sb.append("<frequency>" + (long) freq.asMHz() + "</frequency>");
        sb.append("<target-output-power>" + DEFAULT_TARGET_POWER + "</target-output-power>");
        sb.append("<operational-mode>" + DEFAULT_OPERATIONAL_MODE + "</operational-mode>");
        sb.append("<line-als xmlns='http://nec.com/yang/nec-optical-openconfig-platform-optical-channel'>" + "NONE" + "</line-als>");
        sb.append("</config>");
        sb.append("</optical-channel>");
        sb.append("</component>");
        sb.append("</components>");

        log.info("SENT EDIT-CONFIG CHANNEL {}", sb.toString());

        boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing channel frequency");
        }
    }

    private void setLogicalChannelAssignment(NetconfSession session, String operation, String client, String line,
                                             String assignmentIndex, String allocationIndex)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>");
        sb.append("<logical-channels>");
        sb.append("<channel>");
        sb.append("<index>" + client + "</index>");
        sb.append("<config>");
        sb.append("<admin-state>" + operation + "</admin-state>");
        sb.append("</config>");
        sb.append("<logical-channel-assignments>");
        sb.append("<assignment>");
        sb.append("<index>" + assignmentIndex + "</index>");
        sb.append("<config>");
        sb.append("<logical-channel>" + line + "</logical-channel>");
        sb.append("<allocation>" + allocationIndex + "</allocation>");
        sb.append("</config>");
        sb.append("</assignment>");
        sb.append("</logical-channel-assignments>");
        sb.append("</channel>");
        sb.append("</logical-channels>");
        sb.append("</terminal-device>");

        boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing logical channel assignment");
        }
    }

    /**
     * Apply a single flowrule to the device.
     *
     * --- Directionality details:
     * Driver supports ADD (INGRESS) and DROP (EGRESS) rules generated by OpticalCircuit/OpticalConnectivity intents
     * the format of the rules are checked in class TerminalDeviceFlowRule
     *
     * However, the physical transponder is always bidirectional as specified in OpenConfig YANG models
     * therefore ADD and DROP rules are mapped in the same xml that ENABLE (and tune) a transponder port.
     *
     * If the intent is generated as bidirectional both ADD and DROP flowrules are generated for each device, thus
     * the same xml is sent twice to the device.
     *
     * @param session   The Netconf session.
     * @param rule      Flow Rules to be applied.
     * @return true if no Netconf errors are received from the device when xml is sent
     * @throws NetconfException if exchange goes wrong
     */
    protected boolean applyFlowRule(NetconfSession session, TerminalDeviceFlowRule rule) {

        //Configuration of LINE side, used for OpticalConnectivity intents
        //--- configure central frequency
        //--- enable the line port
        if (rule.type == TerminalDeviceFlowRule.Type.LINE_INGRESS ||
                rule.type == TerminalDeviceFlowRule.Type.LINE_EGRESS) {

            FlowRuleParser frp = new FlowRuleParser(rule);

            String portName = phoenixPortNumber((int) frp.getPortNumber().toLong());
            String transceiverName = phoenixTransceiverName(portName);
            String opticalChannelName = phoenixOpticalChannelName(portName);

            Frequency centralFrequency = frp.getCentralFrequency();

            StringBuilder componentConf = new StringBuilder();

            log.info("Sending LINE FlowRule to device {} LINE port {}, trans {}, channel {}, frequency {}",
                    did(), portName, transceiverName, opticalChannelName, centralFrequency);

            try {
                setOpticalChannelFrequency(session, opticalChannelName, centralFrequency);
            } catch (NetconfException e) {
                log.error("Error writing central frequency in the component");
                return false;
            }

            try {
                setPort(session, OPERATION_ENABLED, portName);
            } catch (NetconfException e) {
                log.error("Error enabling the port");
                return false;
            }

            //This is typically performed statically
            /*try {
                setOpticalChannels(session, OPERATION_FALSE, opticalChannelName);
            } catch (NetconfException e) {
                log.error("Error enabling the port");
                return false;
            }*/

            //This is typically performed statically
            /*try {
                setInterface(session, OPERATION_TRUE, portName);
            } catch (NetconfException e) {
                log.error("Error enabling the port");
                return false;
            }*/
        }

        //Configuration of CLIENT side, used for OpticalCircuit intents
        //--- associate the client port to the line port
        //--- enable the client port
        //
        //Assumes only one "assignment" per logical-channel with index 1
        //TODO check the OTN mapping of client ports into the line port frame specified by parameter "<allocation>"
        if (rule.type == TerminalDeviceFlowRule.Type.CLIENT_INGRESS ||
                rule.type == TerminalDeviceFlowRule.Type.CLIENT_EGRESS) {

            String clientPortName;
            String linePortName;
            if (rule.type == TerminalDeviceFlowRule.Type.CLIENT_INGRESS) {
                clientPortName = rule.inPort().toString();
                linePortName = rule.outPort().toString();
            } else {
                clientPortName = rule.outPort().toString();
                linePortName = rule.inPort().toString();
            }

            log.info("Sending CLIENT FlowRule to device {} CLIENT port: {}, LINE port {}",
                    did(), clientPortName, linePortName);

            try {
                setPort(session, OPERATION_ENABLED, clientPortName);
            } catch (NetconfException e) {
                log.error("Error enabling the port");
                return false;
            }

            //This is typically performed statically
            /*try {
                setLogicalChannelAssignment(session, OPERATION_ENABLED, clientPortName, linePortName,
                        DEFAULT_ASSIGNMENT_INDEX, DEFAULT_ALLOCATION_INDEX);
            } catch (NetconfException e) {
                log.error("Error setting the logical channel assignment");
                return false;
            }*/
        }

        return true;
    }

    protected boolean removeFlowRule(NetconfSession session, TerminalDeviceFlowRule rule)
            throws NetconfException {

        //Configuration of LINE side, used for OpticalConnectivity intents
        //--- configure central frequency to ZERO
        //--- disable the line port
        if (rule.type == TerminalDeviceFlowRule.Type.LINE_INGRESS ||
                rule.type == TerminalDeviceFlowRule.Type.LINE_EGRESS) {

            FlowRuleParser frp = new FlowRuleParser(rule);
            String portName = phoenixPortNumber((int) frp.getPortNumber().toLong());

            log.info("Removing LINE FlowRule device {} line port {}", did(), portName);

            try {
                setPort(session, OPERATION_DISABLED, portName);
            } catch (NetconfException e) {
                log.error("Error disabling the port line side");
                return false;
            }
        }

        //Configuration of CLIENT side, used for OpticalCircuit intents
        //--- configure central frequency to ZERO
        //--- disable the line port
        if (rule.type == TerminalDeviceFlowRule.Type.CLIENT_INGRESS ||
                rule.type == TerminalDeviceFlowRule.Type.CLIENT_EGRESS) {

            String clientPortName;
            String linePortName;
            if (rule.type == TerminalDeviceFlowRule.Type.CLIENT_INGRESS) {
                clientPortName = rule.inPort().toString();
                linePortName = rule.outPort().toString();
            } else {
                clientPortName = rule.outPort().toString();
                linePortName = rule.inPort().toString();
            }

            log.debug("Removing CLIENT FlowRule device {} client port: {}, line port {}",
                    did(), clientPortName, linePortName);

            try {
                setPort(session, OPERATION_DISABLED, clientPortName);
            } catch (NetconfException e) {
                log.error("Error disabling the port line side");
                return false;
            }
        }

        return true;
    }

    /**
     * Convert start and end frequencies to OCh signal.
     *
     * FIXME: supports channel spacing 50 and 100
     *
     * @param central central frequency as double in THz
     * @param width width of the channel arounf the central frequency as double in GHz
     * @return OCh signal
     */
    public static OchSignal toOchSignal(Frequency central, double width) {
        int slots = (int) (width / ChannelSpacing.CHL_12P5GHZ.frequency().asGHz());
        int multiplier = 0;

        double centralAsGHz = central.asGHz();

        if (width == 50) {
            multiplier = (int) ((centralAsGHz - Spectrum.CENTER_FREQUENCY.asGHz())
                    / ChannelSpacing.CHL_50GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, multiplier, slots);
        }

        if (width == 100) {
            multiplier = (int) ((centralAsGHz - Spectrum.CENTER_FREQUENCY.asGHz())
                    / ChannelSpacing.CHL_100GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, multiplier, slots);
        }

        return null;
    }

    private List<PortNumber> getLinePorts() {
        List<PortNumber> linePorts;

        DeviceService deviceService = this.handler().get(DeviceService.class);
        linePorts = deviceService.getPorts(data().deviceId()).stream()
                .filter(p -> p.annotations().value(OdtnDeviceDescriptionDiscovery.PORT_TYPE)
                        .equals(OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.value()))
                .map(p -> p.number())
                .collect(Collectors.toList());

        return linePorts;
    }

    private FlowRule findDropRule(PortNumber inPort, PortNumber outPort, Frequency freq) {

        FlowRule rule = getConnectionCache().get(did()).stream()
                .filter(r -> r instanceof TerminalDeviceFlowRule)
                .filter(r -> ((TerminalDeviceFlowRule) r).type.equals(TerminalDeviceFlowRule.Type.LINE_EGRESS))
                .filter(r -> ((TerminalDeviceFlowRule) r).ochSignal().centralFrequency().equals(freq))
                .filter(r -> ((TerminalDeviceFlowRule) r).inPort().equals(inPort))
                .filter(r -> ((TerminalDeviceFlowRule) r).outPort().equals(outPort))
                .findFirst()
                .orElse(null);

        return rule;
    }

    private FlowRule findAddRule(PortNumber inPort, PortNumber outPort, Frequency freq) {

        FlowRule rule = getConnectionCache().get(did()).stream()
                .filter(r -> r instanceof TerminalDeviceFlowRule)
                .filter(r -> ((TerminalDeviceFlowRule) r).type.equals(TerminalDeviceFlowRule.Type.LINE_INGRESS))
                .filter(r -> ((TerminalDeviceFlowRule) r).ochSignal().centralFrequency().equals(freq))
                .filter(r -> ((TerminalDeviceFlowRule) r).inPort().equals(inPort))
                .filter(r -> ((TerminalDeviceFlowRule) r).outPort().equals(outPort))
                .findFirst()
                .orElse(null);

        return rule;
    }
}
