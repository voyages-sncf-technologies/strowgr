/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.template;

import com.github.mustachejava.MustacheException;

import java.util.HashSet;
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
        return new HashSet<>(missingEntries);
    }
}
