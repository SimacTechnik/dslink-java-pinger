package cz.simac.pinger;

import org.dsa.iot.dslink.DSLinkFactory;

public class Main {

    public static void main(String args[]) {
        DSLinkFactory.start(args, new PingerDSLink());
    }
}