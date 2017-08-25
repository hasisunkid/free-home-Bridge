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

    public HttpServer(int port, XmppClient xmppClient) throws IOException {
        super(port);
        this.xmppClient = xmppClient;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        log.info("Running! http://localhost:"+ port);
    }

    @Override
    public Response serve(IHTTPSession session) {

        Method method = session.getMethod();
        String uri = session.getUri();
        log.info(method + " '" + uri + "'  from "+session.getRemoteHostName());
       
        Map<String, List<String>> parms = session.getParameters();

        for (String entr : parms.keySet()) {
            log.debug("Key:" + entr);
        }
        
        if (uri.endsWith("getDevices"))
        {
            return newFixedLengthResponse(Response.Status.OK,"application/json",this.getDevices());
        }
        else if (uri.endsWith("setDataPoint"))
        {
            String id=parms.get("id").get(0);
            String ch=parms.get("ch").get(0);
            String port=parms.get("port").get(0);
            String value=parms.get("value").get(0);
            
            setDataPoint(id, ch, port, value);
            
            return newFixedLengthResponse("OK");
        }
        

        return newFixedLengthResponse("OK");
    }

    private void setDataPoint(String serialNum,String channel,String port, String value)
    {
         RpcManager rpcManager = xmppClient.getManager(RpcManager.class);
        
        try {
            String path=serialNum+"/"+channel+"/"+port;
            log.debug(path+ "set "+value);
    Value response = rpcManager.call(Jid.of("mrha@busch-jaeger.de/rpc"), "RemoteInterface.setDatapoint", Value.of(path),Value.of(value)).getResult();
   log.debug("Result:"); // Colorado
    log.debug(response.getAsString()); // Colorado
    } catch (Exception e) {
    log.error(e);
  }    
    
    
    
    }
    
    private String getDevices()   {
        RpcManager rpcManager = xmppClient.getManager(RpcManager.class);
        
        JsonObjectBuilder resultJS=Json.createObjectBuilder();
        try {
            Value response = rpcManager.call(Jid.of("mrha@busch-jaeger.de/rpc"), "RemoteInterface.getAll", Value.of("de"), Value.of(4), Value.of(0), Value.of(0)).getResult();
            log.debug(response.getAsString()); // Colorado
            Document doc = new SAXBuilder().build(new InputSource(new StringReader(response.getAsString())));
            Element dev = doc.getRootElement().getChild("devices");
            
            
            for (Element el : dev.getChildren()) {
                log.debug("Dev :" + el.getName() + " id :" + el.getAttributeValue("serialNumber"));
                
                
                JsonObjectBuilder device=null;
                for (Element cl : el.getChildren("attribute")) {
                    
                    if (cl.getAttribute("name").getValue().compareToIgnoreCase("displayName") == 0) {
                        log.debug("\tDevice Name " + cl.getText());
                         device=Json.createObjectBuilder();
                         device.add("name", cl.getText());
                         device.add("id", el.getAttributeValue("serialNumber"));
                    }
                }
                
                if (device!=null) 
                {
                    
                
                
                
                Element channels = el.getChild("channels");
                if (channels != null) {
                    for (Element ch : channels.getChildren()) {
                        log.debug("\t\tChanel ID: " + ch.getAttributeValue("i"));
                        JsonObjectBuilder channelJS=Json.createObjectBuilder();
                        
                        
                        for (Element outputs : ch.getChild("outputs").getChildren()) {
                            channelJS.add(outputs.getAttribute("i").getValue(), outputs.getChildText("value"));
                            log.debug("\t\t\t DataPoint :" + outputs.getAttribute("i") + "[" + outputs.getChildText("value") + "]");

                        }
                        
                        for (Element outputs : ch.getChild("inputs").getChildren()) {
                            channelJS.add(outputs.getAttribute("i").getValue(), outputs.getChildText("value"));
                            log.debug("\t\t\t DataPoint :" + outputs.getAttribute("i") + "[" + outputs.getChildText("value") + "]");

                        }
                           
                        device.add(ch.getAttributeValue("i"),channelJS);
                    }
                }
                resultJS.add( el.getAttributeValue("serialNumber"),device);
                }
            }
            
        }
       catch (Exception e) {
    log.error(e);
}
   return resultJS.build().toString();
}   

    }