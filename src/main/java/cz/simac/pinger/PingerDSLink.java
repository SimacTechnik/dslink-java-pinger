package cz.simac.pinger;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PingerDSLink extends DSLinkHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(PingerDSLink.class);

    private DSLink link;

    private Node superRoot;

    public PingerDSLink() {
        super();
    }

    @Override
    public boolean isResponder() {
        return true;
    }

    @Override
    public void onResponderConnected(final DSLink link) {
        LOGGER.info("Connected");
        this.link = link;
    }

    @Override
    public void onResponderInitialized(final DSLink link) {
        LOGGER.info("Initialized");
        superRoot = link.getNodeManager().getSuperRoot();

        Action action = new Action(Permission.WRITE, (ActionResult event) -> {
            Value name = event.getParameter(PingerConstants.NAME);
            Value address = event.getParameter(PingerConstants.ADDRESS);
            Value interval = event.getParameter(PingerConstants.INTERVAL);
            InetAddress addr;
            if (name == null || name.getString().equals("") || address == null || address.getString().equals("")
            || interval == null)
                return;
            try {
                addr = InetAddress.getByName(address.getString());
            } catch (UnknownHostException e) {
                return;
            }
            new Thread(new Pinger(superRoot, name.getString(), addr, interval.getNumber().intValue())).start();
        });

        action.addParameter(new Parameter(PingerConstants.NAME, ValueType.STRING, new Value("")));

        action.addParameter(new Parameter(PingerConstants.ADDRESS, ValueType.STRING, new Value("")));

        action.addParameter(new Parameter(PingerConstants.INTERVAL, ValueType.NUMBER, new Value(1000)));

        superRoot.createChild(PingerConstants.CREATE_PINGER, true)
                .setDisplayName(PingerConstants.CREATE_PINGER)
                .setSerializable(true)
                .setAction(action)
                .build();

        for(Node n : superRoot.getChildren().values()) {
            if(n.getAction() != null)
                continue;
            String name = n.getDisplayName();
            Node address = n.getChild(PingerConstants.ADDRESS, true);
            Node interval = n.getChild(PingerConstants.INTERVAL, true);
            if(address == null || interval == null)
                continue;
            InetAddress addr;
            try {
                addr = InetAddress.getByName(address.getValue().getString());
            } catch (UnknownHostException e) { continue; }
            new Thread(new Pinger(superRoot, name, addr, interval.getValue().getNumber().intValue())).start();
        }
    }

    @Override
    public void onResponderDisconnected(DSLink link) {
        LOGGER.info("Disconnected");
    }

}
