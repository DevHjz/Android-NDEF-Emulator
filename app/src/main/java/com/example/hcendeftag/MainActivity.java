package com.example.hcendeftag;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.hcendeftag.databinding.ActivityMainBinding;
import com.example.hcendeftag.nfc.NfcReaderManager;

import java.util.List;

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

        // 处理启动时的 NFC Intent
        handleNfcIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // 重要：更新 Activity 的 Intent
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
     * 处理 NFC Intent，并传递给 NfcReaderFragment
     */
    private void handleNfcIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                    NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                    NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
                
                // 如果当前不在读卡页面，则导航过去
                if (navController.getCurrentDestination() != null && 
                    navController.getCurrentDestination().getId() != R.id.NfcReaderFragment) {
                    navController.navigate(R.id.action_to_nfc_reader);
                }
                
                // 寻找 NfcReaderFragment 并传递 Intent
                Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
                if (navHostFragment != null) {
                    List<Fragment> fragments = navHostFragment.getChildFragmentManager().getFragments();
                    for (Fragment fragment : fragments) {
                        if (fragment instanceof NfcReaderFragment && fragment.isVisible()) {
                            ((NfcReaderFragment) fragment).onNewIntent(intent);
                            break;
                        }
                    }
                }
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
