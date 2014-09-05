package net.wgr.core;

/**
 * Created by wannes on 8/30/14.
 */
public class StringUtils {

    /**
     * Only checks for positive numbers
     * http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-a-numeric-type-in-java
     * @param str
     * @return
     */
    public static boolean isNumeric(String str)
    {
        for (char c : str.toCharArray())
        {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
}
