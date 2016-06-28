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

package com.vsct.dt.strowgr.admin.gui.tasks;

import com.google.common.collect.ImmutableMultimap;
import com.vsct.dt.strowgr.admin.core.EntryPointRepository;
import io.dropwizard.servlets.tasks.Task;

import java.io.PrintWriter;

/**
 * Haproxy VIP task for binding a vip to a haproxy cluster.
 */
public class HaproxyVipTask extends Task {

    private final EntryPointRepository repository;

    public HaproxyVipTask(EntryPointRepository repository) {
        super("haproxy/vip");
        this.repository = repository;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter printWriter) throws Exception {
        repository.setHaproxyVip(parameters.get("haproxy").asList().get(0), parameters.get("vip").asList().get(0));
    }
}
