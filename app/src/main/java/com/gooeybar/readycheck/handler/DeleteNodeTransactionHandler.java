package com.gooeybar.readycheck.handler;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public class DeleteNodeTransactionHandler implements Transaction.Handler {
    @Override
    public Transaction.Result doTransaction(MutableData mutableData) {
        mutableData.setValue(null);

        return Transaction.success(mutableData);
    }

    @Override
    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

    }
}
