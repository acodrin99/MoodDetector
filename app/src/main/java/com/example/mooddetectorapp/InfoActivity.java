package com.example.mooddetectorapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Information");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        InfoPagerAdapter adapter = new InfoPagerAdapter(this);
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Anxiety");
                    break;
                case 1:
                    tab.setText("Depression");
                    break;
                case 2:
                    tab.setText("What to Do");
                    break;
                case 3:
                    tab.setText("What to Eat");
                    break;
                case 4:
                    tab.setText("How to Think");
                    break;
            }
        }).attach();
    }
}
