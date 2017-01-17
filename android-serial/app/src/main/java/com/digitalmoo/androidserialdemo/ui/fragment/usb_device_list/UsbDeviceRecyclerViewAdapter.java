package com.digitalmoo.androidserialdemo.ui.fragment.usb_device_list;

import android.hardware.usb.UsbDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.digitalmoo.androidserialdemo.R;
import com.digitalmoo.androidserialdemo.ui.fragment.usb_device_list.UsbDeviceListFragment.OnListFragmentInteractionListener;

import java.util.List;

public class UsbDeviceRecyclerViewAdapter extends RecyclerView.Adapter<UsbDeviceRecyclerViewAdapter.ViewHolder> {

    private final List<UsbDevice> mValues;
    private final OnListFragmentInteractionListener mListener;

    public UsbDeviceRecyclerViewAdapter(List<UsbDevice> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_usb_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(String.format("%d:%d",
                holder.mItem.getVendorId(),
                holder.mItem.getProductId()));
        holder.mContentView.setText(holder.mItem.getDeviceName());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public UsbDevice mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.id);
            mContentView = (TextView) view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
