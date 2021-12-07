package com.audiobook.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.audiobook.utils.FragmentCreator;

import org.jetbrains.annotations.NotNull;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.adapter
 * @Date 2021/10/25 21:18
 */
public class MainContentAdapter extends FragmentPagerAdapter {
    public MainContentAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

   private FragmentCreator mFragmentCreator;

    @NonNull
    @Override
    public Fragment getItem(int position) {
        //return FragmentCreator.getFragment(position);
        return mFragmentCreator.getInstance().getFragment(position);
    }

    @Override
    public int getCount() {
        return FragmentCreator.PAGE_COUNT;
    }

    @Override
    public void destroyItem(@NonNull @NotNull ViewGroup container, int position, @NonNull @NotNull Object object) {
        mFragmentCreator.getInstance().removeFragment(position);
    }
}
