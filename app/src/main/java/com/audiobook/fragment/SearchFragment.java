package com.audiobook.fragment;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiobook.R;
import com.audiobook.adapter.AlbumListAdapter;
import com.audiobook.adapter.SearchRecommendAdapter;
import com.audiobook.base.BaseFragment;
import com.audiobook.customView.FlowTextLayout;
import com.audiobook.presenter.AlbumDetailPresenter;
import com.audiobook.presenter.SearchPresenter;
import com.audiobook.utils.LogUtil;
import com.audiobook.utils.UILoader;
import com.audiobook.view.ISearchCallback;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.word.HotWord;
import com.ximalaya.ting.android.opensdk.model.word.QueryResult;

import net.lucode.hackware.magicindicator.buildins.UIUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.fragment
 * @Date 2022/2/11 16:04
 */
public class SearchFragment extends BaseFragment implements ISearchCallback, AlbumListAdapter.onAlbumItemClickListener {
    private static final String TAG = "SearchFragment";
    private UILoader mUiLoader;
    private ImageView mBackBtn;
    private EditText mInputBox;
    private TextView mSearchBtn;
    private FrameLayout mResultContainer;
    private SearchPresenter mSearchPresenter;
    private RecyclerView mResultListView;
    private AlbumListAdapter mAlbumListAdapter;
    private FlowTextLayout mFlowTextLayout;
    private InputMethodManager mImm;
    private ImageView mDelBtn;
    public static final int TIME_SHOW_IMM = 500;
    private RecyclerView mSearchRecommendList;
    private SearchRecommendAdapter mSearchRecommendAdapter;
    private TwinklingRefreshLayout mRefreshLayout;
    private boolean mNeedSuggestWords = true;
    private FragmentManager mFm;

    @Override
    protected View onSubViewLoaded(LayoutInflater layoutInflater, ViewGroup container) {
        LinearLayout rootView = (LinearLayout) layoutInflater.inflate(R.layout.activity_search, container, false);
        mFm = getFragmentManager();
        initView(rootView);
        if (mUiLoader == null) {
            mUiLoader = new UILoader(getContext()) {
                @Override
                protected View getSuccessView(ViewGroup container) {
                    return createSuccessView(container);
                }
            };
        }
        //解绑
        if (mUiLoader.getParent() instanceof ViewGroup) {
            ((ViewGroup) mUiLoader.getParent()).removeView(mUiLoader);
        }
        mResultContainer.addView(mUiLoader);
        mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        initPresenter();
        return rootView;
    }

    private void initPresenter() {
        //注册UI更新的接口
        mSearchPresenter = SearchPresenter.getSearchPresenter();
        mSearchPresenter.registerViewCallback(this);
        //去拿热词
        mSearchPresenter.getHotWord();
        initEvent();
    }

    private void initView(LinearLayout rootView) {
        mBackBtn = rootView.findViewById(R.id.search_back);
        mInputBox = rootView.findViewById(R.id.search_input);
        mDelBtn = rootView.findViewById(R.id.search_input_delete);
        mDelBtn.setVisibility(View.GONE);
        mInputBox.postDelayed(() -> {
            mInputBox.requestFocus();
            mImm.showSoftInput(mInputBox, InputMethodManager.SHOW_IMPLICIT);
        }, TIME_SHOW_IMM);
        mSearchBtn = rootView.findViewById(R.id.search_btn);
        mResultContainer = rootView.findViewById(R.id.search_container);
    }

