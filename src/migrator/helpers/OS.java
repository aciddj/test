package migrator.helpers;

/**
 * Created by Walter Ego on 11.05.2017.
 */
public class OS {
    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac");
    }

    public static boolean isNix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    }

    public static boolean isSolaris() {
        return OS.contains("sunos");
    }

    public static String getVersion() {
        return System.getProperty("os.version");
    }

    public static int getVersionFirstDigit() {
        String fullVesion = getVersion();
        int firstDot = fullVesion.indexOf('.');
        return Integer.parseInt(fullVesion.substring(0, firstDot));
    }
}
