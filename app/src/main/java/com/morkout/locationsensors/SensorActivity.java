package com.morkout.locationsensors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.os.AsyncTask;

public class SensorActivity extends Activity implements SensorEventListener {
	private static final String TAG = "SensorActivity";
	private static final String IP = "10.11.130.152";
	private boolean flag = false;

	private SensorManager mSensorManager;

	private Sensor mSensorAccelerometer;
	private Sensor mSensorGravity;
	private Sensor mSensorGyroscope;
	private Sensor mSensorLight;
	private Sensor mSensorLinearAcceleration;
	private Sensor mSensorMagneticField;
	private Sensor mSensorRotationVector;

	private static final int TYPE_ORIENTATION = 999;
	private int mSensorSelected;
	private Boolean mLogOn;

	private long lastUpdate;
	private int count;

	private Date mSensorDataUpdatedTime[];

	private TextView mTextView;    
	private GestureDetector mGestureDetector;

	float[] mAccelerometer;
	float[] mGravity;
	float[] mGyroscope;
	float[] mLight;
	float[] mLinearAcceleration;
	float[] mMagneticField;
	float[] mOrientation;
	float[] mRotationVector;

	private final float[] mRotationMatrix = new float[16];
	private GeomagneticField mGeomagneticField;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensor);
		mTextView =  (TextView) findViewById(R.id.tvSensor);

		lastUpdate = System.currentTimeMillis();
		count = 1;
		mLogOn = true;
		mSensorSelected = -1;

		//getWindow().addFlags(WindowUtils.FLAG_DISABLE_HEAD_GESTURES);		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);		
		
		if (LocationActivity.getCurrentLocation() != null) {
			mGeomagneticField = new GeomagneticField((float) LocationActivity.getCurrentLocation().getLatitude(),
					(float) LocationActivity.getCurrentLocation().getLongitude(), (float) LocationActivity.getCurrentLocation().getAltitude(),
					LocationActivity.getCurrentLocation().getTime());		
		}
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

		String allString = "";
		for (Sensor s : deviceSensors) {
			allString += s.getName() +":"+s.getType()+",";
		}     

		mTextView.setText("Playing with Sensors");
		//mTextView.setText(allString);
		Log.i(TAG, allString);


		mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);		
		mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mSensorLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		mSensorMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


		mGestureDetector = new GestureDetector(this);

		mGestureDetector.setBaseListener(new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TAP) {
					openOptionsMenu();

					return true;
				} 
				return false;
			}
		});		

	}

	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}        


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.sensors, menu);

		return true;
	} 


	String display(float[] values) {
		return "\n" + values[0] + "\n" + values[1] + "\n" + values[2];
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.logon:			
			if (item.getTitle().equals("TURN LOG OFF")) {
				item.setTitle("TURN LOG ON");
				mLogOn = false;
			}
			else {
				item.setTitle("TURN LOG ON");
				mLogOn = true;
			}
			return true;

		case R.id.accelerometer:	
			mSensorSelected = Sensor.TYPE_ACCELEROMETER;
			if (mAccelerometer != null) mTextView.setText( getString(R.string.accelerometer) + ": " + display(mAccelerometer));
			return true;

		case R.id.gravity:	
			mSensorSelected = Sensor.TYPE_GRAVITY;
			if (mGravity != null) mTextView.setText( getString(R.string.gravity) + ": " + display(mGravity));
			return true;

		case R.id.gyroscope:	
			mSensorSelected = Sensor.TYPE_GYROSCOPE;
			if (mGyroscope != null) mTextView.setText( getString(R.string.gyroscope) + ": " + display(mGyroscope));
			return true;

		case R.id.light:	
			mSensorSelected = Sensor.TYPE_LIGHT;
			// The light sensor returns a single value. 
			// Many sensors return 3 values, one for each axis. 						
			if (mLight != null) mTextView.setText( getString(R.string.light) + ": " + mLight[0]);
			return true;

		case R.id.linearacceleration:	
			mSensorSelected = Sensor.TYPE_LINEAR_ACCELERATION;
			if (mLinearAcceleration != null) mTextView.setText( getString(R.string.linearacceleration) + ": " + display(mLinearAcceleration));
			return true;

		case R.id.magneticfield:	
			mSensorSelected = Sensor.TYPE_MAGNETIC_FIELD;
			if (mMagneticField != null) mTextView.setText( getString(R.string.magneticfield) + ": " + display(mMagneticField));

			return true;

		case R.id.orientation:	
			mSensorSelected = TYPE_ORIENTATION;
			// values[1]: pitch, rotation around the X axis
			// -58 is 32 (wake up angle), -78 is 12 degree (wake up angle)
			if (mOrientation != null) mTextView.setText( getString(R.string.orientation) + ": " + mOrientation[0] + "," + mOrientation[1] + "," + mOrientation[2]);
			return true;

		case R.id.rotationvector:	
			mSensorSelected = Sensor.TYPE_ROTATION_VECTOR;
			if (mRotationVector != null) mTextView.setText( getString(R.string.rotationvector) + ": " + display(mRotationVector));

			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}    	      




	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	private void writeSensorDataToFile(String sensorType, float data[]) {
		final String user_id = "dev-001";
		Log.d("Sophie", "Writing " + sensorType);
		try {
			flag=true;
			File file = new File(Environment.getExternalStorageDirectory(), "SensorData0.txt");
			Log.d("Sophie", "file got");
			FileOutputStream outputStream = new FileOutputStream(file, true);
			String time = Long.toString(System.currentTimeMillis());
			String content = user_id + "\t" + sensorType + "\t" + data[0] + "\t" + data[1] + "\t" + data[2] + "\t" +
				time + "\n";//
			AsyncTask<String, Void, Void> execute = new SendPostTask().execute(sensorType, Float.toString(data[0]), Float.toString(data[1]), Float.toString(data[2]),
							time);
			Log.d("content", content);
			outputStream.write(content.getBytes());
			outputStream.close();
			flag=false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}





	@Override
	public final void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			mAccelerometer = event.values.clone();
			if (new Date().getTime() - mSensorDataUpdatedTime[0].getTime() < 5) return;
			writeSensorDataToFile("Accelerometer", mAccelerometer);
			mSensorDataUpdatedTime[0] = new Date();
		}
		if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
			mGravity = event.values.clone();
			if (new Date().getTime() - mSensorDataUpdatedTime[1].getTime() < 5) return;
			writeSensorDataToFile("Gravity", mGravity);
			mSensorDataUpdatedTime[1] = new Date();
		}    			
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			mGyroscope = event.values.clone();
			if (new Date().getTime() - mSensorDataUpdatedTime[2].getTime() < 5) return;
			writeSensorDataToFile("Gyroscope", mGyroscope);
			mSensorDataUpdatedTime[2] = new Date();
		}    	
		if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
			mLight = event.values.clone();
			if (new Date().getTime() - mSensorDataUpdatedTime[3].getTime() < 5) return;
			writeSensorDataToFile("Light", mLight);
			mSensorDataUpdatedTime[3] = new Date();
		}
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			mLinearAcceleration = event.values.clone();
			if (new Date().getTime() - mSensorDataUpdatedTime[4].getTime() < 5) return;
			writeSensorDataToFile("LinearAcceleration", mLinearAcceleration);
			mSensorDataUpdatedTime[4] = new Date();
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mMagneticField = event.values.clone();
			if (new Date().getTime() - mSensorDataUpdatedTime[5].getTime() < 5) return;
			writeSensorDataToFile("MagneticField", mMagneticField);
			mSensorDataUpdatedTime[5] = new Date();
		}
		// special handling for orientation
		if (mAccelerometer != null && mMagneticField != null) {
			float rotation[] = new float[16];
			float orientation[] = new float[3];
			boolean success = SensorManager.getRotationMatrix(rotation, orientation, mAccelerometer, mMagneticField);
			if (success) {
				mOrientation = new float[3];
				SensorManager.getOrientation(rotation, mOrientation);
				// orientation contains: azimuth, pitch and roll
				mOrientation[0] = 180 + (float) Math.toDegrees(mOrientation[0]);
				mOrientation[1] = 90 + (float) Math.toDegrees(mOrientation[1]);
				mOrientation[2] = (float) Math.toDegrees(mOrientation[2]);
				if (new Date().getTime() - mSensorDataUpdatedTime[6].getTime() < 5) return;
				writeSensorDataToFile("Orientation", mOrientation);
				mSensorDataUpdatedTime[6] = new Date();
			}
		}   
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			mRotationVector = event.values.clone();
			if (new Date().getTime() - mSensorDataUpdatedTime[7].getTime() < 5) return;
			writeSensorDataToFile("RotationVector", mRotationVector);
			mSensorDataUpdatedTime[7] = new Date();
		}
	}



	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mSensorGravity, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mSensorLight, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mSensorLinearAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mSensorMagneticField, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mSensorRotationVector, SensorManager.SENSOR_DELAY_FASTEST);

		Date initTime = new Date();
		mSensorDataUpdatedTime = new Date[8];
		for (int i = 0; i < 8; ++i) mSensorDataUpdatedTime[i] = initTime;
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		//		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);		
	}



	// shake detection
	private void getAccelerometer(SensorEvent event) {
		float[] values = event.values;
		// Movement
		float x = values[0];
		float y = values[1];
		float z = values[2];

		float accelation = (x * x + y * y + z * z)
				/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
		long actualTime = System.currentTimeMillis();
		//Log.i(TAG, ""+accelationSquareRoot);
		if (accelation >= 1.2) // 2 is a big movement of head on Glass! 1 is too sensitive. 1.2 is a good one.
		{
			if (actualTime - lastUpdate < 20) {
				return;
			}
			lastUpdate = actualTime;
			mTextView.setText("Glass Shaked #" + (count++));
		}
	}    

	private void getMagneticField(SensorEvent event) {
		if (LocationActivity.getCurrentLocation() == null) {
			mTextView.setText("Go Get Location First");
			return;
		}

		float[] values = event.values;
		float x = values[0];
		float y = values[1];
		float z = values[2];
		float mag = (x * x + y * y + z * z);

		GeomagneticField geoField = new GeomagneticField((float) LocationActivity.getCurrentLocation().getLatitude(),
				(float) LocationActivity.getCurrentLocation().getLongitude(), 
				(float) LocationActivity.getCurrentLocation().getAltitude(),
				LocationActivity.getCurrentLocation().getTime());
		float expectedMag = (float) Math.sqrt(geoField.getX() + geoField.getY() + geoField.getZ());
		if (mag > 1.4*expectedMag || mag < 0.6*expectedMag) {
			mTextView.setText("Possible Metal Nearby!\n"+mag+"\n"+expectedMag);
		} else {
			mTextView.setText("No Metal Nearby");
		}
	}

	private void showCompassReading(SensorEvent event) {
		if (LocationActivity.getCurrentLocation() == null) {
			mTextView.setText("Go Get Location First");
			return;
		}
		System.out.println("event.values:" + Arrays.toString(event.values));
		SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
		SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
				SensorManager.AXIS_Z, mRotationMatrix);
		SensorManager.getOrientation(mRotationMatrix, mOrientation);

		// Convert the heading (which is relative to magnetic north) to one that is
		// relative to true north, using the user's current location to compute this.
		float magneticHeading = (float) Math.toDegrees(mOrientation[0]);
		float heading = MathUtils.mod(computeTrueNorth(magneticHeading), 360.0f)
				- 6;
		mTextView.setText(""+heading);		
	}


	/**
	 * Use the magnetic field to compute true (geographic) north from the specified heading
	 * relative to magnetic north.
	 *
	 * @param heading the heading (in degrees) relative to magnetic north
	 * @return the heading (in degrees) relative to true north
	 */
	private float computeTrueNorth(float heading) {
		if (mGeomagneticField != null) {
			return heading + mGeomagneticField.getDeclination();
		} else {
			return heading;
		}
	}




	private class SendPostTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			myRequestPost(params[0],  params[1],  params[2],  params[3], params[4]);
			return null;
		}
		private int myRequestPost(String type, String x, String y, String z,String timestamp) {

			int resultCode = 0;

			String url = "http://"+IP+":8881/poSensorData.php";

			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
//			nameValuePairs.add(new BasicNameValuePair("", user_id));
			nameValuePairs.add(new BasicNameValuePair("type", type));
			nameValuePairs.add(new BasicNameValuePair("x", x));
			nameValuePairs.add(new BasicNameValuePair("y", y));
			nameValuePairs.add(new BasicNameValuePair("z", z));
			String time_real = Long.toString(System.currentTimeMillis());//  /1000L
			nameValuePairs.add(new BasicNameValuePair("time", timestamp));
			nameValuePairs.add(new BasicNameValuePair("time_real", time_real));



			try {
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				HttpResponse response = client.execute(post);
				System.out.println("\nSending 'POST' request to URL : " + url);
				System.out.println("Post parameters : " + post.getEntity());
				System.out.println("Response Code : " +
						response.getStatusLine().getStatusCode());

				resultCode = response.getStatusLine().getStatusCode();
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}

				System.out.println(result.toString());
			} catch (Exception e) {
				Log.e("POST", e.getMessage());
			}

			return resultCode;
		}
		protected void onPostExecute(Void result) {
			Log.d("Sophie", "Posted sensor data");
		}
	}

}

