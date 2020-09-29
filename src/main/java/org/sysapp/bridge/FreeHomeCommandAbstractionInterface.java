/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import java.util.List;
import java.util.Map;

/**
 *
 * @author enrico
 */
public interface FreeHomeCommandAbstractionInterface {
    
    public String getName();
    public FreeHomeCommandAbstractionInterface execute(Map<String, String> parms, FreeHomeXMPBasicCommands basicCommands);
    public Map<String,String> execute(String command,String value, FreeHomeXMPBasicCommands basicCommands);
    public int getStatus();
    public String getResponse();
    public String getErrorMessage();
    public Map<String,String> getTopics(FreeHomeXMPBasicCommands basicCommands);
    public List<String> subsciptionList(FreeHomeXMPBasicCommands basicCommands);
    public boolean matchWithTopicPath(String path);
   
    
}
