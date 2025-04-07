package com.reeman.agv.fragments.task;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.reeman.agv.R;
import com.reeman.agv.base.BaseFragment;
import com.reeman.agv.viewModel.TaskArrivedInfoModel;
import com.reeman.commons.constants.Constants;
import com.reeman.commons.utils.StringUtils;

import java.util.concurrent.TimeUnit;

public class ArrivedFragment2 extends BaseFragment {

    private TextView tvCountDownTime;

    private final OnArrivedBtnListener listener;

    public ArrivedFragment2(OnArrivedBtnListener listener) {
        this.listener = listener;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_arrived2;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        String taskArrivedInfoStr = bundle.getString(Constants.KEY_TASK_ARRIVED_INFO, "");
        TaskArrivedInfoModel taskArrivedInfoModel = new Gson().fromJson(taskArrivedInfoStr, TaskArrivedInfoModel.class);
        initView(taskArrivedInfoModel);
    }


    private void initView(TaskArrivedInfoModel taskArrivedInfoModel) {
        TextView tvTaskMode = findView(R.id.tv_task_mode);
        TextView tvRouteName = findView(R.id.tv_route_name);
        TextView tvNowFloor = findView(R.id.tv_now_floor);
        TextView tvTaskStartTime = findView(R.id.tv_task_start_time);
        TextView tvArrived = findView(R.id.tv_arrived);
        TextView tvNextPoint = findView(R.id.tv_next_point);
        EditText alipayNumber = findView(R.id.alipayNumberInput);
        tvCountDownTime = findView(R.id.tv_next_point_countdown);
        TextView tvReturn = findView(R.id.tv_return_to_product_point);


        tvTaskMode.setText(taskArrivedInfoModel.getTaskMode());
        tvTaskStartTime.setText(getString(R.string.text_task_start_time,taskArrivedInfoModel.getTaskStartTime()));
        tvArrived.setText(getString(R.string.text_arrived, taskArrivedInfoModel.getCurrentPoint()));

        if (TextUtils.isEmpty(taskArrivedInfoModel.getRouteName())) {
            tvRouteName.setVisibility(View.GONE);
        } else {
            tvRouteName.setText(getString(R.string.text_route_name, taskArrivedInfoModel.getRouteName()));
        }
        if (TextUtils.isEmpty(taskArrivedInfoModel.getNextPoint())) {
            tvNextPoint.setVisibility(View.GONE);
        } else {
            if (!TextUtils.isEmpty(taskArrivedInfoModel.getNextFloor())){
                tvNextPoint.setText(getString(R.string.text_wait_for_go_to_next_point,getString(R.string.text_point_with_floor,taskArrivedInfoModel.getNextFloor(),taskArrivedInfoModel.getNextPoint())));
            }else {
                tvNextPoint.setText(getString(R.string.text_wait_for_go_to_next_point, taskArrivedInfoModel.getNextPoint()));
            }
        }
        if (TextUtils.isEmpty(taskArrivedInfoModel.getNowFloor())) {
            tvNowFloor.setVisibility(View.GONE);
        } else {
            tvNowFloor.setText(getString(R.string.text_now_floor, taskArrivedInfoModel.getNowFloor()));
        }
        if (taskArrivedInfoModel.getCountDownTime() > 0) {
            updateCountDownTimer(taskArrivedInfoModel.getCountDownTime());
        } else {
            tvCountDownTime.setVisibility(View.INVISIBLE);
        }
        if (taskArrivedInfoModel.isReturnToChargePoint()){
            tvReturn.setText(getString(R.string.text_return_charge_point));
        }else {
            tvReturn.setText(getString(R.string.text_return_product_point));
        }
        tvReturn.setVisibility(taskArrivedInfoModel.getShowReturnButton() ? View.VISIBLE : View.GONE);
        tvReturn.setOnClickListener(this);
    }

    public void updateCountDownTimer(long seconds) {
        if(seconds == 0){
            tvCountDownTime.setVisibility(View.GONE);
            return;
        }
        if (tvCountDownTime.getVisibility() != View.VISIBLE){
            tvCountDownTime.setVisibility(View.VISIBLE);
        }
        long millisSeconds = seconds * 1000;
        int hour = (int) TimeUnit.MILLISECONDS.toHours(millisSeconds);
        int minute = (int) TimeUnit.MILLISECONDS.toMinutes(millisSeconds) % 60;
        int second = (int) TimeUnit.MILLISECONDS.toSeconds(millisSeconds) % 60;
        SpannableStringBuilder str = new SpannableStringBuilder();
        if (hour > 0) {
            SpannableString hourStr = new SpannableString(getString(R.string.text_hours_later, hour));
            StringUtils.updateNumber(hourStr, hourStr.toString().split("\\d")[0].length(), String.valueOf(hour).length());
            str.append(hourStr);
        }
        if (minute > 0) {
            SpannableString minuteStr = new SpannableString(getString(R.string.text_minutes_later, minute));
            StringUtils.updateNumber(minuteStr, minuteStr.toString().split("\\d")[0].length(), String.valueOf(minute).length());
            str.append(minuteStr);
        }
        SpannableString secondStr = new SpannableString(getString(R.string.text_task_continued_in_minutes, second));
        StringUtils.updateNumber(secondStr, secondStr.toString().split("\\d")[0].length(), String.valueOf(second).length());
        str.append(secondStr);
        tvCountDownTime.setText(str);
    }

    @Override
    protected void onCustomClickResult(int id) {
        switch (id) {
            case R.id.tv_return_to_product_point:
                listener.onReturnBtnClick();
                break;
        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
    }

    public interface OnArrivedBtnListener {
        void onReturnBtnClick();
    }

}
