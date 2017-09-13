/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge.commands;

import java.util.List;
import java.util.Map;
import org.sysapp.bridge.FreeHomeCommandAbstractionInterface;
import org.sysapp.bridge.FreeHomeXMPBasicCommands;

/**
 *
 * @author enrico
 */
public class ShutterControll implements FreeHomeCommandAbstractionInterface {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ShutterControll.class);
    private final static String S_PO_UP_DOWN = "idp0000";
    private final static String S_PO_STOP = "idp0001";
    private int status = 200;
    private String error = "no Errors";
    private String result = "";

    public ShutterControll() {

        log.info("http://...../shut?id=XXXXX&ch=xxxxx&set=[UP|DOWN|STOP] ");

    }

    @Override
    public String getName() {
        return "shut";
    }

    @Override
    public FreeHomeCommandAbstractionInterface execute(Map<String, List<String>> parms, FreeHomeXMPBasicCommands basicCommands) {
        String id;
        String ch;
        if (parms.containsKey("alias")) {
            String path[] = basicCommands.resolveDeviceAlias(parms.get("alias").get(0));
            id = path[0];
            ch = path[1];
        } else {
            id = parms.get("id").get(0);
            ch = parms.get("ch").get(0);
        }
        // get Parameter;
        if (parms.containsKey("set")) {
            String get = parms.get("set").get(0);
            if (get.compareToIgnoreCase("up") == 0) {
                basicCommands.setDataPoint(id, ch, S_PO_UP_DOWN, "0");
            } else if (get.compareToIgnoreCase("down") == 0) {
                basicCommands.setDataPoint(id, ch, S_PO_UP_DOWN, "1");
            } else if (get.compareToIgnoreCase("stop") == 0) {
                basicCommands.setDataPoint(id, ch, S_PO_STOP, "1");
            }

        }
        return this;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getResponse() {
        return result;
    }

    @Override
    public String getErrorMessage() {
        return error;
    }

}
