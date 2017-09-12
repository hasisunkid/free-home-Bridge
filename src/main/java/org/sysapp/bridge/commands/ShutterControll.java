/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge.commands;

import java.util.List;
import java.util.Map;
import org.sysapp.bridge.FreeHomeCommandAbstractionInterface;
import org.sysapp.bridge.FreeHomeXMPBasicCommands;

/**
 *
 * @author enrico
 */
public class ShutterControll implements FreeHomeCommandAbstractionInterface {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ShutterControll.class);
    
    public ShutterControll()
    {
     
        
        
        log.info("http://...../shut?id=XXXXX&ch=xxxxx&set=[UP|DOWN|STOP] ");
      
        
        
        
    }
    @Override
    public String getName() {
        return "shut";
    }

    @Override
    public FreeHomeCommandAbstractionInterface execute(Map<String, List<String>> parms, FreeHomeXMPBasicCommands basicCommands) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getStatus() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getResponse() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getErrorMessage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
