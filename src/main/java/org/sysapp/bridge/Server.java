/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import fi.iki.elonen.NanoHTTPD;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.security.auth.login.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.core.config.Configurator;

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
        Configurator.initialize(null, prop.getProperty("log4j.config", "./log4j.xml"));
        log.info("init Server load configuration from :" + args[0]);
        String host = prop.getProperty("sysap.host");
        String name = prop.getProperty("sysap.loginname");
        String pwd = prop.getProperty("sysap.password");
        String domain = prop.getProperty("sysap.service.domain", "busch-jaeger.de");
        String resource = prop.getProperty("sysap.resource");

        String mqtt_broker = prop.getProperty("mqtt.broker", "localhost");
        String mqtt_user = prop.getProperty("mqtt.user", "");
        String mqtt_pwd = prop.getProperty("mqtt.pwd", "");
        long mqtt_poll = Long.valueOf(prop.getProperty("mqtt.pollint", "30"));
        int mqtt_port = Integer.valueOf(prop.getProperty("mqtt.port", "1883"));

        int port_bosh = Integer.valueOf(prop.getProperty("sysap.port.bosh", "5280"));
        int port_tcp = Integer.valueOf(prop.getProperty("sysap.port.tcp", "5222"));
        int port_bridge = Integer.valueOf(prop.getProperty("bridge.port", "8085"));

        boolean xmppDebug = Boolean.valueOf(prop.getProperty("xmpp.debug", "false"));
        boolean http_protokoll = Boolean.valueOf(prop.getProperty("server.http", "false"));
        boolean mqtt_protokoll = Boolean.valueOf(prop.getProperty("server.mqtt", "true"));

        int cacheRetention = Integer.valueOf(prop.getProperty("bridge.cacheretention", "12000"));
        String id = null;

        InputStream in = new URL("http://" + host + "/settings.json").openStream();

        try {

            JsonReader jsonReader = Json.createReader(in);
            JsonObject settingsJS = jsonReader.readObject();
            for (JsonValue val : settingsJS.getJsonArray("users")) {
                String jid = val.asJsonObject().getString("jid");
                log.debug("Name:" + val.asJsonObject().getString("name"));
                log.debug("jid:" + val.asJsonObject().getString("jid"));

                if (name.compareToIgnoreCase(val.asJsonObject().getString("name")) == 0) {
                    log.info("using jid :" + jid);
                    id = jid.split("@")[0];
                    log.info("ID:" + id);
                }

            }

            if (id == null) {
                log.error("Cant find JID for " + name);
                System.exit(-1);
            }

        } finally {
            IOUtils.closeQuietly(in);
        }

        TcpConnectionConfiguration tcpConfiguration = TcpConnectionConfiguration.builder().hostname(host).port(5222)
                .secure(false).build();

        BoshConnectionConfiguration boshConfiguration = BoshConnectionConfiguration.builder().hostname(host).port(5280)
                .secure(false).path("/http-bind/").build();
        XmppSessionConfiguration configuration = null;
        if (xmppDebug) {
            configuration = XmppSessionConfiguration.builder().debugger(ConsoleDebugger.class) // .authenticationMechanisms("DIGEST-MD5")
                    .build();
        } else {
            configuration = XmppSessionConfiguration.builder().build();
        }

        XmppClient xmppClient = XmppClient.create(domain, configuration, tcpConfiguration, boshConfiguration);
        xmppClient.connect();
        xmppClient.login(id, pwd, resource);
        BridgeServer server = new BridgeServer(xmppClient, cacheRetention);
        server.addCommand("org.sysapp.bridge.commands.HeatControll");
        server.addCommand("org.sysapp.bridge.commands.ShutterControll");
        server.registerAliastable(prop);

        if (mqtt_protokoll) {
            MqttServer mqtt = new MqttServer();
            mqtt.start(mqtt_broker, mqtt_port, mqtt_user, mqtt_pwd, mqtt_poll, server);

        }
        if (http_protokoll) {
            HttpServer httpServer = new HttpServer(port_tcp, server);
            httpServer.start();
        }
    }

}
