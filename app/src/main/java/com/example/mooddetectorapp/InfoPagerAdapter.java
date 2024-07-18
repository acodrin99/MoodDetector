package com.example.mooddetectorapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class InfoPagerAdapter extends FragmentStateAdapter {

    public InfoPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AnxietyFragment();
            case 1:
                return new DepressionFragment();
            case 2:
                return new WhatToDoFragment();
            case 3:
                return new WhatToEatFragment();
            case 4:
                return new HowToThinkFragment();
            default:
                return new AnxietyFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5; // Number of tabs
    }
}
