package com.audiobook.utils;

import com.audiobook.base.BaseFragment;
import com.audiobook.fragment.HistoryFragment;
import com.audiobook.fragment.RecommendFragment;
import com.audiobook.fragment.SubscriptionFragment;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.utils
 * @Date 2021/10/25 21:34
 */
public class FragmentCreator {

    private volatile static FragmentCreator mFragmentCreator = null;

    public static FragmentCreator getInstance() {
        if (mFragmentCreator == null) {
            synchronized (FragmentCreator.class) {
                if (mFragmentCreator == null) {
                    mFragmentCreator = new FragmentCreator();
                }
            }
        }
        return mFragmentCreator;
    }

    private FragmentCreator() {
        sCache = new HashMap<>();
    }

    public final static int INDEX_RECOMMEND = 0;
    public final static int INDEX_SUBSCRIPTION = 1;
    public final static int INDEX_HISTORY = 2;

    public final static int PAGE_COUNT = 3;

    private Map<Integer, BaseFragment> sCache;

    public BaseFragment getFragment(int index) {
        BaseFragment baseFragment = sCache.get(index);
        if (baseFragment != null) {
            return baseFragment;
        }
        switch (index) {
            case INDEX_RECOMMEND:
                baseFragment = new RecommendFragment();
                break;
            case INDEX_SUBSCRIPTION:
                baseFragment = new SubscriptionFragment();
                break;
            case INDEX_HISTORY:
                baseFragment = new HistoryFragment();
                break;
        }
        sCache.put(index, baseFragment);
        return baseFragment;
    }

    public void removeFragment(int index) {
        if (sCache.containsKey(index)) {
            sCache.remove(index);
        }
    }
/*
    public final static int INDEX_RECOMMEND = 0;
    public final static int INDEX_SUBSCRIPTION = 1;
    public final static int INDEX_HISTORY = 2;

    public final static int PAGE_COUNT = 3;

    private static final Map<Integer, BaseFragment> sCache = new HashMap<>();

    public static BaseFragment getFragment(int index) {
        BaseFragment baseFragment = sCache.get(index);
        if (baseFragment != null) {
            return baseFragment;
        }
        switch (index) {
            case INDEX_RECOMMEND:
                baseFragment = new RecommendFragment();
                break;
            case INDEX_SUBSCRIPTION:
                baseFragment = new SubscriptionFragment();
                break;
            case INDEX_HISTORY:
                baseFragment = new HistoryFragment();
                break;
        }
        sCache.put(index, baseFragment);
        return baseFragment;
    }
*/

   /* public static void removeFragment(int index){
        if (sCache.containsKey(index)){
            sCache.remove(index);
        }
    }*/
}
