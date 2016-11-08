package com.gooeybar.readycheck.listener;

import android.content.res.Resources;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.gooeybar.readycheck.R;
import com.gooeybar.readycheck.model.GroupItem;
import com.gooeybar.readycheck.model.State;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.Iterator;

public class GroupItemValueEventListener implements ValueEventListener {

    private GroupItem groupItem;
    private ArrayAdapter<GroupItem> adapter;
    private Resources resources;
    private DatabaseReference mGroupRef;

    public GroupItemValueEventListener(GroupItem groupItem, ArrayAdapter<GroupItem> adapter, Resources resources, DatabaseReference mGroupRef) {
        // initially 1 until it can receive the currencyPerClick
        this.groupItem = groupItem;
        this.adapter = adapter;
        this.resources = resources;
        this.mGroupRef = mGroupRef;
    }

    public GroupItem getGroupItem() {
        return groupItem;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        String groupName = dataSnapshot.child(resources.getString(R.string.firebase_db_group_name)).getValue(String.class);
        Log.d("LOBBYACTIVITY", "Group Name: " + groupName);
        DataSnapshot membersDataSnapshot = dataSnapshot.child(resources.getString(R.string.firebase_db_members));
        Iterator<DataSnapshot> membersDataSnapshotIterator = membersDataSnapshot.getChildren().iterator();
        long numReadyMembers = 0;

        boolean hasPendingStatuses = false;
        boolean hasNotReadyStatuses = false;

        while (membersDataSnapshotIterator.hasNext()) {
            String memberReadyStatus = membersDataSnapshotIterator.next().child(resources.getString(R.string.firebase_db_ready_status)).getValue(String.class);
            boolean isMemberReady = memberReadyStatus.equals(State.READY.getStatus());
            if (isMemberReady)
                numReadyMembers++;

            hasPendingStatuses = hasPendingStatuses || State.PENDING.getStatus().equals(memberReadyStatus);
            hasNotReadyStatuses = hasNotReadyStatuses || State.NOT_READY.getStatus().equals(memberReadyStatus);
        }

        if (hasPendingStatuses) {
            mGroupRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    if (mutableData.getValue() == null) {
                        return Transaction.abort();
                    }
                    mutableData.child(resources.getString(R.string.firebase_db_group_ready_status)).setValue(State.PENDING.getStatus());

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                }
            });
        }
        else if (hasNotReadyStatuses) {
            mGroupRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    if (mutableData.getValue() == null) {
                        return Transaction.abort();
                    }
                    mutableData.child(resources.getString(R.string.firebase_db_group_ready_status)).setValue(State.NOT_READY.getStatus());

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                }
            });
        }

        long numMembers = membersDataSnapshot.getChildrenCount();

        if (numReadyMembers == numMembers) {
            mGroupRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    if (mutableData.getValue() == null) {
                        return Transaction.abort();
                    }
                    mutableData.child(resources.getString(R.string.firebase_db_group_ready_status)).setValue(State.READY.getStatus());

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                }
            });
        }

        String readyState = dataSnapshot.child(resources.getString(R.string.firebase_db_group_ready_status)).getValue(String.class);

        groupItem.setGroupName(groupName);
        groupItem.setNumMembers(numMembers);
        groupItem.setNumReadyMembers(numReadyMembers);
        groupItem.setReadyState(readyState);
        groupItem.setGroupId(dataSnapshot.getKey());

        adapter.notifyDataSetChanged();


    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        // uhhhh?
    }
}