    private void initEvent() {
        mUiLoader.setOnRetryClickListener(() -> {
            if (mSearchPresenter != null) {
                mSearchPresenter.reSearch();
                mUiLoader.upDateStatus(UILoader.UIStatus.LOADING);
            }
        });
        mRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onLoadMore(TwinklingRefreshLayout refreshLayout) {
                LogUtil.d(TAG, "load more...");
                //加载更多的内容
                if (mSearchPresenter != null) {
                    mSearchPresenter.loadMore();
                }
            }
        });
        mBackBtn.setOnClickListener(v -> mFm.popBackStack());
        mDelBtn.setOnClickListener(v -> mInputBox.setText(""));
        mSearchBtn.setOnClickListener(v -> {
            //去调用搜索的逻辑
            String keyword = mInputBox.getText().toString().trim();
            if (TextUtils.isEmpty(keyword)) {
                //可以给个提示
                Toast.makeText(getContext(), "搜索关键字不能为空.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mSearchPresenter != null) {
                mSearchPresenter.doSearch(keyword);
                mUiLoader.upDateStatus(UILoader.UIStatus.LOADING);
            }
        });
        mInputBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    mSearchPresenter.getHotWord();
                    mDelBtn.setVisibility(View.GONE);
                } else {
                    mDelBtn.setVisibility(View.VISIBLE);
                    if (mNeedSuggestWords) {
                        //触发联想查询
                        getSuggestWord(s.toString());
                    } else {
                        mNeedSuggestWords = true;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mFlowTextLayout.setClickListener(new FlowTextLayout.ItemClickListener() {
            @Override
            public void onItemClick(String text) {
                //不需要相关的联想词
                mNeedSuggestWords = false;
                switch2Search(text);
            }
        });
        if (mSearchRecommendAdapter != null) {
            mSearchRecommendAdapter.setItemClickListener(keyword -> {
                //LogUtil.d(TAG, "mSearchRecommendAdapter  keyword --- > " + keyword);
                //不需要相关的联想词
                mNeedSuggestWords = false;
                //推荐热词的点击
                switch2Search(keyword);
            });
        }

        mAlbumListAdapter.setAlbumItemClickListener(this);
    }

    /**
     * 热词的搜索
     *
     * @param text
     */
    private void switch2Search(String text) {
        if (TextUtils.isEmpty(text)) {
            //可以给个提示
            Toast.makeText(getContext(), "搜索关键字不能为空.", Toast.LENGTH_SHORT).show();
            return;
        }
        //第一步，把热词扔到输入框里
        mInputBox.setText(text);
        mInputBox.setSelection(text.length());
        //第二步，发起搜索
        if (mSearchPresenter != null) {
            mSearchPresenter.doSearch(text);
        }
        //改变UI状态
        if (mUiLoader != null) {
            mUiLoader.upDateStatus(UILoader.UIStatus.LOADING);
        }
    }

    /**
     * 获取联想的关键词
     *
     * @param keyword
     */
    private void getSuggestWord(String keyword) {
        LogUtil.d(TAG, "getSuggestWord --- > " + keyword);
        if (mSearchPresenter != null) {
            mSearchPresenter.getRecommendWord(keyword);
        }
    }


    private View createSuccessView(ViewGroup container) {
        View resultView = LayoutInflater.from(container.getContext()).inflate(R.layout.search_result_layout, container, false);
        //刷新控件
        mRefreshLayout = resultView.findViewById(R.id.search_result_refresh);
        mRefreshLayout.setEnableRefresh(false);
        //显示热词的
        mFlowTextLayout = resultView.findViewById(R.id.recommend_hot_word_view);

        mResultListView = resultView.findViewById(R.id.result_list_view);
        //设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(container.getContext());
        mResultListView.setLayoutManager(layoutManager);
        //设置适配器
        mAlbumListAdapter = new AlbumListAdapter();
        mResultListView.setAdapter(mAlbumListAdapter);
        mResultListView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
                if (layoutManager == null) {
                    return;
                }
                int itemPosition = parent.getChildAdapterPosition(view);
                final int itemCount = layoutManager.getItemCount();
                final int lastItemIndex = itemCount - 1;
                outRect.top = UIUtil.dip2px(view.getContext(), 6);
                outRect.bottom = UIUtil.dip2px(view.getContext(), itemPosition != lastItemIndex ? 0 : 6);
                outRect.left = UIUtil.dip2px(view.getContext(), 6);
                outRect.right = UIUtil.dip2px(view.getContext(), 6);
            }
        });

        //搜索推荐
        mSearchRecommendList = resultView.findViewById(R.id.search_recommend_list);
        //设置布局管理器
        LinearLayoutManager recommendLayoutManager = new LinearLayoutManager(container.getContext());
        mSearchRecommendList.setLayoutManager(recommendLayoutManager);
        mSearchRecommendList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.top = UIUtil.dip2px(view.getContext(), 2);
                outRect.bottom = UIUtil.dip2px(view.getContext(), 2);
                outRect.left = UIUtil.dip2px(view.getContext(), 5);
                outRect.right = UIUtil.dip2px(view.getContext(), 5);
            }
        });
        //设置适配器
        mSearchRecommendAdapter = new SearchRecommendAdapter();
        mSearchRecommendList.setAdapter(mSearchRecommendAdapter);
        return resultView;
    }

    @Override
    public void onSearchResultLoaded(List<Album> result) {
        hideSuccessView();
        mRefreshLayout.setVisibility(View.VISIBLE);
        //隐藏键盘
        mImm.hideSoftInputFromWindow(mInputBox.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        handleSearchResult(result);
    }

    private void handleSearchResult(List<Album> result) {
        if (result != null) {
            if (result.size() == 0) {
                //数据为空
                if (mUiLoader != null) {
                    mUiLoader.upDateStatus(UILoader.UIStatus.EMPTY);
                }
            } else {
                //如果数据不为空，那么就设置数据
                mAlbumListAdapter.setData(result);
                mUiLoader.upDateStatus(UILoader.UIStatus.SUCCESS);
            }
        }
    }

    private void hideSuccessView() {
        mSearchRecommendList.setVisibility(View.GONE);
        mRefreshLayout.setVisibility(View.GONE);
        mFlowTextLayout.setVisibility(View.GONE);
    }

    @Override
    public void onHotWordLoaded(List<HotWord> hotWordList) {
        hideSuccessView();
        mFlowTextLayout.setVisibility(View.VISIBLE);
        if (mUiLoader != null) {
            mUiLoader.upDateStatus(UILoader.UIStatus.SUCCESS);
        }
        LogUtil.d(TAG, "hotWordList --- > " + hotWordList.size());
        List<String> hotWords = new ArrayList<>();
        hotWords.clear();
        for (HotWord hotWord : hotWordList) {
            String searchWord = hotWord.getSearchword();
            hotWords.add(searchWord);
        }
        Collections.sort(hotWords);
        //更新UI。
        mFlowTextLayout.setTextContents(hotWords);
    }

    @Override
    public void onLoadMoreResult(List<Album> result, boolean isOkay) {
        //处理加载更多的结果
        if (mRefreshLayout != null) {
            mRefreshLayout.finishLoadmore();
        }
        if (isOkay) {
            handleSearchResult(result);
        } else {
            Toast.makeText(getContext(), "没有更多内容", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRecommendWordLoaded(List<QueryResult> keyWordList) {
        //关键字的联想词
        LogUtil.d(TAG, "onRecommendWordLoaded --- > " + keyWordList.size());
        if (mSearchRecommendAdapter != null) {
            mSearchRecommendAdapter.setData(keyWordList);
        }
        //控制UI的状态隐藏和显示
        if (mUiLoader != null) {
            mUiLoader.upDateStatus(UILoader.UIStatus.SUCCESS);
        }
        //控制显示和隐藏
        hideSuccessView();
        mSearchRecommendList.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSearchPresenter != null) {
            mSearchPresenter.unRegisterViewCallback(this);
        }
    }

    @Override
    public void onError(int errorCode, String errorMsg) {
        if (mUiLoader != null) {
            mUiLoader.upDateStatus(UILoader.UIStatus.NETWORK_ERROR);
        }
    }

    @Override
    public void onItemClick(int position, Album album) {
        AlbumDetailPresenter.getInstance().setTargetAlbum(album);
        //item被点击了，跳转到详情界面
        /*        startActivity(new Intent(getContext(), DetailActivity.class));*/
        FragmentTransaction ft = mFm.beginTransaction();
        ft.replace(R.id.main_contain, new DetailFragment());
        ft.addToBackStack(null);
        ft.commit();
    }

}
