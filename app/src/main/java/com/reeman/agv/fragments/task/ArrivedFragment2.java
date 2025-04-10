package com.reeman.agv.fragments.task;

import android.content.Context;
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
import com.reeman.commons.state.RobotInfo;
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
        EditText payAccount = findView(R.id.payAccountInput);
        TextView tvReturn = findView(R.id.tv_return_to_product_point);

        payAccount.setText(RobotInfo.INSTANCE.getPayAccount());

        tvReturn.setVisibility(taskArrivedInfoModel.getShowReturnButton() ? View.VISIBLE : View.GONE);
        tvReturn.setOnClickListener(this);
    }

    @Override
    protected void onCustomClickResult(int id) {
        switch (id) {
            case R.id.tv_return_to_product_point:
                EditText payAccount = findView(R.id.payAccountInput);
                String payaccount = payAccount.getText().toString();
                listener.onReturnBtnClick(requireContext(), payaccount);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
    }

    public interface OnArrivedBtnListener {
        void onReturnBtnClick(Context context, String payaccount);
    }

}
