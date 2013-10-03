package com.example.myhealthmeasurementdevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.YuvImage;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final static int REQUEST_ENABLE_BT = 1;

	private ConnectThread connectThread;

	BluetoothAdapter btAdapter;
	AcceptThread accepter;
	
	private String current_status;
	private String current_measurement;
	
	private boolean connected = false;
	private boolean stop = false;
	
	private static final String status_inactive = "Inactive";
	private static final String status_active = "Active";
	
	private static final String measurement_pulse = "pulse";
	private static final String measurement_bloodpressure = "bloodpressure";
	private static final String measurement_ecg = "ecg";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		current_status = status_inactive;
		current_measurement = measurement_pulse;

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
		updateTextViews();
	}
	
	public void onClick(View v) {
		if(connected) {
		    final int id = v.getId();
		    switch (id) {
		    case R.id.bPulse:
		    	current_measurement = measurement_pulse;
		        break;
		    case R.id.bBloodpressure:
		    	current_measurement = measurement_bloodpressure;
		        break;
		    case R.id.bECG:
		    	current_measurement = measurement_ecg;
		        break;
		    case R.id.bStartStop:
		    	if(!stop) {
			    	Button startstop = (Button) findViewById(R.id.bStartStop);
			    	if(current_status == status_active) {
			    		current_status = status_inactive;
			    		stop = true;
			    		startstop.setText("Start");
			    	} else {
			    		current_status = status_active;
			    		startstop.setText("Stop");
			    	}
		    	}
		    	else {
		    		Toast.makeText(this, "Waiting for stop to start again...", Toast.LENGTH_SHORT).show();
		    	}
		        break;    
		    }
		    
		    updateTextViews();
		} else {
			Toast.makeText(this, "Waiting for connection", Toast.LENGTH_SHORT).show();
		}
	}

	public void updateTextViews() {
		TextView measurement = (TextView) findViewById(R.id.tMeasurement);
		measurement.setText(current_measurement);
		
		TextView status = (TextView) findViewById(R.id.tStatus);
		status.setText(current_status);
		
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
					connected = true;
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

			// Keep listening to the InputStream until an exception occurs
			while (true) {

				int available = 0;

				try {
					available = mmInStream.available();
					Thread.sleep(1000);
					
					if(stop) {
						Log.e("simulator", "Sending STOP message");
						String valueToSend =  current_measurement + ";stop";
						byte[] sendPulseMeasurement = valueToSend.getBytes();
						write(sendPulseMeasurement);
						stop = false;
					}
					
					String string = getStringToWrite();
					byte[] test = string.getBytes();
					write(test);
					Log.e("simulator", "Write gestuurd");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

				if (available > 0) {

					try {		
						byte[] buffer;
						
						buffer = new byte[available];

						mmInStream.read(buffer);

						String message = new String(buffer);
						
						/**
						if(message.equals("pulsewaves")) {
							String valueToSend =  "" + pulsemeasurement();
							byte[] sendPulseMeasurement = valueToSend.getBytes();
							write(sendPulseMeasurement);
						}

						Log.e("simulator", "Message is: " + message);**/

					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
			}

		}
		
		private int pulsemeasurement() {
			
			int minimum = 60;
			int maximum = 100;
			
			int value = 0;
			
			value = minimum + (int)(Math.random() * ((maximum - minimum) + 1));
			
			return value;
			
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private String getStringToWrite() {
			String returnStatus = "";
			String returnValue = "No data";
			if(current_status == status_inactive) {
				returnStatus = status_inactive;
			} else {
				returnStatus = current_measurement;
				
				if(current_measurement == measurement_pulse) {
					returnValue = getPulseMeasurement();
				} else if (current_measurement == measurement_bloodpressure) {
					returnValue = getBloodpressureMeasurement();
				} else if (current_measurement == measurement_ecg) {
					returnValue = getECGMeasurement();
				}
			}
			
			return returnStatus + ";" + returnValue;
		}
	}
	
	public String getPulseMeasurement() {
		Random r = new Random();
		Integer pulsemeasurement = (r.nextInt(40) + 60);
		return pulsemeasurement.toString();
	}
	
	public String getBloodpressureMeasurement() {
		Random r = new Random();
		Integer hypotension = (r.nextInt(20) + 70);
		Integer hypertension = (r.nextInt(20) + 110);
		
		return hypotension + ";" + hypertension;
	}
	
	public String getECGMeasurement() {
		Random r = new Random();
		Integer printerval = (r.nextInt(25) + 50);
		Integer prsegment = (r.nextInt(10) + 20);
		Integer qrscomplex = (r.nextInt(10) + 25);
		Integer stsegment = (r.nextInt(10) + 30);
		Integer qtinterval = (r.nextInt(25) + 75);
		Integer qtrough = (r.nextInt(4) - 5);
		Integer rpeak = (r.nextInt(10) + 40);
		Integer strough	= (r.nextInt(5) - 7); 
		Integer tpeak = (r.nextInt(4) + 4);
		Integer ppeak = (r.nextInt(4) + 2);
		return printerval + ";" + prsegment + ";" + qrscomplex + ";" + stsegment + ";" + qtinterval + ";" + qtrough + ";"
				 + rpeak + ";" + strough + ";" + tpeak + ";" + ppeak;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
