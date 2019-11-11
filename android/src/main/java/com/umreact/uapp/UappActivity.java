package com.umreact.uapp;

import android.os.Bundle;
import org.json.JSONObject;
import android.content.Intent;
import org.android.agoo.common.AgooConstants;

import com.umeng.message.UmengNotifyClickActivity;

public class UappActivity extends UmengNotifyClickActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent =  getPackageManager().getLaunchIntentForPackage(getPackageName());
        startActivity(intent);
    }

    @Override
    public void onMessage(Intent intent) {
        super.onMessage(intent);
        try {
            String body = intent.getStringExtra(AgooConstants.MESSAGE_BODY);
            JSONObject msg = new JSONObject(body);
            UappModule.handlePushMessage(msg);
        } catch (Throwable e) {
            // do nothing
        }
        onDestroy();
    }
}
