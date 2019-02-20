package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    private final static int SERVER_PORT = 10000;
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final String [] ports = new String[] {"11108","11112","11116","11120","11124"};
    private  static  final Uri  CONTENT_URI = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger1.provider");
    Socket [] sockets = new Socket[5];
    int sequenceNumber =0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException io)
        {
            io.printStackTrace();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }



        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
       final Button send_button = (Button) findViewById(R.id.button4);

       send_button.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               TextView editText = (TextView) findViewById(R.id.editText1);
               String message = editText.getText().toString().trim();
               editText.setText("");
               TextView displayText = (TextView) findViewById(R.id.textView1);
               //displayText.append(message);
               displayText.append("\n");
               new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);

           }
       });

    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            StringBuilder currentMessage=new StringBuilder();
            try   {
                while (true) {    // Infinite loop to keep this code running all the time
                    ContentValues content = new ContentValues();
                    /* Reading the data from the socket can be done by creating object of any byte/character stream. socket.getInputStream() returns an object of inputstream so it has to be wrapped in InputStreamReader in order to use character stream  */
                    Socket socket =serverSocket.accept();
                    DataInputStream reader = new DataInputStream((socket.getInputStream()));
                    currentMessage.append(reader.readUTF());
                    publishProgress(currentMessage.toString());
                    content.put("key",String.valueOf(sequenceNumber++));
                    content.put("value",currentMessage.toString());
                   // sequenceNumber++;
                    getContentResolver().insert(CONTENT_URI, content );
                    currentMessage.setLength(0);
                }

            } catch (IOException e) {
                Log.e(TAG, "ServerTask socket IOException "+e.getMessage());
            }
            catch (Exception ex1){
                ex1.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            remoteTextView.append("\n");
            return;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Log.i("doBack-client" ,"Started");
                DataOutputStream toSend;
                Socket sockets = null;
                int i=0;
                while(i < ports.length) {

                    sockets = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(ports[i]));
                    String msgToSend = msgs[0];
                    toSend = new DataOutputStream(sockets.getOutputStream()); // Creating an object of DataOutputStream
                    toSend.writeUTF(msgToSend);     //writeChars(msgToSend); // // .writeUTF(msgToSend);
                    Log.i("Scokets- \t", " Writing to socket with port number--" +ports[i]);
                    i++;
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (Exception e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
