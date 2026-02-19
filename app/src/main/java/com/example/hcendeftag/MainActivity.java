package com.example.hcendeftag;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.hcendeftag.databinding.ActivityMainBinding;
import com.example.hcendeftag.nfc.NfcReaderManager;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;
    private NfcReaderManager nfcReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        nfcReader = new NfcReaderManager(this);

        // FAB 按钮：启用 NdefService
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableComponent(getPackageManager(), NdefHceService.COMPONENT);
                Toast.makeText(MainActivity.this, "HCE 服务已启用", Toast.LENGTH_SHORT).show();
            }
        });

        // 处理来自 NFC 标签的 Intent
        handleNfcIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNfcIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcReader.enableForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcReader.disableForegroundDispatch();
    }

    /**
     * 处理 NFC Intent
     */
    private void handleNfcIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (action.equals("android.nfc.action.NDEF_DISCOVERED") ||
                    action.equals("android.nfc.action.TAG_DISCOVERED") ||
                    action.equals("android.nfc.action.TECH_DISCOVERED")) {
                // 导航到 NFC 读卡 Fragment
                navController.navigate(R.id.action_to_nfc_reader);
            }
        }
    }

    public static void enableComponent(PackageManager pm, ComponentName component) {
        pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
