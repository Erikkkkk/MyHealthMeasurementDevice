package com.example.myhealthmeasurementdevice;

import java.util.Random;

public class Measurement {

	public Measurement() {
		
	}
	
	public String getPulseMeasurement() {
		Random r = new Random();
		int pulsemeasurement = (r.nextInt(40) + 60);
		return "" + pulsemeasurement;
	}
	
	public String getBloodpressureMeasurement() {
		Random r = new Random();
		int pulsemeasurement = (r.nextInt(40) + 60);
		return "" + pulsemeasurement;
	}
	
	public String getECGMeasurement() {
		Random r = new Random();
		int pulsemeasurement = (r.nextInt(40) + 60);
		return "" + pulsemeasurement;
	}
}
