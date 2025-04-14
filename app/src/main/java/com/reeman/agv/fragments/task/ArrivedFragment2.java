package com.reeman.agv.fragments.task;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
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
import com.reeman.commons.state.OrderInfo;
import com.reeman.commons.state.RobotInfo;
import com.reeman.commons.utils.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

        payAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                // 支付宝账号格式: 邮箱格式或手机号格式
                if (!isValidAlipayAccount(input)) {
                    payAccount.setError("请输入正确的支付宝账号格式");
                } else {
                    payAccount.setError(null);
                }
            }
        });

        //限制输入内容
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                // 只允许字母、数字、@、.、_ 和 -
                if (!Character.isLetterOrDigit(c) && c != '@' && c != '.' && c != '_' && c != '-') {
                    return "";
                }
            }
            return null;
        };
        payAccount.setFilters(new InputFilter[]{filter});

        payAccount.setText(OrderInfo.getInstance().getPayAccount());
        tvReturn.setVisibility(taskArrivedInfoModel.getShowReturnButton() ? View.VISIBLE : View.GONE);
        tvReturn.setOnClickListener(this);
    }

    // 验证支付宝账号格式的方法
    private boolean isValidAlipayAccount(String input) {
        // 支付宝账号可以是邮箱或手机号
        // 邮箱格式验证
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        // 手机号格式验证 (11位数字)
        Pattern phonePattern = Pattern.compile("^[1][3-9][0-9]{9}$");

        return emailPattern.matcher(input).matches() || phonePattern.matcher(input).matches();
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
