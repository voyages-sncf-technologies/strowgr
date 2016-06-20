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

package com.vsct.strowgr.monitoring.aggregator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class StringDates {

    private static final DateTimeFormatter dateTimeFormatter  = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int               MILLISEC_PER_SEC   = 1000;
    private static final int               SECONDS_PER_MINUTE = 60;
    private static final int               SECONDS_PER_HOUR   = 60 * SECONDS_PER_MINUTE;
    private static final int               HOURS_PER_DAY      = 24;
    private static final int               SECONDS_PER_DAY    = SECONDS_PER_HOUR * HOURS_PER_DAY;
    private static final int               MILLISEC_PER_DAY   = MILLISEC_PER_SEC * SECONDS_PER_DAY;

    /* ISO_LOCAL_DATE = YYYY-MM-DD */
    public static String ISO_LOCAL_DATE(Date date) {
        return LocalDate.ofEpochDay(date.getTime() / MILLISEC_PER_DAY).format(dateTimeFormatter);
    }

}
