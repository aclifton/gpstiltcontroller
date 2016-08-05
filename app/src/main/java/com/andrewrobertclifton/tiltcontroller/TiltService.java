package com.andrewrobertclifton.tiltcontroller;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TiltService extends Service {
    public TiltService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
