package com.cabsoft.rainbowbr.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cabsoft.convenience.CabR;
import com.cabsoft.rainbowbr.components.basic.CabLoadingDialog;
import com.cabsoft.rainbowbr.components.nfilter.CabNFilterComponent;
import com.cabsoft.rainbowbr.property.CabBankAppProperty;
import com.cabsoft.rainbowbr.schememgr.CabWebManager;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.lenddo.data.AndroidData;
import com.lenddo.data.listeners.OnDataSendingCompleteCallback;
import com.lenddo.data.models.ClientOptions;
import com.linkprice.app_interlock.Lpfront;
import com.nshc.nfilter.NFilter;
import com.pacesystem.lib.RecognitionResult;
import com.pacesystem.paceidcardrecog.PreviewActivity;
import com.welcome.scraping.AllScrap;
import com.welcome.scraping.ScrapRunnable;
import com.welcomeloan.mobile.R;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

//import com.kwic.saib.pub.KW_CIPHER_TYPE;
//import com.kwic.saib.pub.KeySecurity;
//import com.kwic.saib.pub.SmartAIB;

/**
 * @author yspark
 * @mainpage Cabsoft BankX Android
 * @brief 본 프로젝트는 스마트폰 네이티브 기능과 보안프로그램의 기능을 <br>
 * 웹뷰를 통해서 원할히 호출하여 시용할 수 있도록 패키징하는 것을 목적으로 한다.<br>
 * @detail 본 프로젝트는 웹뷰를 출력해주는 CabWebActivity, <br>
 * 스키마의 제어를 해주는 CabWebManager, <br>
 * 컴포넌트들의 셋팅과 주요 셋팅의 정보를 맡고 있는 CabBankAppProperty <br>
 * 그리고 각 라이브러리들의 호출과 래핌을 맡고 있는 컴포넌트들로 이루어져 있다. <br>
 */
@SuppressLint("SetJavaScriptEnabled")
public class CabWebActivity extends Activity {

    private final int REQ_PREVIEW = 0;

    private RecognitionResult mRecognitionResult;

    // private ProgressDialog mProgress;
    private CabLoadingDialog mLoadingDialog;
    private CabWebManager mWebManager;
    public WebView mMainWebView;

    private InputMethodManager imm;

    private String lenddoMobildId;

    public final int MY_NEEDED_PERMISSIONS = 1;

    boolean isFirst;

    //adid
    final Adid adid  = new Adid();

    //렌도 백그라운드 실행
    private class MyLenddoTask extends AsyncTask<Void, Integer, Void> {

        private Context context;
        private String mobileId;

        public MyLenddoTask(Context context, String mobileId) {
            this.context = context;
            this.mobileId = mobileId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d("lenddo", "CabWebActivity ::: clientOption create start");
            ClientOptions clientOption = new ClientOptions();
            Log.d("lenddo", "CabWebActivity ::: clientOption create end");
            clientOption.setWifiOnly(false);
            // 아래 url 설정이 바뀌면 데이터 전송이 되지 않음
            clientOption.setApiGatewayUrl("https://gateway-kr.partner-service.link");

            clientOption.enablePhoneNumberHashing();
            clientOption.disableSMSBodyCollection();
            clientOption.enableContactsNameHashing();
            clientOption.enableContactsEmailHashing();
            clientOption.enableCalendarOrganizerHashing();
            clientOption.enableCalendarDisplayNameHashing();
            clientOption.disableLocationDataCollection();
            clientOption.enableLogDisplay(true);

            clientOption.registerDataSendingCompletionCallback(new OnDataSendingCompleteCallback() {

                @Override
                public void onDataSendingSuccess() {
                    Log.d("lenddo", "CabWebActivity ::: lenddo success callback");

//						mMainWebView.post(new Runnable() {
//							@Override
//							public void run() {
//								mMainWebView.loadUrl("javascript:lenddoCallback('Y', 'LENDDO SUCCESS')");
//							}
//						});
                }

                @Override
                public void onDataSendingFailed(final Throwable t) {
                    Log.d("lenddo", "CabWebActivity ::: lenddo sending failed callback");

//						mMainWebView.post(new Runnable() {
//							@Override
//							public void run() {
//								mMainWebView.loadUrl("javascript:lenddoCallback('N', 'LENDDO ERROR.'" + t.getMessage() + ")");
//							}
//						});
                }

                @Override
                public void onDataSendingError(final int statusCode, final String errorMessage) {
                    Log.d("lenddo", "CabWebActivity ::: lenddo sending error callback");
//						mMainWebView.post(new Runnable() {
//							@Override
//							public void run() {
//								mMainWebView.loadUrl("javascript:lenddoCallback('N','LENDDO ERROR. statusCode : '" + String.valueOf(statusCode) + " :: errorMessage : " + errorMessage + ")");
//							}
//						});

                }
            });

            String partnerKey = getResources().getString(com.welcomeloan.mobile.R.string.partner_script_id);
            Log.d("lenddo", "partner script id : " + getString(com.welcomeloan.mobile.R.string.partner_script_id));
            String apiKey = getResources().getString(com.welcomeloan.mobile.R.string.api_secret_key);
            Log.d("lenddo", "api_secret_key : " + getString(com.welcomeloan.mobile.R.string.api_secret_key));

            Log.d("lenddo", "CabWebActivity ::: AndroidData.setup start");

            AndroidData.setup(getApplicationContext(), partnerKey, apiKey, clientOption);
            Log.d("lenddo", "CabWebActivity ::: AndroidData.setup end");

            AndroidData.startAndroidData(CabWebActivity.this, this.mobileId);
            Log.d("lenddo", "CabWebActivity ::: startAndroidData  end");
            return null;
        }
    }

    //ADID 전송을 위한 코드 추가. jjc
    public class Adid {
        String adid;
        public void setAdid(String adid) {
            this.adid = adid;
        }
        public String getAdid() {
            return this.adid;
        }
    }

    private Runnable createScrapResult(final AllScrap allScrap) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                String result = "";
                HashMap<String, String> datas = new HashMap<String, String>();
                String errDoc = "";

