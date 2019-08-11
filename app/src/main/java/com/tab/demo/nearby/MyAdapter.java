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
    private Context mContext;

    public void setmDataset(SimpleArrayMap<String, EndpointStatus> mDataset) {
        this.mDataset = mDataset;
        notifyDataSetChanged();
    }

    private SimpleArrayMap<String, EndpointStatus> mDataset;

    public MyAdapter(Context context) {
        this.mContext = context;
        this.mDataset = new SimpleArrayMap<String, EndpointStatus>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        holder.mTv1.setText(mDataset.keyAt(position));
        holder.mTv2.setText(mDataset.valueAt(position).getName());
        holder.mTv3.setText(mDataset.valueAt(position).getStatus());
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

}
