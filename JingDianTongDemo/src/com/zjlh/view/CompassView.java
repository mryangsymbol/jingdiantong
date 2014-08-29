package com.zjlh.view;

import android.content.Context;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.zjlh.jingdiantongdemo.R;

public class CompassView extends ImageView {
	private Matrix matrix = new Matrix();
	private SensorManager manager;
	
	public CompassView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public CompassView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CompassView(Context context) {
		super(context);
		init();
	}

	private void init() {
		setScaleType(ImageView.ScaleType.MATRIX);
		setImageResource(R.drawable.compass);
		
		manager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
	}
	
	public void enable() {
		Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		manager.registerListener(sensorListener, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public void disable() {
		manager.unregisterListener(sensorListener);
	}
	
	public void setDegree(float degrees) {
		matrix.setRotate(degrees, getWidth()/2, getHeight()/2);
		setImageMatrix(matrix);
	}
	
	private SensorEventListener sensorListener = new SensorEventListener() {
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		public void onSensorChanged(SensorEvent event) {
			float degrees = event.values[0];
			setDegree(-degrees);
		}
	};
}
