/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge.commands;

import java.util.ArrayList;
import java.util.HashMap;
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
    
    private final String topicPrefix = "free_home/shutter";
    
    public ShutterControll() {

        log.info("http://...../shut?id=XXXXX&ch=xxxxx&set=[UP|DOWN|STOP] ");

    }

    @Override
    public String getName() {
        return "shut";
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
        // get Parameter;
        if (parms.containsKey("set")) {
            String get = parms.get("set");
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

    @Override
    public Map<String, String> getTopics(FreeHomeXMPBasicCommands basicCommands) {
        HashMap<String, String> topics = new HashMap<>();
         basicCommands.getAlias().keySet().stream().filter((alias) -> alias.toLowerCase().endsWith("shut")).forEach((alias)
                -> {
            String path[] = basicCommands.resolveDeviceAlias(alias);
            String id = path[0];
            String ch = path[1];

             topics.put(String.format("%s/%s/state", topicPrefix, alias), "stop");
              
        }
        );
        return topics;
    }

    @Override
    public List<String> subsciptionList(FreeHomeXMPBasicCommands basicCommands) {
            ArrayList<String> subTopics = new ArrayList<>();

        basicCommands.getAlias().keySet().stream().filter((alias) -> alias.toLowerCase().endsWith("shut")).forEach((alias)
                -> {
            subTopics.add(String.format("%s/%s/command", topicPrefix, alias));
             
        });
        return subTopics;
    }

    @Override
    public Map<String,String> execute(String command, String value, FreeHomeXMPBasicCommands basicCommands) {
        HashMap<String, String> affectedTopics = new HashMap<>();
        String alias = command.split("/")[2];
        String path[] = basicCommands.resolveDeviceAlias(alias);
        String id = path[0];
        String ch = path[1];
        boolean   invalid=false;
         if (command.endsWith("command")) {
            log.debug("change shutter state "+command+" "+value);
            if (value.equalsIgnoreCase("up")) {
                
               basicCommands.setDataPoint(id, ch, S_PO_UP_DOWN, "0");
               affectedTopics.put(String.format("%s/%s/state", topicPrefix, alias), "up");
            } else if (value.equalsIgnoreCase("down"))
            {
                  basicCommands.setDataPoint(id, ch, S_PO_UP_DOWN, "1");
                  affectedTopics.put(String.format("%s/%s/state", topicPrefix, alias), "down");
            } else if (value.equalsIgnoreCase("stop"))
            {
                    
                 basicCommands.setDataPoint(id, ch, S_PO_STOP, "1");
                 affectedTopics.put(String.format("%s/%s/state", topicPrefix, alias), "stop");
            }else
            {
                invalid=true;
            }
         
         }
         
         if (invalid)
         {
             log.warn(String.format("command (%s) not valid with %S", command,value));
         }
        return affectedTopics;
    }

    @Override
    public boolean matchWithTopicPath(String path) {
          return path.startsWith(topicPrefix);
    }

}
