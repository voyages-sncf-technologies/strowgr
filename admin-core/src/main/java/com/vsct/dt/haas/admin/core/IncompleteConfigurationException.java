package com.vsct.dt.haas.admin.core;

import java.util.Set;

/**
 * Created by william_montaz on 15/04/2016.
 */
public class IncompleteConfigurationException extends Exception {
    private final Set<String> missingEntries;

    public IncompleteConfigurationException(Set<String> missingEntries) {
        this.missingEntries = missingEntries;
    }

    @Override
    public String getMessage() {
        StringBuilder s = new StringBuilder("Template valorization requires missing entries : \n");
        for(String e : missingEntries){
            s.append("\t- ").append(e).append("\n");
        }
        return s.toString();
    }
}
