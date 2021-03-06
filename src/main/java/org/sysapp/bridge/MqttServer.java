/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author eobs
 */
public class MqttServer extends TimerTask implements MqttCallback {

    /**
     * @param args the command line arguments
     */
    private static final Logger log = LogManager.getLogger(MqttServer.class);

    private final HashMap<String, String> topics;

    private final MemoryPersistence persistence = new MemoryPersistence();

    private MqttClient mqttClient;

    private FreeHomeXMPBasicCommands commands;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static boolean requestRunning = false;

    public MqttServer() {
        this.topics = new HashMap<>();
    }

    public void start(String host, int port, String user, String pwd, long poolIntervall,
            FreeHomeXMPBasicCommands commands) {

        try {
            String broker = String.format("tcp://%s:%d", host, port);
            String clientId = "Free_Home_Bridge";
            mqttClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName("iobroker");
            connOpts.setPassword("123".toCharArray());
            connOpts.setCleanSession(true);
            
            log.info("Connecting to broker: " + broker);
            mqttClient.setCallback(this);
            mqttClient.connect(connOpts);
            log.info("Connected");
            this.commands = commands;

            // MqttMessage message = new MqttMessage("23".getBytes());
            // sampleClient.publish("freehome/heat/ist", message);
            subscribe(commands);
            Timer t = new Timer();
            t.scheduleAtFixedRate(this, 0, poolIntervall * 1000);
        } catch (MqttException ex) {
            log.error("Server init error", ex);
        }
    }

    @Override
    public void run() {
        log.info("Start polling values");

        buildToppicList(commands);

    }

    private void subscribe(FreeHomeXMPBasicCommands basicCommand) {
        basicCommand.getCommands().forEach((name, command) -> {
            command.subsciptionList(basicCommand).forEach((subTop) -> {
                MqttMessage message = new MqttMessage("".getBytes());
                try {
                    //this.mqttClient.publish(subTop, message);
                    this.mqttClient.subscribe(subTop);
                    log.info("subscribe to"+subTop);
                } catch (MqttException ex) {
                    log.error("can't publish topic", ex);
                }
            });
        });

    }

    private void buildToppicList(FreeHomeXMPBasicCommands basicCommand) {

        Runnable toppocBuildExecutor = new Runnable() {
            @Override
            public void run() {
                if (requestRunning) {
                    log.debug("skip run");
                    return;
                }
                requestRunning = true;
                basicCommand.getCommands().forEach((name, command) -> {
                    command.getTopics(basicCommand).forEach((topic, value) -> {
                        String tValue = topics.getOrDefault(topic, null);
                        if (tValue == null || (!tValue.equalsIgnoreCase(value) && (value.length() > 0))) {
                            log.info(String.format("Change on topic %s : %s old value was %s", topic, value, tValue));
                            MqttMessage message = new MqttMessage(value.getBytes());
                            topics.put(topic, value);

                            try {
                                mqttClient.publish(topic, message);
                            } catch (MqttException ex) {
                                log.error("can't publish topic", ex);
                            }
                        }
                        requestRunning = false;
                    });
                });
            }
        };

        executor.execute(toppocBuildExecutor);
        // try {
        // executor.shutdown();
        // executor.awaitTermination(1, TimeUnit.MINUTES);
        // } catch (InterruptedException ex) {
        // log.warn("timeout ", ex);
        // }
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        
        
        int conncount=1;
        while (!mqttClient.isConnected()){
            log.warn("Connection lost waiting 10 s try nr:"+conncount, thrwbl);
                conncount++;
        try {
            TimeUnit.SECONDS.sleep(10);
            mqttClient.reconnect();
            log.info("reconnect");
        } catch (MqttException | InterruptedException ex) {
            log.error("can' t reconnect");
        }
        } 
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        log.info(topic + " " + new String(mm.getPayload()));

        Runnable deligate = new Runnable() {
            @Override
            public void run() {
                commands.getCommands().values().stream().filter((comm) -> comm.matchWithTopicPath(topic)).findFirst()
                        .ifPresent((comm) -> {
                            comm.execute(topic, new String(mm.getPayload()), commands).forEach((t, v) -> {
                                topics.put(t, v);
                                MqttMessage message = new MqttMessage(v.getBytes());

                                try {
                                    log.info(String.format("publish %s :%s", t, v));
                                    mqttClient.publish(t, message);
                                } catch (MqttException ex) {
                                    log.error("can't publish topic", ex);
                                }
                            });

                        });
            }
        };
        executor.execute(deligate);

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {

        log.debug("delivery compleet OK ");

    }

}
