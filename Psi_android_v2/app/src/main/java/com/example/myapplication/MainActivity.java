package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.bloom.Bloom;
import com.example.myapplication.bloom.BloomIO;
import com.example.myapplication.util.Rand;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //private Button btn_getKey;
    //private ImageView img_getKey;
    private ProgressDialog progressDialog;
    private String phoneNum;
    private Button btn_compute;
    private ListView listView;
    private TextView text_user;
    private SearchView searchView;
    private List<String> numList;
    private List<PhoneNumData> dataList;
    private List<BigInteger> numIntList;
    private Button btn_logout;
    private String rootDir;

    private final String IP = "10.0.2.2";
    private final int PORT = 12345;
    private Socket socket = null;
    private InetAddress inetAddress;

    private PSI psi;

    private Handler handler;
    private final int TOAST_DB_loaded = 1;
    private final int TOAST_APP_empty = 2;
    private final int TOAST_DB_empty = 3;
    private final int TOAST_Malware = 4;
    private final int TOAST_NOT_Malware = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ///////////
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().penaltyDeath().build());
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        //////////////
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_main);
        // 检查读取联系人权限
        checkForPermission();
        // 活动初始化，包括控件的指定和设置监听等
        activityInit();

        Log.d("MainActivity-Oncreate","new psi,dir is:"+rootDir);
        psi = new PSIRSA(rootDir);
        // base准备
        basePhase();
        // 检查数据的更新
        getUpdate();
        // 测试
        //test();

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.arg1 == TOAST_DB_loaded) {
                    Toast.makeText(MainActivity.this, "DB has been downloaded!", Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == TOAST_APP_empty) {
                    Toast.makeText(MainActivity.this, "Please input an App!", Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == TOAST_DB_empty) {
                    Toast.makeText(MainActivity.this, "Please load DB first!", Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == TOAST_Malware) {
                    Toast.makeText(MainActivity.this, "This is a malware!!!", Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == TOAST_NOT_Malware) {
                    Toast.makeText(MainActivity.this, "This is a secure APP!", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }



    private void callMainThread (int arg) {
        Message msg = handler.obtainMessage();
        msg.arg1 = arg;
        handler.sendMessage(msg);
    }

    public void test() {
        Bloom bloom1 = new Bloom();
        bloom1.bloom_init(1000, 0.001);
        bloom1.bloom_add("lxp");
        bloom1.bloom_add("wt");
        BloomIO.bloomWriter(bloom1, rootDir + "/bloom.json");
        Bloom bloom2 = BloomIO.bloomReader(rootDir + "/bloom.json");
        System.out.println(rootDir);
        System.out.println(bloom2.bloom_check("lxp"));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            /*
            case (R.id.button_getKey): {
                // 获取公钥
                basePhase();
                break;
            }
            */
            // 开始计算
            case (R.id.button_compute): {

                // 开始计算
                try {
                    onlinePhase();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            // 退出登录
            case (R.id.button_logout): {
                // 写入登录的手机号为空
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("phoneNum", "");
                editor.apply();

                byeServer();
                // 跳转到登录界面
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                this.finish();
                break;
            }
            default:
                break;
        }
    }


    private void activityInit() {
        Log.d("MainActivity-activityInit", "初始化控件");
        // 控件的指定
        //btn_getKey = findViewById(R.id.button_getKey);
        btn_compute = findViewById(R.id.button_compute);
        //img_getKey = findViewById(R.id.image_getKey);
        //btn_getKey.setOnClickListener(this);
        listView = findViewById(R.id.listView);
        searchView = findViewById(R.id.searchView);
        text_user = findViewById(R.id.textView_user);
        btn_compute.setOnClickListener(this);
        btn_logout = findViewById(R.id.button_logout);
        btn_logout.setOnClickListener(this);
        // 从登录界面获取登录的手机号
        Intent intent = getIntent();
        phoneNum = intent.getStringExtra("phoneNum");
        text_user.setText(phoneNum);
        // 获取应用存储文件的文件夹
        rootDir = this.getFilesDir().toString();
        // 数据初始化，包括联系人的读入、密钥的检查和生成
        dataInit();
        // ListView的适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, numList);
        listView.setAdapter(adapter);
        listView.setTextFilterEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!TextUtils.isEmpty(s)) {
                    listView.setFilterText(s);
                } else {
                    listView.clearTextFilter();
                }
                return false;
            }
        });
        Log.d("MainActivity-activityInit", "初始化完毕");
    }


    /**
     * 检查权限
     */
    private void checkForPermission() {
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, 201);
            Log.d("MainActivity-check", "正在获取权限");
        } else {
            Log.d("MainActivity-check", "已拥有权限");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 201) {
            Log.d("MainActivity-RequestPermissions", "获取权限成功");
        } else {
            Log.d("MainActivity-RequestPermissions", "获取权限失败");
            Toast.makeText(MainActivity.this, "请重新获取权限", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 数据初始化，显示所有联系人，并初始化rKey
     */
    private void dataInit() {
        setProgressDialog("", "数据初始化");
        dataList = new PhoneUtil(this).getPhone();
        numList = new ArrayList<>();
        numIntList = new ArrayList<>();
        // 将所有联系人写入phonenum.txt
        String num = null;
        dataWriter("phonenum.txt", "", Context.MODE_PRIVATE);
        for (PhoneNumData data : dataList) {
            num = data.getNum().replace(" ", "").substring(3);
            Log.d("MainActivity-dataInit","phonenum:"+num);
            numList.add(num);
            dataWriter("phonenum.txt", num + "\n", Context.MODE_APPEND);
        }
        Log.d("MainActivity-dataInit", "写入联系人信息");
        deleteProgressDialog();
    }


    /**
     * 生成进度对话框
     *
     * @param title 对话框标题
     * @param msg   对话框内容
     */
    private void setProgressDialog(String title, String msg) {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle(title);
        progressDialog.setMessage(msg);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    /**
     * 销毁进度对话框
     */
    private void deleteProgressDialog() {
        progressDialog.dismiss();
        progressDialog = null;
    }

    /**
     * 从服务器端实现数据更新
     */
    // TODO: 数据更新
    private void getUpdate() {
        Log.d("MainActivity-getUpdate", "检查更新");
        setProgressDialog("", "正在检查服务器端数据更新");
        boolean checkResult = serverDataCheck();
        deleteProgressDialog();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("数据更新");
        alertDialog.setCancelable(false);
        if (checkResult) {
            alertDialog.setMessage("服务器端数据有更改，请点击更新按钮进行数据更新");
            alertDialog.setPositiveButton("更新", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(MainActivity.this, "数据更新已完成", Toast.LENGTH_SHORT).show();
                }
            });
            alertDialog.show();
            setProgressDialog("", "正在更新数据");
            serverDataUpdate();
            deleteProgressDialog();
            Log.d("MainActivity-getUpdate", "需要更新");
        } else {
            alertDialog.setMessage("服务器端数据没有更改，无需数据更新");
            alertDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            alertDialog.show();
            Log.d("MainActivity-getUpdate", "无需更新");
        }
        Log.d("主界面-getUpdate", "检查完毕");
    }

    /**
     * 检查数据更新
     *
     * @return 是否需要更新
     */
    // TODO: 检查数据更新（服务器端可以用日期名作为bloom文件的命名）
    private boolean serverDataCheck() {
        return true;
    }

    /**
     * 获取数据更新
     */
    // TODO: 获取数据更新
    private void serverDataUpdate() {

    }

    /**
     * 获取公钥
     */
    // TODO: base阶段
    private void basePhase() {

        //img_getKey.setImageResource(R.drawable.trueimage);
        setProgressDialog("", "正在进行密钥初始化");

        /*
        // 如果没有rKey.txt或pub_key.xml（首次使用），进行base阶段
        if (!new File(rootDir + "/rKey.txt").exists() || !new File(rootDir.replace("files","shared_prefs")+ "/pub_key.xml").exists()) {
            Log.d("主界面-dataInit", "获取公钥");
            getPublicKey();
            Log.d("主界面-dataInit", "公钥获取成功");
            Log.d("主界面-dataInit", "生成rKey");
            rKeyGenerator();
            Log.d("主界面-dataInit", "rkey生成完毕");
        }

         */

        callServer(new String(""),"DB");
        deleteProgressDialog();


    }

    // TODO: 获取公钥，并存入pref（可查看本代码中pref和editor的使用）
    private void getPublicKey() {
        byte[][] PK = Utils.receive2DBytes(socket);
        // 测试
        SharedPreferences pref = getSharedPreferences("pub_key", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("pubkey_N", Utils.bytesToBigInteger(PK[1],0,PK[0].length).toString(16));
        editor.putString("pubkey_E", Utils.bytesToBigInteger(PK[0],0,PK[0].length).toString(16));
        editor.apply();

    }

    private void rKeyGenerator() {
        SharedPreferences pref = getSharedPreferences("pub_key", MODE_PRIVATE);
        String nStr = pref.getString("pubkey_N", ""), eStr = pref.getString("pubkey_E", "");
        BigInteger N = new BigInteger(nStr, 16), E = new BigInteger(eStr, 16);
        BigInteger r, r_inv, r_e;
        dataWriter("rKey.txt", "", Context.MODE_PRIVATE);
        for (int i = 0; i < 1000; i++) {
            do {
                r = Rand.randInt(1024);
            } while (r.gcd(N).compareTo(BigInteger.ONE) != 0);
            r_inv = r.modInverse(N);
            r_e = r.modPow(E, N);
            dataWriter("rKey.txt", r.toString(16) + "," + r_inv.toString(16) + "," + r_e.toString(16), Context.MODE_APPEND);
        }
    }

    /**
     * 开始计算
     */
    // TODO: online阶段
    private void onlinePhase() throws IOException {
        String app;
        FileInputStream in = null;
        BufferedReader reader = null;
        try {
            File file = new File(rootDir+"/phonenum.txt");
            if(file.exists() && file.length() == 0) {
                Log.d("MainActivity-onlinePhase","open file failed!");
                callMainThread(TOAST_APP_empty);
            } else if (psi.getDBSize() == 0) {
                callMainThread(TOAST_DB_empty);
            } else{
                in = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(in));
                while ((app = reader.readLine()) != null) {
                    Log.d("MainActivity-onlinePhase","QUERY: "+app);
                    callServer(app, "QUERY");
                    //for(int time = 0;time<500;time++);/////////////////////////
                    //break;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(reader != null)
                reader.close();
            if(in != null)
                in.close();
        }

    }

    public void callServer(String app, String type) {
        try {
            inetAddress = InetAddress.getByName(IP);
            Log.d("MainActivity-callServer","inetAddress:"+inetAddress.toString());
            socket = new Socket(inetAddress, PORT);
            Utils.sendString(socket, type);
            if (type.equals("DB")) {
                //callMainThread(TOAST_DB_loaded);
                psi.downloadDB(socket);
                //callMainThread(TOAST_DB_loaded);
            } else if (type.equals("QUERY")) {
                if (psi.sendQuery(app, socket)) {
                    Log.d("MainActivity-RESULT FOR PSI","Yes I hava found one! at least");
                    callMainThread(TOAST_Malware);
                } else {
                    Log.d("MainActivity-RESULT FOR PSI","No this is not!");
                    callMainThread(TOAST_NOT_Malware);
                }
            }
            //Utils.sendString(socket, "END");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } /*finally {
            if( socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }*/
    }

    public void byeServer(){
        Utils.sendString(socket, "END");
        if( socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 文件的写入openFileOutput
     *
     * @param filename 文件名称
     * @param data     数据
     * @param mode     模式（追加/覆盖）
     */
    private void dataWriter(String filename, String data, int mode) {

        FileOutputStream out = null;
        BufferedWriter writer = null;
        Log.d("MainActivity-DataWriter",filename);
        try {
            if (mode == Context.MODE_APPEND) {
                out = openFileOutput(filename, Context.MODE_APPEND);
            } else if (mode == Context.MODE_PRIVATE) {
                out = openFileOutput(filename, Context.MODE_PRIVATE);
            }
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 文件的读取openFileInput
     *
     * @param filename 文件名称
     * @return String类型的数据内容
     */
    private Pair<String, Integer> dataReader(String filename) {
        FileInputStream in = null;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        String line = null;
        int lineNum = 0;
        try {
            in = openFileInput(filename);
            reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                content.append(line);
                lineNum++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new Pair<>(content.toString(), lineNum);
    }

}

