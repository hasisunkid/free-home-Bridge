/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.sysapp.bridge.FreeHomeCommandAbstractionInterface;
import org.sysapp.bridge.FreeHomeXMPBasicCommands;
import org.sysapp.bridge.ValueCache;

/**
 *
 * @author enrico
 */
public class HeatControll implements FreeHomeCommandAbstractionInterface {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HeatControll.class);
    private final static String R_PO_R_TEMP = "odp0010";
    private final static String R_PO_H_TEMP = "odp0006";
    private final static String R_PO_H_SWITCH_POS = "odp0007";
    private final static String R_PO_H_ON_OFF = "odp0008";
    private final static String S_PO_H_ON_OFF = "idp0011";
    private final static String S_PO_H_SET = "idp000E";
    private int status = 200;
    private String error = "no Errors";
    private String result = "";
    private final String topicPrefix = "free_home/heat";

    public HeatControll() {
        log.info("HeatControll command enabled");
        log.info("http://...../heat?id=XXXXX&ch=xxxxx&get=room  returns room temperature");
        log.info("http://...../heat?id=XXXXX&ch=xxxxx&get=setting  returns current setted temperature");
        log.info("http://...../heat?id=XXXXX&ch=xxxxx&get=state returns ON OFF");
        log.info("----------------------------------------------------------------------------------------");

        log.info("http://...../heat?id=XXXXX&ch=xxxxx&set=[ON|OFF] switche ON OFF");
        log.info("http://...../heat?id=XXXXX&ch=xxxxx&set=temp&value=[degr] adjust heat tempreture value float");
        log.info("----------------------------------------------------------------------------------------");
        log.info("----------------------------------------------------------------------------------------");

    }

    @Override
    public String getName() {
        return "heat";
    }

    @Override
    public Map<String, String> execute(String command, String value, FreeHomeXMPBasicCommands basicCommands) {

        HashMap<String, String> affectedTopics = new HashMap<>();
        String alias = command.split("/")[2];
        String path[] = basicCommands.resolveDeviceAlias(alias);
        String id = path[0];
        String ch = path[1];

        if (command.endsWith("command/state")) {
            log.debug("change heating state");
            if (value.equalsIgnoreCase("on")) {
                //     turn heat on
                basicCommands.setDataPoint(id, ch, S_PO_H_ON_OFF, "1");
                affectedTopics.put(String.format("%s/%s/state", topicPrefix, alias), "on");

            } else {
                basicCommands.setDataPoint(id, ch, S_PO_H_ON_OFF, "0");
                affectedTopics.put(String.format("%s/%s/state", topicPrefix, alias), "off");
            }
        } else if (command.endsWith("command/temp")) {
            basicCommands.setDataPoint(id, ch, S_PO_H_ON_OFF, "1");
//          
            try {
                float new_tmp = Float.parseFloat(value);
                if (new_tmp < 0) {
                    new_tmp = 0;
                }
                if (new_tmp > 35) {
                    new_tmp = 35;
                }
                float current_tmp = Float.parseFloat(basicCommands.getValue(id, ch, R_PO_H_TEMP, false));
                float current_pos = Float.parseFloat(basicCommands.getValue(id, ch, R_PO_H_SWITCH_POS, true));
                float delta_tmp = new_tmp - current_tmp;
                String d_t = String.format(Locale.US, "%.1f", delta_tmp + current_pos);
                log.debug(" sending delta position T " + d_t);
                basicCommands.setDataPoint(id, ch, S_PO_H_SET, d_t);
                affectedTopics.put(String.format("%s/%s/setpoint", topicPrefix, alias), String.format("%.2f", new_tmp));
            } catch (NumberFormatException ex) {
                log.warn(value + " is not a valid flotingpoint number toppic:" + command);
            }
        }

        return affectedTopics;

    }

    @Override
    public FreeHomeCommandAbstractionInterface execute(Map<String, String> parms, FreeHomeXMPBasicCommands basicCommands) {
        String id;
        String ch;
        if (parms.containsKey("alias")) {
            String path[] = basicCommands.resolveDeviceAlias(parms.get("alias"));
            id = path[0];
            ch = path[1];
        } else {
            id = parms.get("id");
            ch = parms.get("ch");
        }

        log.debug("Heat Request on " + id + ":" + ch);
        // get Parameter;
        if (parms.containsKey("get")) {
            String get = parms.get("get");
            if (get.compareToIgnoreCase("room") == 0) {
                this.result = basicCommands.getValue(id, ch, R_PO_R_TEMP, true);
                return this;
            } else if (get.compareToIgnoreCase("setting") == 0) {
                log.debug("Heat Request setting");
                // first check if heat is on
                if (basicCommands.getValue(id, ch, R_PO_H_ON_OFF, false).equalsIgnoreCase("1")) {
                    this.result = basicCommands.getValue(id, ch, R_PO_H_TEMP, false);

                } else {
                    this.result = "0";
                }

            } else if (get.compareToIgnoreCase("state") == 0) {
                log.debug("Heat Request state");
                if (basicCommands.getValue(id, ch, R_PO_H_ON_OFF, false).compareToIgnoreCase("1") == 0) {
                    this.result = "ON";

                } else {
                    this.result = "OFF";
                }

            }
        } //set Heat Values
        else if (parms.containsKey("set")) {
            String set = parms.get("set");
            if (set.equalsIgnoreCase("on")) {
                // turn heat on
                basicCommands.setDataPoint(id, ch, S_PO_H_ON_OFF, "1");
            } else if (set.equalsIgnoreCase("off")) {
                basicCommands.setDataPoint(id, ch, S_PO_H_ON_OFF, "0");
            } else if (set.equalsIgnoreCase("temp")) {
                // first switch heat on
                basicCommands.setDataPoint(id, ch, S_PO_H_ON_OFF, "1");

                float new_tmp = Float.parseFloat(parms.get("value"));
                if (new_tmp < 0) {
                    new_tmp = 0;
                }
                if (new_tmp > 35) {
                    new_tmp = 35;
                }
                float current_tmp = Float.parseFloat(basicCommands.getValue(id, ch, R_PO_H_TEMP, false));
                float current_pos = Float.parseFloat(basicCommands.getValue(id, ch, R_PO_H_SWITCH_POS, true));
                float delta_tmp = new_tmp - current_tmp;
                String d_t = String.format(Locale.US, "%.1f", delta_tmp + current_pos);
                log.debug(" sending delta position T " + d_t);
                basicCommands.setDataPoint(id, ch, S_PO_H_SET, d_t);
                this.result = basicCommands.getValue(id, ch, R_PO_H_TEMP, true);
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

    @Override
    public Map<String, String> getTopics(FreeHomeXMPBasicCommands command) {

        HashMap<String, String> topics = new HashMap<>();
        String topic = "free_home/heat";
        command.getAlias().keySet().stream().filter((alias) -> alias.toLowerCase().endsWith("heat")).forEach((alias)
                -> {
            String path[] = command.resolveDeviceAlias(alias);
            String id = path[0];
            String ch = path[1];

            topics.put(String.format("%s/%s/temp", topic, alias), command.getValue(id, ch, R_PO_R_TEMP, true));
            topics.put(String.format("%s/%s/state", topic, alias), command.getValue(id, ch, R_PO_H_ON_OFF, true).equalsIgnoreCase("1") ? "ON" : "OFF");
            if (command.getValue(id, ch, R_PO_H_ON_OFF, false).equalsIgnoreCase("1")) {
                topics.put(String.format("%s/%s/setpoint", topic, alias), command.getValue(id, ch, R_PO_H_TEMP, true));

            } else {
                topics.put(String.format("%s/%s/setpoint", topic, alias), "0");
            }
        }
        );
        return topics;
    }

    @Override
    public List<String> subsciptionList(FreeHomeXMPBasicCommands basicCommands) {
        ArrayList<String> subTopics = new ArrayList<>();

        basicCommands.getAlias().keySet().stream().filter((alias) -> alias.toLowerCase().endsWith("heat")).forEach((alias)
                -> {
            subTopics.add(String.format("%s/%s/command/temp", topicPrefix, alias));
            subTopics.add(String.format("%s/%s/command/state", topicPrefix, alias));
        });
        return subTopics;
    }

    @Override
    public boolean matchWithTopicPath(String path) {
        return path.startsWith(topicPrefix);
    }

}
