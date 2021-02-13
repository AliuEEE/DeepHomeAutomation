package com.example.ero.deephomeautomation;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    int connected_status = 0;

    Button connect;
    Button mExit;
    ListView listview;
    BluetoothAdapter bluetoothAdapter;  //Making bluetooth variable
    ArrayList<String> bluetoothDevices;
    ArrayList<BluetoothDevice> mbluetoothDevices;
    ArrayAdapter<String> arrayAdapter;
    String deviceString;
    Button okBT;
    Button cancelBT;
    Button voiceControl;
    String mStatus = " ";
    Button lightON;
    Button lightOFF;
    Button fanOn;
    Button fanOff;
    Button ACon;
    Button ACoff;
    Button light2On;
    Button light2Off;
    private Toast mToastToShow;
    private Toast mToastToShow2;
    CountDownTimer toastCountDown;
    private UUID deviceUUID;
    ConnectThread mConnect;
    private static final String TAG = "BluetoothConnectionServ";
    ConnectedThread mService;

    ///commands
    private static final String LIGHT_COMMAND="light on";
    private static final String FAN_COMMAND="fan on";
    private static final String AC_COMMAND="AC on";
    private static final String LIGHT2_COMMAND="bulb on";


    AlertDialog dialog;

    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //BroadCast bluetooth ON/OFF status
    private final BroadcastReceiver mBroadCastReceiever1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action =intent.getAction();
            // logging the bluetooth process
            if(action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);
                switch (state){
                    case BluetoothAdapter.STATE_OFF:
                        okBT.setEnabled(true);
                        cancelBT.setEnabled(true);
                        break;

                    case BluetoothAdapter.STATE_ON:
                        okBT.setEnabled(true);
                        cancelBT.setEnabled(true);
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;


                    case BluetoothAdapter.STATE_TURNING_ON:
                        ToastMssg("Bluetooth is turning on");
                        okBT.setEnabled(false);
                        cancelBT.setEnabled(false);
                        break;
                }
            }

        }
    };

    private final BroadcastReceiver mBroadCastReceiever3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action =intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                ToastMssg2("Searching for bluetooth devices...", 12000, 1000);
                okBT.setEnabled(false);
                cancelBT.setEnabled(false);
            }
            // logging the bluetooth process
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.i("found", "device found!");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                String rssi = Integer.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE));
                if(deviceName == null || deviceName.equals("")){
                    deviceString =  deviceHardwareAddress + " RSSI: "+ rssi;
                }
                else{
                    deviceString = deviceName + "\n" + deviceHardwareAddress + " RSSI: " + rssi;
                }
                if(!mbluetoothDevices.contains(device)){
                    bluetoothDevices.add(deviceString);
                    mbluetoothDevices.add(device);
                }

                arrayAdapter.notifyDataSetChanged();

            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                ToastMssg("Search finished");
                toastCountDown.cancel();
                mToastToShow.cancel();
                okBT.setEnabled(true);
                cancelBT.setEnabled(true);
            }

        }
    };

    //destroy connection when broadcast gets closed
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mBroadCastReceiever1);
        unregisterReceiver(mBroadCastReceiever3);
    }

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }



        // Defines several constants used when transmitting messages between the
        // service and the UI.


        private class ConnectedThread extends Thread {
            private static final String TAG = "MY_APP_DEBUG_TAG";
            private Handler handler; // handler that gets info from Bluetooth service
            private final BluetoothSocket mmSocket;
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;
            private byte[] mmBuffer; // mmBuffer store for the stream

            public ConnectedThread(BluetoothSocket socket) {
                mmSocket = socket;
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams; using temp objects because
                // member streams are final.
                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }
                try {
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run() {
                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try {
                        // Read from the InputStream.
                        numBytes = mmInStream.read(mmBuffer);
                        // Send the obtained bytes to the UI activity.
                        //Message readMsg = handler.obtainMessage(
                                //MessageConstants.MESSAGE_READ, numBytes, -1,
                               // mmBuffer);
                        //readMsg.sendToTarget();
                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }

            // Call this from the main activity to send data to the remote device.
            public void write(byte[] bytes) {
                try {
                    mmOutStream.write(bytes);

                    // Share the sent message with the UI activity.
                    //Message writtenMsg = handler.obtainMessage(
                           // MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                   // writtenMsg.sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when sending data", e);

                    // Send a failure message back to the activity.
                    //Message writeErrorMsg =
                           // handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                    //Bundle bundle = new Bundle();
                   // bundle.putString("toast",
                           // "Couldn't send data to the other device");
                   // writeErrorMsg.setData(bundle);
                   // handler.sendMessage(writeErrorMsg);
                }
            }

            // Call this method from the main activity to shut down the connection.
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the connect socket", e);
                }
            }
        }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            okBT.setEnabled(false);
            cancelBT.setEnabled(false);
            BluetoothSocket tmp = null;
            mmDevice = device;
            deviceUUID = uuid;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                ToastMssg("Unable to connect, try again");
                okBT.setEnabled(true);
                cancelBT.setEnabled(true);
                mConnect=null;
                Log.e(TAG, "Socket's create() method failed", e);

            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                okBT.setEnabled(true);
                cancelBT.setEnabled(true);
                ToastMssg("Connection not successful, try again");
                mConnect=null;
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            okBT.setEnabled(true);
            cancelBT.setEnabled(true);
            manageMyConnectedSocket(mmSocket, mmDevice);
            //mProgessDialog.dismiss();
        }
        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    public void startService(BluetoothDevice device, UUID uuid){
        mConnect = new ConnectThread(device,uuid);
        mConnect.run();
        //mConnect.start();
    }

    public void manageMyConnectedSocket(BluetoothSocket mmSocket, BluetoothDevice mmDevice){
            connected_status =1;
            dialog.dismiss();
            connect.setText("CONNECTED");
            connect.setBackgroundResource(R.drawable.button_connected);
            mService = new ConnectedThread(mmSocket);
            mService.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        connect = findViewById(R.id.connection);
        mExit=findViewById(R.id.exit);
        lightON = findViewById(R.id.lightON);
        lightOFF = findViewById(R.id.lightOFF);
        fanOn = findViewById(R.id.fanON);
        fanOff = findViewById(R.id.fanOFf);
        ACon = findViewById(R.id.acON);
        ACoff = findViewById(R.id.acOFF);
        light2On = findViewById(R.id.light2ON);
        light2Off = findViewById(R.id.light2OFF);
        voiceControl = findViewById(R.id.voiceControl);

        //dialog for list connection
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectDialog(connected_status);
            }
        });
       //Light ON
        lightON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService != null){
                     mService.write(LIGHT_COMMAND.getBytes());
                    lightON.setBackgroundResource(R.drawable.button_on_state);
                    lightOFF.setBackgroundResource(R.drawable.button_normal_state);
                    lightON.setEnabled(false);
                    lightOFF.setEnabled(true);
                }
                else{
                    ToastMssg("Device not connected");
                }

            }
        });
        //Light OFF
        lightOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService != null){
                    mService.write("light off".getBytes());
                    lightOFF.setBackgroundResource(R.drawable.button_on_state);
                    lightON.setBackgroundResource(R.drawable.button_normal_state);
                    lightOFF.setEnabled(false);
                    lightON.setEnabled(true);
                }
                else{
                    ToastMssg("Device not connected");

                }
            }
        });

        //fan ON
        fanOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService != null){
                    mService.write(FAN_COMMAND.getBytes());
                    fanOn.setBackgroundResource(R.drawable.button_on_state);
                    fanOff.setBackgroundResource(R.drawable.button_normal_state);
                    fanOn.setEnabled(false);
                    fanOff.setEnabled(true);
                }
                else{
                    ToastMssg("Device not connected");
                }

            }
        });
        //fan OFF
        fanOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService != null){
                    mService.write("fan off".getBytes());
                    fanOff.setBackgroundResource(R.drawable.button_on_state);
                    fanOn.setBackgroundResource(R.drawable.button_normal_state);
                    fanOff.setEnabled(false);
                    fanOn.setEnabled(true);
                }
                else{
                    ToastMssg("Device not connected");

                }
            }
        });


        ACon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mService != null){
                    mService.write(AC_COMMAND.getBytes());
                    ACon.setBackgroundResource(R.drawable.button_on_state);
                    ACoff.setBackgroundResource(R.drawable.button_normal_state);
                    ACon.setEnabled(false);
                    ACoff.setEnabled(true);
                }
                else{
                    ToastMssg("Device not connected");
                }

            }
        });

        ACoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mService != null){
                    mService.write("AC off".getBytes());
                    ACoff.setBackgroundResource(R.drawable.button_on_state);
                    ACon.setBackgroundResource(R.drawable.button_normal_state);
                    ACoff.setEnabled(false);
                    ACon.setEnabled(true);
                }
                else{
                    ToastMssg("Device not connected");

                }
            }
        });

        //light2 ON
        light2On.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService != null){
                    mService.write(LIGHT2_COMMAND.getBytes());
                    light2On.setBackgroundResource(R.drawable.button_on_state);
                    light2Off.setBackgroundResource(R.drawable.button_normal_state);
                    light2On.setEnabled(false);
                    light2Off.setEnabled(true);
                }
                else{
                    ToastMssg("Device not connected");
                }

            }
        });
        //Light2 OFF
        light2Off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService != null){
                    mService.write("bulb off".getBytes());
                    light2Off.setBackgroundResource(R.drawable.button_on_state);
                    light2On.setBackgroundResource(R.drawable.button_normal_state);
                    light2Off.setEnabled(false);
                    light2On.setEnabled(true);
                }
                else{
                    ToastMssg("Device not connected");

                }
            }
        });



        //Voice Control
        voiceControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService != null)
                {
                    voiceActivation();
                }
                else{
                    ToastMssg("Device not connected");
                }

            }
        });

        mExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExitDialog();
            }
        });
    }

    public void voiceActivation(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());

        if(intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent, 10);
        }
        else {
            ToastMssg("Your device does not support speech Input");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case 10:
                if(resultCode == RESULT_OK && data != null){
                    ArrayList<String>command = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if(command.get(0).equals("light on")){
                        mService.write(LIGHT_COMMAND.getBytes());
                        lightON.setBackgroundResource(R.drawable.button_on_state);
                        lightOFF.setBackgroundResource(R.drawable.button_normal_state);
                        lightON.setEnabled(false);
                        lightOFF.setEnabled(true);
                    }

                    else if(command.get(0).equals("light off")){
                        mService.write("light off".getBytes());
                        lightOFF.setBackgroundResource(R.drawable.button_on_state);
                        lightON.setBackgroundResource(R.drawable.button_normal_state);
                        lightOFF.setEnabled(false);
                        lightON.setEnabled(true);
                    }

                    else if (command.get(0).equals("fan on")){
                        mService.write(FAN_COMMAND.getBytes());
                        fanOn.setBackgroundResource(R.drawable.button_on_state);
                        fanOff.setBackgroundResource(R.drawable.button_normal_state);
                        fanOn.setEnabled(false);
                        fanOff.setEnabled(true);
                    }
                    else if(command.get(0).equals("fan off")){
                        mService.write("fan off".getBytes());
                        fanOff.setBackgroundResource(R.drawable.button_on_state);
                        fanOn.setBackgroundResource(R.drawable.button_normal_state);
                        fanOff.setEnabled(false);
                        fanOn.setEnabled(true);
                    }

                    else if(command.get(0).equals("AC on")){
                        mService.write(AC_COMMAND.getBytes());
                        ACon.setBackgroundResource(R.drawable.button_on_state);
                        ACoff.setBackgroundResource(R.drawable.button_normal_state);
                        ACon.setEnabled(false);
                        ACoff.setEnabled(true);
                    }
                    else if(command.get(0).equals("AC off")){
                        mService.write("AC off".getBytes());
                        ACoff.setBackgroundResource(R.drawable.button_on_state);
                        ACon.setBackgroundResource(R.drawable.button_normal_state);
                        ACoff.setEnabled(false);
                        ACon.setEnabled(true);
                    }

                    else if(command.get(0).equals("light2 on")){
                        mService.write(LIGHT2_COMMAND.getBytes());
                        light2On.setBackgroundResource(R.drawable.button_on_state);
                        light2Off.setBackgroundResource(R.drawable.button_normal_state);
                        light2On.setEnabled(false);
                        light2Off.setEnabled(true);
                    }
                    else if(command.get(0).equals("light2 off")){
                        mService.write("light2 off".getBytes());
                        light2Off.setBackgroundResource(R.drawable.button_on_state);
                        light2On.setBackgroundResource(R.drawable.button_normal_state);
                        light2Off.setEnabled(false);
                        light2On.setEnabled(true);
                    }



                    else{
                        ToastMssg("Cannot recognize voice command");
                        //ToastMssg(command.get(0));
                    }
                }


                break;
        }
    }

    public Toast ToastMssg2(String msg, int toastDurationInMilliSeconds, int Interval){
        mToastToShow = Toast.makeText(this, msg, Toast.LENGTH_LONG);

        // Set the countdown to display the toast
        toastCountDown = new CountDownTimer(toastDurationInMilliSeconds, Interval /*Tick duration*/) {
            public void onTick(long millisUntilFinished) {
                mToastToShow.show();
            }
            public void onFinish() {
                mToastToShow.cancel();
            }
        };

        // Show the toast and starts the countdown
        mToastToShow.show();
        toastCountDown.start();
        return mToastToShow;
    }

    public  Toast ToastMssg(String msg){
        mToastToShow2 = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        mToastToShow2.show();
        return  mToastToShow2;
    }

    /*
    private void
    checkBTpermission(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissionCheck != 0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }

        }

        else{
            Log.i("no need", "no need");
        }
    }*/


    public void search(){
        //first check if bluetooth is enabled on device
        //if bluetooth is not available on your device
        if(bluetoothAdapter == null){
            ToastMssg("Bluetooth is not available on your device");
        }
        //if bluetooth is switched off turn on
        else if(!bluetoothAdapter.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadCastReceiever1, BTIntent);
        }
        else if(bluetoothAdapter.isEnabled()){
            // showToast("Bluetooth is turning off...");

            bluetoothDevices.clear();
            arrayAdapter.notifyDataSetChanged();
            mbluetoothDevices.clear();
            //checkBTpermission();
            bluetoothAdapter.startDiscovery();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mBroadCastReceiever3, intentFilter);

        }


    }





    public void connectDialog(int connected_status){
        if(connected_status == 0){

            Typeface face = ResourcesCompat.getFont(this, R.font.titillium_web);
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            TextView title = new TextView(this);
            title.setText("Select HC-06 Bluetooth device");
            title.setTypeface(face);
            title.setPadding(5, 5, 5, 5);   // Set Position
            title.setGravity(Gravity.CENTER);
            title.setTextSize(16);
            title.setAllCaps(true);
            title.setBackgroundColor(Color.parseColor("#292826"));
            title.setTextColor(Color.parseColor("#ffffff"));
            alertDialog.setCustomTitle(title);

            listview = new ListView(this);
            bluetoothDevices = new ArrayList<String>();
            mbluetoothDevices = new ArrayList<BluetoothDevice>();
            arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bluetoothDevices);
            //Initialize bluetooth adapter
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            listview.setAdapter(arrayAdapter);
            bluetoothDevices.clear();
            arrayAdapter.notifyDataSetChanged();
            mbluetoothDevices.clear();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    deviceString = deviceName + "\n" + deviceHardwareAddress;

                    if (!mbluetoothDevices.contains(device)) {
                        bluetoothDevices.add(deviceString);
                        mbluetoothDevices.add(device);

                    }
                    arrayAdapter.notifyDataSetChanged();

                }
            }


            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //bluetoothAdapter.cancelDiscovery();
                    parent.getChildAt(position).setBackgroundColor(Color.parseColor("#f9d342"));
                    if(mConnect == null){
                        okBT.setEnabled(false);
                        okBT.setEnabled(false);
                        startService(mbluetoothDevices.get(position), MY_UUID_SECURE );
                    }
                    else{
                        ToastMssg("A connection exists!");
                    }
                    //String selectedItem = (String) parent.getItemAtPosition(position);
                    //Log.i("device details", selectedItem);
                    // Log.i("bluetoothdevice", mbluetoothDevices.get(position).toString());



                }
            });
            alertDialog.setView(listview);

            // you can more buttons


            // you can more buttons
            alertDialog.setNeutralButton("SEARCH", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    search();
                }
            });
            alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });



            // you can more buttons
            // new Dialog(getApplicationContext());
            // AlertDialog dialog = alertDialog.create();
            dialog = alertDialog.create();
            dialog.show();

            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);

            // Set Properties for OK Button
            okBT = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            //LinearLayout.LayoutParams neutralBtnLP  = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams neutralBtnLP = (LinearLayout.LayoutParams) okBT.getLayoutParams();
            neutralBtnLP.gravity = Gravity.FILL_HORIZONTAL;
            okBT.setPadding(50, 10, 10, 10);   // Set Position
            okBT.setTextColor(Color.BLUE);
            okBT.setLayoutParams(neutralBtnLP);
            okBT.setTypeface(face);
            okBT.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    search();
                }
            });


            cancelBT = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            //LinearLayout.LayoutParams negBtnLP  = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams negBtnLP = (LinearLayout.LayoutParams) okBT.getLayoutParams();
            negBtnLP.gravity = Gravity.FILL_HORIZONTAL;
            cancelBT.setTextColor(Color.RED);
            cancelBT.setLayoutParams(negBtnLP);
            cancelBT.setTypeface(face);
        }

        else{
            ToastMssg("device already connected");
        }


    }


    public void showExitDialog(){
        Typeface face = ResourcesCompat.getFont(this, R.font.titillium_web);
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        TextView title = new TextView(this);
        title.setText("Deep Home Automation");
        title.setTypeface(face);
        title.setPadding(10, 10, 10, 10);   // Set Position
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);
        title.setTextSize(18);
        title.setAllCaps(true);
        title.setBackgroundColor(Color.parseColor("#111111"));
        title.setTextColor(Color.parseColor("#ffffff"));
        alertDialog.setCustomTitle(title);

        // Set Message
        TextView msg = new TextView(this);
        // Message Properties
        msg.setText("Do you want to exit \n the application?");
        // if (Build.VERSION.SDK_INT < 23) {
        //msg.setTextAppearance(this, R.style.TextAppearance_AppCompat_Medium);
        //} else {
        //msg.setTextAppearance(resId);
        // }

        msg.setTypeface(face);
        msg.setGravity(Gravity.CENTER_HORIZONTAL);
        msg.setTextColor(Color.BLACK);
        msg.setTextSize(18);
        alertDialog.setView(msg);

        // Set Button
        // you can more buttons
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,"exit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Perform Action on Button
                // finish();
                if(bluetoothAdapter != null){
                    bluetoothAdapter.disable();
                    System.exit(0);
                }
                else{
                    System.exit(0);
                }


            }
        });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,"CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Perform Action on Button
                // Intent intent = new Intent(Main2Activity.this, Main2Activity.class);
                ///startActivity(intent);
            }
        });

        new Dialog(getApplicationContext());
        alertDialog.show();

        // Set Properties for OK Button
        final Button okBT = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        LinearLayout.LayoutParams neutralBtnLP = (LinearLayout.LayoutParams) okBT.getLayoutParams();
        neutralBtnLP.gravity = Gravity.FILL_HORIZONTAL;
        okBT.setPadding(50, 10, 10, 10);   // Set Position
        okBT.setTextColor(Color.BLUE);
        okBT.setLayoutParams(neutralBtnLP);
        okBT.setTypeface(face);


        final Button cancelBT = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        LinearLayout.LayoutParams negBtnLP = (LinearLayout.LayoutParams) okBT.getLayoutParams();
        negBtnLP.gravity = Gravity.FILL_HORIZONTAL;
        cancelBT.setTextColor(Color.RED);
        cancelBT.setLayoutParams(negBtnLP);
        cancelBT.setTypeface(face);
    }



    //override on backpress method
    @Override
    public void onBackPressed() {
        // your stuff here
        showExitDialog();
        //super.onBackPressed();
    }
}
