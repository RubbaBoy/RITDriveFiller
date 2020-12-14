package is.yarr.rdf;

public class Utility {

    /**
     * Gets a human readable form of the given bytes, e.g. 1000 to 1 KB.
     * Source by aioobe on StackOverflow
     *
     * @param bytes The amount of bytes
     * @return The human-readable simplified form
     * @see <a href="https://stackoverflow.com/a/3758880/3929546">How to convert byte size into human readable format in Java? - aioobe</a>
     */
    public static String humanReadableByteCountSI(long bytes) {
        String s = bytes < 0 ? "-" : "";
        long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        return b < 1000L ? bytes + " B"
                : b < 999_950L ? String.format("%s%.1f kB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f MB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f GB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f TB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f PB", s, b / 1e3)
                : String.format("%s%.1f EB", s, b / 1e6);
    }
}
