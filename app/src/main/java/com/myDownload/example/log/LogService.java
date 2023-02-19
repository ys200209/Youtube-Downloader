package com.myDownload.example.log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

//        mReference = mDatabase.getReference("log");
//        mReference.child("log").setValue("check");

        /*mChild = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {
            }
        };*/
//        mReference.addChildEventListener(mChild);
    }

    public void save(LogData logData) {
        if (mDatabase == null) {
            initDatabase();
        }

        mReference = mDatabase.getReference();
        mReference.child("logData").push().setValue(logData);
    }
}
