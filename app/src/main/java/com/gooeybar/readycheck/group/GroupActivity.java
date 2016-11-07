package com.gooeybar.readycheck.group;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gooeybar.readycheck.R;
import com.gooeybar.readycheck.base.BaseActivity;
import com.gooeybar.readycheck.custom_views.ViewWeightAnimationWrapper;
import com.gooeybar.readycheck.handler.DeleteNodeTransactionHandler;
import com.gooeybar.readycheck.handler.RenameNodeTransactionHandler;
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
    private String groupName;
    private String mNickname;

    private FloatingActionButton initReadyCheckButton;

    private ImageButton readyImageButton, notReadyImageButton;

    private LinearLayout readyCheckLayout;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mGroupsRef;
    private DatabaseReference mMembersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        setupAds();

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
                        .runTransaction(new RenameNodeTransactionHandler(State.READY.getStatus()));
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
                        .runTransaction(new RenameNodeTransactionHandler(State.NOT_READY.getStatus()));
            }
        });

        readyCheckLayout = (LinearLayout) findViewById(R.id.ready_check_layout);

        getMembers();
    }

    private void getMembers() {
        mGroupsRef.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                groupName = dataSnapshot.child(getResources().getString(R.string.firebase_db_group_name)).getValue(String.class);

                setTitle(groupName);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    getSupportActionBar().setSubtitle("Group ID: " + groupId);

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

                DataSnapshot mUserDataSnapshot = dataSnapshot.child(getResources().getString(R.string.firebase_db_members)).child(firebaseUid);

                String myReadyState = mUserDataSnapshot.child(getResources().getString(R.string.firebase_db_ready_status)).getValue(String.class);

                mNickname = mUserDataSnapshot.child(getResources().getString(R.string.firebase_db_display_name)).getValue(String.class);

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

                String readyState = dataSnapshot.child(getResources().getString(R.string.firebase_db_group_ready_status)).getValue(String.class);

                if (State.PENDING.getStatus().equals(readyState)) {
                    ViewWeightAnimationWrapper animationWrapper = new ViewWeightAnimationWrapper(readyCheckLayout);
                    Log.d("TEST", "UP Weight is currently: " + animationWrapper.getWeight());
                    ObjectAnimator anim = ObjectAnimator.ofFloat(animationWrapper,
                            "weight",
                            animationWrapper.getWeight(),
                            2.0f);
                    anim.setDuration(500);
                    anim.start();
                } else {
                    ViewWeightAnimationWrapper animationWrapper = new ViewWeightAnimationWrapper(readyCheckLayout);
                    Log.d("TEST", "DOWN Weight is currently: " + animationWrapper.getWeight());
                    ObjectAnimator anim = ObjectAnimator.ofFloat(animationWrapper,
                            "weight",
                            animationWrapper.getWeight(),
                            0.0f);
                    anim.setDuration(500);
                    anim.start();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void changeGroupNameDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_group_name);

        // Set up the input
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        float sizeInDp = getResources().getDimension(R.dimen.activity_horizontal_margin)/2;

        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (sizeInDp*scale + 0.5f);

        layout.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);

        final EditText inputGroupName = new EditText(this);
        inputGroupName.setInputType(InputType.TYPE_CLASS_TEXT);
        inputGroupName.setHint(R.string.group_name_hint);
        inputGroupName.setText(groupName);
        layout.addView(inputGroupName);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int which) {
                final String newGroupName = inputGroupName.getText().toString();

                mGroupsRef.child(groupId).child(getResources().getString(R.string.firebase_db_group_name)).runTransaction(new RenameNodeTransactionHandler(newGroupName));
        }});

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }

    private void changeDisplayNameDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_display_name);

        // Set up the input
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        float sizeInDp = getResources().getDimension(R.dimen.activity_horizontal_margin)/2;

        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (sizeInDp*scale + 0.5f);

        layout.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);

        final EditText inputDisplayName = new EditText(this);
        inputDisplayName.setInputType(InputType.TYPE_CLASS_TEXT);
        inputDisplayName.setHint(R.string.group_name_hint);
        inputDisplayName.setText(mNickname);
        layout.addView(inputDisplayName);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int which) {
                final String newDisplayName = inputDisplayName.getText().toString();

                mGroupsRef
                        .child(groupId)
                        .child(getResources().getString(R.string.firebase_db_members))
                        .child(firebaseUid)
                        .child(getResources().getString(R.string.firebase_db_display_name))
                        .runTransaction(new RenameNodeTransactionHandler(newDisplayName));
            }});

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }

    private void leaveGroupDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.leave_group_title);
        builder.setMessage(R.string.leave_group_confirmation);

        // Set up the buttons
        builder.setPositiveButton(R.string.leave, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int which) {
                mMembersRef.child(firebaseUid).child(groupId).runTransaction(new DeleteNodeTransactionHandler());

                mGroupsRef.child(groupId).child(getResources().getString(R.string.firebase_db_members)).child(firebaseUid).runTransaction(new DeleteNodeTransactionHandler());

                finish();
            }});

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }

    private void deleteGroupDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_group_title);
        builder.setMessage(R.string.delete_group_confirmation);

        // Set up the buttons
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int which) {
                mMembersRef.child(firebaseUid).child(groupId).runTransaction(new DeleteNodeTransactionHandler());

                mGroupsRef.child(groupId).child(getResources().getString(R.string.firebase_db_members)).runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        // delete the group id from the member
                        mutableData.setValue(null);

                        for (MutableData memberMutableData : mutableData.getChildren()) {
                            String memberID = memberMutableData.getKey();
                            memberMutableData.setValue(null);

                            mMembersRef.child(memberID).child(groupId).runTransaction(new DeleteNodeTransactionHandler());
                        }

                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        // when this is done, finish the activity!
                        finish();
                    }
                });
            }});

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.change_group_name:
                changeGroupNameDialog();
                return true;
            case R.id.change_display_name:
                changeDisplayNameDialog();
                return true;
            case R.id.leave_group:
                leaveGroupDialog();
                return true;
            case R.id.delete_group:
                deleteGroupDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    break;
                case NOT_READY:
                    memberStatusImage.setImageDrawable(getDrawable(R.drawable.not_ready_circle));
                    break;
                case PENDING:
                    memberStatusImage.setImageDrawable(getDrawable(R.drawable.pending_circle));
                    break;
                case INACTIVE:
                    memberStatusImage.setImageDrawable(getDrawable(R.drawable.inactive_circle));
                    break;
            }

            return view;
        }

    }
}
