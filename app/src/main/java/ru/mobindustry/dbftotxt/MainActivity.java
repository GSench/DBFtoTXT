package ru.mobindustry.dbftotxt;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

//import android.util.Log;


public class MainActivity extends AppCompatActivity {

    public static final String SPREF = "ConvertPreferences";
    public static final String FIRST_LAUNCH = "First Launch";

    int PICK_FILE = 1;

    public File path;
    public File file;

    AppCompatActivity act;

    Button convertButton;
    TextView processText, filePath;
    Spinner inputEnc, outputEnc;
    ProgressBar progressBar;
    LinearLayout settings;
    private InterstitialAd mInterstitialAd;

    boolean bound = false;
    ServiceConnection sConn;
    ConverterService converterService;
    private PermissionManager permissionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        act=this;

        initViews();

        if(savedInstanceState!=null){
            String file = savedInstanceState.getString(ConverterService.FILE, null);
            String path = savedInstanceState.getString(ConverterService.PATH, null);
            if(file!=null){
                filePath.setText(path);
                this.file = new File(file);
                this.path = new File(path);
            }
        }

        sConn = new ServiceConnection() {

            public void onServiceConnected(ComponentName name, IBinder binder) {
                converterService = ((ConverterService.MyBinder) binder).getService();
                bound = true;
                //Log.d("DBF_TO_TXT", "MainActivity onServiceConnected, bound = true");
                startTracking();
            }

            public void onServiceDisconnected(ComponentName name) {
                bound = false;
                //Log.d("DBF_TO_TXT", "MainActivity onServiceDisconnected, bound = false");
            }
        };

