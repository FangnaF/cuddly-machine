package com.example.fang.disymptomaticmachine;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static boolean deviceFlag=false;
    private Button beginDevice;
    private MyReceiver mReceiver;
    private static int CMD_STOP_SERVICE=1;
    private static int CMD_SEND_DATA=2;
    private static int CMD_SHOW_TOAST=3;
    private static int CMD_SYSTEM_EXIT=4;
    private static int CMD_RETURN=5;

    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beginDevice=(Button)findViewById(R.id.beginDevice);
        beginDevice.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        mReceiver = new MyReceiver();
        IntentFilter mFilter=new IntentFilter();
        mFilter.addAction("android.intent.action.cmdactivity");
        MainActivity.this.registerReceiver(mReceiver, mFilter);
    }



    private static final String TAG = "MainActivity";

    /*

     * BroadcastReceiver for Service

     */

    public class MyReceiver extends BroadcastReceiver {



        @Override

        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals("android.intent.action.cmdactivity")){

                Bundle bundle = intent.getExtras();

                int cmd = bundle.getInt("cmd");



                if(cmd == CMD_SHOW_TOAST){

			        String str = bundle.getString("str");

                    Toast.makeText(MainActivity.this,str,Toast.LENGTH_SHORT).show();

                }

                else if(cmd == CMD_SYSTEM_EXIT){

                    System.exit(0);

                }else if(cmd == CMD_RETURN){
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Intent i = new Intent(MainActivity.this,finishTestActivity.class);
                    startActivity(i);
                }

            }

        }

    }


    /* * Send message to service */
    public void mSendBroadcast(int cmd, int value) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.cmdservice");
        intent.putExtra("cmd", cmd);
        intent.putExtra("value", value);
        sendBroadcast(intent);
        Log.d(TAG, "sendBroadcast: " + CMD_SEND_DATA + " " + value);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.aboutItem:
                Intent i=new Intent(this,AboutActivity.class);
                startActivity(i);
                break;

            case R.id.searchForDeviceItem:
                Intent searchForDevice=new Intent(this,BluetoothService.class);
                startService(searchForDevice);
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){


            case R.id.beginDevice:
                if(deviceFlag) {
                    mSendBroadcast(CMD_SEND_DATA, 1);
                    if (progressDialog == null) {

                        progressDialog = ProgressDialog.show(MainActivity.this, "正在检测",
                                "请稍后", true, false);
                    } else if (progressDialog.isShowing()) {
                        progressDialog.setTitle("正在检测");
                        progressDialog.setMessage("请稍后");
                    }

                    progressDialog.show();


                }else{
                    Toast.makeText(this,"请先连接设备",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
