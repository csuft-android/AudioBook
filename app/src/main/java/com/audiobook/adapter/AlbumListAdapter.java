package com.audiobook.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.audiobook.R;
import com.audiobook.utils.LogUtil;
import com.ximalaya.ting.android.opensdk.model.album.Album;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.adapter
 * @Date 2021/10/25 23:25
 */
public class AlbumListAdapter extends RecyclerView.Adapter<AlbumListAdapter.InnerHolder> {

    private static final String TAG = "AlbumListAdapter";
    private List<Album> mData = new ArrayList<>();
    private onAlbumItemClickListener mItemClickListener = null;
    private onAlbumItemLongClickListener mOnAlbumItemLongClickListener = null;

    @NonNull
    @Override
    public InnerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //这里是载入View
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommend, parent, false);
        return new InnerHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull InnerHolder holder, int position) {
        //这里是设置数据
        //把当前的位置给item
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.d(TAG, "点击的专辑==》" + v.getTag());
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(position, mData.get(position));
                }
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (mOnAlbumItemLongClickListener != null) {
                int clickPosition = (Integer) v.getTag();
                mOnAlbumItemLongClickListener.onItemLongClick(mData.get(clickPosition));
            }
            //true表示消费了该事件
            return true;
        });

        holder.setData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        //返回要显示的个数
        if (mData != null) {
            return mData.size();
        }
        return 0;
    }

    public void setData(List<Album> albumList) {
        if (mData != null) {
            mData.clear();
            mData.addAll(albumList);
        }
        //更新UI
        notifyDataSetChanged();
    }


    public class InnerHolder extends RecyclerView.ViewHolder {
        public InnerHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void setData(Album album) {
            //找到这个控件，设置数据
            //专辑封面
            ImageView albumCoverIv = itemView.findViewById(R.id.album_cover);
            //title
            TextView albumTitleTv = itemView.findViewById(R.id.album_title_tv);
            //描述
            TextView albumDesTv = itemView.findViewById(R.id.album_description_tv);
            //播放数量
            TextView albumPlayCountTv = itemView.findViewById(R.id.album_play_count);
            //专辑内容数量
            TextView albumContentCountTv = itemView.findViewById(R.id.album_content_size);

            albumTitleTv.setText(album.getAlbumTitle());
            albumDesTv.setText(album.getAlbumIntro());
            albumPlayCountTv.setText(String.valueOf(album.getPlayCount()));
            albumContentCountTv.setText(String.valueOf(album.getIncludeTrackCount()));

            String coverUrlLarge = album.getCoverUrlLarge();
            if (!TextUtils.isEmpty(coverUrlLarge)) {
                Glide.with(itemView.getContext()).load(coverUrlLarge).into(albumCoverIv);
            } else {
                albumCoverIv.setImageResource(R.mipmap.ximalay_logo);
            }
        }
    }

    public void setAlbumItemClickListener(onAlbumItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public interface onAlbumItemClickListener {
        void onItemClick(int position, Album album);
    }

    public void setOnAlbumItemLongClickListener(onAlbumItemLongClickListener onAlbumItemLongClickListener) {
        this.mOnAlbumItemLongClickListener = onAlbumItemLongClickListener;
    }

    /**
     * item长按的接口
     */
    public interface onAlbumItemLongClickListener {
        void onItemLongClick(Album album);
    }
}

