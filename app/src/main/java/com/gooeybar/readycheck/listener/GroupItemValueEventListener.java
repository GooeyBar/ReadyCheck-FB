package com.gooeybar.readycheck.listener;

import android.content.res.Resources;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.gooeybar.readycheck.R;
import com.gooeybar.readycheck.model.GroupItem;
import com.gooeybar.readycheck.model.State;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Iterator;

/**
 * Created by creja_000 on 10/22/2016.
 */

public class GroupItemValueEventListener implements ValueEventListener {

    private GroupItem groupItem;
    private ArrayAdapter<GroupItem> adapter;
    private Resources resources;

    public GroupItemValueEventListener(GroupItem groupItem, ArrayAdapter<GroupItem> adapter, Resources resources) {
        // initially 1 until it can receive the currencyPerClick
        this.groupItem = groupItem;
        this.adapter = adapter;
        this.resources = resources;
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
        while (membersDataSnapshotIterator.hasNext()) {
            boolean isMemberReady = membersDataSnapshotIterator.next().getValue(String.class).equals(State.READY.getStatus());
            if (isMemberReady)
                numReadyMembers++;
        }
        long numMembers = membersDataSnapshot.getChildrenCount();

        String readyState = dataSnapshot.child(resources.getString(R.string.firebase_db_group_ready_status)).getValue(String.class);

        groupItem.setGroupName(groupName);
        groupItem.setNumMembers(numMembers);
        groupItem.setNumReadyMembers(numReadyMembers);
        groupItem.setReadyState(readyState);

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        // uhhhh?
    }
}
