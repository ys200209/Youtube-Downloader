package com.myDownload.example.utils.log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogData {
    private final String date;
    private final String location;
    private final String message;
    private final Throwable throwable;

    public LogData(Date date, String location, String message, Throwable throwable) {
        this.date = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss").format(date);
        this.location = location;
        this.message = message;
        this.throwable = throwable;
    }

    public String getDate() {
        return date;
    }

    public String getLocation() {
        return location;
    }

    public String getMessage() {
        return message;
    }

    public boolean isException() {
        return throwable != null;
    }

    public String getMessageThrowable() {
        if (isException()) {
            return throwable.getMessage();
        }
        return null;
    }

    @Override
    public String toString() {
        return "LogData{" +
                "date=" + date +
                ", location='" + location + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
