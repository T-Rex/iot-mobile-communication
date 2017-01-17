package com.digitalmoo.androidserialdemo.ui.fragment.usb_device_list;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.digitalmoo.androidserialdemo.R;

import java.util.ArrayList;

public class UsbDeviceListFragment extends Fragment {

    private OnListFragmentInteractionListener mListener;

    public UsbDeviceListFragment() {
    }

    public static UsbDeviceListFragment newInstance() {
        return new UsbDeviceListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_usbdevice_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            UsbManager usbManager = (UsbManager)getActivity().getSystemService(Context.USB_SERVICE);
            recyclerView.setAdapter(new UsbDeviceRecyclerViewAdapter(
                    new ArrayList(usbManager.getDeviceList().values()), mListener));
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnListFragmentInteractionListener {

        void onListFragmentInteraction(UsbDevice item);
    }
}
