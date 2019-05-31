package net.osmtracker.util;

import org.apache.commons.codec.binary.StringUtils;

import java.util.Arrays;

public class GpxFormattingHelpers {
    public static String NiceTab (int i) {

        StringBuilder sb = new StringBuilder();
        final char[] c = new char[i];
        Arrays.fill(c,'\t');
        sb.append(c);
        return String.valueOf(sb);
    }

    public static String NiceNewline () {
        return "\n";
    }
}
