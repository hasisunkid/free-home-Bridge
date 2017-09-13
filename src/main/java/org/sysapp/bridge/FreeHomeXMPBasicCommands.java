/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

/**
 *
 * @author enrico
 */
public interface FreeHomeXMPBasicCommands {
     public void setDataPoint(String serialNum, String channel, String port, String value) ;
     public String getValue(String id, String ch, String port,boolean useCache) ;
     public String[] resolveDeviceAlias(String alias);
}
