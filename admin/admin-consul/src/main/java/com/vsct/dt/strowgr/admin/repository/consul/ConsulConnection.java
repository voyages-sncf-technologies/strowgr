package com.vsct.dt.strowgr.admin.repository.consul;

import rx.Observable;

/**
 * ~  Copyright (C) 2016 VSCT
 * ~
 * ~  Licensed under the Apache License, Version 2.0 (the "License");
 * ~  you may not use this file except in compliance with the License.
 * ~  You may obtain a copy of the License at
 * ~
 * ~   http://www.apache.org/licenses/LICENSE-2.0
 * ~
 * ~  Unless required by applicable law or agreed to in writing, software
 * ~  distributed under the License is distributed on an "AS IS" BASIS,
 * ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * ~  See the License for the specific language governing permissions and
 * ~  limitations under the License.
 * ~
 */
public class ConsulConnection {

    public Observable<ConsulRepository.Session> createSession(int ttl, ConsulRepository.SESSION_BEHAVIOR behavior) {
        return null;
    }

    public Observable<ConsulRepository.Session> renewSession(String id) {
        return null;
    }

    public Observable<ConsulRepository.Session> destroySession(String id) {
        return null;
    }
}
