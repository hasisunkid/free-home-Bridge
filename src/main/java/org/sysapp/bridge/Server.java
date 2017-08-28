/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.TcpConnectionConfiguration;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.core.session.XmppSessionConfiguration;
import rocks.xmpp.core.session.debug.ConsoleDebugger;
import rocks.xmpp.extensions.httpbind.BoshConnectionConfiguration;
import rocks.xmpp.extensions.rpc.RpcException;
import rocks.xmpp.extensions.rpc.RpcManager;
import rocks.xmpp.extensions.rpc.model.Value;

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
        BasicConfigurator.configure();
        log.info("init Server load configuration from :" + args[0]);

        InputStream input = new FileInputStream(args[0]);
        
        

        // load a properties file
        Properties prop = new Properties();
        prop.load(input);
        
        String host=prop.getProperty("sysap.host");
        String id=prop.getProperty("sysap.id");
        String pwd=prop.getProperty("sysap.password");
        String domain=prop.getProperty("sysap.service.domain","busch-jaeger.de");
        String resource=prop.getProperty("sysap.resource");
        
        
        
        int port_bosh= Integer.valueOf(prop.getProperty("sysap.port.bosh","5280"));
        int port_tcp= Integer.valueOf(prop.getProperty("sysap.port.tcp","5222"));
        int port_bridge= Integer.valueOf(prop.getProperty("bridge.port","8085"));
        
        boolean xmppDebug= Boolean.valueOf(prop.getProperty("xmpp.debug","false"));
        

        TcpConnectionConfiguration tcpConfiguration = TcpConnectionConfiguration.builder()
                .hostname(host)
                .port(5222).secure(false)
                .build();

        BoshConnectionConfiguration boshConfiguration = BoshConnectionConfiguration.builder()
                .hostname(host)
                .port(5280).secure(false)
                .path("/http-bind/")
                .build();
        XmppSessionConfiguration configuration=null ;
        if (xmppDebug)
        {
            configuration = XmppSessionConfiguration.builder()
                .debugger(ConsoleDebugger.class) //.authenticationMechanisms("DIGEST-MD5")
                .build();
        }
        else
        {
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


