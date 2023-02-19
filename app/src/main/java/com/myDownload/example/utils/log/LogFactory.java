package com.myDownload.example.utils.log;

import java.util.Date;

public class LogFactory {
    public static LogData generate(Date date, String location, String message, Throwable ex) {
        return new LogData(date, location, message, ex);
    }
}
