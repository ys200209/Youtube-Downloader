package com.myDownload.example;

import android.provider.BaseColumns;

public final class RegisterRequest {

    public static final class CreateTB implements BaseColumns {
        //public static final String KEYNUM = "keynum";
        public static final String TITLE = "title";
        public static final String THUMBNAIL = "thumbnail";
        public static final String PROGRESS = "progress";
        public static final String ISDOWNLOAD = "isdownload"; // 다운로드중 | 일시정지 | 다운로드됨 | 다운로드 실패
        public static final String FILESIZE = "filesize";
        public static final String VIDEOURL = "videourl";
        public static final String QUALITY = "quality";
        public static final String _TABLENAME2 = "testtableaa";
        public static final String _CREATE2 = "create table if not exists "+_TABLENAME2+"("
                /*+KEYNUM+" INTEGER PRIMARY KEY AUTOINCREMENT, " */+TITLE+" text NOT NULL PRIMARY KEY, "
                +THUMBNAIL+" blob NOT NULL, " +PROGRESS+" text NOT NULL, "
                +ISDOWNLOAD+" text NOT NULL, " +FILESIZE+" text NOT NULL, "
                +VIDEOURL+" text NOT NULL, " +QUALITY+" text NOT NULL );";
    }

}