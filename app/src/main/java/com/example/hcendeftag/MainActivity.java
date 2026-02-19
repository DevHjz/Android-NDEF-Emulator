package com.example.hcendeftag;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.hcendeftag.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean isAdding = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        
        // 初始加载列表页面
        showFragment(new TagListFragment(), "TagList", false);

        binding.fab.setOnClickListener(view -> {
            if (!isAdding) {
                showFragment(new AddTagFragment(), "AddTag", true);
                binding.fab.setImageResource(android.R.drawable.ic_menu_revert);
                isAdding = true;
                binding.toolbar.setTitle("添加 NDEF 标签");
            } else {
                onBackPressed();
            }
        });
    }

    public void showFragment(Fragment fragment, String tag, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment_content_main, fragment, tag);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            isAdding = false;
            binding.fab.setImageResource(android.R.drawable.ic_input_add);
            binding.toolbar.setTitle(R.string.app_name);
        } else {
            super.onBackPressed();
        }
    }

    public void onTagSaved() {
        onBackPressed();
    }
}
