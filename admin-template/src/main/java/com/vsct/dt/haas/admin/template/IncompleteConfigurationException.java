package com.vsct.dt.haas.admin.template;

import com.github.mustachejava.MustacheException;

import java.util.Set;

/**
 * Created by william_montaz on 15/04/2016.
 */
public class IncompleteConfigurationException extends MustacheException {
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

    public Set<String> getMissingEntries() {
        return missingEntries;
    }
}
