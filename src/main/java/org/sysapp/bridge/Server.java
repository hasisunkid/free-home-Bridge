/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom2.JDOMException;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.TcpConnectionConfiguration;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.core.session.XmppSessionConfiguration;
import rocks.xmpp.core.session.debug.ConsoleDebugger;
import rocks.xmpp.extensions.httpbind.BoshConnectionConfiguration;

/**
 *
 * @author eobs
 */
public class Server {

    /**
     * @param args the command line arguments
     */
    private static Logger log = Logger.getLogger("Server");

    public static void main(String[] args) throws XmppException, JDOMException, IOException {
        InputStream input = new FileInputStream(args[0]);
       

        

        // load a properties file
        Properties prop = new Properties();
        prop.load(input);
         PropertyConfigurator.configure(prop);
        log.info("init Server load configuration from :" + args[0]);
        String host = prop.getProperty("sysap.host");
        String name = prop.getProperty("sysap.loginname");
        String pwd = prop.getProperty("sysap.password");
        String domain = prop.getProperty("sysap.service.domain", "busch-jaeger.de");
        String resource = prop.getProperty("sysap.resource");
        String id=null;

        InputStream in = new URL("http://"+host+"/settings.json").openStream();

        try {
            
             JsonReader jsonReader = Json.createReader(in);
             JsonObject settingsJS=jsonReader.readObject();
            for (JsonValue val: settingsJS.getJsonArray("users"))
            {
                String jid=val.asJsonObject().getString("jid");
                log.debug("Name:"+val.asJsonObject().getString("name"));
                log.debug("jid:"+val.asJsonObject().getString("jid"));
                
                if (name.compareToIgnoreCase(val.asJsonObject().getString("name"))==0)
                        {
                            log.info("using jid :"+jid);
                            id=jid.split("@")[0];
                            log.info("ID:"+id);
                        }
                
            }
            
            if (id==null) 
            {
                log.error("Cant find JID for "+name);
                System.exit(-1);
            }
             
            
        } finally {
            IOUtils.closeQuietly(in);
        }

        int port_bosh = Integer.valueOf(prop.getProperty("sysap.port.bosh", "5280"));
        int port_tcp = Integer.valueOf(prop.getProperty("sysap.port.tcp", "5222"));
        int port_bridge = Integer.valueOf(prop.getProperty("bridge.port", "8085"));

        boolean xmppDebug = Boolean.valueOf(prop.getProperty("xmpp.debug", "false"));

        TcpConnectionConfiguration tcpConfiguration = TcpConnectionConfiguration.builder()
                .hostname(host)
                .port(5222).secure(false)
                .build();

        BoshConnectionConfiguration boshConfiguration = BoshConnectionConfiguration.builder()
                .hostname(host)
                .port(5280).secure(false)
                .path("/http-bind/")
                .build();
        XmppSessionConfiguration configuration = null;
        if (xmppDebug) {
            configuration = XmppSessionConfiguration.builder()
                    .debugger(ConsoleDebugger.class) //.authenticationMechanisms("DIGEST-MD5")
                    .build();
        } else {
            configuration = XmppSessionConfiguration.builder()
                    .build();
        }

        XmppClient xmppClient = XmppClient.create(domain, configuration, tcpConfiguration, boshConfiguration);

        new HttpServer(port_bridge, xmppClient);

//    xmppClient.addInboundPresenceListener(e -> {
//        System.out.println(e.getPresence().getStatus());
//    // Handle inbound presence.
//});
//// Listen for messages
//xmppClient.addInboundMessageListener(e -> {System.out.println(e.getMessage().getBody());
//    // Handle inbound message
//});
//// Listen for roster pushes
//xmppClient.getManager(RosterManager.class).addRosterListener(e -> {
//System.out.println(e.toString());
//});
//        RpcManager rpcManager = xmppClient.getManager(RpcManager.class);
//        rpcManager.setRpcHandler((requester, methodName, parameters) -> {
//
//            if (!parameters.isEmpty()) {
//
//                System.out.println("Method name:" + methodName);
//                return Value.of("Colorado");
//
//            }
//            throw new RpcException(123, "Invalid method name or parameter.");
//        });
//
//        xmppClient.connect();
//        // it is not a real user name ???
//        xmppClient.login(id, pwd, resource);
        xmppClient.connect();
//        // it is not a real user name ???
        xmppClient.login(id, pwd, resource);
    }

}
