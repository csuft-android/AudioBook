package com.audiobook.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


/**
 * @author 优雅永不过时
 * @Package com.audiobook.base
 * @Date 2021/10/25 21:24
 */
public abstract class BaseFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return onSubViewLoaded(inflater, container);
    }

    protected abstract View onSubViewLoaded(LayoutInflater layoutInflater, ViewGroup container);

}
