package com.myDownload.example.log;

import java.util.Date;

public class LogData {
    private final Date date;
    private final String location;
    private final String message;

    public LogData(Date date, String location, String message) {
        this.date = date;
        this.location = location;
        this.message = message;
    }
}
