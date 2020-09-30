/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import static org.sysapp.bridge.FreeHomeXMPBasicCommands.requestCacheTime;
import org.xml.sax.InputSource;
import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.extensions.rpc.RpcManager;
import rocks.xmpp.extensions.rpc.model.Value;

/**
 *
 * @author eobs
 */
public class BridgeServer extends FreeHomeXMPBasicCommands {
    
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
        private static boolean reloadCach=false;

    public BridgeServer(XmppClient xmppClient, int cachRetention) {

     
            if (cachRetention == 0) {
                log.info("Disable sysap request Cache");
                this.useCache = false;
            } else {
                log.info("Enable sysap request Cache retention time is " + cachRetention);
                this.useCache = true;
                maxCacheTime = (long) cachRetention;
            }
            
            this.xmppClient = xmppClient;
            //start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            
            rpcManager = xmppClient.getManager(RpcManager.class);
            commands = new HashMap<String, FreeHomeCommandAbstractionInterface>();
            executor.execute(cacheRefresh(false));
               try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
            log.info("Cahce initi done");
        } catch (InterruptedException ex) {
           log.error("Init cahce timeout");
        }
    }

    @Override
    public String getValue(String id, String ch, String port, boolean useCache) {
        
        
        if (!ValueCache.GetInstance().isCahceValid(maxCacheTime))
        {
           log.info("Refresh cache");
        executor.execute(cacheRefresh(false));
               try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
            log.info("cahce reinit done");
        } catch (InterruptedException ex) {
           log.error("Init cache timeout");
        }
        }
        return ValueCache.GetInstance().getValue(id, ch, port);

    }

    public void setDataPoint(String serialNum, String channel, String port, String value) {

        try {
            String path = serialNum + "/" + channel + "/" + port;
            log.debug(path + "set " + value);
            Value response = rpcManager.call(Jid.of("mrha@busch-jaeger.de/rpc"), "RemoteInterface.setDatapoint", Value.of(path), Value.of(value)).getResult();
            log.debug("Result:"); // Colorado
            log.debug(response.getAsString()); // Colorado
            response = null;
               ValueCache.GetInstance().addValue(serialNum,channel,port, "on");
            //System.gc();
        } catch (Exception e) {
            log.error(e);
        }

    }

    private Runnable cacheRefresh(boolean useCache) {
        
       
        Runnable cahchRefresh = new Runnable() {
            @Override
            public void run() {
//                if (((System.currentTimeMillis() - requestCacheTime) < maxCacheTime) && useCache) {
//                    log.debug("Read from Cahche ");
//
//                } 
//                else if(reloadCach)
//                {
//                    log.debug("cache reload in progress skip it");
//                }else {
                    log.debug("Reload Cache");
                    reloadCach=true;
                    try {
                        Value response = rpcManager.call(Jid.of("mrha@busch-jaeger.de/rpc"), "RemoteInterface.getAll", Value.of("de"), Value.of(4), Value.of(0), Value.of(0)).getResult();
                        //log.debug(response.getAsString()); // Colorado
                        Document doc = new SAXBuilder().build(new InputSource(new StringReader(response.getAsString())));
                        Element dev = doc.getRootElement().getChild("devices");
                        ValueCache vc = ValueCache.GetInstance();
                        vc.clear();
                        for (Element el : dev.getChildren()) {
                            log.debug("Dev :" + el.getName() + " id :" + el.getAttributeValue("serialNumber"));
                            String id = el.getAttributeValue("serialNumber");

                            Element channels = el.getChild("channels");
                            if (channels != null) {
                                for (Element ch : channels.getChildren()) {
                                    log.debug("\t\tChanel ID: " + ch.getAttributeValue("i"));
                                    String chID = ch.getAttributeValue("i");

                                    for (Element outputs : ch.getChild("outputs").getChildren()) {
                                        //channelJS.add(outputs.getAttribute("i").getValue(), outputs.getChildText("value"));
                                        log.debug("\t\t\t DataPoint :" + outputs.getAttribute("i") + "[" + outputs.getChildText("value") + "]");
                                        vc.addValue(id, chID, outputs.getAttribute("i").getValue(), outputs.getChildText("value"));
                                    }

                                    for (Element outputs : ch.getChild("inputs").getChildren()) {
                                        //channelJS.add(outputs.getAttribute("i").getValue(), outputs.getChildText("value"));
                                        log.debug("\t\t\t DataPoint :" + outputs.getAttribute("i") + "[" + outputs.getChildText("value") + "]");
                                        vc.addValue(id, chID, outputs.getAttribute("i").getValue(), outputs.getChildText("value"));
                                    }

                                }
                            }

                        }
                       
                       
                    } catch (Exception e) {
                        log.error(e);
                    }
                  
                
                 
            }
            
        };
        
        return cahchRefresh;
        
    }

    @Override
    public JsonObject getDevices(boolean useCahce) {

        if (((System.currentTimeMillis() - requestCacheTime) < maxCacheTime) && (useCahce)) {
            log.info("Read from Cahche ");
            return statusJS;
        } else {
            requestCacheTime = System.currentTimeMillis();

        }

        JsonObjectBuilder resultJS = Json.createObjectBuilder();
        try {
            Value response = rpcManager.call(Jid.of("mrha@busch-jaeger.de/rpc"), "RemoteInterface.getAll", Value.of("de"), Value.of(4), Value.of(0), Value.of(0)).getResult();
            //log.debug(response.getAsString()); // Colorado
            Document doc = new SAXBuilder().build(new InputSource(new StringReader(response.getAsString())));
            Element dev = doc.getRootElement().getChild("devices");

            for (Element el : dev.getChildren()) {
                log.debug("Dev :" + el.getName() + " id :" + el.getAttributeValue("serialNumber"));

                JsonObjectBuilder device = null;
                for (Element cl : el.getChildren("attribute")) {

                    if (cl.getAttribute("name").getValue().compareToIgnoreCase("displayName") == 0) {
                        log.debug("\tDevice Name " + cl.getText());
                        device = Json.createObjectBuilder();
                        device.add("name", cl.getText());
                        device.add("id", el.getAttributeValue("serialNumber"));
                    }
                }

                if (device != null) {

                    Element channels = el.getChild("channels");
                    if (channels != null) {
                        for (Element ch : channels.getChildren()) {
                            log.debug("\t\tChanel ID: " + ch.getAttributeValue("i"));
                            JsonObjectBuilder channelJS = Json.createObjectBuilder();

                            for (Element outputs : ch.getChild("outputs").getChildren()) {
                                channelJS.add(outputs.getAttribute("i").getValue(), outputs.getChildText("value"));
                                log.debug("\t\t\t DataPoint :" + outputs.getAttribute("i") + "[" + outputs.getChildText("value") + "]");

                            }

                            for (Element outputs : ch.getChild("inputs").getChildren()) {
                                channelJS.add(outputs.getAttribute("i").getValue(), outputs.getChildText("value"));
                                log.debug("\t\t\t DataPoint :" + outputs.getAttribute("i") + "[" + outputs.getChildText("value") + "]");

                            }

                            device.add(ch.getAttributeValue("i"), channelJS);
                        }
                    }
                    resultJS.add(el.getAttributeValue("serialNumber"), device);
                }
            }
            /**
             * doc = null; dev = null; response = null; System.gc();
            *
             */
        } catch (Exception e) {
            log.error(e);
        }

        JsonObject result = resultJS.build();
        if (result != null) {
            log.debug("Cache is valid ");
            statusJS = result;
        } else {
            log.warn("Cahce not valid using old one");
        }

        return statusJS;
    }

    public void addCommand(String className) {
        try {
            Class commandClass = Class.forName(className);
            FreeHomeCommandAbstractionInterface commandInst = (FreeHomeCommandAbstractionInterface) commandClass.newInstance();
            this.commands.put("/" + commandInst.getName(), commandInst);

        } catch (Exception ex) {
            log.error("Cant use command " + className);
            log.error(ex);
        }

    }

    @Override
    public String[] resolveDeviceAlias(String alias) {
        String path[] = this.alias.get(alias);
        log.info("resolve " + alias + " to :" + path[0] + "/" + path[1]);
        return path;
    }

    public void registerAliastable(Properties prop) {
        this.alias = new HashMap<String, String[]>();
        for (Map.Entry<Object, Object> e : prop.entrySet()) {
            String key = (String) e.getKey();
            if (key.startsWith("alias")) {
                String aliasName = key.split("\\.")[1];

                String value = (String) e.getValue();
                this.alias.put(aliasName, value.split("/"));
                log.info("Alias " + aliasName + " = " + value);
            }

        }
    }

}
