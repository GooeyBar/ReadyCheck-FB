package com.gooeybar.readycheck.lobby;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gooeybar.readycheck.R;
import com.gooeybar.readycheck.base.BaseActivity;
import com.gooeybar.readycheck.group.GroupActivity;
import com.gooeybar.readycheck.listener.GroupItemValueEventListener;
import com.gooeybar.readycheck.model.GroupItem;
import com.gooeybar.readycheck.model.State;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static com.gooeybar.readycheck.login.SignInActivity.RC_SIGN_OUT;
import static com.gooeybar.readycheck.model.State.INACTIVE;
import static com.gooeybar.readycheck.model.State.NOT_READY;
import static com.gooeybar.readycheck.model.State.PENDING;
import static com.gooeybar.readycheck.model.State.READY;

public class LobbyActivity extends BaseActivity {

    private List<GroupItem> groups = new ArrayList<>();

    private ListView groupListView;

    private FloatingActionButton newGroupFab;

    private ArrayAdapter<GroupItem> adapter;

    private String firebaseUid;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mGroupsRef;
    private DatabaseReference mMembersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        groupListView = (ListView) findViewById(R.id.group_list_view);

        adapter = new GroupArrayAdapter();
        groupListView.setAdapter(adapter);

        // Set up firebase references
        mGroupsRef = mRootRef.child(getResources().getString(R.string.firebase_db_groups));
        mMembersRef = mRootRef.child(getResources().getString(R.string.firebase_db_members));

        firebaseUid = getIntent().getExtras().getString(getString(R.string.intent_extra_unique_id));

        newGroupFab = (FloatingActionButton) findViewById(R.id.new_group_fab);

        newGroupFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createGroupDialog();
            }
        });

        getGroups();
    }

    private void getGroups() {
        Log.d("LOBBYACTIVITY", "USER ID: " + firebaseUid);
        // listener for member details
        mMembersRef.child(firebaseUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // member does not belong to any groups
                if (dataSnapshot.getValue() == null) {
                    Log.d("LOBBYACTIVITY", "Member is null");
                    return;
                }

                Log.d("LOBBYACTIVITY", "Member is not null");

                groups.clear();

                Iterator<DataSnapshot> groupIdDataSnapshotIterator = dataSnapshot.getChildren().iterator();

                Log.d("LOBBYACTIVITY", "Iterator size: " + dataSnapshot.getChildrenCount());
                Log.d("LOBBYACTIVITY", "Iterator has next: " + groupIdDataSnapshotIterator.hasNext());
                while(groupIdDataSnapshotIterator.hasNext()) {
                    GroupItem groupItem = new GroupItem();

                    DataSnapshot groupIdDataSnapshot = groupIdDataSnapshotIterator.next();

                    // listener for group details
                    mGroupsRef
                            .child(groupIdDataSnapshot.getKey())
                            .addValueEventListener(new GroupItemValueEventListener(groupItem, adapter, getResources(), mGroupsRef.child(groupIdDataSnapshot.getKey())));

                    groups.add(groupItem);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void signOut() {
        Log.d("LOGIN", "Sign out clicked!!!");

        setResult(Activity.RESULT_OK);
        finish();
    }

    private void joinGroupDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Join a group!");
        builder.setMessage("Type in the unique 4 digit group id!");

        // Set up the input
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText inputGroupId = new EditText(this);
        inputGroupId.setInputType(InputType.TYPE_CLASS_TEXT);
        final EditText inputDisplayName = new EditText(this);
        inputDisplayName.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(inputGroupId);
        layout.addView(inputDisplayName);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("JOIN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int which) {
                final String groupId = inputGroupId.getText().toString();
                final String displayName = inputDisplayName.getText().toString();

                mGroupsRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() == null) {
                            inputGroupId.setText("");
                            Toast.makeText(LobbyActivity.this, "Please type a valid group id", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        FirebaseMessaging.getInstance().subscribeToTopic(groupId);

                        mGroupsRef.child(groupId).child(getResources().getString(R.string.firebase_db_members)).child(firebaseUid).runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                mutableData.child(getResources().getString(R.string.firebase_db_ready_status)).setValue(NOT_READY.getStatus());
                                mutableData.child(getResources().getString(R.string.firebase_db_display_name)).setValue(displayName);
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                            }
                        });
                        mMembersRef.child(firebaseUid).child(groupId).runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                mutableData.setValue(true);
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                            }
                        });
                        dataSnapshot.child(getResources().getString(R.string.firebase_db_members)).child(firebaseUid);

                        dialogInterface.dismiss();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }

    private void createGroupDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create a group!");
        builder.setMessage("Type in a group name!");

        // Set up the input
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText inputGroupName = new EditText(this);
        inputGroupName.setInputType(InputType.TYPE_CLASS_TEXT);
        final EditText inputDisplayName = new EditText(this);
        inputDisplayName.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(inputGroupName);
        layout.addView(inputDisplayName);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int which) {
                final String groupName = inputGroupName.getText().toString();
                final String displayName = inputDisplayName.getText().toString();

                mGroupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // generate random key with no vowels of size 5
                        while (true) {
                            char[] chars = "bcdfghjklmnpqrstvwxyz0123456789".toCharArray();
                            StringBuilder sb = new StringBuilder();
                            Random random = new Random();
                            for (int i = 0; i < 5; i++) {
                                char c = chars[random.nextInt(chars.length)];
                                sb.append(c);
                            }
                            final String key = sb.toString();

                            if (dataSnapshot.child(key).getValue() == null) {

                                FirebaseMessaging.getInstance().subscribeToTopic(key);

                                mGroupsRef.child(key).runTransaction(new Transaction.Handler() {
                                    @Override
                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                        mutableData.child(getResources().getString(R.string.firebase_db_group_name)).setValue(groupName);
                                        MutableData memberIdMutableData = mutableData.child(getResources().getString(R.string.firebase_db_members)).child(firebaseUid);
                                        memberIdMutableData.child(getResources().getString(R.string.firebase_db_ready_status)).setValue(INACTIVE.getStatus());
                                        memberIdMutableData.child(getResources().getString(R.string.firebase_db_display_name)).setValue(displayName);
                                        mutableData.child(getResources().getString(R.string.firebase_db_group_ready_status)).setValue(INACTIVE.getStatus());

                                        return Transaction.success(mutableData);
                                    }

                                    @Override
                                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                    }
                                });
                                mMembersRef.child(firebaseUid).runTransaction(new Transaction.Handler() {
                                    @Override
                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                        mutableData.child(key).setValue(true);

                                        return Transaction.success(mutableData);
                                    }

                                    @Override
                                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                                    }
                                });
                                break;
                            }
                        }

                        dialogInterface.dismiss();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
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
        inflater.inflate(R.menu.lobby_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.join_group_button:
                joinGroupDialog();
                return true;
            case R.id.sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_OUT) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("LOGIN", "Sign out button clicked");
                signOut();
            }
        }
    }

    private class GroupArrayAdapter extends ArrayAdapter<GroupItem> {
        public GroupArrayAdapter() {
            super(LobbyActivity.this, R.layout.group_item, groups);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null)
                view = getLayoutInflater().inflate(R.layout.group_item, parent, false);

            final GroupItem groupItem = groups.get(position);

            String groupName = groupItem.getGroupName();
            long numReadyMembers = groupItem.getNumReadyMembers();
            long numMembers = groupItem.getNumMembers();

            // represent in format : <numReadyMembers>/<numMembers> Ready
            String statusFraction = numReadyMembers + "/" + numMembers + " Ready";

            TextView groupNameTextView = (TextView) view.findViewById(R.id.group_name_text);
            groupNameTextView.setText(groupName);

            TextView statusFractionTextView = (TextView) view.findViewById(R.id.status_fraction_text);
            statusFractionTextView.setText(statusFraction);

            ImageButton readyImageButton = (ImageButton) view.findViewById(R.id.ready_button);
            ImageButton notReadyImageButton = (ImageButton) view.findViewById(R.id.not_ready_button);

            readyImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mGroupsRef
                            .child(groupItem.getGroupId())
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
                            .child(groupItem.getGroupId())
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

            ImageView groupStatusImage = (ImageView) view.findViewById(R.id.status_image);

            switch(groupItem.getReadyState()) {
                case READY:
                    groupStatusImage.setImageDrawable(getDrawable(R.drawable.dot_green));
                    readyImageButton.setVisibility(View.INVISIBLE);
                    notReadyImageButton.setVisibility(View.INVISIBLE);
                    break;
                case NOT_READY:
                    groupStatusImage.setImageDrawable(getDrawable(R.drawable.dot_red));
                    readyImageButton.setVisibility(View.INVISIBLE);
                    notReadyImageButton.setVisibility(View.INVISIBLE);
                    break;
                case PENDING:
                    groupStatusImage.setImageDrawable(getDrawable(R.drawable.dot_yellow));
                    readyImageButton.setVisibility(View.VISIBLE);
                    notReadyImageButton.setVisibility(View.VISIBLE);
                    break;
                case INACTIVE:
                    groupStatusImage.setImageDrawable(getDrawable(R.drawable.dot_grey));
                    readyImageButton.setVisibility(View.INVISIBLE);
                    notReadyImageButton.setVisibility(View.INVISIBLE);
                    break;
            }

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent clickerIntent = new Intent(LobbyActivity.this, GroupActivity.class);
                    clickerIntent.putExtra(getResources().getString(R.string.intent_extra_unique_id), firebaseUid);
                    clickerIntent.putExtra(getResources().getString(R.string.intent_extra_group_id), groupItem.getGroupId());
                    startActivityForResult(clickerIntent, RC_SIGN_OUT);
                }
            });

            return view;
        }

    }
}
