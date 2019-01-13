package com.example.fang.disymptomaticmachine;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;


public class BluetoothService extends Service {

    private static final UUID HC_UUID   = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBtAdapter = null;

    private BluetoothSocket mBtSocket   = null;

    private OutputStream outStream = null;
private  CommandReceiver cmdReceiver;
    private InputStream inStream  = null;
private MyThread mThread;
    private boolean mBtFlag = true;
    private boolean threadFlag;
    private static final String TAG = "BluetoothService";
    private static int CMD_STOP_SERVICE=1;
    private static int CMD_SEND_DATA=2;
    private static int CMD_SHOW_TOAST=3;
    private static int CMD_SYSTEM_EXIT=4;
    private static int CMD_RETURN=5;

    /**

     * Called by onStartCommand, initialize and start runtime thread

     */

    private void myStartService() {

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if ( mBtAdapter == null ) {

            showToast("未发现蓝牙");

            mBtFlag  = false;

            return;

        }

        if ( !mBtAdapter.isEnabled() ) {

            mBtFlag  = false;

            //myStopService();
            stopSelf();
            showToast("请打开蓝牙");

            return;

        }



        showToast("开始搜索！");

        threadFlag = true;

         mThread = new MyThread();

        mThread.start();

    }




    /**

     * Thread runtime

     */

    public class MyThread extends Thread {

        @Override

        public void run() {

            super.run();

            myBtConnect();

            while( threadFlag ) {

                readSerial();

                try{

                    Thread.sleep(30);

                }catch(Exception e){

                    e.printStackTrace();

                }

            }

        }

    }




    /**

     * device control

     */

    public void myBtConnect() {

        showToast("连接中...");



        /* Discovery device */

//	BluetoothDevice mBtDevice = mBtAdapter.getRemoteDevice(HC_MAC);

        BluetoothDevice mBtDevice = null;

        Set<BluetoothDevice> mBtDevices = mBtAdapter.getBondedDevices();

        if ( mBtDevices.size() > 0 ) {

            for (Iterator<BluetoothDevice> iterator = mBtDevices.iterator(); iterator.hasNext(); ) {

                mBtDevice = (BluetoothDevice)iterator.next();

                showToast("发现设备："+mBtDevice.getName() + "|" + mBtDevice.getAddress()+"，但未连接");


                try {
                    if(mBtDevice.getName().equals("DIMachine")) {
                        showToast("连接设备" + mBtDevice.getName() + "|" + mBtDevice.getAddress());
                        mBtSocket = mBtDevice.createRfcommSocketToServiceRecord(HC_UUID);
                    }
                } catch (IOException e) {

                    e.printStackTrace();

                    mBtFlag = false;

                    showToast("创建蓝牙对话失败，请重新连接");

                }

            }

        }else{
            showToast("未发现设备，请先配对");
        }






        mBtAdapter.cancelDiscovery();



        /* Setup connection */

        try {
            if(mBtSocket!=null) {
                mBtSocket.connect();
                MainActivity.deviceFlag = true;
                showToast("连接成功");

            }else{
                mBtFlag = false;
            }
        } catch (IOException e) {

            e.printStackTrace();

            try {
                MainActivity.deviceFlag=false;
                showToast("连接失败，关闭");

                mBtSocket.close();

                mBtFlag = false;

            } catch (IOException e1) {

                e1.printStackTrace();

            }

        }



        /* I/O initialize */

        if ( mBtFlag ) {

            try {

                inStream  = mBtSocket.getInputStream();

                outStream = mBtSocket.getOutputStream();

            } catch (IOException e) {

                e.printStackTrace();

            }

        }

        //showToast("Bluetooth is ready!");

    }




    /**

     * Read serial data from HC06

     */

    public int readSerial() {

        int ret = 0;

        byte[] rsp = null;



        if ( !mBtFlag ) {

            return -1;

        }

        try {

            rsp = new byte[inStream.available()];

            ret = inStream.read(rsp);
        if(ret !=0){
            showReturn(new String(rsp));
        }


        } catch (IOException e) {

            // TODO Auto-generated catch block

            e.printStackTrace();

        }

        return ret;

    }



    /**

     * Write serial data to HC06

     * @param value - command

     */

    public void writeSerial(int value) {

        String ha = "" + value;

        try {

            outStream.write(ha.getBytes());

            outStream.flush();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        myStartService();
        return super.onStartCommand(intent, flags, startId);
    }



    /* * BroadcastReceiver for Activity */
    private class CommandReceiver extends BroadcastReceiver{
        @Override	public void onReceive(Context context, Intent intent) {
            if ( intent.getAction().equals("android.intent.action.cmdservice") ){
                int cmd = intent.getIntExtra("cmd", -1);
                int value = intent.getIntExtra("value", -1);
                if ( cmd == CMD_STOP_SERVICE ) {
                    stopSelf();
                }else if ( cmd == CMD_SEND_DATA ) {
                    writeSerial(value);
                }
            }
        }
    }


    /* * Tell Activity to show message on screen */
    public void showToast(String str) {
        Intent intent = new Intent();
        intent.putExtra("cmd", CMD_SHOW_TOAST);
        intent.putExtra("str", str);
        intent.setAction("android.intent.action.cmdactivity");
        sendBroadcast(intent);
    }
    /* * Tell Activity to show message on screen */
    public void showReturn(String str) {
        Intent intent = new Intent();
        intent.putExtra("cmd", CMD_RETURN);
        intent.putExtra("str", str);
        intent.setAction("android.intent.action.cmdactivity");
        sendBroadcast(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        cmdReceiver = new CommandReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.cmdservice");
        registerReceiver(cmdReceiver, filter);

    }

    @Override

    public void onDestroy() {

        Log.i(TAG, "onDestroy()");

        super.onDestroy();

        this.unregisterReceiver(cmdReceiver);

        threadFlag = false;
        MainActivity.deviceFlag=false;
        boolean retry = true;

        while ( retry ) {

            try {

                retry = false;

                mThread.join();

            } catch (Exception e) {

                e.printStackTrace();

            }

        }

    }



    public BluetoothService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
