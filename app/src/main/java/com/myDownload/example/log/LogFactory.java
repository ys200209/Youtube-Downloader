package com.myDownload.example.log;

import java.util.Date;

public class LogFactory {
    public static LogData generate(Date date, String location, String message) {
        return new LogData(date, location, message);
    }
}
