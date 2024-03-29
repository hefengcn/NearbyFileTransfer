package com.tab.demo.nearby;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;
import androidx.recyclerview.widget.RecyclerView;

class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private static final String TAG = "MyAdapter";
    private Context mContext;
    private SimpleArrayMap<String, EndpointStatus> mDataset;

    private ItemClickListener mItemClickListener;

    public interface ItemClickListener {
        public void onItemClick(int position);
    }

    public void setOnItemClickListener(ItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;

    }

    public MyAdapter(Context context) {
        this.mContext = context;
        this.mDataset = new SimpleArrayMap<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {

        holder.mTv1.setText("ID: " + mDataset.keyAt(position));
        holder.mTv2.setText("NAME: " + mDataset.valueAt(position).getName());
        holder.mTv3.setText("STATUS: " + mDataset.valueAt(position).getStatus());

        if (mItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemClickListener.onItemClick(position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView mTv1, mTv2, mTv3;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mTv1 = itemView.findViewById(R.id.remote_endpoint_id);
            mTv2 = itemView.findViewById(R.id.remote_endpoint_name);
            mTv3 = itemView.findViewById(R.id.remote_endpoint_status);
        }
    }

    public void setmDataset(SimpleArrayMap<String, EndpointStatus> mDataset) {
        this.mDataset = mDataset;
        notifyDataSetChanged();
    }

}
