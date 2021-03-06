package com.messages.recovery.deleted.messages.recovery.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.recovery.deleted.messages.recovery.R;
import com.messages.recovery.deleted.messages.recovery.activities.ActivityMessagesViewer;
import com.messages.recovery.deleted.messages.recovery.activities.MainActivity;
import com.messages.recovery.deleted.messages.recovery.adapters.AdapterMain;
import com.messages.recovery.deleted.messages.recovery.constants.Constant;
import com.messages.recovery.deleted.messages.recovery.constants.TableName;
import com.messages.recovery.deleted.messages.recovery.database.MyDataBaseHelper;
import com.messages.recovery.deleted.messages.recovery.interfaces.OnRecyclerItemClickeListener;
import com.messages.recovery.deleted.messages.recovery.models.Messages;
import com.messages.recovery.deleted.messages.recovery.models.Users;
import com.google.android.gms.ads.AdListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FragmentFacebook extends FragmentBase {


    private Toolbar toolbar;
    private TextView toolBarTitleTv;
    private ProgressBar loadingBar;
    private CheckBox selectAllMenuItem;
    private RelativeLayout emptyAnimViewRoot;

    private String TAG = "FragmentFacebook";
    private View view;
    private Context context;
    private RelativeLayout recyclerRootView;
    private RecyclerView recyclerView;
    private FloatingActionButton btnFab;
    private AdapterMain mAdapter;
    private ArrayList<Users> usersList = new ArrayList<>();
    private FacebookMessagesReceiver facebookMessagesReceiver;
    private MyDataBaseHelper myDataBaseHelper;

    public boolean isContextualMenuOpen = false;
    public boolean isSelectAll = false;
    private ArrayList<Users> multiSelectedItemList;
    private String selected;
    private String currentFragmentTitle = "Facebook";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setUpStatusBar(getContext().getResources().getColor(R.color.colorFragmentFbStatusBar));
        view = inflater.inflate(R.layout.fragment_facebook, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext();
        if (haveNetworkConnection()) {
            requestBanner((FrameLayout) view.findViewById(R.id.fr_facebook_bannerContainer));
        }
        reqNewInterstitial(context);
        initViews();
        setUpToolBar();
        iniRecyclerView();
        registerReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        getMessageInBackgroundTask();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_ac_main, menu);
        Log.d(TAG, "onCreateOptionsMenu: ");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.acMain_menu_btnShareUs: {
                ((MainActivity) getActivity()).shareUs();

            }
            break;
            case R.id.acMain_menu_btnRateUs: {
                ((MainActivity) getActivity()).rateUs();
            }
            break;

            case R.id.menu_contextual_btnDelete:
                if (!multiSelectedItemList.isEmpty()) {
                    deleteMultipleDialog();
                } else {
                    Toast.makeText(context, "Please select any item first", Toast.LENGTH_SHORT).show();
                }
        }
        return true;
    }

    private void initViews() {
        myDataBaseHelper = new MyDataBaseHelper(getContext());
        recyclerRootView = (RelativeLayout) view.findViewById(R.id.rootView_recycler_fr_facebook);
        toolbar = (Toolbar) view.findViewById(R.id.fr_facebook_toolbar);
        emptyAnimViewRoot = view.findViewById(R.id.emptyAnimView_root_fr_fb);
        btnFab = view.findViewById(R.id.btnFab_fr_facebook);
        btnFab.setOnClickListener(onFabButtonClicked);
        loadingBar = (ProgressBar) view.findViewById(R.id.fr_facebook_loadingBar);
        loadingBar.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener onFabButtonClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        String messengerUrl;
                        if (isMessengerAppInstalled()) {
                            messengerUrl = "fb://messaging";
                            Intent messengerIntent = new Intent(Intent.ACTION_VIEW);
                            messengerIntent.setData(Uri.parse(messengerUrl));
                            startActivity(messengerIntent);
                        } else {
                            Toast.makeText(context, "Facebook Messenger is not installed.", Toast.LENGTH_SHORT).show();
                        }
                    }

                });
            } else {
                String messengerUrl;
                if (isMessengerAppInstalled()) {
                    messengerUrl = "fb://messaging";
                    Intent messengerIntent = new Intent(Intent.ACTION_VIEW);
                    messengerIntent.setData(Uri.parse(messengerUrl));
                    startActivity(messengerIntent);
                } else {
                    Toast.makeText(context, "Facebook Messenger is not installed.", Toast.LENGTH_SHORT).show();
                }
            }


        }
    };

    public boolean isMessengerAppInstalled() {
        try {
            context.getPackageManager().getApplicationInfo("com.facebook.orca", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void setUpToolBar() {
        selected = getResources().getString(R.string.item_selected);
        ((AppCompatActivity) Objects.requireNonNull(getActivity())).setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorFragmentFbToolbar));
        toolBarTitleTv = (TextView) view.findViewById(R.id.toolBar_title_tv);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isContextualMenuOpen) {
                    closeContextualMenu();
                } else {
                    ((MainActivity) getActivity()).onBackPressed();

                }
            }
        });
        updateToolBarTitle(currentFragmentTitle);
    }


    private void iniRecyclerView() {
        recyclerView = view.findViewById(R.id.recycler_view_fr_facebook);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

    }

    private void buildRecyclerView() {
        if (usersList.isEmpty()) {
            emptyAnimViewRoot.setVisibility(View.VISIBLE);
        } else {
            emptyAnimViewRoot.setVisibility(View.INVISIBLE);
        }
        mAdapter = new AdapterMain(context, null, this, null, null, usersList, Constant.ACTIVE_FRAGMENT_FACEBOOK);
        recyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        mAdapter.setOnRecyclerItemClickListener(new OnRecyclerItemClickeListener() {
            @Override
            public void onItemClicked(int position) {

                updateTableMessages(position);
                Intent intent = new Intent(context, ActivityMessagesViewer.class);
                intent.putExtra(Constant.KEY_INTENT_SELECTED_MAIN_ITEM_TITLE, usersList.get(position).getUserTitle());
                intent.putExtra(Constant.KEY_INTENT_SELECTED_TABLE_NAME, TableName.TABLE_NAME_MESSAGES_FACEBOOK);
                intent.putExtra(Constant.KEY_INTENT_SELECTED_MESSAGES_TITLE, "Facebook Messages");
                startActivity(intent);

            }

            @Override
            public void onItemLongClicked(int position) {
                openContextualMenu();
            }

            @Override
            public void onItemCheckBoxClicked(View view, int position) {
                try {
                    if (((CheckBox) view).isChecked()) {
                        multiSelectedItemList.add(usersList.get(position));
                        String text = multiSelectedItemList.size() + selected;
                        updateToolBarTitle(text);
                        if (multiSelectedItemList.size() == usersList.size()) {
                            selectAllMenuItem.setChecked(true);
                        }
                    } else {
                        multiSelectedItemList.remove(usersList.get(position));
                        String text = multiSelectedItemList.size() + selected;
                        updateToolBarTitle(text);
                        if (multiSelectedItemList.size() == usersList.size()) {
                            selectAllMenuItem.setChecked(false);
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "onItemCheckBoxClicked: " + e);
                }
            }
        });


    }

    private void updateToolBarTitle(String title) {
        toolBarTitleTv.setText(title);
    }

    private void openContextualMenu() {
        multiSelectedItemList = new ArrayList<>();
        isContextualMenuOpen = true;
        isSelectAll = false;
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_ac_main_contextual);
        toolbar.setNavigationIcon(R.drawable.ic_cross);
        selectAllMenuItem = (CheckBox) toolbar.getMenu().findItem(R.id.menu_contextual_btnSelecAll).getActionView();
        selectAllMenuItem.setChecked(false);
        updateToolBarTitle("0" + selected);
        mAdapter.notifyDataSetChanged();
        selectAllMenuItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    selectAllItems(true);
                    isSelectAll = true;
                } else {
                    selectAllItems(false);
                    isSelectAll = false;
                }
            }
        });
    }

    private void selectAllItems(boolean isSelectAll) {
        if (isSelectAll) {
            if (!multiSelectedItemList.isEmpty()) {
                multiSelectedItemList.removeAll(usersList);
                multiSelectedItemList.clear();
            }
            for (int i = 0; i < usersList.size(); i++) {
                multiSelectedItemList.add(usersList.get(i));
            }
            String text = multiSelectedItemList.size() + selected;
            updateToolBarTitle(text);
        } else {
            multiSelectedItemList.removeAll(usersList);
            multiSelectedItemList.clear();
            String text = multiSelectedItemList.size() + selected;
            updateToolBarTitle(text);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void closeContextualMenu() {
        multiSelectedItemList.removeAll(usersList);
        multiSelectedItemList.clear();
        isContextualMenuOpen = false;
        isSelectAll = false;
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_ac_main);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        updateToolBarTitle(currentFragmentTitle);
        mAdapter.notifyDataSetChanged();
    }

    private void getMessageInBackgroundTask() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                recyclerRootView.setVisibility(View.INVISIBLE);
                loadingBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                if (!usersList.isEmpty()) {
                    usersList.clear();
                }
                usersList.addAll(myDataBaseHelper.getALLUsers(TableName.TABLE_NAME_USER_FACEBOOK));
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                buildRecyclerView();
                loadingBar.setVisibility(View.INVISIBLE);
                recyclerRootView.setVisibility(View.VISIBLE);
            }
        }.execute();
    }

    private void updateTableMessages(int position) {
        Users currentUser = usersList.get(position);
        List<Messages> list = myDataBaseHelper.getSelectedMessages(TableName.TABLE_NAME_MESSAGES_FACEBOOK, currentUser.getUserTitle());
        list.get(list.size() - 1).setReadStatus("Read");
        Messages messages = list.get(list.size() - 1);
        myDataBaseHelper.updateMessages(TableName.TABLE_NAME_MESSAGES_FACEBOOK, messages);
    }

    private void updateMessages() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                if (!usersList.isEmpty()) {
                    usersList.clear();
                }
                usersList.addAll(myDataBaseHelper.getALLUsers(TableName.TABLE_NAME_USER_FACEBOOK));
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                buildRecyclerView();
            }
        }.execute();
    }


    private void deleteMultipleDialog() {
        new AlertDialog.Builder(context)
                .setTitle(R.string.delete_alert)
                .setMessage(R.string.delete_user_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                recyclerRootView.setVisibility(View.INVISIBLE);
                                loadingBar.setVisibility(View.VISIBLE);
                            }

                            @Override
                            protected Void doInBackground(Void... voids) {

                                for (int i = 0; i < multiSelectedItemList.size(); i++) {
                                    myDataBaseHelper.deleteUsers(TableName.TABLE_NAME_USER_WHATS_APP, multiSelectedItemList.get(i).getUserTitle());
                                    usersList.remove(multiSelectedItemList.get(i));
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                super.onPostExecute(aVoid);
                                closeContextualMenu();
                                buildRecyclerView();
                                loadingBar.setVisibility(View.GONE);
                                recyclerRootView.setVisibility(View.VISIBLE);
                            }
                        }.execute();


                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }


    private void registerReceiver() {
        facebookMessagesReceiver = new FacebookMessagesReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_INTENT_FILTER_FACEBOOK_RECEIVER);
        getContext().registerReceiver(facebookMessagesReceiver, intentFilter);
    }

    public class FacebookMessagesReceiver extends BroadcastReceiver {
        @Override

        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "onReceive: Received Notification");
            updateMessages();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(facebookMessagesReceiver);
        if (!usersList.isEmpty()) {
            usersList.clear();
        }
    }


}
