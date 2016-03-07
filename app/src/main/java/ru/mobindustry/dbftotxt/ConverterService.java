package ru.mobindustry.dbftotxt;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
//import android.util.Log;

import org.jamel.dbf.DbfReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Григорий on 09.02.2016.
 */
public class ConverterService extends Service {

    public static final String FILE = "file";
    public static final String PATH = "path";
    public static final String INP_ENC = "input_enc";
    public static final String OUT_ENC = "output_enc";

    public static final int ID = 45364372;

    MyBinder binder = new MyBinder();
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;

    File file, path;

    public File getFile() {
        return file;
    }

    public File getPath() {
        return path;
    }

    public int getLoaded() {
        return loaded;
    }

    public int getFrom() {
        return from;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getError() {
        return error;
    }

    private volatile int loaded = 0;
    private volatile int from = 0;
    private volatile int oldPc = 0;
    private volatile boolean finished = false;
    private volatile String error;

    @Override
    public void onCreate(){
        Notification.Builder builder = new Notification.Builder(this).setSmallIcon(R.drawable.ic_app);
        Notification notification;
        notification = builder.build();
        startForeground(ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        //Log.d("DBF_TO_TXT", "ConverterService onStartCommand");
        file = new File(intent.getStringExtra(FILE));
        path = new File(intent.getStringExtra(PATH));
        String inputEncoding, outputEncoding;
        inputEncoding = intent.getStringExtra(INP_ENC);
        outputEncoding = intent.getStringExtra(OUT_ENC);
        setNotification();
        process(file, path, inputEncoding, outputEncoding);
        //Log.d("DBF_TO_TXT", "ConverterService onStartCommandFinished");
        return START_NOT_STICKY;
    }

    private void setNotification(){
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.converting))
                .setContentText(getString(R.string.converting))
                //TODO icon, text
                .setSmallIcon(R.drawable.ic_app);
        mBuilder.setProgress(0, 0, true);
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(notifyPendingIntent);
        mNotifyManager.notify(ID, mBuilder.build());
    }

    public void process(final File file, final File path, final String inpEnc, final String outEnc){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String name = file.getName();
                name = name.substring(0, name.lastIndexOf("."))+".txt";
                File txt = new File(path, name);
                try {
                    txt.createNewFile();
                } catch (IOException e) {
                    setError(getString(R.string.create_error));
                    return;
                }
                OutputStream outputStream;
                try {
                    outputStream = new FileOutputStream(txt);
                } catch (FileNotFoundException e) {
                    setError(getString(R.string.write_error));
                    return;
                }
                Processor processor = null;
                DbfReader reader = null;
                int count = 0;
                try {
                    processor = new Processor(outputStream, inpEnc, outEnc);
                    reader = new DbfReader(file);
                    count = reader.getRecordCount();
                } catch (Exception e){
                    setError(getString(R.string.convert_error));
                }
                try {
                    Object[] row;
                    while ((row = reader.nextRecord()) != null) {
                        processor.processRow(row);
                        setProcess(processor.getCount(), count);
                    }
                    setFinished();
                } catch (Exception e){
                    setError(getString(R.string.convert_error));
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                    if (processor != null) {
                        processor.close();
                    }
                }
            }
        }).start();
    }

    private void setProcess(int loaded, int from){
        this.loaded=loaded;
        this.from=from;
        int pc = (int) ((float) loaded / from * 100);
        if(pc !=oldPc){
            oldPc= pc;
            mBuilder.setProgress(from, loaded, false);
            mBuilder.setContentText(pc +" %");
            mNotifyManager.notify(ID, mBuilder.build());
        }
    }

    private void setFinished(){
        finished=true;
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.converting))
                .setContentText(getString(R.string.converted))
                        //TODO icon, text
                .setSmallIcon(R.drawable.ic_app);
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(notifyPendingIntent);
        mNotifyManager.notify(ID, mBuilder.build());
        stopForeground(false);
    }

    private void setError(String error){
        this.error=error;
        //Log.d("DBF_TO_TXT", "ConverterService error: "+error);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.converting))
                .setContentText(error)
                        //TODO icon, text
                .setSmallIcon(R.drawable.ic_app);
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(notifyPendingIntent);
        mNotifyManager.notify(ID, mBuilder.build());
        stopForeground(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class MyBinder extends Binder {
        ConverterService getService() {
            return ConverterService.this;
        }
    }
}
