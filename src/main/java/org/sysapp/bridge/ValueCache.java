/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sysapp.bridge;

import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is a Singelton Class to cache Values
 * 
 * @author eobs
 */
public class ValueCache {

    private static ValueCache valueCacheInstance;
    private final HashMap<String, String> valueCache = new HashMap<String, String>();
    private static final Logger log = LogManager.getLogger(ValueCache.class);
    private long lastRefresh;

    public ValueCache() {

        lastRefresh = System.currentTimeMillis();
    }

    public boolean isCahceValid(long retention) {
        boolean valid = ((System.currentTimeMillis() - lastRefresh) < retention) && !this.valueCache.isEmpty();
        log.debug(String.format("cahce is valid %b it retaind %d ms ", valid,
                (System.currentTimeMillis() - lastRefresh)));
        return valid;
    }

    public static ValueCache GetInstance() {
        if (valueCacheInstance == null) {
            log.debug("Create new Instance");
            valueCacheInstance = new ValueCache();
        }
        return valueCacheInstance;
    }

    public void clear() {
        log.debug("Clean Cache");
        lastRefresh = System.currentTimeMillis();

        this.valueCache.clear();
    }

    public void addValue(String path, String value) {
        log.debug("set Value " + path + " :: " + value);
        valueCache.put(path, value.toLowerCase());

    }

    public void addValue(String id, String channel, String port, String value) {
        String path = id + ":" + channel + ":" + port;
        this.addValue(path.toLowerCase(), value);
    }

    public String getValue(String id, String channel, String port) {
        String path = id + ":" + channel + ":" + port;
        return this.getValue(path);

    }

    public String getValue(String path) {
        String value = this.valueCache.get(path.toLowerCase());
        if (value == null) {
            log.warn("No value found for path :" + path);
            value = "na";
        }
        log.debug("value request :" + path + " = " + value);
        return value.toLowerCase();
    }

    public void toLog() {
        this.valueCache.forEach((k, v) -> {
            log.info(String.format("cache %s \t::\t %s", k, v));
        });
    }

}
