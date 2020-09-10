package com.example.recoverdeletedmessages.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recoverdeletedmessages.R;
import com.example.recoverdeletedmessages.activities.ActivityMessagesViewer;
import com.example.recoverdeletedmessages.adapters.AdapterMain;
import com.example.recoverdeletedmessages.constants.Constant;
import com.example.recoverdeletedmessages.constants.TableName;
import com.example.recoverdeletedmessages.database.MyDataBaseHelper;
import com.example.recoverdeletedmessages.interfaces.OnRecyclerItemClickeListener;
import com.example.recoverdeletedmessages.models.Users;
import com.example.recoverdeletedmessages.services.NotificationService;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class FragmentWhatsApp extends FragmentBase {

    private String TAG = "FragmentWhatsApp";
    private View view;
    private Toolbar toolbar;
    private TextView toolBarTitleTv;
    private ProgressBar loadingBar;
    private CheckBox selectAllMenuItem;
    private Context context;
    private RecyclerView recyclerView;
    private AdapterMain mAdapter;
    private ArrayList<Users> usersList = new ArrayList<>();
    private WhatsAppDataReceiver whatsAppDataReceiver;
    private MyDataBaseHelper myDataBaseHelper;
    public boolean isContextualMenuOpen = false;
    public boolean isSelectAll = false;
    private ArrayList<Users> multiSelectedItemList;
    private String selected;
    private String currentFragmentTitle = "Whats App";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_whatsapp, container, false);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext();


        initViews();
        setUpToolBar();
        iniRecyclerView();
        getMessageInBackgroundTask();
        registerReceiver();
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
            case R.id.acMain_menu_btnCreateNotification: {
                startNotificationService();
            }
            break;
            case R.id.acMain_btm_nav_btnSettings: {
                settingsIntent();
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
        toolbar = (Toolbar) view.findViewById(R.id.fr_whatsApp_toolbar);
        loadingBar = (ProgressBar) view.findViewById(R.id.fr_whatsApp_loadingBar);
        loadingBar.setVisibility(View.INVISIBLE);
    }

    private void setUpToolBar() {
        selected = getResources().getString(R.string.item_selected);
        ((AppCompatActivity) Objects.requireNonNull(getActivity())).setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        toolBarTitleTv = (TextView) view.findViewById(R.id.toolBar_title_tv);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isContextualMenuOpen) {
                    closeContextualMenu();
                }
            }
        });
        updateToolBarTitle(currentFragmentTitle);
    }


    private void iniRecyclerView() {
        recyclerView = view.findViewById(R.id.recycler_view_fr_whatsApp);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

    }

    private void buildRecyclerView() {
        mAdapter = new AdapterMain(context, this, null, null, null, usersList, Constant.ACTIVE_FRAGMENT_WHATS_APP);
        recyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        mAdapter.setOnRecyclerItemClickListener(new OnRecyclerItemClickeListener() {
            @Override
            public void onItemClicked(int position) {
                Intent intent = new Intent(context, ActivityMessagesViewer.class);
                intent.putExtra(Constant.KEY_INTENT_SELECTED_MAIN_ITEM_TITLE, usersList.get(position).getUserTitle());
                intent.putExtra(Constant.KEY_INTENT_SELECTED_TABLE_NAME, TableName.TABLE_NAME_MESSAGES_WHATS_APP);
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
                loadingBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                if (!usersList.isEmpty()) {
                    usersList.clear();
                }
                usersList.addAll(myDataBaseHelper.getALLUsers(TableName.TABLE_NAME_USER_WHATS_APP));
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                buildRecyclerView();
                loadingBar.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    private void registerReceiver() {
        whatsAppDataReceiver = new WhatsAppDataReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_INTENT_FILTER_WHATS_APP_RECEIVER);
        getContext().registerReceiver(whatsAppDataReceiver, intentFilter);
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
                                loadingBar.setVisibility(View.GONE);
                            }
                        }.execute();


                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public class WhatsAppDataReceiver extends BroadcastReceiver {
        @Override

        public void onReceive(Context context, Intent intent) {

            long id = intent.getLongExtra(Constant.KEY_INTENT_ID, 0);
            String title = intent.getStringExtra(Constant.KEY_INTENT_TITLE);
            String message = intent.getStringExtra(Constant.KEY_INTENT_MESSAGE);
            String largeIconUri = intent.getStringExtra(Constant.KEY_INTENT_LATG_ICON_URI);
            long timeStamp = intent.getLongExtra(Constant.KEY_INTENT_TIMESTAMP, 0);

            boolean recordExists = myDataBaseHelper.checkIsRecordExist(TableName.TABLE_NAME_USER_WHATS_APP, myDataBaseHelper.KEY_USER_TITLE, title);
            if (!recordExists) {
                myDataBaseHelper.insertUsers(TableName.TABLE_NAME_USER_WHATS_APP, id, title, largeIconUri);
            }
            if (!message.contains("new messages")) {
                myDataBaseHelper.insertMessages(TableName.TABLE_NAME_MESSAGES_WHATS_APP, title, message, timeStamp);
            }

            Log.d(TAG, "onReceive: Received Notification");

            getMessageInBackgroundTask();
        }
    }


    private void settingsIntent() {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);
        Log.d(TAG, "settingsIntent: Clicked ");
    }

    private void startNotificationService() {
        Intent serviceIntent = new Intent(context, NotificationService.class);
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        getContext().unregisterReceiver(whatsAppDataReceiver);
        if (isContextualMenuOpen) {
            closeContextualMenu();
        }
        if (!usersList.isEmpty()) {
            usersList.clear();
        }
    }


    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constant.REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
        }
    }

}
