package com.andrewrobertclifton.tiltcontroller;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

public class TiltService extends Service implements SensorEventListener {

    private static final String TAG = TiltService.class.getSimpleName();
    private static final String ACTION_START = "com.andrewrobertclifton.tiltcontroller.ACTION_START";
    private static final String ACTION_STOP = "com.andrewrobertclifton.tiltcontroller.ACTION_STOP";
    private static final String ACTION_CALIBRATE = "com.andrewrobertclifton.tiltcontroller.ACTION_CALIBRATE";
    private static final String FAKE_GPS_ACTION = "com.lexa.fakegps.START";
    private static final String FAKE_GPS_PACKAGE = "com.lexa.fakegps";
    private static final String FAKE_GPS_EXTRA_LAT = "lat";
    private static final String FAKE_GPS_EXTRA_LONG = "long";

    private static final int UPDATE_INTERVAL = 500;
    private static final double DELTA = .00005;
    private static final double THRESHOLD = Math.PI / 6;

    private static final int NOTIFICATION_ID = 0;
    private SensorManager sensorManager;

    private NotificationManager notificationManager;
    private LocationManager locationManager;
    private Looper looper;

    private boolean running = false;
    private boolean calibrate = false;

    private final HandlerThread handlerThread;

    private Handler handler;

    private final float[] mAccelerometerReading = new float[3];

    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];

    private final float[] orientationAngles = new float[3];
    private final float[] orientationAnglesOffset = new float[3];


    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (running) {
                updateOrientationAngles();
                if (calibrate) {
                    calibrate = false;
                    System.arraycopy(orientationAngles, 0, orientationAnglesOffset, 0, orientationAngles.length);
                }
                try {
                    Location location = getLocationManager().getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    boolean modified = false;
                    Log.d(TAG, String.format("lat:%f , lon: %f", location.getLatitude(), location.getLongitude()));
                    if (orientationAngles[2] - orientationAnglesOffset[2] > THRESHOLD) {
                        modified = true;
                        lon = lon + DELTA;
                    } else if (orientationAngles[2] - orientationAnglesOffset[2] < -THRESHOLD) {
                        modified = true;
                        lon = lon - DELTA;
                    }
                    if (orientationAngles[1] - orientationAnglesOffset[1] > THRESHOLD) {
                        modified = true;
                        lat = lat + DELTA;
                    } else if (orientationAngles[1] - orientationAnglesOffset[1] < -THRESHOLD) {
                        modified = true;
                        lat = lat - DELTA;
                    }
                    if (modified) {
                        sendGPSUpdateIntent(lat, lon);
                    }

                } catch (SecurityException e) {

                }
                handler.postDelayed(runnable, UPDATE_INTERVAL);
            } else {
                getNotificationManager().cancel(NOTIFICATION_ID);
            }
        }
    };

    public TiltService() {
        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        looper = handlerThread.getLooper();
        handler = new Handler(looper);

    }

    public LocationManager getLocationManager() {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
        return locationManager;
    }

    public NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    public SensorManager getSensorManager() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        }
        return sensorManager;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, getNotification());
        getNotificationManager().notify(NOTIFICATION_ID,getNotification());
        if (intent != null) {
            if (TiltService.ACTION_START.equals(intent.getAction()) && !running) {
                running = true;
                registerListeners();
                handler.post(runnable);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        calibrate = true;
                    }
                },1000);
            } else if (TiltService.ACTION_STOP.equals((intent.getAction()))) {
                running = false;
                getSensorManager().unregisterListener(this);
                stopSelf();
            } else if(TiltService.ACTION_CALIBRATE.equals(intent.getAction())){
                calibrate = true;
            }

        }
        return Service.START_STICKY;
    }

    private void registerListeners() {
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
    }

    private void unRegisterListeners() {
        getSensorManager().unregisterListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterListeners();
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }
    }

    private Notification getNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getResources().getString(R.string.app_name));
        builder.setSmallIcon(R.drawable.ic_stat_main);
        builder.setOngoing(true);

        Intent intentContent = new Intent(Intent.ACTION_MAIN);
        intentContent.addCategory(Intent.CATEGORY_LAUNCHER);
        intentContent.setClass(this,TiltService.class);
        PendingIntent pendingIntentContent = PendingIntent.getService(this,0,intentContent,0);
        builder.setContentIntent(pendingIntentContent);

        Intent intentCalibrate = new Intent(TiltService.ACTION_CALIBRATE);
        intentCalibrate.setClass(this,TiltService.class);
        PendingIntent pendingIntentCalibrate = PendingIntent.getService(this,0,intentCalibrate,0);
        builder.addAction(R.drawable.ic_stat_calibrate, "Calibrate", pendingIntentCalibrate);

        Intent intentStop = new Intent(TiltService.ACTION_STOP);
        intentStop.setClass(this,TiltService.class);
        PendingIntent pendingIntentStop = PendingIntent.getService(this,0,intentStop,0);
        builder.addAction(R.drawable.ic_stat_stop, "Stop", pendingIntentStop);

        return builder.build();
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(mRotationMatrix, orientationAngles);

        // "orientationAngles" now has up-to-date information.
    }

    public void sendGPSUpdateIntent(double lat, double lon) {
        Intent intent = new Intent(FAKE_GPS_ACTION);
        intent.putExtra(FAKE_GPS_EXTRA_LAT, lat);
        intent.putExtra(FAKE_GPS_EXTRA_LONG, lon);
        intent.setPackage(FAKE_GPS_PACKAGE);
        this.startService(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public static void startService(Context context) {
        Intent intent = new Intent(context, TiltService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, TiltService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }
}
