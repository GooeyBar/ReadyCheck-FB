package com.gooeybar.readycheck.handler;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public class RenameNodeTransactionHandler implements Transaction.Handler {

    private String value;

    public RenameNodeTransactionHandler(String value) {
        this.value = value;
    }

    @Override
    public Transaction.Result doTransaction(MutableData mutableData) {
        mutableData.setValue(value);
        return Transaction.success(mutableData);
    }

    @Override
    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

    }
}
