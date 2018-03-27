/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;
import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.extensions.rpc.RpcManager;
import rocks.xmpp.extensions.rpc.model.Value;

/**
 *
 * @author eobs
 */
public class HttpServer extends NanoHTTPD implements FreeHomeXMPBasicCommands {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HttpServer.class);
    private XmppClient xmppClient;
    private RpcManager rpcManager;
    private static long requestCacheTime = 0;
    private static JsonObject statusJS;
    private static long maxCacheTime = 120000;
    private HashMap<String, FreeHomeCommandAbstractionInterface> commands;
    private HashMap<String, String[]> alias;
    private boolean useCache=true;

    public HttpServer(int port, XmppClient xmppClient,long cahceRetetnin) throws IOException {
        super(port);
        
        if (cahceRetetnin==0)
        {
            log.info("Disable sysap request Cache");
            this.useCache=false;
        }
        else
        {
            log.info("Enable sysap request Cache retention time is "+cahceRetetnin);
            this.useCache=true;
            maxCacheTime=(long) cahceRetetnin;
        }
        
        this.xmppClient = xmppClient;
        //start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        log.info("Running! http://localhost:" + port);
        rpcManager = xmppClient.getManager(RpcManager.class);
        commands = new HashMap<String, FreeHomeCommandAbstractionInterface>();
    }

    @Override
    public Response serve(IHTTPSession session) {

        Method method = session.getMethod();
        String uri = session.getUri();
        //log.info(method + " '" + uri + "'  from " + session.getRemoteHostName());

        Map<String, String> parms = session.getParms();
        
        for (String entr : parms.keySet()) {
            log.debug("Key:" + entr);
        }
        if (uri.endsWith("getSingleValue")) {

            String id = parms.get("id") ;
            String ch = parms.get("ch") ;
            String port = parms.get("port") ;
            String value = getValue(id, ch, port, true);

            if (value == null) {

                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/txt", "0");
            }

            return newFixedLengthResponse(Response.Status.OK, "application/txt", value);

        } else if (uri.endsWith("getDevices")) {

            return newFixedLengthResponse(Response.Status.OK, "application/json", this.getDevices(true).toString());
        } else if (uri.endsWith("setDataPoint")) {
            String id = parms.get("id") ;
            String ch = parms.get("ch") ;
            String port = parms.get("port") ;
            String value = parms.get("value") ;

            setDataPoint(id, ch, port, value);

            return newFixedLengthResponse("OK");
        } else {
            FreeHomeCommandAbstractionInterface command = commands.get(uri);
            if (command != null) {
                if (command.execute(parms, this).getStatus() == 200) {
                    return newFixedLengthResponse(command.getResponse());
                } else {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/txt", command.getErrorMessage());
                }

            } else {
                log.warn(uri + " unknowen command");
            }

        }

        return newFixedLengthResponse("OK");
    }

    public String getValue(String id, String ch, String port, boolean useCache) {
        String value = null;
        try {
            
            JsonObject idJS = this.getDevices(useCache).getJsonObject(id);
            JsonObject chJS = idJS.getJsonObject(ch);
            value = chJS.getString(port);
        } catch (Exception e) {
            log.debug("Caller ID:"+id+" Channel: "+ch+" Port:"+port);
            log.debug("===========================");
            log.debug(statusJS);
            
            log.debug("===========================");
            
            log.error("Value Problem", e);

        }
        return value;
    }

    public void setDataPoint(String serialNum, String channel, String port, String value) {

        try {
            String path = serialNum + "/" + channel + "/" + port;
            log.debug(path + "set " + value);
            Value response = rpcManager.call(Jid.of("mrha@busch-jaeger.de/rpc"), "RemoteInterface.setDatapoint", Value.of(path), Value.of(value)).getResult();
            log.debug("Result:"); // Colorado
            log.debug(response.getAsString()); // Colorado
            response = null;
            System.gc();
        } catch (Exception e) {
            log.error(e);
        }

    }

    private JsonObject getDevices(boolean useCahce) {

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
            doc = null;
            dev = null;
            response = null;
            System.gc();
        } catch (Exception e) {
            log.error(e);
        }
        statusJS = resultJS.build();
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
        log.info("resolve "+alias+" to :"+path[0]+"/"+path[1]);
        return path;
    }

    public void registerAliastable(Properties prop) {
        this.alias=new HashMap<String,String[]>();
        for (Map.Entry<Object, Object> e : prop.entrySet()) {
            String key = (String) e.getKey();
            if (key.startsWith("alias"))
            {
                String aliasName =key.split("\\.")[1];
                 
                 String value = (String) e.getValue();
                 this.alias.put(aliasName, value.split("/"));
                 log.info("Alias "+aliasName+" = "+value);
            }
           

        }
    }

}
