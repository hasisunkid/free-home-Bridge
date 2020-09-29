/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.sysapp.bridge.BridgeServer.log;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.extensions.rpc.RpcManager;

/**
 *
 * @author eobs
 */
public class HttpServer extends NanoHTTPD{
    
    FreeHomeXMPBasicCommands command ;
    
    public HttpServer(int port, FreeHomeXMPBasicCommands command) throws IOException {
        
        super(port);
        this.command=command;
       log.info("Running! http://localhost:" + port);
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
            String value = command.getValue(id, ch, port, true);

            if (value == null) {

                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/txt", "0");
            }

            return newFixedLengthResponse(Response.Status.OK, "application/txt", value);

        } else if (uri.endsWith("getDevices")) {

            return newFixedLengthResponse(Response.Status.OK, "application/json", command.getDevices(true).toString());
        } else if (uri.endsWith("setDataPoint")) {
            String id = parms.get("id") ;
            String ch = parms.get("ch") ;
            String port = parms.get("port") ;
            String value = parms.get("value") ;

           command.setDataPoint(id, ch, port, value);

            return newFixedLengthResponse("OK");
        } else {
            FreeHomeCommandAbstractionInterface fhcm = command.getCommands().get(uri);
            if (fhcm != null) {
                if (fhcm.execute(parms, command).getStatus() == 200) {
                    return newFixedLengthResponse(fhcm.getResponse());
                } else {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/txt", fhcm.getErrorMessage());
                }

            } else {
                log.warn(uri + " unknowen command");
            }

        }

        return newFixedLengthResponse("OK");
    }

}
