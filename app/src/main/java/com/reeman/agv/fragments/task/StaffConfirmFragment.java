package com.reeman.agv.fragments.task;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.reeman.agv.R;
import com.reeman.agv.base.BaseFragment;
import com.reeman.agv.viewModel.TaskArrivedInfoModel;
import com.reeman.commons.constants.Constants;
import com.reeman.commons.state.OrderInfo;

import timber.log.Timber;

public class StaffConfirmFragment extends BaseFragment {

    private final OnArrivedBtnListener listener;

    private static final String CORRECT_PASSWORD = "123456"; // 正确密码

    private LinearLayout passwordLayer;
    private LinearLayout contentLayer;
    private EditText passwordInput;
    private Button confirmBtn;
    private TextView errorText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 加载布局文件
        return inflater.inflate(R.layout.fragment_staff_confirm, container, false);
    }


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

        // 初始化视图
        passwordLayer = view.findViewById(R.id.password_layer);
        contentLayer = view.findViewById(R.id.content_layer);
        passwordInput = view.findViewById(R.id.password_input);
        confirmBtn = view.findViewById(R.id.btn_confirm_password);
        errorText = view.findViewById(R.id.tv_password_error);

        // 设置密码确认按钮点击事件
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputPassword = passwordInput.getText().toString();

                if (inputPassword.equals(CORRECT_PASSWORD)) {
                    // 密码正确，显示内容层
                    passwordLayer.setVisibility(View.GONE);
                    contentLayer.setVisibility(View.VISIBLE);
                } else {
                    // 密码错误提示
                    errorText.setText("密码错误，请重新输入");
                    errorText.setVisibility(View.VISIBLE);
                    passwordInput.setText("");
                }
            }
        });
        
        
        TextView payAccount = view.findViewById(R.id.payAccountText);
        TextView confirmBtn = view.findViewById(R.id.confirm_button);
        TextView ignoreBtn = view.findViewById(R.id.ignore_btn);

        payAccount.setText(OrderInfo.getInstance().getPayAccount());

        confirmBtn.setOnClickListener(this);
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
