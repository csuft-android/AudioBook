package com.audiobook.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.audiobook.R;
import com.ximalaya.ting.android.opensdk.model.track.Track;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.adapter
 * @Date 2021/10/26 17:40
 */
@SuppressLint("SimpleDateFormat")
public class DetailListAdapter extends RecyclerView.Adapter<DetailListAdapter.InnerHolder> {

    private List<Track> mDetailData = new ArrayList<>();
    //格式化时间
    private SimpleDateFormat mUpdateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat mDurationFormat = new SimpleDateFormat("mm:ss");
    private Context mContext;

    @NonNull
    @Override
    public DetailListAdapter.InnerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_album_detail, parent, false);
        return new InnerHolder(itemView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull DetailListAdapter.InnerHolder holder, int position) {
        //找到控件，设置数据
        View itemView = holder.itemView;
        Track track = mDetailData.get(position);
        holder.setData(track, position);

        //设置Item的点击事件
        itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                //参数需要有列表和位置
                mOnItemClickListener.onItemClick(mDetailData, position);
            }
        });
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(List<Track> detailData, int position);
    }

    @Override

    public int getItemCount() {
        if (mDetailData != null) {
            return mDetailData.size();
        }
        return 0;
    }

    public void setData(List<Track> tracks) {
        //清除原来的数据
        mDetailData.clear();
        //添加新的数据
        mDetailData.addAll(tracks);
        //更新UI
        notifyDataSetChanged();
    }

    public class InnerHolder extends RecyclerView.ViewHolder {


        public InnerHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void setData(Track track, int position) {
            TextView orderText = itemView.findViewById(R.id.order_text);
            TextView detailItemTitle = itemView.findViewById(R.id.detail_item_title);
            TextView detailItemPlayCount = itemView.findViewById(R.id.detail_item_play_count);
            TextView detailItemDuration = itemView.findViewById(R.id.detail_item_duration);
            TextView detailItemUpdateTime = itemView.findViewById(R.id.detail_item_update_time);
            //顺序Id
            orderText.setText(String.valueOf(position + 1));
            //标题
            detailItemTitle.setText(track.getTrackTitle());
            //播放次数
            detailItemPlayCount.setText(String.valueOf(track.getPlayCount()));
            //时长
            long durationMil = track.getDuration() * 1000L;
            String duration = mDurationFormat.format(durationMil);
            detailItemDuration.setText(duration);
            //更新日期
            String updateTimeText = mUpdateFormat.format(track.getUpdatedAt());
            detailItemUpdateTime.setText(updateTimeText);
        }
    }
}