        checkPermissions();
    }

    private void checkPermissions(){
        permissionManager = new PermissionManager(this);
        permissionManager.requestBasePermissions(this, new function() {
            @Override
            public void run(String... params) {}
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        permissionManager.onPermissionCallback(requestCode, permissions, grantResults);
    }

    private void initAds(SharedPreferences spref){
        if(spref.getBoolean(FIRST_LAUNCH, true)) {
            MobileAds.initialize(this, getString(R.string.admob_app_id));
            spref.edit().putBoolean(FIRST_LAUNCH, false).apply();
        }
        AdView mAdView = (AdView) findViewById(R.id.adView);
        try{
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } catch (Exception e){
            e.printStackTrace();
        }
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.fullscreen_add_id));
    }

    private void initViews(){
        convertButton = (Button)findViewById(R.id.convert_btn);
        processText = ((TextView) findViewById(R.id.process));
        filePath = ((TextView) findViewById(R.id.path));
        settings = (LinearLayout)findViewById(R.id.settings);
        progressBar=(ProgressBar)findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        ArrayList<String> input = new ArrayList<String>();
        input.add(getString(R.string.inp_enc));
        input.addAll(Arrays.asList(getResources().getStringArray(R.array.ENCODINGS)));
        ArrayList<String> output = new ArrayList<String>();
        output.add(getString(R.string.out_enc));
        output.addAll(Arrays.asList(getResources().getStringArray(R.array.ENCODINGS)));

        ArrayAdapter<String> adapterInp = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, input);
        adapterInp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> adapterOut = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, output);
        adapterOut.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SharedPreferences spref = getSharedPreferences(SPREF, MODE_PRIVATE);

        inputEnc = (Spinner) findViewById(R.id.inp_enc);
        inputEnc.setAdapter(adapterInp);
        inputEnc.setPrompt(getString(R.string.inp_enc));
        inputEnc.setSelection(spref.getInt(ConverterService.INP_ENC, 0));
        outputEnc = (Spinner) findViewById(R.id.out_enc);
        outputEnc.setAdapter(adapterOut);
        outputEnc.setPrompt(getString(R.string.out_enc));
        outputEnc.setSelection(spref.getInt(ConverterService.OUT_ENC, 0));
        initAds(spref);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try{
            bindService(new Intent(this, ConverterService.class), sConn, 0);
            //Log.d("DBF_TO_TXT", "MainActivity onStart, bindService");
        } catch (Throwable e){
            //Log.d("DBF_TO_TXT", "MainActivity onStart, bindService ERROR");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!bound) return;
        bound = false;
        try {
            unbindService(sConn);
            //Log.d("DBF_TO_TXT", "MainActivity onStop, unbindService, bound = false");
        } catch (Throwable e){
            //Log.d("DBF_TO_TXT", "MainActivity onStop, unbindService ERROR, bound = false");
        }
    }

    public void selectFile(View v){
        pickFile();
    }

    public void convert(View v){
        doConvert();
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    private void showAd() {
        if(mInterstitialAd.isLoaded())
            mInterstitialAd.show();
        else{
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mInterstitialAd.show();
                }
            });
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }

    }

    private void doConvert(){
        if(file==null){
            Toast.makeText(this, getString(R.string.file_not_selected), Toast.LENGTH_LONG).show();
            return;
        }
        int inp = inputEnc.getSelectedItemPosition();
        int out = outputEnc.getSelectedItemPosition();
        getSharedPreferences(SPREF, MODE_PRIVATE)
                .edit()
                .putInt(ConverterService.INP_ENC, inp)
                .putInt(ConverterService.OUT_ENC, out)
                .apply();
        String[] ENCODINGS = getResources().getStringArray(R.array.ENCODINGS);
        String inpName = ENCODINGS[inp==0 ? inp : inp-1];
        String outName = ENCODINGS[out==0 ? out : out-1];
        Intent intent = new Intent(this, ConverterService.class);
        intent.putExtra(ConverterService.FILE, file.getAbsolutePath());
        intent.putExtra(ConverterService.PATH, path.getAbsolutePath());
        intent.putExtra(ConverterService.INP_ENC, inpName);
        intent.putExtra(ConverterService.OUT_ENC, outName);
        startService(intent);
        //Log.d("DBF_TO_TXT", "MainActivity startService");
    }

    private void openSettings(){
        if(settings.getVisibility()==View.VISIBLE) return;
        settings.setVisibility(View.VISIBLE);
    }

    private void closeSettings(){
        if(settings.getVisibility()==View.INVISIBLE||settings.getVisibility()==View.GONE) return;
        settings.setVisibility(View.GONE);
    }

    public void startTracking(){
        final AppCompatActivity act = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (bound){
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(converterService.getError()!=null){
                                Toast.makeText(act, converterService.getError(), Toast.LENGTH_LONG).show();
                                processText.setText("");
                                convertButton.setClickable(true);
                                openSettings();
                                progressBar.setVisibility(View.INVISIBLE);
                                filePath.setText(converterService.getFile().getAbsolutePath());
                                converterService.stopSelf();
                            } else if(converterService.isFinished()){
                                showAd();
                                processText.setText(getString(R.string.converted));
                                filePath.setText(converterService.getFile().getAbsolutePath());
                                convertButton.setClickable(true);
                                openSettings();
                                progressBar.setVisibility(View.INVISIBLE);
                                converterService.stopSelf();
                            } else {
                                processText.setText((int) ((float) converterService.getLoaded() / converterService.getFrom() * 100f) + " %");
                                convertButton.setClickable(false);
                                closeSettings();
                                progressBar.setVisibility(View.VISIBLE);
                                progressBar.setMax(converterService.getFrom());
                                progressBar.setProgress(converterService.getLoaded());
                                filePath.setText(converterService.getFile().getAbsolutePath());
                            }
                            //Log.d("DBF_TO_TXT", "MainActivity process: " + processText.getText().toString());
                        }
                    });
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(requestCode==PICK_FILE){
            if(resultCode==RESULT_OK){
                //TODO fix NullPointerException bug
                file = UriUtils.getFile(this, resultData.getData()); // - UriUtils.getFile works bad
                path = file.getParentFile(); //Need to implement new FAS API or another way to get destination file
                filePath.setText(file.getAbsolutePath());
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void pickFile() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT){
            Intent intent = new Intent();
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            startActivityForResult(intent, PICK_FILE);
        } else {
            Intent i = new Intent(this, FilePickerActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
            startActivityForResult(i, PICK_FILE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle instanceState) {
        if (file != null) instanceState.putString(ConverterService.FILE, file.getAbsolutePath());
        if (path != null) instanceState.putString(ConverterService.PATH, path.getAbsolutePath());
        super.onSaveInstanceState(instanceState);
    }

}
