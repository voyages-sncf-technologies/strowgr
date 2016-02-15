package com.vsct.dt.haas.admin;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Preconditions {

    public static void checkStringNotEmpty(String s, String message) {
        checkNotNull(s, message);
        checkArgument(s.length() > 0, message);
    }
}
