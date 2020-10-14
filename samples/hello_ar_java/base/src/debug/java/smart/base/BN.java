package smart.base;

import android.base.CN;

public class BN {
    public static String HOST = "https://dev11-sfb.kebhana.com:18880";
    public static String IMPORT_HOST = "211.32.31.141";
    public static int IMPORT_PORT = 8080;
    public static boolean IS_DEV_FIDO_SERVER = true;

    static void host(CN server) {
        switch (server) {
            case REAL:
                HOST = "https://sfb.kebhana.com";
                IS_DEV_FIDO_SERVER = false;
                IMPORT_HOST = "image.kebhana.com";
                IMPORT_PORT = 8080;
                break;
            case STG:
                HOST = "https://stg11-sfb.kebhana.com:18880";
                IS_DEV_FIDO_SERVER = true;
                IMPORT_HOST = "211.32.31.141";
                IMPORT_PORT = 8080;
                break;
            case DEV:
                HOST = "https://dev11-sfb.kebhana.com:18880";
                IS_DEV_FIDO_SERVER = true;
                IMPORT_HOST = "211.32.31.141";
                IMPORT_PORT = 8080;
                break;
        }
    }
}