package com.gooeybar.readycheck.group;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.gooeybar.readycheck.R;
import com.gooeybar.readycheck.base.BaseActivity;
import com.gooeybar.readycheck.model.MemberItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GroupActivity extends BaseActivity {
    private ListView membersListView;
    private ArrayAdapter<MemberItem> adapter;
    private List<MemberItem> members;

    private String firebaseUid;
    private String groupId;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mGroupsRef;
    private DatabaseReference mMembersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        firebaseUid = getIntent().getExtras().getString(getString(R.string.intent_extra_unique_id));
        groupId = getIntent().getExtras().getString(getString(R.string.intent_extra_group_id));

        membersListView = (ListView) findViewById(R.id.group_members_list_view);
        members = new ArrayList<MemberItem>();
        adapter = new MemberArrayAdapter();
        membersListView.setAdapter(adapter);

        // Set up firebase references
        mGroupsRef = mRootRef.child(getResources().getString(R.string.firebase_db_groups));
        mMembersRef = mRootRef.child(getResources().getString(R.string.firebase_db_members));

        getMembers();
    }

    private void getMembers() {
        mGroupsRef.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                members.clear();

                Iterator<DataSnapshot> membersDataSnapshotIterator = dataSnapshot.child(getResources().getString(R.string.firebase_db_members)).getChildren().iterator();
                while (membersDataSnapshotIterator.hasNext()) {
                    MemberItem memberItem = new MemberItem();
                    DataSnapshot memberDataSnapshot = membersDataSnapshotIterator.next();
                    String memberReadyStatus = memberDataSnapshot.child(getResources().getString(R.string.firebase_db_ready_status)).getValue(String.class);
                    String memberDisplayName = memberDataSnapshot.child(getResources().getString(R.string.firebase_db_display_name)).getValue(String.class);
                    memberItem.setReadyState(memberReadyStatus);
                    memberItem.setMemberName(memberDisplayName);
                    members.add(memberItem);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private class MemberArrayAdapter extends ArrayAdapter<MemberItem> {
        public MemberArrayAdapter() {
            super(GroupActivity.this, R.layout.member_item, members);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null)
                view = getLayoutInflater().inflate(R.layout.member_item, parent, false);

            MemberItem memberItem = members.get(position);

            TextView memberDisplayNameText = (TextView) view.findViewById(R.id.member_name_text);

            memberDisplayNameText.setText(memberItem.getMemberName());

            ImageView memberStatusImage = (ImageView) view.findViewById(R.id.status_image);

            return view;
        }

    }
}
