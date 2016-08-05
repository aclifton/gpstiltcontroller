package com.andrewrobertclifton.tiltcontroller;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
    private static final int UPDATE_INTERVAL = 1000;
    private static final int NOTIFICATION_ID = 0;
    private SensorManager sensorManager;

    private NotificationManager notificationManager;
    private Looper looper;

    private boolean running = false;
    private boolean calibrate = false;

    public NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

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
                    System.arraycopy(orientationAngles, 0, orientationAnglesOffset, 0, orientationAngles.length);
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (Object f : orientationAngles) {
                    stringBuilder.append(f);
                    stringBuilder.append(" , ");
                }
                getNotificationManager().notify(NOTIFICATION_ID, getNotification());
                Log.d(TAG, stringBuilder.toString());
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
        if (intent == null) {
            return Service.START_STICKY;
        }
        if (TiltService.ACTION_START.equals(intent.getAction()) && !running) {
            running = true;
            calibrate = true;
            registerListeners();
            handler.post(runnable);
            startForeground(NOTIFICATION_ID, getNotification());
        } else if (TiltService.ACTION_STOP.equals((intent.getAction()))) {
            running = false;
            getSensorManager().unregisterListener(this);
            stopSelf();
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
        builder.setSmallIcon(R.mipmap.ic_launcher);
        return builder.build();
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        sensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        sensorManager.getOrientation(mRotationMatrix, orientationAngles);

        // "orientationAngles" now has up-to-date information.
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
