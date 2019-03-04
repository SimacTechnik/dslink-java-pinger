package cz.simac.pinger;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class Pinger implements Runnable {

    private Node node;

    private volatile int interval;

    private InetAddress address;

    private boolean exit = false;

    private Node activeNode;

    private Node lastActiveNode;

    private Node intervalNode;

    private DateTimeFormatter formatter =
            DateTimeFormatter.ofLocalizedDateTime( FormatStyle.LONG )
                    .withLocale( Locale.UK )
                    .withZone( ZoneId.systemDefault() );

    public Pinger(Node rootNode, String name, InetAddress address, int interval) {
        this.node = createNode(rootNode, name, address, interval);
        this.address = address;
        this.interval = interval;
    }

    private void setInterval(int interval) {
        if(interval != this.interval)
            intervalNode.setValue(new Value(interval));
        this.interval = interval;
    }

    @Override
    public void run() {
        while(!exit) {
            try {
                Instant now = Instant.now();
                if(address.isReachable(interval)) {
                    lastActiveNode.setValue(new Value(formatter.format(Instant.now())));
                    if (!activeNode.getValue().getBool())
                        activeNode.setValue(new Value(true));
                }
                else if (activeNode.getValue().getBool()){
                    activeNode.setValue(new Value(false));
                }
                Thread.sleep(Math.max(interval - Duration.between(now, Instant.now()).toMillis(), 0));
            } catch (IOException | InterruptedException e) {}
        }
    }

    private Node createNode(Node rootNode, String name, InetAddress address, int interval) {
        Node n = rootNode.createChild(name, true)
                .setDisplayName(name)
                .setSerializable(true)
                .build();

        Action deleteAction = new Action(Permission.WRITE, this::handleDeleteAction);

        n.createChild(PingerConstants.DELETE_PINGER, true)
                .setDisplayName(PingerConstants.DELETE_PINGER)
                .setSerializable(true)
                .setAction(deleteAction)
                .build();

        Action changeInterval = new Action(Permission.WRITE, (ActionResult event) -> {
            Value newInterval = event.getParameter(PingerConstants.INTERVAL);
            if(newInterval != null) {
                setInterval(newInterval.getNumber().intValue());
            }
        });

        changeInterval.addParameter(new Parameter(PingerConstants.INTERVAL, ValueType.NUMBER, new Value(interval)));

        n.createChild(PingerConstants.CHANGE_INTERVAL, true)
                .setDisplayName(PingerConstants.CHANGE_INTERVAL)
                .setSerializable(true)
                .setAction(changeInterval)
                .build();

        n.createChild(PingerConstants.ADDRESS, true)
                .setDisplayName(PingerConstants.ADDRESS)
                .setSerializable(true)
                .setValue(new Value(address.getHostAddress()))
                .build();

        activeNode = n.createChild(PingerConstants.ACTIVE, true)
                .setDisplayName(PingerConstants.ACTIVE)
                .setSerializable(true)
                .setValueType(ValueType.BOOL)
                .setValue(new Value(false))
                .build();

        lastActiveNode = n.createChild(PingerConstants.LAST_RESPONSE, true)
                .setDisplayName(PingerConstants.LAST_RESPONSE)
                .setSerializable(true)
                .setValueType(ValueType.STRING)
                .setValue(new Value(""))
                .build();

        intervalNode = n.createChild(PingerConstants.INTERVAL, true)
                .setDisplayName(PingerConstants.INTERVAL)
                .setSerializable(true)
                .setValueType(ValueType.NUMBER)
                .setValue(new Value(interval))
                .build();

        return n;
    }

    private void handleDeleteAction(ActionResult event) {
        node.delete(true);
        exit = true;
    }
}
