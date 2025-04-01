package com.reeman.agv.activities;


import static com.reeman.agv.base.BaseApplication.ros;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.reeman.agv.R;
import com.reeman.commons.utils.ClickHelper;

public class UserMainActivity extends AppCompatActivity implements ClickHelper.OnFastClickListener {


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);

        ClickHelper clickHelper = new ClickHelper(this);
        TextView btn = findViewById(R.id.title);
        btn.setOnClickListener((v)->{
            clickHelper.fastClick();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onFastClick() {
        finish();
    }
}