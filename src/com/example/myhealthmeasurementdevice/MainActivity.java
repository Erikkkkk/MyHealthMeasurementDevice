package com.example.myhealthmeasurementdevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final static int REQUEST_ENABLE_BT = 1;

	private ConnectThread connectThread;

	BluetoothAdapter btAdapter;
	AcceptThread accepter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(
					MainActivity.this);
			builder.setMessage("Your device doesn't support bluetooth.")
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							});
			builder.setCancelable(false);
			builder.create();
			builder.show();
		}

		if (!btAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		Intent setDiscoverableIntent = new Intent(
				btAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(setDiscoverableIntent, RESULT_OK);

		listenForConnectionOfMyHealth();
	}

	private void listenForConnectionOfMyHealth() {
		accepter = new AcceptThread();
		accepter.start();
	}

	private void manageConnectedSocket(BluetoothSocket socket) {
		connectThread = new ConnectThread(socket);
		connectThread.start();
	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;
		private static final String NAME = "MeasurementdeviceJcheed";
		private final UUID MY_UUID = UUID
				.fromString("889a38c0-251b-11e3-8224-0800200c9a66");

		public AcceptThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client
				// code
				tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME,
						MY_UUID);
			} catch (IOException e) {
			}
			mmServerSocket = tmp;
		}

		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				try {
					socket = mmServerSocket.accept();
					Log.e("simulator", "Connection accepted!");
				} catch (IOException e) {

				}

				// If a connection was accepted
				if (socket != null) {
					// Do work to manage the connection (in a separate thread)
					manageConnectedSocket(socket);
					try {
						mmServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {
			}
		}
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			int bytes; // bytes returned from read()

			Log.e("simulator", "Write gestuurd");
			String string = "Hallo lees dit!!!";
			byte[] test = string.getBytes();
			write(test);

			// Keep listening to the InputStream until an exception occurs
			while (true) {

				int available = 0;

				try {
					available = mmInStream.available();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				if (available > 0) {

					try {
						
						byte[] buffer;
						
						buffer = new byte[available];

						mmInStream.read(buffer);

						String message = new String(buffer);

						Log.e("simulator", "Message is: " + message);

					} catch (IOException e) {
						break;
					}
				}
			}

		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) {
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
