/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
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
public class HttpServer extends NanoHTTPD {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HttpServer.class);
    private XmppClient xmppClient;
    private RpcManager rpcManager;
    private static long requestCacheTime=0;
    private JsonObject statusJS;
    private static long maxCacheTime=120000;

    public HttpServer(int port, XmppClient xmppClient) throws IOException {
        super(port);
        this.xmppClient = xmppClient;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        log.info("Running! http://localhost:" + port);
         rpcManager= xmppClient.getManager(RpcManager.class);
    }

    @Override
    public Response serve(IHTTPSession session) {

        Method method = session.getMethod();
        String uri = session.getUri();
        log.info(method + " '" + uri + "'  from " + session.getRemoteHostName());

        Map<String, List<String>> parms = session.getParameters();

        for (String entr : parms.keySet()) {
            log.debug("Key:" + entr);
        }
        if (uri.endsWith("getSingleValue"))
        {
            
            String id = parms.get("id").get(0);
            String ch = parms.get("ch").get(0);
            String port = parms.get("port").get(0);
            String value;
            try
            {
            JsonObject idJS=this.getDevices().getJsonObject(id);
            JsonObject chJS=idJS.getJsonObject(ch);
            value=chJS.getString(port);
            }
            catch (Exception e)
            {
                log.error("Value Problem",e);
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/txt","0");
            }
                
            return newFixedLengthResponse(Response.Status.OK, "application/txt", value);
            
        }
        else if  (uri.endsWith("getDevices")) {
            
            return newFixedLengthResponse(Response.Status.OK, "application/json", this.getDevices().toString());
        } else if (uri.endsWith("setDataPoint")) {
            String id = parms.get("id").get(0);
            String ch = parms.get("ch").get(0);
            String port = parms.get("port").get(0);
            String value = parms.get("value").get(0);

            setDataPoint(id, ch, port, value);

            return newFixedLengthResponse("OK");
        }

        return newFixedLengthResponse("OK");
    }

    private void setDataPoint(String serialNum, String channel, String port, String value) {

        try {
            String path = serialNum + "/" + channel + "/" + port;
            log.debug(path + "set " + value);
            Value response = rpcManager.call(Jid.of("mrha@busch-jaeger.de/rpc"), "RemoteInterface.setDatapoint", Value.of(path), Value.of(value)).getResult();
            log.debug("Result:"); // Colorado
            log.debug(response.getAsString()); // Colorado
            response=null;
            System.gc();
        } catch (Exception e) {
            log.error(e);
        }

    }

    private JsonObject getDevices() {
        
        if ((System.currentTimeMillis()-requestCacheTime)<maxCacheTime)
            {
                log.info("Read from Cahche ");
                return statusJS ; 
            }
            else
            {
                requestCacheTime=System.currentTimeMillis();
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
         doc=null;
         dev=null;
         response=null;
         System.gc();
        } catch (Exception e) {
            log.error(e);
        }
        statusJS=resultJS.build();
        return statusJS ;
    }

}
