package uk.co.speedfox.appliancefinder;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    public static final String GET_APPLIANCE_NAME_CMD = "getApplianceName";

    private class ApplianceListener implements Runnable{

        private volatile boolean running = true;

        @Override
        public void run() {
            try {
                int port = 2121;

                DatagramSocket dsocket = new DatagramSocket(port);
                byte[] buffer = new byte[2048];
                final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (running) {
                    dsocket.receive(packet);
                    final String data = new String(buffer, 0, packet.getLength());
                    if(!GET_APPLIANCE_NAME_CMD.equals(data)){
                        logger.log(Level.SEVERE,"UDP packet received: " + data + " from " + packet.getAddress().getHostAddress());
                        packet.setLength(buffer.length);
                        runOnUiThread(new Runnable() {
                            final String address = packet.getAddress().getHostAddress();
                            @Override
                            public void run() {
                                listAdapter.addEntry(address, data);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }

        }

        public void stop(){
            running = false;
        }
    }

    private class SendPacketOperation implements Runnable {

        private boolean repeat = true;

        @Override
        public void run(){
            AsyncTask task = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    sendPacket();
                    if(repeat){
                        broadcastHandler.postDelayed(SendPacketOperation.this, 5000);
                    }
                    return null;
                }
            };
            task.execute();
        }


        public void sendPacket() {
            logger.log(Level.SEVERE, "Sending packet");
            try {
                DatagramSocket clientSocket = new DatagramSocket();

                clientSocket.setBroadcast(true);
                WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                DhcpInfo dhcp = wifi.getDhcpInfo();
                // handle null somehow

                int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
                byte[] quads = new byte[4];
                for (int k = 0; k < 4; k++)
                    quads[k] = (byte) (broadcast >> (k * 8));
                InetAddress address = InetAddress.getByAddress(quads);
                logger.log(Level.SEVERE, "To address " + address.getHostName());

                byte[] sendData;

                sendData = GET_APPLIANCE_NAME_CMD.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData,
                        sendData.length, address, 2121);
                clientSocket.send(sendPacket);
                clientSocket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public void stop(){
            repeat = false;
        }
    }

    Logger logger;
    ApplianceListener listener;
    Thread listenerThread;
    Handler broadcastHandler;
    SendPacketOperation broadcaster;
    ListView listView;
    ApplianceListAdapter listAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger = Logger.getLogger("appliance finder");

        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.applianceList);
        listAdapter = new ApplianceListAdapter();
        listView.setAdapter(listAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String ipAddress = listAdapter.getIpAddress(i);
                Intent intent = new Intent(MainActivity.this, ApplianceActivity.class);
                Bundle b = new Bundle();
                b.putString("ip_address", ipAddress);
                intent.putExtras(b);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        listener.stop();
        broadcaster.stop();
        try {
            listenerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        listAdapter.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listener = new ApplianceListener();
        listenerThread = new Thread(listener);
        listenerThread.start();
        broadcastHandler = new Handler();
        broadcaster = new SendPacketOperation();
        broadcastHandler.post(broadcaster);
    }



    public void sendPacket(View v){

    }

}
