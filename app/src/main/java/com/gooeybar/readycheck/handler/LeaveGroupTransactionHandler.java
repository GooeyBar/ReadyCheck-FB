package com.gooeybar.readycheck.handler;

import android.content.res.Resources;

import com.gooeybar.readycheck.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public class LeaveGroupTransactionHandler implements Transaction.Handler {

    private Resources resources;
    private String firebaseUid;

    public LeaveGroupTransactionHandler(Resources resources, String firebaseUid) {
        this.resources = resources;
        this.firebaseUid = firebaseUid;
    }

    @Override
    public Transaction.Result doTransaction(MutableData mutableData) {
        mutableData.child(resources.getString(R.string.firebase_db_members)).child(firebaseUid).setValue(null);

        if (!mutableData.child(resources.getString(R.string.firebase_db_members)).hasChildren()) {
            mutableData.setValue(null);
        }

        return Transaction.success(mutableData);
    }

    @Override
    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

    }
}
