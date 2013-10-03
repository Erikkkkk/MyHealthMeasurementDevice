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
	private static final String MEASUREMENT_PULSE = "pulse";
	private static final String MEASUREMENT_BLOODPRESSURE = "bloodpressure";
	private static final String MEASUREMENT_ECG = "ecg";
	
	private BluetoothAdapter btAdapter;
	private AcceptThread accepter;
	private ConnectThread connectThread;
	
	private String current_measurement;
	private boolean connected = false;
	private boolean send = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		current_measurement = MEASUREMENT_PULSE;

		// Make sure device has bluetooth
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

		// Enable bluetooth
		if (!btAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		// Make sure device is findable
		Intent setDiscoverableIntent = new Intent(
				btAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(setDiscoverableIntent, RESULT_OK);

		//Start listening
		listenForConnectionOfMyHealth();
		setMainContentView();
		updateViews();
	}
	
	/**
	 * Handles the button clicks 
	 * @param v
	 */
	public void onClick(View v) {
		if(connected) {
		    final int id = v.getId();
		    switch (id) {
		    case R.id.bPulse:
		    	current_measurement = MEASUREMENT_PULSE;
		        break;
		    case R.id.bBloodpressure:
		    	current_measurement = MEASUREMENT_BLOODPRESSURE;
		        break;
		    case R.id.bECG:
		    	current_measurement = MEASUREMENT_ECG;
		        break;
		    case R.id.bStartStop:
		    	if(!send) {
		    		send = true;
		    	} else {
		    		Toast.makeText(this, "Still sending previous measurement, pleast wait a moment.", Toast.LENGTH_SHORT).show();
		    	}
		        break;    
		    }
		    
		    updateViews();
		} else {
			Toast.makeText(this, "Waiting for MyHealthApp to connect", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Update the views that display the status and measurement type that is selected
	 */
	public void updateViews() {
		if(connected) {
			TextView measurement = (TextView) findViewById(R.id.tMeasurement);
			measurement.setText(current_measurement);			
		}
	}
	
	public void setMainContentView() {
		
		MainActivity.this.runOnUiThread(new Runnable() {

	        public void run() {
	        	if(connected) {
	    			Log.e("simulator", "Verander content view");
	    			setContentView(R.layout.activity_main);
	    			Log.e("simulator", "Veranderd");
	    		} else {
	    			setContentView(R.layout.progressbar_with_message);
	    		}
	        }
	    });
	}
	
	// Starts the AcceptThread
	private void listenForConnectionOfMyHealth() {
		accepter = new AcceptThread();
		accepter.start();
	}

	// Start the ConnectedThread and passes the socket
	private void manageConnectedSocket(BluetoothSocket socket) {
		connected = true;
		connectThread = new ConnectThread(socket);
		connectThread.start();
		setMainContentView();
	}

	/**
	 * 
	 * Waits for connection. When accepted passes the new socket to manageConnectedSocket() and
	 * closes the bluetoothAdapter.
	 *
	 */
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

	/**
	 * 
	 * Manages the connection to MyHealthApp. Writes data depending on current_status and current_measurement.
	 * 
	 *
	 */
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
					
					if(send) {						
						String measurementString = getStringToWrite();
						byte[] measurement = measurementString.getBytes();
						write(measurement);
						Log.e("simulator", "Measurement " + current_measurement + " send");
						showToastMessagesend();
						
						send = false;
					}
					
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

					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
			}

		}
		
		/* writes to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/* Shuts down the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	// Return appropriate string that represents a measurement in CSV format.
	private String getStringToWrite() {
		String type = current_measurement;
		String data = "No data";

		if (current_measurement == MEASUREMENT_PULSE) {
			data = getPulseMeasurement();
		} else if (current_measurement == MEASUREMENT_BLOODPRESSURE) {
			data = getBloodpressureMeasurement();
		} else if (current_measurement == MEASUREMENT_ECG) {
			data = getECGMeasurement();
		}

		return type + ";" + data;
	}
	
	/**
	 * Returns a string that represents a pulse measurement
	 * @return
	 */
	public String getPulseMeasurement() {
		Random r = new Random();
		Integer pulsemeasurement = (r.nextInt(40) + 60);
		return pulsemeasurement.toString();
	}
	
	/**
	 * Returns a string that represents a blood pressure measurement
	 * Sends values in CSV format
	 * @return
	 */
	public String getBloodpressureMeasurement() {
		Random r = new Random();
		Integer hypotension = (r.nextInt(20) + 70);
		Integer hypertension = (r.nextInt(20) + 110);
		
		return hypotension + ";" + hypertension;
	}
	
	/**
	 * Returns a string that represents a ECG measurement unit
	 * Sends values in CSV format
	 * @return 
	 */
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
	
	public void showToastMessagesend() {
		
		MainActivity.this.runOnUiThread(new Runnable() {

	        public void run() {
	        	Toast.makeText(MainActivity.this, "Measurement send", Toast.LENGTH_SHORT).show();
	        }
	    });
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
