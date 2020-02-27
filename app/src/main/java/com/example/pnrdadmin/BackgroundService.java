package com.example.pnrdadmin;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Binder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationBuilderWithBuilderAccessor;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.greenrobot.eventbus.EventBus;

public class BackgroundService extends Service {

    private static final String CHANNEL_ID= "My_Channel";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = "com.example.pnrdadmin"+".started_from_notification" ;

    private final IBinder mBinder = new LocalBinder();
    private static final long UPDATE_INTERVAL_IN_MIL = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MIL = UPDATE_INTERVAL_IN_MIL/2;
    private static final int NOTI_ID=1223;
    private boolean mChangingConfiguration = false;
    private NotificationManager mNotifcationManager;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Handler mServiceHandler;
    private Location mLoaction;

    public BackgroundService(){

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
        if(startedFromNotification)
        {
            removeLocationUpdates();
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    public void removeLocationUpdates() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            Common.setRequestingLocationUpdates(this, false);
            stopSelf();
        }catch (SecurityException ex)
        {
            Common.setRequestingLocationUpdates(this, true);
            Log.e("pnrdAdmin", "Lost Location permission.Could not remove updates. "+ex);

        }
    }

    @Override
    public void onCreate() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());

            }
        };

        createLocationRequest();
        getastLocation();

        HandlerThread handlerThread = new HandlerThread("pnrdAdmin");
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotifcationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotifcationManager.createNotificationChannel(mChannel);
        }
    }

    private void getastLocation(){
        try
        {
            fusedLocationProviderClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if(task.isSuccessful() && task.getResult()!=null)
                            {
                                mLoaction = task.getResult();
                            }
                            else
                            {
                                Log.e("pnrdAdmin", "Failed to get the location");
                            }

                        }
                    });
        } catch (SecurityException ex) {
            Log.e("pnrdAdmin", "Lost Location Permission: "+ex);
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MIL);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MIL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    private void onNewLocation(Location lastLocation){
        mLoaction = lastLocation;
        EventBus.getDefault().postSticky(new SendLocationToActivity(mLoaction));

        if(serviceIsRunningInForeground(this))
            mNotifcationManager.notify(NOTI_ID,getNotification());
    }

     Notification getNotification(){
        Intent intent = new Intent(this,BackgroundService.class);
        String text = Common.getLocationText(mLoaction);

        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION,true);
         PendingIntent servicePendingIntent = PendingIntent.getService(this,0,intent,
                 PendingIntent.FLAG_UPDATE_CURRENT);
         PendingIntent activityPendingIntent = PendingIntent.getActivity(this,0,new Intent(this,HomeActivity.class),0);

         NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                 .addAction(R.drawable.ic_launch_black_24dp,"Launch",activityPendingIntent)
                 .addAction(R.drawable.ic_cancel_black_24dp,"Remove",servicePendingIntent)
                 .setContentText(text)
                 .setContentTitle(Common.getLocationTitle(this))
                 .setOngoing(true)
                 .setPriority(Notification.PRIORITY_HIGH)
                 .setSmallIcon(R.mipmap.ic_launcher)
                 .setTicker(text)
                 .setWhen(System.currentTimeMillis());

         if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
             builder.setChannelId(CHANNEL_ID);

         }
        return builder.build();

    }

    private boolean serviceIsRunningInForeground(Context context){
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service:manager.getRunningServices(Integer.MAX_VALUE))
            if(getClass().getName().equals(service.service.getClassName()))
                if(service.foreground)
                    return true;
        return false;
    }

    public void requestLocationUpdates() {
        Common.setRequestingLocationUpdates(this,true);
        startService(new Intent(getApplicationContext(), BackgroundService.class));

        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
        }
        catch (SecurityException ex)
        {
            Log.e("pnrdAdmin","Lost Location Permissions. Could not request it. "+ex);
        }
    }

    public class LocalBinder extends Binder{
        BackgroundService getService(){return BackgroundService.this;}
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration =  false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(!mChangingConfiguration && Common.requestingLocalUpdates(this))
            startForeground(NOTI_ID, getNotification());
        return true;
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacks(null);
        super.onDestroy();
    }
}
