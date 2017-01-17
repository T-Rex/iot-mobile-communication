package com.digitalmoo.androidserialdemo.ui.fragment.usb_device;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.digitalmoo.androidserialdemo.R;

import java.io.IOException;

public class UsbDeviceFragment extends Fragment {

    private static final String TAG = UsbDeviceFragment.class.getSimpleName();
    private static final String ARG_DEVICE = "device";

    public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;
    private static final int DEFAULT_BAUD_RATE = 9600;

    private UsbManager mUsbManager = null;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection = null;
    private UsbEndpoint mReadEndpoint = null;
    private UsbEndpoint mWriteEndpoint = null;

    private byte[] mReadBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
    private byte[] mWriteBuffer = new byte [DEFAULT_WRITE_BUFFER_SIZE];

    private final Object mReadBufferLock = new Object();
    private final Object mWriteBufferLock = new Object();

    private View connectButton = null;
    private View disconnectButton = null;
    private View ledControlButton = null;

    boolean ledState = false;

    public UsbDeviceFragment() {
        // Required empty public constructor
    }

    public static UsbDeviceFragment newInstance(UsbDevice device) {
        UsbDeviceFragment fragment = new UsbDeviceFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DEVICE, device);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        if (getArguments() != null) {
            mDevice = getArguments().getParcelable(ARG_DEVICE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_usbdevice, container, false);
        (connectButton = view.findViewById(R.id.button_connect)).setOnClickListener((v) -> {
            try {
                open(mUsbManager.openDevice(mDevice));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
            finally {
                updateUi();
            }
        });
        (disconnectButton = view.findViewById(R.id.button_disconnect)).setOnClickListener((v) -> {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                updateUi();
            }
        });
        (ledControlButton = view.findViewById(R.id.button_led_control)).setOnClickListener((v) -> {
            ledState = !ledState;
            try {
                write(new byte [] { (byte)(ledState ? 0 : 1) }, 200);
            } catch (IOException e) {
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUi();
    }

    private void updateUi() {
        connectButton.setEnabled(mConnection == null);
        disconnectButton.setEnabled(mConnection != null);
    }

    private void open(UsbDeviceConnection connection) throws IOException {
        if (mConnection != null) {
            connection.close();
            throw new IOException("Already opened.");
        }
        mConnection = connection;
        boolean opened = false;
        try {
            for (int i = 0; i < mDevice.getInterfaceCount(); i++) {
                UsbInterface usbIface = mDevice.getInterface(i);
                if (mConnection.claimInterface(usbIface, true)) {
                    Log.d(TAG, "claimInterface " + i + " SUCCESS");
                } else {
                    Log.d(TAG, "claimInterface " + i + " FAIL");
                }
            }

            UsbInterface dataIface = mDevice.getInterface(mDevice.getInterfaceCount() - 1);
            for (int i = 0; i < dataIface.getEndpointCount(); i++) {
                UsbEndpoint ep = dataIface.getEndpoint(i);
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                        mReadEndpoint = ep;
                    } else {
                        mWriteEndpoint = ep;
                    }
                }
            }
            opened = true;
            setBaudRate(DEFAULT_BAUD_RATE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (!opened) {
                try {
                    close();
                } catch (IOException e) {
                    // Ignore IOExceptions during close()
                }
            }
        }
    }

    private void close() throws IOException {
        if (mConnection == null) {
            throw new IOException("Already closed");
        }
        try {
            mConnection.close();
        } finally {
            mConnection = null;
        }
    }

    public int read(byte[] dest, int timeoutMillis) throws IOException {
        final int numBytesRead;
        synchronized (mReadBufferLock) {
            int readAmt = Math.min(dest.length, mReadBuffer.length);
            numBytesRead = mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, readAmt,
                    timeoutMillis);
            if (numBytesRead < 0) {
                // This sucks: we get -1 on timeout, not 0 as preferred.
                // We *should* use UsbRequest, except it has a bug/api oversight
                // where there is no way to determine the number of bytes read
                // in response :\ -- http://b.android.com/28023
                return 0;
            }
            System.arraycopy(mReadBuffer, 0, dest, 0, numBytesRead);
        }
        return numBytesRead;
    }

    public int write(byte[] src, int timeoutMillis) throws IOException {
        int offset = 0;

        while (offset < src.length) {
            final int writeLength;
            final int amtWritten;

            synchronized (mWriteBufferLock) {
                final byte[] writeBuffer;

                writeLength = Math.min(src.length - offset, mWriteBuffer.length);
                if (offset == 0) {
                    writeBuffer = src;
                } else {
                    // bulkTransfer does not support offsets, make a copy.
                    System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
                    writeBuffer = mWriteBuffer;
                }

                amtWritten = mConnection.bulkTransfer(mWriteEndpoint, writeBuffer, writeLength,
                        timeoutMillis);
            }
            if (amtWritten <= 0) {
                throw new IOException("Error writing " + writeLength
                        + " bytes at offset " + offset + " length=" + src.length);
            }

            Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
            offset += amtWritten;
        }
        return offset;
    }


    /*private byte[] getLineEncoding(int baudRate) {
        final byte[] lineEncodingRequest = { (byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08 };
        //Get the least significant byte of baudRate,
        //and put it in first byte of the array being sent
        lineEncodingRequest[0] = (byte)(baudRate & 0xFF);

        //Get the 2nd byte of baudRate,
        //and put it in second byte of the array being sent
        lineEncodingRequest[1] = (byte)((baudRate >> 8) & 0xFF);

        //ibid, for 3rd byte (my guess, because you need at least 3 bytes
        //to encode your 115200+ settings)
        lineEncodingRequest[2] = (byte)((baudRate >> 16) & 0xFF);

        return lineEncodingRequest;

    }

    private void setBaudRate(int baudRate) throws IOException {
        if(mConnection.controlTransfer(0x21, 0x20, 0, 0, getLineEncoding(baudRate), 7, 0) == 0) {
            throw new IOException("Error setting baud rate. #1");
        }
    }*/

    /*private void setBaudRate(int baudRate) throws IOException {
        do {
            if(mConnection.controlTransfer(0x40, 0, 0, 0, null, 0, 0) == 0) break; // reset
            if(mConnection.controlTransfer(0x40, 0, 1, 0, null, 0, 0) == 0) break; // clear Rx
            if(mConnection.controlTransfer(0x40, 0, 2, 0, null, 0, 0) == 0) break; // clear Tx
            if(mConnection.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 0) == 0) break; //Baud rate 9600
            return;
        }
        while(false);
        throw new IOException("Error setting baud rate. #1");
    }*/

    /*private void setBaudRate(int baudRate) throws IOException {
        do {
            if (mConnection.controlTransfer(0x21, 34, 0, 0, null, 0, 0) == 0) break;
            if (mConnection.controlTransfer(0x21, 32, 0, 0,
                    new byte[]{(byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08}, 7, 0) == 0) break;
            return;
        }
        while(false);
        throw new IOException("Error setting baud rate. #1");
    }*/

    private void setBaudRate(int baudRate) throws IOException {
        doBlackMagic();
        setParameters(9600, 8, STOPBITS_1, PARITY_NONE);
    }



    private final void ctrlOut(int request, int value, int index, byte[] data)
            throws IOException {
        outControlTransfer(PROLIFIC_CTRL_OUT_REQTYPE, request, value, index,
                data);
    }

    /** 1 stop bit. */
    public static final int STOPBITS_1 = 1;

    /** 1.5 stop bits. */
    public static final int STOPBITS_1_5 = 3;

    /** 2 stop bits. */
    public static final int STOPBITS_2 = 2;
    /** No parity. */
    public static final int PARITY_NONE = 0;

    /** Odd parity. */
    public static final int PARITY_ODD = 1;

    /** Even parity. */
    public static final int PARITY_EVEN = 2;

    /** Mark parity. */
    public static final int PARITY_MARK = 3;

    /** Space parity. */
    public static final int PARITY_SPACE = 4;


    public void setParameters(int baudRate, int dataBits, int stopBits,
                              int parity) throws IOException {

        byte[] lineRequestData = new byte[7];

        lineRequestData[0] = (byte) (baudRate & 0xff);
        lineRequestData[1] = (byte) ((baudRate >> 8) & 0xff);
        lineRequestData[2] = (byte) ((baudRate >> 16) & 0xff);
        lineRequestData[3] = (byte) ((baudRate >> 24) & 0xff);

        switch (stopBits) {
            case STOPBITS_1:
                lineRequestData[4] = 0;
                break;

            case STOPBITS_1_5:
                lineRequestData[4] = 1;
                break;

            case STOPBITS_2:
                lineRequestData[4] = 2;
                break;

            default:
                throw new IllegalArgumentException("Unknown stopBits value: " + stopBits);
        }

        switch (parity) {
            case PARITY_NONE:
                lineRequestData[5] = 0;
                break;

            case PARITY_ODD:
                lineRequestData[5] = 1;
                break;

            case PARITY_EVEN:
                lineRequestData[5] = 2;
                break;

            case PARITY_MARK:
                lineRequestData[5] = 3;
                break;

            case PARITY_SPACE:
                lineRequestData[5] = 4;
                break;

            default:
                throw new IllegalArgumentException("Unknown parity value: " + parity);
        }

        lineRequestData[6] = (byte) dataBits;

        ctrlOut(SET_LINE_REQUEST, 0, 0, lineRequestData);

        resetDevice();
    }

    private static final int FLUSH_RX_REQUEST = 0x08;
    private static final int FLUSH_TX_REQUEST = 0x09;

    private void resetDevice() throws IOException {
        purgeHwBuffers(true, true);
    }

    public boolean purgeHwBuffers(boolean purgeReadBuffers, boolean purgeWriteBuffers) throws IOException {
        if (purgeReadBuffers) {
            vendorOut(FLUSH_RX_REQUEST, 0, null);
        }

        if (purgeWriteBuffers) {
            vendorOut(FLUSH_TX_REQUEST, 0, null);
        }

        return purgeReadBuffers || purgeWriteBuffers;
    }


    private static final int DEVICE_TYPE_HX = 0;
    private static final int DEVICE_TYPE_0 = 1;
    private static final int DEVICE_TYPE_1 = 2;

    private static final int SET_LINE_REQUEST = 0x20;

    private int mDeviceType = DEVICE_TYPE_HX;

    private void doBlackMagic() throws IOException {
        vendorIn(0x8484, 0, 1);
        vendorOut(0x0404, 0, null);
        vendorIn(0x8484, 0, 1);
        vendorIn(0x8383, 0, 1);
        vendorIn(0x8484, 0, 1);
        vendorOut(0x0404, 1, null);
        vendorIn(0x8484, 0, 1);
        vendorIn(0x8383, 0, 1);
        vendorOut(0, 1, null);
        vendorOut(1, 0, null);
        vendorOut(2, (mDeviceType == DEVICE_TYPE_HX) ? 0x44 : 0x24, null);
    }

    private final byte[] vendorIn(int value, int index, int length)
            throws IOException {
        return inControlTransfer(PROLIFIC_VENDOR_IN_REQTYPE,
                PROLIFIC_VENDOR_READ_REQUEST, value, index, length);
    }

    private final void vendorOut(int value, int index, byte[] data)
            throws IOException {
        outControlTransfer(PROLIFIC_VENDOR_OUT_REQTYPE,
                PROLIFIC_VENDOR_WRITE_REQUEST, value, index, data);
    }


    private static final int USB_RECIP_INTERFACE = 0x01;
    private static final int PROLIFIC_VENDOR_READ_REQUEST = 0x01;
    private static final int PROLIFIC_VENDOR_WRITE_REQUEST = 0x01;
    private static final int USB_READ_TIMEOUT_MILLIS = 1000;

    private static final int PROLIFIC_VENDOR_OUT_REQTYPE = UsbConstants.USB_DIR_OUT
            | UsbConstants.USB_TYPE_VENDOR;

    private static final int PROLIFIC_VENDOR_IN_REQTYPE = UsbConstants.USB_DIR_IN
            | UsbConstants.USB_TYPE_VENDOR;

    private static final int PROLIFIC_CTRL_OUT_REQTYPE = UsbConstants.USB_DIR_OUT
            | UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE;

    private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;

    private final byte[] inControlTransfer(int requestType, int request,
                                           int value, int index, int length) throws IOException {
        byte[] buffer = new byte[length];
        int result = mConnection.controlTransfer(requestType, request, value,
                index, buffer, length, USB_READ_TIMEOUT_MILLIS);
        if (result != length) {
            throw new IOException(
                    String.format("ControlTransfer with value 0x%x failed: %d",
                            value, result));
        }
        return buffer;
    }

    private final void outControlTransfer(int requestType, int request,
                                          int value, int index, byte[] data) throws IOException {
        int length = (data == null) ? 0 : data.length;
        int result = mConnection.controlTransfer(requestType, request, value,
                index, data, length, USB_WRITE_TIMEOUT_MILLIS);
        if (result != length) {
            throw new IOException(
                    String.format("ControlTransfer with value 0x%x failed: %d",
                            value, result));
        }
    }
}
