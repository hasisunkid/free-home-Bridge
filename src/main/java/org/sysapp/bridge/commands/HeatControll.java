/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge.commands;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.sysapp.bridge.FreeHomeCommandAbstractionInterface;
import org.sysapp.bridge.FreeHomeXMPBasicCommands;

/**
 *
 * @author enrico
 */
public class HeatControll implements FreeHomeCommandAbstractionInterface {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HeatControll.class);
    private final static String R_PO_R_TEMP="odp0010" ;
    private final static String R_PO_H_TEMP="odp0006" ;
    private final static String R_PO_H_SWITCH_POS="odp0007" ;
    private final static String R_PO_H_ON_OFF="odp0008" ;
    private final static String S_PO_H_ON_OFF="idp0011" ;
    private final static String S_PO_H_SET="idp000E" ;
    private int status = 200;
    private String error="no Errors";
    private String result="";

    
    public HeatControll()
    {
        log.info("HeatControll command enabled");
        log.info("http://...../heat?id=XXXXX&ch=xxxxx&get=room  returns room temperature");
        log.info("http://...../heat?id=XXXXX&ch=xxxxx&get=setting  returns current setted temperature");
        log.info("http://...../heat?id=XXXXX&ch=xxxxx&get=state returns ON OFF");
        log.info("----------------------------------------------------------------------------------------");
        
        log.info("http://...../heat?id=XXXXX&ch=xxxxx&set=[ON|OFF] switche ON OFF");
        log.info("http://...../heat?id=XXXXX&ch=xxxxx&set=temp&value=[degr] adjust heat tempreture value float");
        log.info("----------------------------------------------------------------------------------------");
        log.info("----------------------------------------------------------------------------------------");
        
        
        
    }
    @Override
    public String getName() {
        return "heat";
    }

    @Override
    public FreeHomeCommandAbstractionInterface execute(Map<String, String> parms, FreeHomeXMPBasicCommands basicCommands) {
         String id  ;
         String ch;
        if (parms.containsKey("alias"))
        {
            String path[] = basicCommands.resolveDeviceAlias(parms.get("alias"));
            id = path [0];
            ch = path [1];
        }
        else
        {
         id = parms.get("id");
         ch  = parms.get("ch");
        }
        
        log.debug("Heat Request on "+id+":"+ch);
        // get Parameter;
        if (parms.containsKey("get"))
        {
             String get= parms.get("get");
             if (get.compareToIgnoreCase("room")==0)
             {
                 this.result=basicCommands.getValue(id, ch, R_PO_R_TEMP, true);
                 return this;
             }
             else if (get.compareToIgnoreCase("setting")==0)
             {
                 log.debug("Heat Request setting");
                 // first check if heat is on
                  if (basicCommands.getValue(id, ch, R_PO_H_ON_OFF, false).equalsIgnoreCase("1"))
                  {
                  this.result=basicCommands.getValue(id, ch, R_PO_H_TEMP, false);
                  
                  }
                  else
                  {
                  this.result="0";
                  }
                 
             }
             else if (get.compareToIgnoreCase("state")==0)
                     {
                         log.debug("Heat Request state");
                         if (basicCommands.getValue(id, ch, R_PO_H_ON_OFF, false).compareToIgnoreCase("1")==0)
                         {
                             this.result="ON";
                             
                         }
                         else
                         {
                             this.result="OFF";
                         }
                         
                     }
        }
        //set Heat Values
        else if (parms.containsKey("set"))
        { String set= parms.get("set");
            if (set.equalsIgnoreCase("on"))
            {
              // turn heat on
                basicCommands.setDataPoint(id, ch, S_PO_H_ON_OFF, "1");
            }
            else if (set.equalsIgnoreCase("off"))
            {
                basicCommands.setDataPoint(id, ch, S_PO_H_ON_OFF, "0");
            }
            else if (set.equalsIgnoreCase("temp"))
            {
                // first switch heat on
                basicCommands.setDataPoint(id, ch, S_PO_H_ON_OFF, "1");
                
                float new_tmp=Float.parseFloat(parms.get("value"));
                if (new_tmp<0 ) new_tmp=0;
                if (new_tmp>35) new_tmp=35;
                float current_tmp=Float.parseFloat(basicCommands.getValue(id, ch, R_PO_H_TEMP, false));
                float current_pos=Float.parseFloat(basicCommands.getValue(id, ch, R_PO_H_SWITCH_POS, true));
                float delta_tmp=new_tmp-current_tmp;
                String d_t=String.format(Locale.US,"%.1f", delta_tmp+current_pos);
                log.debug(" sending delta position T "+d_t);
                basicCommands.setDataPoint(id, ch, S_PO_H_SET, d_t);
                this.result=basicCommands.getValue(id, ch, R_PO_H_TEMP, true);
            }
        }
        return this;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getResponse() {
        return result;
    }

    @Override
    public String getErrorMessage() {
        return error;
    }
    
}
