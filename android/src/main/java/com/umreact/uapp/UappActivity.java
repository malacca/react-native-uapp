package com.umreact.uapp;

import android.util.Log;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

/**
 * TODO 厂商push通道接入
 * 处理完成后, 要在主工程的 manifest 添加
 * 
 *  <activity
 *      android:exported="true"
 *      android:launchMode="singleTask"
 *      android:label="@string/app_name"
 *      android:theme="@android:style/Theme.Translucent"
 *      android:name="com.umreact.uapp.UappActivity"
 *  />
 *
 */
public class UappActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("UMLog", "_P___________________________ttt_");
        Log.i("UMLog", getPackageName());
        //Log.i("UMLog", MainActivity.class);

        Intent intent =  getPackageManager().getLaunchIntentForPackage(getPackageName());

        //intent.setPackage(null);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);

        //finish();




//        Intent intent = new Intent();
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        intent.setPackage(getApplicationContext().getPackageName());
//
//        startActivity(intent);


//        Intent startIntent = new Intent();
//        startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        startIntent.setPackage(getApplicationContext().getPackageName());
//        startActivity(startIntent);

//        Intent startIntent = new Intent(context, MainActivity.class);
//        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(startIntent);

        //getApplicationContext()
        //UappModule.handleIntent(getIntent());
    }
}
