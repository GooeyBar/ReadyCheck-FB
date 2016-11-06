package com.gooeybar.readycheck.group;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.gooeybar.readycheck.R;
import com.gooeybar.readycheck.base.BaseActivity;
import com.gooeybar.readycheck.model.MemberItem;
import com.gooeybar.readycheck.model.State;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
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

    private FloatingActionButton initReadyCheckButton;

    private ImageButton readyImageButton, notReadyImageButton;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mGroupsRef;
    private DatabaseReference mMembersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        firebaseUid = getIntent().getExtras().getString(getString(R.string.intent_extra_unique_id));
        groupId = getIntent().getExtras().getString(getString(R.string.intent_extra_group_id));

        setTitle(groupId);

        membersListView = (ListView) findViewById(R.id.group_members_list_view);
        members = new ArrayList<MemberItem>();
        adapter = new MemberArrayAdapter();
        membersListView.setAdapter(adapter);

        // Set up firebase references
        mGroupsRef = mRootRef.child(getResources().getString(R.string.firebase_db_groups));
        mMembersRef = mRootRef.child(getResources().getString(R.string.firebase_db_members));

        initReadyCheckButton = (FloatingActionButton) findViewById(R.id.group_rcheck_fab);

        initReadyCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGroupsRef.child(groupId).runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        // set group ready status to PENDING
                        mutableData.child(getResources().getString(R.string.firebase_db_group_ready_status)).setValue(State.PENDING.getStatus());

                        MutableData membersMutableData = mutableData.child(getResources().getString(R.string.firebase_db_members));
                        Iterator<MutableData> membersMutableDataIterator = membersMutableData.getChildren().iterator();

                        // set each member ready status to NOT_READY
                        while (membersMutableDataIterator.hasNext()) {
                            MutableData memberMutableData = membersMutableDataIterator.next();
                            memberMutableData.child(getResources().getString(R.string.firebase_db_ready_status)).setValue(State.PENDING.getStatus());
                        }

                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        String id = (int)(Math.random() * 5000) + "";
                        mRootRef.child("notificationRequests").child(id).runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                mutableData.child("username").setValue(groupId);
                                mutableData.child("message").setValue("READY CHECK FOR " + groupId);

                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                            }
                        });
                    }
                });
            }
        });

        readyImageButton = (ImageButton) findViewById(R.id.accept_ready_check_button);
        notReadyImageButton = (ImageButton) findViewById(R.id.decline_ready_check_button);

        readyImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGroupsRef
                        .child(groupId)
                        .child(getResources().getString(R.string.firebase_db_members))
                        .child(firebaseUid)
                        .child(getResources().getString(R.string.firebase_db_ready_status))
                        .runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                mutableData.setValue(State.READY.getStatus());

                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                            }
                        });
            }
        });

        notReadyImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGroupsRef
                        .child(groupId)
                        .child(getResources().getString(R.string.firebase_db_members))
                        .child(firebaseUid)
                        .child(getResources().getString(R.string.firebase_db_ready_status))
                        .runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                mutableData.setValue(State.NOT_READY.getStatus());

                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                            }
                        });
            }
        });

        getMembers();
    }

    private void getMembers() {
        mGroupsRef.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String groupName = dataSnapshot.child(getResources().getString(R.string.firebase_db_group_name)).getValue(String.class);

                setTitle(groupName + "   -   " + groupId);

                members.clear();

                boolean hasPendingStatuses = false;
                boolean hasNotReadyStatuses = false;

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

                    hasPendingStatuses = hasPendingStatuses || memberReadyStatus.equals(State.PENDING.getStatus());
                    hasNotReadyStatuses = hasNotReadyStatuses || memberReadyStatus.equals(State.NOT_READY.getStatus());
                }

                if (hasPendingStatuses) {
                    mGroupsRef.child(groupId).runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            mutableData.child(getResources().getString(R.string.firebase_db_group_ready_status)).setValue(State.PENDING.getStatus());

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                        }
                    });
                }
                else if (hasNotReadyStatuses) {
                    mGroupsRef.child(groupId).runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            mutableData.child(getResources().getString(R.string.firebase_db_group_ready_status)).setValue(State.NOT_READY.getStatus());

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                        }
                    });
                }

                String readyState = dataSnapshot.child(getResources().getString(R.string.firebase_db_group_ready_status)).getValue(String.class);

                if (State.PENDING.getStatus().equals(readyState)) {
                    readyImageButton.setVisibility(View.VISIBLE);
                    notReadyImageButton.setVisibility(View.VISIBLE);
                } else {
                    readyImageButton.setVisibility(View.INVISIBLE);
                    notReadyImageButton.setVisibility(View.INVISIBLE);
                }

                String myReadyState = dataSnapshot.child(getResources().getString(R.string.firebase_db_members)).child(firebaseUid).child(getResources().getString(R.string.firebase_db_ready_status)).getValue(String.class);

                if (State.READY.getStatus().equals(myReadyState)) {
                    readyImageButton.setImageResource(R.drawable.ic_check_circle_green_24dp);
                    notReadyImageButton.setImageResource(R.drawable.ic_cancel_black_24dp);
                } else if (State.NOT_READY.getStatus().equals(myReadyState)) {
                    readyImageButton.setImageResource(R.drawable.ic_check_circle_black_24dp);
                    notReadyImageButton.setImageResource(R.drawable.ic_cancel_red_24dp);
                } else {
                    readyImageButton.setImageResource(R.drawable.ic_check_circle_black_24dp);
                    notReadyImageButton.setImageResource(R.drawable.ic_cancel_black_24dp);
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

            switch(memberItem.getReadyState()) {
                case READY:
                    memberStatusImage.setImageDrawable(getDrawable(R.drawable.ready_circle));
                    readyImageButton.setVisibility(View.INVISIBLE);
                    notReadyImageButton.setVisibility(View.INVISIBLE);
                    break;
                case NOT_READY:
                    memberStatusImage.setImageDrawable(getDrawable(R.drawable.not_ready_circle));
                    readyImageButton.setVisibility(View.INVISIBLE);
                    notReadyImageButton.setVisibility(View.INVISIBLE);
                    break;
                case PENDING:
                    memberStatusImage.setImageDrawable(getDrawable(R.drawable.pending_circle));
                    readyImageButton.setVisibility(View.VISIBLE);
                    notReadyImageButton.setVisibility(View.VISIBLE);
                    break;
                case INACTIVE:
                    memberStatusImage.setImageDrawable(getDrawable(R.drawable.inactive_circle));
                    readyImageButton.setVisibility(View.INVISIBLE);
                    notReadyImageButton.setVisibility(View.INVISIBLE);
                    break;
            }

            return view;
        }

    }
}