                try {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    for (ScrapRunnable sr : allScrap.runnables) {
                        JSONObject obj = new JSONObject(sr.getStrScrapResult());
                        result = obj.getString("errYn");
                        Log.i("all scrap", "scrap result ::: " + sr.getScrapType() + " --- " + obj.toString());
                        if ("N".equals(result)) {
                            datas.put(sr.getScrapType(), obj.toString());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    result = "EXCEPTION";
                }

                mMainWebView.loadUrl("javascript:scrapCallback('" + result + "', '" + datas + "', '" + errDoc + "')");
            }
        };

        return run;
    }

    private Handler handleScrap = new Handler() {

        @SuppressWarnings({"rawtypes"})
        public void handleMessage(Message msg) {
            HashMap map = (HashMap) msg.obj;

            String result = "";
            String data = "";
            String errDoc = "";

            try {

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                JSONObject obj = new JSONObject((String) map.get("rtnMsg"));

                result = obj.getString("RESULT");

                if ("SUCCESS".equals(result)) {
                    data = obj.toString();
                } else {
                    data = obj.getString("ERRMSG");
                    errDoc = obj.getString("ERRDOC");
                }

            } catch (Exception e) {
                e.printStackTrace();

                result = "EXCEPTION";
                data = e.getMessage();
            }

            // longTextLogger("CabWebActivity", data);

            mMainWebView.loadUrl("javascript:scrapCallback('" + result + "', '" + data + "', '" + errDoc + "')");

        }

    };

    private Handler handleScrapBank = new Handler() {

        @SuppressWarnings({"rawtypes"})
        public void handleMessage(Message msg) {
            HashMap map = (HashMap) msg.obj;

            String result = "";
            String data = "";
            String errDoc = "";

            try {

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                Log.d("text", "msg:" + (String) map.get("rtnMsg"));

                JSONObject obj = new JSONObject((String) map.get("rtnMsg"));

                result = obj.getString("RESULT");

                if ("SUCCESS".equals(result)) {
                    data = obj.toString();
                } else {
                    data = obj.getString("ERRMSG");
                    errDoc = obj.getString("ERRDOC");
                }

            } catch (Exception e) {
                e.printStackTrace();

                result = "EXCEPTION";
                data = e.getMessage();
            }

            // longTextLogger("CabWebActivity", data);

            mMainWebView.loadUrl("javascript:scrapCallbackBank1('" + result + "', '" + data + "', '" + errDoc + "')");

        }
    };

    private Handler handleScrapBank2 = new Handler() {

        @SuppressWarnings({"rawtypes"})
        public void handleMessage(Message msg) {
            HashMap map = (HashMap) msg.obj;

            String result = "";
            String data = "";
            String errDoc = "";

            try {

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                Log.d("text", "msg:" + (String) map.get("rtnMsg"));

                JSONObject obj = new JSONObject((String) map.get("rtnMsg"));

                result = obj.getString("RESULT");

                if ("SUCCESS".equals(result)) {
                    data = obj.toString();
                } else {
                    data = obj.getString("ERRMSG");
                    errDoc = obj.getString("ERRDOC");
                }

            } catch (Exception e) {
                e.printStackTrace();

                result = "EXCEPTION";
                data = e.getMessage();
            }

            // longTextLogger("CabWebActivity", data);

            mMainWebView.loadUrl("javascript:scrapCallbackBank2('" + result + "', '" + data + "', '" + errDoc + "')");

        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressLint("JavascriptInterface")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("lenddo", "mainActivity onCreate");

        isFirst = checkAppFirstExecute();
        Log.d("lenddo", "WRITE_EXTERNAL_STORAGE shouldShowrequestPermissionRational : " + isFirst);

        //adid 코드 추가 jjc
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AdvertisingIdClient.Info advertisingIdIdnfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                    if (!advertisingIdIdnfo.isLimitAdTrackingEnabled()) {
                        adid.setAdid(advertisingIdIdnfo.getId());
                        Log.d("ADID", "ADID1 ::: " + advertisingIdIdnfo.getId());
                        Log.d("ADID", "ADID2 ::: " + adid.getAdid());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        if (Build.VERSION.SDK_INT < 23) {
//            Toast.makeText(CabWebActivity.this, "version ::: " + Build.VERSION.SDK_INT, Toast.LENGTH_SHORT).show();
            //TODO
            // SHOW ACTIVITY FOR PERMISSION
            if (isFirst) {
                showLowerVersion();
            } else {
                webViewInit();
            }
        } else {
            if (isFirst) {
                TedPermission.with(CabWebActivity.this.getApplicationContext())
                        .setPermissionListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted() {
                                webViewInit();
                            }

                            @Override
                            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                                checkPermissions();
                            }
                        }).setRationaleMessage("* 필수 권한설명\r\n1.카메라 : 톡톡대출 신청시 사진촬영을 위해 필요합니다\r\n2.통화상태관리 : 상담전화 연결시 필요합니다\r\n3.저장공간 : 공인인증서 및 보안강화를 위한 OS변조 확인에 필요합니다\r\n4.주소록 : 톡톡대출 신청시 정보 입력에 필요합니다\r\n" +
                        "\r\n* 선택 권한설명\r\n1.문자 : 모바일정보 활용 동의고객에 대한 SMS정보 수집을 위해 사용합니다\r\n2.일정 : 모바일정보 활용 동의고객에 대한 일정정보 수집을 위해 사용합니다\r\n\r\n** 필수 권한을 설정하지 않은 경우에는 앱을 이용하실 수 없습니다.")
//                        .setDeniedMessage("필수권한을 허용해야 앱이 실행됩니다. 실행되지 않을 경우 설정에서 필수 권한 허용 후 실행하시면 됩니다.")
                        .setPermissions(Manifest.permission.CAMERA
                                , Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.CALL_PHONE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_SMS, Manifest.permission.READ_CALENDAR
                        )
                        .check();
            } else {
                checkPermissions();
            }


        }
    }

    private void showLowerVersion() {
        new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setMessage("* 필수 권한설명\r\n1.카메라 : 톡톡대출 신청시 사진촬영을 위해 필요합니다\r\n2.통화상태관리 : 상담전화 연결시 필요합니다\r\n3.저장공간 : 공인인증서 및 보안강화를 위한 OS변조 확인에 필요합니다\r\n4.주소록 : 톡톡대출 신청시 정보 입력에 필요합니다\r\n" +
                        "\r\n* 선택 권한설명\r\n1.문자 : 모바일정보 활용 동의고객에 대한 SMS정보 수집을 위해 사용합니다\r\n2.일정 : 모바일정보 활용 동의고객에 대한 일정정보 수집을 위해 사용합니다\r\n\r\n** 필수 권한을 설정하지 않은 경우에는 앱을 이용하실 수 없습니다.")
                .setCancelable(true)
                .setNegativeButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webViewInit();
                    }
                })
                .show();
    }

    private void showSettingActivity() {
        new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setMessage("* 필수권한\r\n(저장공간, 전화, 주소록, 카메라)을 허용해야 앱이 실행됩니다.\r\n설정화면에서 필수권한을 허용해 주세요.")
                .setCancelable(true)
                .setNegativeButton("종료", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton("설정", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }

    private void checkPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<String>();
        boolean isShow = true;
        int falseCnt = 0;
        int trueCnt = 0;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                isShow = ActivityCompat.shouldShowRequestPermissionRationale(CabWebActivity.this, Manifest.permission.CAMERA);
            }
            Log.d("lenddo", "CAMERA shouldShowrequestPermissionRational : " + ActivityCompat.shouldShowRequestPermissionRationale(CabWebActivity.this, Manifest.permission.CAMERA));
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                isShow = isShow && ActivityCompat.shouldShowRequestPermissionRationale(CabWebActivity.this, Manifest.permission.READ_PHONE_STATE);
            }
            Log.d("lenddo", "READ_PHONE_STATE shouldShowrequestPermissionRational : " + ActivityCompat.shouldShowRequestPermissionRationale(CabWebActivity.this, Manifest.permission.READ_PHONE_STATE));
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CALL_PHONE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                isShow = isShow && ActivityCompat.shouldShowRequestPermissionRationale(CabWebActivity.this, Manifest.permission.CALL_PHONE);
            }
            Log.d("lenddo", "CALL_PHONE shouldShowrequestPermissionRational : " + ActivityCompat.shouldShowRequestPermissionRationale(CabWebActivity.this, Manifest.permission.CALL_PHONE));
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                isShow = isShow && ActivityCompat.shouldShowRequestPermissionRationale(CabWebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            Log.d("lenddo", "WRITE_EXTERNAL_STORAGE shouldShowrequestPermissionRational : " + ActivityCompat.shouldShowRequestPermissionRationale(CabWebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE));
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.GET_ACCOUNTS);
        }

        Log.d("lenddo", "final is show values : " + isShow);

        if (!listPermissionsNeeded.isEmpty()) {
            if (!isShow) {
                showSettingActivity();
            } else {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MY_NEEDED_PERMISSIONS);
            }
        } else {
            webViewInit();
        }
    }

    private boolean getNotShowPermissionPref() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        if (pref.getString("PERMISSION_NOT_SHOW", "N").equals("Y")) {
            return true;
        }
        return false;
    }

    private void savePermissionPref() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("PERMISSION_NOT_SHOW", "Y");
        editor.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_NEEDED_PERMISSIONS: {
                if (grantResults.length > 0) {
                    checkPermissions();
                }
            }
        }
    }

    private void webViewInit() {
        Log.d("lenddo", "mainActivity onStart");
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        String strURL = null;
        String strnFilterKey = null;

        Bundle parameters = this.getIntent().getExtras();

        Map<String, Object> params = new HashMap<String, Object>();
        if (parameters != null) {

            // 현재 넘어온 모든 파라메터를 Map으로 정리.
            for (String strKey : parameters.keySet()) {

                // 기본적으로 말려들어오는 com.android.*는 수집하지 않는다.
                if (!strKey.startsWith("com.android")) {

                    Object objValue = parameters.get(strKey);

                    if (objValue instanceof String) {
                        params.put(strKey, (String) objValue);
                    }

                }

            }

            params.remove("url"); // url은 parameter로 넘기지 않는다.
            params.remove("nFilterKey"); // nFilterKey는 parameter로 넘기지 않는다.

            strURL = parameters.getString("url");
            strnFilterKey = parameters.getString("nFilterKey"); // 이전 화면에서
            // nFilter
            // public key가
            // 넘어왔다.

        }

        // 스크린샷 불가능이라면 secure flag를 설정해준다.
        if (!CabBankAppProperty.enableScreenShot) {
            getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);
        }

        setContentView(CabR.getLayoutId(this, "webactivity_layout"));

        // mProgress = new ProgressDialog(this);

        mMainWebView = (WebView) findViewById(CabR.getId(this, "main_webview"));

        mMainWebView.setBackgroundColor(0x000000); // !주의 - 흰색으로 하면 화면이 깜빡이므로
        // 투명으로 해준다.
        WebSettings settings = mMainWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setTextZoom(100); // 시스템에서 설정한 폰트 scale을 무시한다.

        mMainWebView.setWebViewClient(new CabWebViewClient());
        mMainWebView.setWebChromeClient(new CabChromeWebView());
        mMainWebView.addJavascriptInterface(new AndroidBridge(), "android");
        mMainWebView.getSettings().setBuiltInZoomControls(true);
        mMainWebView.getSettings().setDomStorageEnabled(true);

        mMainWebView.addJavascriptInterface(new WebViewJavaScriptInterface(this), "app");

        // 캐쉬를 하지 않도록 막는다.
        mMainWebView.clearCache(true);
        mMainWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        if (!params.isEmpty()) {
            mWebManager = new CabWebManager(this, strURL, params);
        } else {
            mWebManager = new CabWebManager(this, strURL);
        }

        Lpfront.setActivity(this, getIntent());
        SharedPreferences mPref = Lpfront.getPref(this);

        String LPINFO = mPref.getString("LPINFO", "");
        int rd = mPref.getInt("rd", -1);

        if (!TextUtils.isEmpty(LPINFO)) {

            params.put("LPINFO", LPINFO);
            params.put("rd", String.valueOf(rd));

            SharedPreferences.Editor prefEditor = mPref.edit();
            prefEditor.remove("LPINFO");
            prefEditor.remove("rd");
            prefEditor.apply();
        }

        //최초 웹 팝업은 안드로이드 다이얼로그로 대체
        // 앱 최초실행 체크
        //isFirst = checkAppFirstExecute();
