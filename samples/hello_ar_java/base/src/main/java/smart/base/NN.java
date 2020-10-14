package smart.base;

import android.log.Log;

public class NN extends BN {
    public static final String TRANSFER_AUTH = "/usr/dom/MSBSFBDOM060201.do";//국내이체 인증
    public static final String OPEN_TRANSFER_AUTH = "/usr/koa/MSBSFBKOA990101.do";// 오픈뱅킹이체 인증
    public static final String LOGIN = "/lgn/MSBSFBLGN020101.do";
    public static final String JOIN = "/jin/MSBSFBJIN020101.do";
    public static final String SETTING = "/usr/set/MSBSFBSET010101.do";
    public static final String GUIDE = "/cmn/MSBSFBCOM030101.do";
    public static final String CALL_MULTIPLE = "/cmn/hel/MSBSFBHEL010101.do";
    public static final String OPEN_MANAGE_ACCOUNTS = "/usr/koa/MSBSFBKOA020101.do";//오픈뱅킹 계좌관리

    public static String getUrl(String path) {
        if (path == null || path.length() <= 0) {
            Log.w("!앗 주소가 없다", path);
            return BN.HOST;
        }
        if (path.startsWith("http")) {
            Log.w("!앗 주소가 다른서버다", path);
            return path;
        }
        if (path.startsWith("file://")) {
            Log.w("!앗 주소가 파일이다", path);
            return path;
        }
        return BN.HOST + path;
    }
}