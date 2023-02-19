package com.myDownload.example.utils.log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Date;

public class LogService { // Singleton Holder 방식 (동시성 이슈 문제 해결)
    private static FirebaseDatabase mDatabase;
    private static DatabaseReference mReference;
    private static ChildEventListener mChild;

    private LogService() {
    }

    private static class SingletonHolder {
        private static final LogService instance = new LogService();
    }

    public static LogService getInstance() {
        return LogService.SingletonHolder.instance;
    }

    private static void initDatabase() {
        mDatabase = FirebaseDatabase.getInstance();

//        mReference = mDatabase.getReference("logData");
        mReference = mDatabase.getReference();
    }

    public void saveLogMessage(String location, String message, Throwable ex) {
        save(generateLogData(location, message, ex));
    }

    private LogData generateLogData(String location, String message, Throwable ex) {
        return LogFactory.generate(new Date(), location, message, ex);
    }

    private void save(LogData logData) {
        if (mDatabase == null || mReference == null) {
            initDatabase();
        }

        if (logData.isException()) {
            mReference.child("logData").child("exception").push().setValue(logData);
            return;
        }
        mReference.child("logData").child("trace").push().setValue(logData);
    }
}
