package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    //New add:
    static final int SERVER_PORT = 10000;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final String[] REMOTE_PORTS={REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
    private  ContentResolver mContentResolver;//=getContentResolver();
    private final Uri mUri=buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");;
    int keyid=0;
    int localcounter=0;
    Socket[] portlist= new Socket[5];
    PriorityQueue<Messageobj> pq= new PriorityQueue<Messageobj>(25, new Comparator<Messageobj>(){

        @Override
        public int compare(Messageobj lhs, Messageobj rhs) {
            if(lhs.counter>rhs.counter)return 1;
            if(lhs.counter<rhs.counter)return -1;
            if(lhs.counter==rhs.counter){
                if(lhs.port>rhs.port){
                    return 1;
                }else if(lhs.port<rhs.port){
                    return -1;
                }else{

                    return 0;
                }
            }


            return 0;
        }
    });


    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mContentResolver=getContentResolver();


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_messenger);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        //add

        Button bt4=(Button) findViewById(R.id.button4);
        bt4.setOnClickListener(new View.OnClickListener()
                               {


                                   @Override
                                   public void onClick(View v) {

                                       TextView tv1 = (TextView) findViewById(R.id.textView1);
                                       EditText et=(EditText) findViewById(R.id.editText1);
                                       //tv1.setText(et.getText()+"      sent\n");

                                       String msg= String.valueOf(et.getText());
                                       //tv1.append(" Sent: "+msg + "\t\n");

                                       //TextView localTextView = (TextView) findViewById(R.id.textView1);
                                       //localTextView.append("\n");
                                       new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                                       et.setText("");

                                   }
                               }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            //int keyid=0;
            try {

                while(true) {
                    try {
                        Socket socket = serverSocket.accept();
                        //socket.setSoTimeout(5000);
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                        Messageobj receive_init = (Messageobj) in.readObject();
                        Log.e(TAG,"SERVER RECEIVE REQUIRE MSG: "+receive_init.message);

                        receive_init.counter = localcounter;
                        receive_init.action = "Suggest_pri";
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(receive_init);
                        Log.e(TAG,"SERVER SEND SUGGEST PRI: "+receive_init.message+"/"+receive_init.port);
                        pq.add(receive_init);
                        localcounter++;

                        ObjectInputStream incomingobj = new ObjectInputStream(socket.getInputStream());
                        Messageobj receive_agree = (Messageobj) incomingobj.readObject();
                        Log.e(TAG,"SERVER RECEIVE AGREE MSG: "+receive_agree.message+"/"+receive_agree.port+"/PRI "+receive_agree.counter);
                        Iterator<Messageobj> it = pq.iterator();
                        while (it.hasNext()) {
                            Messageobj curr = it.next();
                            //Log.e(TAG,"SERVER PRINT PQ: "+curr.message+" "+curr.counter+" "+curr.action);
                            if (curr.message.equals(receive_agree.message)) {
                                pq.remove(curr);
                                pq.add(receive_agree);
                            }


                        }
                        Messageobj finalmsg = pq.peek();
                        if (finalmsg.action.equals("Agree_pri")) {
                            Messageobj q = pq.poll();
                            Log.e(TAG, "SERVER PUBLISH MSG: " + q);

                            publishProgress(q.message);

                        }else{

                            while(finalmsg.action.equals("Suggest_pri")){
                                Messageobj k=pq.poll();
                                finalmsg=pq.peek();
                                Log.e(TAG,"WHILE LOOP MSG:"+finalmsg.message+"-"+finalmsg.action);
                            }

                            Messageobj q=pq.poll();
                            Log.e(TAG, "SERVER PUBLISH MSG: " + q);
                            //new add


                            publishProgress(q.message);

                        }

                        socket.close();


                    }catch (Exception e){
                        e.printStackTrace();
                        continue;
                    }

                }
            }catch(Exception e){}

            return null;
        }

        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");
            ContentValues mycontentvalue = new ContentValues();

            mycontentvalue.put("key", String.valueOf(keyid));
            mycontentvalue.put("value", strReceived);

            keyid=keyid+1;
            //
            getContentResolver().insert(mUri,mycontentvalue);


            String filename = "GroupMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }


    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String msgToSend = msgs[0];
            Log.e(TAG,"CLIENT RAW MESSAGE: "+msgToSend);
            PriorityQueue<Messageobj>suggestlist=new PriorityQueue<Messageobj>(5, new Comparator<Messageobj>() {
                @Override
                public int compare(Messageobj lhs, Messageobj rhs) {
                    if(lhs.counter+lhs.port>rhs.counter+rhs.port)return 1;
                    if(lhs.counter+lhs.port<rhs.counter+rhs.port)return -1;
                    if(lhs.counter+lhs.port==rhs.counter+rhs.port)return 0;
                    return 0;
                }
            });
            for(int i=0;i<REMOTE_PORTS.length;i++) {
                try {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORTS[i]));
                    portlist[i]=socket;
                    Messageobj msgobj=new Messageobj(msgToSend,localcounter,Integer.parseInt(REMOTE_PORTS[i]),"Require_pri");


                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    Log.e(TAG,"CLIENT Send initial messgae: "+msgobj.message+"/"+msgobj.port+"/"+msgobj.action);

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(msgobj);
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Messageobj in_msg_obj = (Messageobj) in.readObject();
                    Log.e(TAG,"CLIENT GET SUGGEST PRI: "+in_msg_obj.message+"/"+in_msg_obj.port+"/"+in_msg_obj.action+" PRI:"+in_msg_obj.counter);
                    //respondlist[i]=in_msg_obj;
                    suggestlist.add(in_msg_obj);

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket IOException");
                }catch (Exception e){
                    e.printStackTrace();
                    continue;
                }
            }



            Messageobj agreemsg=suggestlist.peek();
            agreemsg.action="Agree_pri";
            for(int i=0;i<REMOTE_PORTS.length;i++){

                try{
                    Log.e(TAG,"CLIENT BROADCAST AGREE MSG: "+agreemsg.message+"/"+agreemsg.port+"/PRI: "+agreemsg.counter);
                    ObjectOutputStream out = new ObjectOutputStream(portlist[i].getOutputStream());
                    out.writeObject(agreemsg);
                    portlist[i].close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

}
