package com.example.myapplication;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mob.MobSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText phoneNum;
    private EditText verifyCode;
    private Button sendMsg;
    private Button login;
    private EventHandler eh;
    private int time = 60;
    private boolean isSent = false;
    private ProgressDialog progressDialog = null;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        MobSDK.submitPolicyGrantResult(true, null);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        this.init();
        eh = new EventHandler() {
            @Override
            public void afterEvent(int event, int result, Object data) {
                // TODO 此处不可直接处理UI线程，处理后续操作需传到主线程中操作
                Message msg = new Message();
                msg.arg1 = event;
                msg.arg2 = result;
                msg.obj = data;
                mHandler.sendMessage(msg);
            }
        };
        //注册一个事件回调监听，用于处理SMSSDK接口请求的结果
        SMSSDK.registerEventHandler(eh);
    }

    //初始化控件
    private void init() {
        //手机号输入框
        phoneNum = findViewById(R.id.phoneNum);
        //验证码输入框
        verifyCode = findViewById(R.id.verifyCode);
        //消息发送按钮
        sendMsg = findViewById(R.id.sendMsg);
        //登录按钮
        login = findViewById(R.id.login);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        //设置监听
        sendMsg.setOnClickListener(this);
        login.setOnClickListener(this);
        String num = pref.getString("phoneNum", "");
        Log.d("登录界面-init", "获取到的号码为" + num);
        if (num != null && !num.equals("")) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("phoneNum", num);
            startActivity(intent);
            this.finish();
        }
        Log.d("登录界面-初始化", "初始化完成");
    }

    //判断输入的手机号是否满足格式要求
    private boolean isTelNumber(String tel) {
        //正则表达式判断输入手机号是否合法
        String regex = "^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(18[0,2,5-9]))|(19[0-9])\\d{8}$";
        return Pattern.matches(regex, tel);
    }

    private void setProgressDialog(String msg) {
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setTitle("");
        progressDialog.setMessage(msg);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //发送按钮事件
            case R.id.sendMsg: {
                Log.d("登录界面-按钮点击", "发送验证码");
                String numStr = this.phoneNum.getText().toString();
                if (!isTelNumber(numStr)) {
                    Toast.makeText(LoginActivity.this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
                    return;
                }
                this.setProgressDialog("等待发送验证码");
                SMSSDK.getVerificationCode("86", numStr);
                this.isSent = true;
                break;
            }
            //登录按钮事件
            case R.id.login: {
                Log.d("登录界面-按钮点击", "开始登录");
                String codeStr = this.verifyCode.getText().toString();
                String telNum = this.phoneNum.getText().toString();
                if (!isTelNumber(telNum)) {
                    Toast.makeText(LoginActivity.this, "请正确输入手机号", Toast.LENGTH_SHORT).show();
                    return;
                } else if (!this.isSent) {
                    Toast.makeText(LoginActivity.this, "请先获取验证码", Toast.LENGTH_SHORT).show();
                    return;
                } else if (!codeStr.isEmpty()) {
                    //提交验证码
                    SMSSDK.submitVerificationCode("86", telNum, codeStr);
                    this.setProgressDialog("等待登录验证");
                    //Toast.makeText(MainActivity.this, "看", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "请输入验证码", Toast.LENGTH_LONG).show();
                    return;
                }
                break;
            }
            default:
                break;
        }
    }

    //使用完EventHandler需注销，否则可能出现内存泄漏
    protected void onDestroy() {
        super.onDestroy();
        SMSSDK.unregisterEventHandler(eh);
    }


    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int event = msg.arg1;
            int result = msg.arg2;
            Object data = msg.obj;
            Log.d("登录界面-Handler", "event==" + event + "|---|" + "result==" + result);

            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }


            if (result == SMSSDK.RESULT_COMPLETE) {
                //回调完成
                Log.d("登录界面-Handler-CallBack", "handleMessage: --回调完成--");
                //获取到验证码
                if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                    Toast.makeText(LoginActivity.this, "验证码已正确发送，注意查收", Toast.LENGTH_SHORT).show();
                    Log.d("登录界面-Handler-Send", "验证码成功发送");
                    Clock_Count();
                } else if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    Log.d("登录界面-Handler-Login", "登录成功");
                    editor = pref.edit();
                    editor.putString("phoneNum", phoneNum.getText().toString());
                    editor.apply();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("phoneNum", phoneNum.getText().toString());
                    startActivity(intent);
                    LoginActivity.this.finish();
                } else if (event == SMSSDK.EVENT_GET_SUPPORTED_COUNTRIES) {
                    //返回支持发送验证码的国家列表
                    Log.d("登录界面-Handler-Country", "handleMessage: 获取国家列表成功");
                } else {
                    Log.d("登录界面-Handler-Country", "handleMessage: 捕捉");
                }
            } else {
                int status = 0;
                try {
                    Throwable throwable = (Throwable) data;
                    throwable.printStackTrace();
                    JSONObject object = new JSONObject(throwable.getMessage());
                    status = object.optInt("status");//错误代码
                    Log.d("登录界面-Handler-msg", "回调失败");
                    Log.d("登录界面-Handler-msg", "handleMessage: data==" + data + " status==" + status);
                } catch (JSONException je) {
                    //SMSLog.getInstance().w(e);
                } catch (Exception e) {
                    Log.d("登录界面-Handler-msg", "回调失败（其他原因）");
                }
                if (status == 468) {
                    Toast.makeText(LoginActivity.this, "验证码输入错误，请重新输入", Toast.LENGTH_SHORT).show();
                } else if (status == 466) {
                    Toast.makeText(LoginActivity.this, "验证码为空", Toast.LENGTH_SHORT).show();
                } else if (status == 462) {
                    Toast.makeText(LoginActivity.this, "当前操作过于频繁", Toast.LENGTH_SHORT).show();
                } else if (status == 467) {
                    Toast.makeText(LoginActivity.this, "验证失败3次，验证码失效，请重新请求", Toast.LENGTH_SHORT).show();
                } else if (status == 477) {
                    Toast.makeText(LoginActivity.this, "当前手机号发送短信的数量超过限额", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("登录界面-Handler-msg", "错误" + status);
                }
            }

            return false;
        }
    });

    //验证码按钮恢复倒计时，60s
    private void Clock_Count() {
        //让发送按钮失效，不可点击
        this.sendMsg.setEnabled(false);
        CountDownTimer timer = new CountDownTimer(time * 1000, 1000) {
            @Override
            public void onTick(long l) {
                String counter = time-- + "秒 后再次获取";
                LoginActivity.this.sendMsg.setText(counter);
            }

            @Override
            public void onFinish() {
                LoginActivity.this.sendMsg.setText("发送验证码");
                LoginActivity.this.sendMsg.setEnabled(true);
                //重置计时
                time = 60;
            }
        };
        timer.start();
    }
}
