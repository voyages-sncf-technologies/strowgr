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
