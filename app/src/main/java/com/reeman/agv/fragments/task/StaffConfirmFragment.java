package com.reeman.agv.fragments.task;

import android.content.Context;
import android.os.Bundle;
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

public class StaffConfirmFragment extends BaseFragment {

    private final OnArrivedBtnListener listener;

    public StaffConfirmFragment(OnArrivedBtnListener listener) {
        this.listener = listener;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_staff_confirm;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText payAccount = findView(R.id.payAccountText);
        TextView confirmBtn = findView(R.id.confirm_button);
        TextView ignoreBtn = findView(R.id.ignore_btn);

        payAccount.setText(RobotInfo.INSTANCE.getPayAccount());

        confirmBtn.setVisibility(View.VISIBLE);
        confirmBtn.setOnClickListener(this);

        ignoreBtn.setVisibility(View.VISIBLE);
        ignoreBtn.setOnClickListener(this);
    }

    @Override
    protected void onCustomClickResult(int id) {
        switch (id) {
            case R.id.confirm_button:
                listener.onReturnBtnClick(requireContext(), Constants.ORDER_STATUS_SUCCESS);
                break;
            case R.id.ignore_btn:
                listener.onReturnBtnClick(requireContext(), Constants.ORDER_STATUS_FAILED);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
    }

    public interface OnArrivedBtnListener {
        void onReturnBtnClick(Context context, Integer status);
    }

}
