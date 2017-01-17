package com.digitalmoo.androidserialdemo.ui.activity;

import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.digitalmoo.androidserialdemo.R;
import com.digitalmoo.androidserialdemo.ui.fragment.usb_device.UsbDeviceFragment;
import com.digitalmoo.androidserialdemo.ui.fragment.usb_device_list.UsbDeviceListFragment;

public class MainActivity extends AppCompatActivity implements UsbDeviceListFragment.OnListFragmentInteractionListener {

    private static final String TAG_USB_DEVICE = "usb_device";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, UsbDeviceListFragment.newInstance())
                .commit();
    }

    @Override
    public void onListFragmentInteraction(UsbDevice item) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, UsbDeviceFragment.newInstance(item))
                .addToBackStack(TAG_USB_DEVICE)
                .commit();
    }
}
