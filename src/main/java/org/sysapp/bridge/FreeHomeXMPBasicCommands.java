/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import java.util.HashMap;
 
import javax.json.JsonObject;
import org.apache.logging.log4j.LogManager;
 
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.extensions.rpc.RpcManager;

import org.apache.logging.log4j.Logger;


/**
 *
 * @author enrico
 */
public abstract class  FreeHomeXMPBasicCommands {
     public abstract void setDataPoint(String serialNum, String channel, String port, String value) ;
     public abstract String getValue(String id, String ch, String port,boolean useCache) ;
     public abstract String[] resolveDeviceAlias(String alias);
     public abstract JsonObject getDevices(boolean useCahce);
     
      
    public static Logger log =  LogManager.getLogger(FreeHomeXMPBasicCommands.class);
    protected XmppClient xmppClient;
    protected RpcManager rpcManager;
    protected static long requestCacheTime = 0;
    protected static JsonObject statusJS;
    protected static long maxCacheTime = 120000;

    public XmppClient getXmppClient() {
        return xmppClient;
    }

    public RpcManager getRpcManager() {
        return rpcManager;
    }

    public static long getRequestCacheTime() {
        return requestCacheTime;
    }

    public static JsonObject getStatusJS() {
        return statusJS;
    }

    public static long getMaxCacheTime() {
        return maxCacheTime;
    }

    public HashMap<String, FreeHomeCommandAbstractionInterface> getCommands() {
        return commands;
    }

    public HashMap<String, String[]> getAlias() {
        return alias;
    }

    public boolean isUseCache() {
        return useCache;
    }
    protected HashMap<String, FreeHomeCommandAbstractionInterface> commands;
    protected HashMap<String, String[]> alias;
    protected boolean useCache=true;
}
