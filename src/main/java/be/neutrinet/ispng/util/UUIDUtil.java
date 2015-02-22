package be.neutrinet.ispng.util;

/**
 * Created by wannes on 2/20/15.
 */
public class UUIDUtil {

    /**
     * Determines if a string is a uuid.
     *
     * @param uuid the uuid to check
     * @return true if the string is a uuid
     */
    public static boolean isUUID(String uuid) {
        if (uuid.length() == 36) {
            String[] parts = uuid.split("-");
            if (parts.length == 5) {
                if ((parts[0].length() == 8) && (parts[1].length() == 4) &&
                        (parts[2].length() == 4) && (parts[3].length() == 4) &&
                        (parts[4].length() == 12)) {
                    return true;
                }
            }
        }

        return false;
    }
}