//        if (isFirst) {
//            params.put("isFirst", "Y");
//        } else {
        params.put("isFirst", "N");
        params.put("adid","");
//        }

		/*
         * // 저축은행 중앙회는 다음과 같이 할 것 // mWebManager.callHomeURL(mMainWebView);
		 */

        mWebManager.callHomeURL(mMainWebView, params);
        Log.d("adid", "webViewInit mMainWebView URL ::: " + mMainWebView.getUrl());

        // 이전 화면에서 nFilter key가 넘어왔다면
        // 강제로 nFilter component의 action을 호출해준다.
        if (strnFilterKey != null) {

            try {

                strnFilterKey = URLEncoder.encode(strnFilterKey, "utf-8");

                String url = "cab://nfilter/setPublicKey?{\"publicKey\":\"" + strnFilterKey + "\"}";
                mWebManager.checkScheme(url, mMainWebView);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

        mLoadingDialog = new CabLoadingDialog(this);
    }

    public class WebViewJavaScriptInterface {
        private Context context;

        public WebViewJavaScriptInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void callLenddo(String message) {


            /***** LENDDO *****/
            Log.d("lenddo", "CabWebActivity ::: lenddo start");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSS");
            String currentDate = sdf.format(new Date());
            Log.d("lenddo", "CabWebActivity ::: " + currentDate);
            Log.d("lenddo", "CabWebActivity :::  LENDDO MOBILE ID ::: L00080" + currentDate);
            Log.d("lenddo", "CabWebActivity ::: startAndroidData  start");
            lenddoMobildId = "L00080" + currentDate;
            AndroidData.clear(this.context);
            try {
                MyLenddoTask lenddoTask = new MyLenddoTask(this.context, lenddoMobildId);
                lenddoTask.execute();
            } catch (Exception e) {
                Log.d("lenddo", e.getMessage());
            }
            mMainWebView.post(new Runnable() {
                @Override
                public void run() {
                    mMainWebView.loadUrl("javascript:lenddoCallback('" + lenddoMobildId + "')");
                }
            });
            //mMainWebView.loadUrl("javascript:lenddoCallback('Y', 'LENDDO SUCCESS')");
        }

        //하이브리드 앱 화면에서 adid를 가져올 수 있는 함수 추가
        @JavascriptInterface
        public void getAdid() {
            Log.d("ADID", "CALL GETADID()");
            mMainWebView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("ADID", "ADID3 ::: " + adid.getAdid());
                    mMainWebView.loadUrl("javascript:adidCallBack('" + adid.getAdid() + "')");
                }
            });
        }
    }

    /**
     * 앱 최초실행여부 확인
     */
    public boolean checkAppFirstExecute() {
        SharedPreferences pref = getSharedPreferences("isFirst", Activity.MODE_PRIVATE);
        boolean isFirst = pref.getBoolean("isFirst", true);
        if (isFirst) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("isFirst", false);
            editor.commit();
        }
        return isFirst;
    }

    /**
     * 일반적인 실행이 아닌 푸쉬등으로 들어오는 경우 이쪽으로 이동한다. 기존의 셋팅부분을 다시 셋팅하여준다.
     */
    @Override
    protected void onNewIntent(Intent intent) {

        String strURL = null;
        String strnFilterKey = null;

        Bundle parameters = intent.getExtras();

        Map<String, Object> params = new HashMap<String, Object>();
        if (parameters != null) {

            // 현재 넘어온 모든 파라메터를 HashMap으로 정리.
            for (String strKey : parameters.keySet()) {

                // 기본적으로 말려들어오는 com.android.*는 수집하지 않는다.
                if (!strKey.startsWith("com.android")) {

                    Object objValue = parameters.get(strKey);

                    if (objValue instanceof String) {
                        params.put(strKey, (String) objValue);
                    }

                }

            }

            params.remove("url"); // url은 parameter로 넘기지 않는다.
            params.remove("nFilterKey"); // nFilterKey는 parameter로 넘기지 않는다.

            strURL = parameters.getString("url");
            strnFilterKey = parameters.getString("nFilterKey"); // 이전 화면에서
            // nFilter
            // public key가
            // 넘어왔다.

        }

        if (!params.isEmpty()) {
            mWebManager = new CabWebManager(this, strURL, params);
        } else {
            mWebManager = new CabWebManager(this, strURL);
        }

		/*
         * // 저축은행 중앙회는 다음과 같이 할 것 // mWebManager.callHomeURL(mMainWebView);
		 */

        mWebManager.callHomeURL(mMainWebView, params);

        // 이전 화면에서 key가 넘어왔다면
        // 강제로 nFilter component의 action을 호출해준다.
        if (strnFilterKey != null) {

            try {
                strnFilterKey = URLEncoder.encode(strnFilterKey, "utf-8");

                String url = "cab://nfilter/setPublicKey?{\"publicKey\":\"" + strnFilterKey + "\"}";
                mWebManager.checkScheme(url, mMainWebView);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

        super.onNewIntent(intent);
    }

    /**
     * 안드로이드 전용 Bridge
     *
     * @author kangseungchul
     */
    private class AndroidBridge {

        Handler handler = new Handler();

        @JavascriptInterface
        public void setBasicInstanceName(final String arg1, final String arg2) {

            handler.post(new Runnable() {

                public void run() {

                    String strFormat = "%s://basic/setInstanceName?{\"instanceName\":\"%s\"}";
                    String strArg2 = arg2.replace("\"", "\\\"");

                    String url = null;
                    url = String.format(strFormat, arg1, strArg2);
                    mWebManager.checkScheme(url, mMainWebView);

                }

            });

        }
    }

    /**
     * @author Cabsoft
     */
    private class CabChromeWebView extends WebChromeClient {
        @Override
        public boolean onJsAlert(final WebView view, String url, String message, final JsResult result) {
            new AlertDialog.Builder(CabWebActivity.this).setMessage(message).setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    result.confirm();
                }
            }).create().show();
            return true;
        }

        ;

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            new AlertDialog.Builder(CabWebActivity.this).setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            }).setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface arg0) {
                    result.cancel();

                }
            }).create().show();
            return true;
        }

        ;
    }

    private String getPaceUrlParam(String url, String key) {
        String value = "";

        int paramStart = url.indexOf("?");

        if (paramStart == -1) {
            return value;
        }

        int keyIdx = url.indexOf(key + "=", paramStart);

        if (keyIdx > -1) {

            int start = keyIdx + key.length() + 1;
            int end = url.indexOf("&", start);

            if (end == -1) {
                value = url.substring(start, url.length());
            } else {
                value = url.substring(start, end);
            }
        }

        return value;
    }

    /**
     * @author Cabsoft
     */
    private class CabWebViewClient extends WebViewClient {

        @SuppressLint("HandlerLeak")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

			/* 톡톡대출 관련 인터페이스 */
            if (url.startsWith("pace://")) {

                if (url.contains("preview")) { // 신분증 촬영

                    int manualDelay = 30000;

                    String strDelay = getPaceUrlParam(url, "manual");

                    if (!TextUtils.isEmpty(strDelay) && TextUtils.isDigitsOnly(strDelay)) {
                        manualDelay = Integer.parseInt(strDelay);
                    }

                    Intent intent = new Intent(CabWebActivity.this, PreviewActivity.class);
                    intent.putExtra("menualDelay", manualDelay);
                    startActivityForResult(intent, REQ_PREVIEW);

                } else if (url.contains("result")) { // 신분증 인식 결과 & 신분증 이미지 반환

                    if (mRecognitionResult != null) {

                        // String imgPath =
                        // mRecognitionResult.cardImgPath;//data.getStringExtra(PreviewActivity.RES_CARD_IMG);
                        String strImg = Base64.encodeToString(mRecognitionResult.arrCardImg, Base64.NO_WRAP);

                        // result : 성공,실패
                        // type : 신분증 종류
                        // name : 이름
                        // jumin : 주민등록번호
                        // issue date : 주민등록증 발급일자
                        // lisencenumber : 운전면허번호
                        // squence : 암호일련번호
                        // impgBase64 : 촬영 이미지

                        String publish = mRecognitionResult.publish == null ? "" : mRecognitionResult.publish;
                        String licenseNumber = mRecognitionResult.licenseNumber == null ? "" : mRecognitionResult.licenseNumber;
                        String licenseCheckDigit = mRecognitionResult.licenseCheckDigit == null ? "" : mRecognitionResult.licenseCheckDigit;

                        String scriptParam = "true, '" + mRecognitionResult.type + "', '" + mRecognitionResult.name + "', '" + mRecognitionResult.idNumber1 + "', '" + mRecognitionResult.idNumber2
                                + "', '" + publish + "', '" + licenseNumber + "', '" + licenseCheckDigit + "', '" + strImg + "'";

                        mMainWebView.loadUrl("javascript:resultCallback(" + scriptParam + ")");

                        mRecognitionResult = null;

                    } else {

                        mMainWebView.loadUrl("javascript:resultCallback(false)");

                    }

                } else if (url.contains("telno")) { // 폰 전화번호 반환
                    TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

                    String strTelno = tm.getLine1Number();

                    if (strTelno != null) {
                        strTelno = strTelno.replace("+82", "0");
                    } else {
                        strTelno = "";
                    }

                    mMainWebView.loadUrl("javascript:telnoCallback('" + strTelno + "')");
                } else if (url.contains("hide_keypad")) { // 소프트 키패드 닫기

                    View focusView = getCurrentFocus();
                    if (focusView != null) {
                        imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
                    }

                } else if (url.contains("decryptStringForId")) { // 보안키패드 평문 추출

                    String plainTxt = "";
                    String id = getPaceUrlParam(url, "id");

                    mMainWebView.loadUrl("javascript:decryptStringForIdCallback('" + plainTxt + "', '" + id + "')");

                } else if (url.contains("pre_scrap")) { // ios 호환용 더미

                    String idx = getPaceUrlParam(url, "certidx");

                    System.out.println("pre_scrap : " + idx);

                    mMainWebView.loadUrl("javascript:preScrapCallback('SUCCESS', '')");

                } else if (url.contains("scrap_bank1")) { // ios 호환용 더미
                    final JSONObject json = new JSONObject();

                    try {
                        final HashMap<String, HashMap<String, Object>> mapRoot = new HashMap<String, HashMap<String, Object>>();
                        CabNFilterComponent nfilter = (CabNFilterComponent) mWebManager.getComponents("nfilter");

                        final String nfilterPublicKey = nfilter.getPublishKey();
                        final String nfilterCoworkKey = NFilter.COWORKER_CODE;

                        String aesenc = nfilter.aesencDataForId("certificatePass");
                        Log.i("TEST", "aesenc:" + aesenc);

                        HashMap<String, Object> encMap = new HashMap<String, Object>();
//                        encMap.put("encType", KW_CIPHER_TYPE.KW_CIPHER_SEED128);
                        encMap.put("encKey", "welcomeloan_kwic");
                        // mapRoot.put("JUMIN", encMap);
                        mapRoot.put("NUMBER", encMap);

                        String decUrl = URLDecoder.decode(url, "UTF-8");
                        String encPwd = getPaceUrlParam(decUrl, "passwd");

                        // NFilter nFilter = new NFilter(CabWebActivity.this);
                        // nFilter.setPublicKey("");
                        /*
                         * byte[] decPwd =
						 * NFilterUtils.getInstance().nSaferDecrypt(encPwd);
						 * String decPwdStr = new String(decPwd);
						 * Log.i("TEST","decPwdStr:"+encPwd);
						 * Log.i("TEST","decPwdStr:"+decPwdStr);
						 * Log.i("TEST","encURL:"+getPaceUrlParam(decUrl,
						 * "encUrl"));
						 */
                        final String nFilterPubKey = getPaceUrlParam(decUrl, "publickey");
                        final String nFilterCoworkKey = getPaceUrlParam(decUrl, "coworkkey");
                        // Log.i("TEST","nFilter public key:"+ nFilterPubKey);
                        // Log.i("TEST","nFilter cowork key:"+
                        // nFilterCoworkKey);
                        /*
                         * String decNum =
						 * Crypto.decrypt(getPaceUrlParam(decUrl,
						 * "number").replaceAll(" ", "+"), "welcomeloan_kwic",
						 * "utf-8"); String decJumin =
						 * Crypto.decrypt(getPaceUrlParam(decUrl, "jumin"),
						 * "welcomeloan_kwic", "utf-8");
						 * Log.i("text","decNum:"+decNum);
						 * Log.i("text","decJumin:"+decJumin);
						 */
                        String passwd = getPaceUrlParam(decUrl, "passwd");

                        // Log.i("TEST","decurl: "+ decUrl);
                        // Log.i("TEST","jumin:"+getPaceUrlParam(decUrl,
                        // "jumin"));
                        json.put("ORG", "1001"); /* 서비스분류코드 */
                        json.put("FCODE", getPaceUrlParam(decUrl, "code")); /* 서비스분류명칭 */
                        json.put("CERTKEY", "13FCAE3400^ffmBA3Dk/5VERFwg"); /*
                                                                             * 인증키(
																			 * 라이센스키
																			 * )
																			 */
                        json.put("MODULE", "3"); /* 서비스구분코드 : 가입증명서 + 보험료납부내역 */
                        json.put("CERTNAME", getPaceUrlParam(decUrl, "certnm")); /* 인증서명 */
                        json.put("CERTPWD", aesenc); // getPaceUrlParam(decUrl,
                        // "passwd"));
                        // /*인증서비밀번호*/
                        json.put("JUMIN", getPaceUrlParam(decUrl, "jumin")); /* 주민번호 */

                        json.put("STARTDATE", getPaceUrlParam(decUrl, "startdate")); /* 조회시작일자 */
                        json.put("ENDDATE", getPaceUrlParam(decUrl, "enddate")); /* 조회종료일자 */
                        // json.put("ENCKEY", getPaceUrlParam(decUrl,
                        // "encKey")); /*구간 암호화 키 값*/
                        // json.put("ENCURL", getPaceUrlParam(decUrl,
                        // "encUrl")); /*구간 암호화 전송 URL*/
                        json.put("INXECURE", "Y");
                        json.put("NUMBER", getPaceUrlParam(decUrl, "number").replaceAll(" ", "+")); /* 계좌번호 */
                        json.put("LOGINMETHOD", getPaceUrlParam(decUrl, "loginmethod"));
                        json.put("CUS_KIND", getPaceUrlParam(decUrl, "cus_kind")); /* 계좌번호 */

                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        Log.i("text", "json:" + json.toString());
                        handleScrap.postDelayed(new Runnable() {

                            @Override
                            public void run() {
//                                SmartAIB smartAIB = new SmartAIB(CabWebActivity.this, handleScrapBank);// .AIB_Execute(json.toString());
//                                smartAIB.setAIBLog(true);
//                                smartAIB.setCipherInfoDict_for_input(mapRoot);
//                                /** nFilter 키보드보안으로 인증서비밀번호를 암호화할 경우 */
//                                smartAIB.setKeySecurity(KeySecurity.KEY_SECURITY_NFILTER_STANDALONE, new String[]{nfilterPublicKey, nfilterCoworkKey});
//
//                                smartAIB.AIB_Execute(json.toString());
                            }

                        }, 1000);

                    } catch (Exception e) {
                        e.printStackTrace();

                        mMainWebView.loadUrl("javascript:scrapCallback('FAIL', '처리중 오류가 발생했습니다.')");
                    }

                } else if (url.contains("scrap_bank2")) { // ios 호환용 더미
                    final JSONObject json = new JSONObject();

                    try {
                        final HashMap<String, HashMap<String, Object>> mapRoot = new HashMap<String, HashMap<String, Object>>();
                        CabNFilterComponent nfilter = (CabNFilterComponent) mWebManager.getComponents("nfilter");

                        final String nfilterPublicKey = nfilter.getPublishKey();
                        final String nfilterCoworkKey = NFilter.COWORKER_CODE;

                        String aesenc = nfilter.aesencDataForId("certificatePass");
                        Log.i("TEST", "aesenc:" + aesenc);

                        HashMap<String, Object> encMap = new HashMap<String, Object>();
//                        encMap.put("encType", KW_CIPHER_TYPE.KW_CIPHER_SEED128);
                        encMap.put("encKey", "welcomeloan_kwic");
                        // mapRoot.put("JUMIN", encMap);
                        mapRoot.put("NUMBER", encMap);

                        Log.i("TEST", "nfilterPublicKey!!!!:" + nfilter.getPublishKey());
                        Log.i("TEST", "mStrnFilterKey!!!!:" + nfilterPublicKey);
                        Log.i("TEST", "nfilterCoworkKey!!!!:" + NFilter.COWORKER_CODE);
                        String decUrl = URLDecoder.decode(url, "UTF-8");
                        Log.i("TEST", "decUrl:" + decUrl);
                        String encPwd = getPaceUrlParam(decUrl, "passwd");

                        // NFilter nFilter = new NFilter(CabWebActivity.this);
                        // nFilter.setPublicKey("");
                        /*
                         * byte[] decPwd =
						 * NFilterUtils.getInstance().nSaferDecrypt(encPwd);
						 * String decPwdStr = new String(decPwd);
						 * Log.i("TEST","decPwdStr:"+encPwd);
						 * Log.i("TEST","decPwdStr:"+decPwdStr);
						 * Log.i("TEST","encURL:"+getPaceUrlParam(decUrl,
						 * "encUrl"));
						 */
                        final String nFilterPubKey = getPaceUrlParam(decUrl, "publickey");
                        final String nFilterCoworkKey = getPaceUrlParam(decUrl, "coworkkey");
                        // Log.i("TEST","nFilter public key:"+ nFilterPubKey);
                        // Log.i("TEST","nFilter cowork key:"+
                        // nFilterCoworkKey);
                        /*
                         * String decNum =
						 * Crypto.decrypt(getPaceUrlParam(decUrl,
						 * "number").replaceAll(" ", "+"), "welcomeloan_kwic",
						 * "utf-8"); String decJumin =
						 * Crypto.decrypt(getPaceUrlParam(decUrl, "jumin"),
						 * "welcomeloan_kwic", "utf-8");
						 * Log.i("text","decNum:"+decNum);
						 * Log.i("text","decJumin:"+decJumin);
						 */
                        String passwd = getPaceUrlParam(decUrl, "passwd");

                        Log.i("TEST", "decurl: " + decUrl);
                        Log.i("TEST", "jumin:" + getPaceUrlParam(decUrl, "jumin"));
                        json.put("ORG", "1001"); /* 서비스분류코드 */
                        json.put("FCODE", getPaceUrlParam(decUrl, "code")); /* 서비스분류명칭 */
                        json.put("CERTKEY", "13FCAE3400^ffmBA3Dk/5VERFwg"); /*
                                                                             * 인증키(
																			 * 라이센스키
																			 * )
																			 */
                        json.put("MODULE", "3"); /* 서비스구분코드 : 가입증명서 + 보험료납부내역 */
                        json.put("CERTNAME", getPaceUrlParam(decUrl, "certnm")); /* 인증서명 */
                        json.put("CERTPWD", aesenc); // getPaceUrlParam(decUrl,
                        // "passwd"));
                        // /*인증서비밀번호*/
                        json.put("JUMIN", getPaceUrlParam(decUrl, "jumin")); /* 주민번호 */

                        json.put("STARTDATE", getPaceUrlParam(decUrl, "startdate")); /* 조회시작일자 */
                        json.put("ENDDATE", getPaceUrlParam(decUrl, "enddate")); /* 조회종료일자 */
                        // json.put("ENCKEY", getPaceUrlParam(decUrl,
                        // "encKey")); /*구간 암호화 키 값*/
                        // json.put("ENCURL", getPaceUrlParam(decUrl,
                        // "encUrl")); /*구간 암호화 전송 URL*/
                        json.put("INXECURE", "Y");
                        json.put("NUMBER", getPaceUrlParam(decUrl, "number").replaceAll(" ", "+")); /* 계좌번호 */
                        json.put("LOGINMETHOD", getPaceUrlParam(decUrl, "loginmethod"));
                        json.put("CUS_KIND", getPaceUrlParam(decUrl, "cus_kind")); /* 계좌번호 */

                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        Log.i("text", "json:" + json.toString());
                        handleScrap.postDelayed(new Runnable() {

                            @Override
                            public void run() {
//                                SmartAIB smartAIB = new SmartAIB(CabWebActivity.this, handleScrapBank2);// .AIB_Execute(json.toString());
//                                smartAIB.setAIBLog(true);
//                                smartAIB.setCipherInfoDict_for_input(mapRoot);
//                                /** nFilter 키보드보안으로 인증서비밀번호를 암호화할 경우 */
//                                smartAIB.setKeySecurity(KeySecurity.KEY_SECURITY_NFILTER_STANDALONE, new String[]{nfilterPublicKey, nfilterCoworkKey});
//
//                                smartAIB.AIB_Execute(json.toString());
                            }

                        }, 1000);

                    } catch (Exception e) {
                        e.printStackTrace();

                        mMainWebView.loadUrl("javascript:scrapCallback('FAIL', '처리중 오류가 발생했습니다.')");
                    }

                } else if (url.contains("scrap_nhis")) { // ios 호환용 더미
                    final JSONObject json = new JSONObject();

                    try {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        final String decUrl = URLDecoder.decode(url, "UTF-8");
                        Log.i("scrap_nhis", "decUrl:" + decUrl);
                        //인증서 정보 암호화
                        CabNFilterComponent nfilter = (CabNFilterComponent) mWebManager.getComponents("nfilter");

                        // TODO: 2018. 2. 1. 동의 시 상품 상태를 보고 어떤 스크래핑을 돌려야 될지 정해줘야됨.
                        // 임시 데이터 넣기
                        ArrayList<String> scrapTypes = new ArrayList<String>();
                        scrapTypes.add(0, "NHIS");
                        scrapTypes.add(1, "BANK");
                        scrapTypes.add(2, "ONNARA");

                        final AllScrap allScrap = new AllScrap(decUrl, nfilter, scrapTypes, CabWebActivity.this);
                        allScrap.runScrap();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (true) {
                                    boolean allDone = true;
                                    for(Future<?> future : allScrap.futures){
                                        allDone &= future.isDone(); // check if future is done
                                    }
                                    if (allDone) {
                                        Runnable uiCall = createScrapResult(allScrap);
                                        runOnUiThread(uiCall);
                                        break;
                                    }
                                }
                            }
                        }).start();
                    } catch (Exception e) {
                        Log.e("scrap", "nhis scrap exception ::: " + e.getMessage());
                        e.printStackTrace();

                        mMainWebView.loadUrl("javascript:scrapCallback('FAIL', '처리중 오류가 발생했습니다.')");
                    }

                } else if (url.contains("scrap")) { // 건강보험 스크래핑

                    final JSONObject json = new JSONObject();

                    try {
                        final HashMap<String, HashMap<String, Object>> mapRoot = new HashMap<String, HashMap<String, Object>>();

                        HashMap<String, Object> encMap = new HashMap<String, Object>();
//                        encMap.put("encType", KW_CIPHER_TYPE.KW_CIPHER_SEED128);
                        encMap.put("encKey", "welcomeloan_kwic");
                        mapRoot.put("JUMIN", encMap);
                        /*
                         * obj1.put("CERTPWD","27bf3854545ace2f802ea4a1e4b4d373")
						 * ; // 키보드 보안 프로그램에서 나온 패스워드 암호화된 값
						 * obj1.put("NFILTER_PUBLICKEY",
						 * "MDIwGhMABBYDB451twFPYO8akZ7nnHLogeYE645rBBTyTA5NvX3xZcVpGSpHborXJHAhvA=="
						 * ); // NFilter 서버에서 받은 Public Key
						 * obj1.put("NFILTER_COWORKKEY"
						 * ,"95428f9e76bc7c50532c582d097b23b7"); // NFilter API를
						 * 통하여 얻을 수 있는 CoworkerCode
						 */

                        CabNFilterComponent nfilter = (CabNFilterComponent) mWebManager.getComponents("nfilter");

                        final String nfilterPublicKey = nfilter.getPublishKey();
                        final String nfilterCoworkKey = NFilter.COWORKER_CODE;
                        // String nfilterPublicKey = nfilter.getPublishKey();
                        // String nfilterCoworkKey = NFilter.COWORKER_CODE;

                        String aesenc = nfilter.aesencDataForId("certificatePass");

                        String encJumin = getPaceUrlParam(url, "jumin");

                        String decUrl = URLDecoder.decode(url, "UTF-8");

                        json.put("ORG", "62"); /* 서비스분류코드 */
                        json.put("FCODE", getPaceUrlParam(decUrl, "code")); /* 서비스분류명칭 */
                        json.put("CERTKEY", "13FCAE3400^ffmBA3Dk/5VERFwg"); /*
                                                                             * 인증키(
																			 * 라이센스키
																			 * )
																			 */
                        // json.put("MODULE", "1,2"); /*서비스구분코드 : 가입증명서 +
                        // 보험료납부내역*/
                        json.put("MODULE", "2,3"); /* 서비스구분코드 : 가입증명서 + 보험료납부내역 */
                        json.put("CERTNAME", getPaceUrlParam(decUrl, "certnm")); /* 인증서명 */
                        json.put("CERTPWD", aesenc/*
                                                 * getPaceUrlParam(decUrl,
												 * "passwd")
												 */); /* 인증서비밀번호 */
                        json.put("JUMIN", encJumin); /* 주민번호 */
                        json.put("STARTDATE", getPaceUrlParam(decUrl, "startdate")); /* 조회시작일자 */
                        json.put("ENDDATE", getPaceUrlParam(decUrl, "enddate")); /* 조회종료일자 */
                        // json.put("ENCKEY", getPaceUrlParam(decUrl,
                        // "encKey")); /*구간 암호화 키 값*/
                        // json.put("ENCURL", getPaceUrlParam(decUrl,
                        // "encUrl")); /*구간 암호화 전송 URL*/
                        json.put("FAXNUM", getPaceUrlParam(decUrl, "faxnum")); /* 조회종료일자 */
                        json.put("INXECURE", "Y");
                        // json.put("NFILTER_PUBLICKEY", nfilterPublicKey);
                        // json.put("NFILTER_COWORKKEY", nfilterCoworkKey);

                        final String strJson = json.toString().replace("\\/", "/");

                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        handleScrap.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                // new SmartAIB(CabWebActivity.this,
                                // handleScrap).AIB_Execute(strJson);
//                                SmartAIB smartAIB = new SmartAIB(CabWebActivity.this, handleScrap);// .AIB_Execute(json.toString());
//                                smartAIB.setAIBLog(true);
//                                smartAIB.setCipherInfoDict_for_input(mapRoot);
//                                /** nFilter 키보드보안으로 인증서비밀번호를 암호화할 경우 */
//                                smartAIB.setKeySecurity(KeySecurity.KEY_SECURITY_NFILTER_STANDALONE, new String[]{nfilterPublicKey, nfilterCoworkKey});
//
//                                smartAIB.AIB_Execute(json.toString());

                            }

                        }, 1000);

                    } catch (Exception e) {
                        e.printStackTrace();

                        mMainWebView.loadUrl("javascript:scrapCallback('FAIL', '처리중 오류가 발생했습니다.')");
                    }

                } else if (url.contains("doLenddo")) {

                }

                return true;
            } else {
                return mWebManager.checkScheme(url, view);
            }
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // if(mProgress.isShowing()){
            // mProgress.hide();
            // }
            if (mLoadingDialog.isShowing()) {
                mLoadingDialog.hide();
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            // mProgress.setCancelable(false);

            CabWebActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    if (!mLoadingDialog.isShowing()) {
                        mLoadingDialog.show();
                    }
                }
            });
        }

        @Override
        public void onReceivedError(final WebView view, int errorCode, String description, String failingUrl) {
            Log.e("cabsoft", failingUrl);
            mWebManager.callErrorURL(view);
        }
        /*
         * @Override public void onReceivedSslError(WebView view,
		 * SslErrorHandler handler, SslError error) { handler.proceed(); }
		 */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQ_PREVIEW) {

            if (resultCode == RESULT_OK) {

                BaseApplication baseApp = (BaseApplication) getApplication();

                mRecognitionResult = baseApp.getRecognitionResult();
                baseApp.setRecognitionResult(null);

                mMainWebView.loadUrl("javascript:previewCallback(true)");
            } else {
                mMainWebView.loadUrl("javascript:previewCallback(false)");
            }

        }

        if (!mWebManager.onActivityResult(requestCode, resultCode, data, mMainWebView)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (!mWebManager.onBackPressed(mMainWebView)) {
            super.onBackPressed();
        }
    }

    private byte[] loadImageByte(String path, boolean isDelete) {

        byte[] arrImage = null;
        ByteArrayOutputStream baos = null;
        FileInputStream fis = null;

        File filePath = null;

        try {
            filePath = new File(path);

            if (filePath.exists()) {

                byte[] buffer = new byte[8192];

                baos = new ByteArrayOutputStream();
                fis = new FileInputStream(filePath);

                int read = -1;
                while ((read = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                arrImage = baos.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (baos != null)
                try {
                    baos.close();
                } catch (IOException e) {
                }
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                }
        }

        if (arrImage != null && isDelete) {
            filePath.delete();
        }

        return arrImage;
    }

    private void longTextLogger(String tag, String str) {

        if (str.length() > 3000) {
            Log.w(tag, str.substring(0, 3000));

            longTextLogger(tag, str.substring(3000, str.length()));
        } else {
            Log.w(tag, str);
        }
    }

}
