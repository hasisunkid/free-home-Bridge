/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import java.util.HashMap;
 

/**
 * This is a Singelton Class to cache Values
 * @author eobs
 */
public class ValueCache {
    
    private static ValueCache valueCacheInstance;
    private final HashMap<String,String> valueCache=new HashMap<String, String>();
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ValueCache.class);
    public static ValueCache GetInstance()
    {
        if (valueCacheInstance==null)
        {
            log.debug("Create new Instance");
            valueCacheInstance=new ValueCache();
        }
        return valueCacheInstance;
    }
    
    public void clear()
    {
        log.debug("Clean Cache");
        this.valueCache.clear();
    }
    
    public void addValue(String path,String value)
    {
        log.debug("set Value "+path+" :: "+value);
        valueCache.put(path,value);
    }
    
    public void addValue(String id, String channel, String port,String value)
    {
        String path=id+":"+channel+":"+port;
        this.addValue(path.toUpperCase(), value);
    }
    
    public String getValue(String id, String channel, String port)
    {
        String path=id+":"+channel+":"+port;
        return this.getValue(path);
        
    }
    
    public String getValue(String path)
    {
        String value= this.valueCache.get(path.toUpperCase());
        if (value== null)
        {
            log.warn("No value found for path :"+path);
            value="";
        }
        log.debug("value request :"+path+" = "+value);
        return value;
    }
    
    
}
