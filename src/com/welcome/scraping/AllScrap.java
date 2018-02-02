package com.welcome.scraping;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.cabsoft.rainbowbr.components.nfilter.CabNFilterComponent;
import com.infotech.IFTCrypto.iftCoreEnV2;
import com.nshc.nfilter.NFilter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Created by welcomeloan on 2018. 2. 1..
 */

//infoTech 은행 스크래핑을 위한 enum
enum BankCode {
    BANK_KDB        {
        public String getCode() {return "002";}
        public String getName() {return "산업은행";}
    },
    BANK_IBK        {
        public String getCode() {return "003";}
        public String getName() {return "기업은행";}
    },
    BANK_KB         {
        public String getCode() {return "004";}
        public String getName() {return "국민은행";}
    },
    BANK_SUHYUP     {
        public String getCode() {return "007";}
        public String getName() {return "수협은행";}
    },
    BANK_NH         {
        public String getCode() {return "011";}
        public String getName() {return "농협은행";}
    },
    BANK_WOORI      {
        public String getCode() {return "020";}
        public String getName() {return "우리은행";}
    },
    BANK_SC         {
        public String getCode() {return "023";}
        public String getName() {return "SC은행";}
    },
    BANK_CITY       {
        public String getCode() {return "027";}
        public String getName() {return "씨티은행";}
    },
    BANK_DGB        {
        public String getCode() {return "031";}
        public String getName() {return "대구은행";}
    },
    BANK_BUSAN      {
        public String getCode() {return "032";}
        public String getName() {return "부산은행";}
    },
    BANK_KJ         {
        public String getCode() {return "034";}
        public String getName() {return "광주은행";}
    },
    BANK_JEJU      {
        public String getCode() {return "035";}
        public String getName() {return "제은행";}
    },
    BANK_JB         {
        public String getCode() {return "037";}
        public String getName() {return "전북은행";}
    },
    BANK_KN         {
        public String getCode() {return "039";}
        public String getName() {return "경남은행";}
    },
    BANK_MG         {
        public String getCode() {return "045";}
        public String getName() {return "새마을금고";}
    },
    BANK_SINHYUP    {
        public String getCode() {return "048";}
        public String getName() {return "신협";}
    },
    BANK_EPOST      {
        public String getCode() {return "079";}
        public String getName() {return "우체국";}
    },
    BANK_KEB        {
        public String getCode() {return "081";}
        public String getName() {return "하나은행";}
    },
    BANK_SHINHAN    {
        public String getCode() {return "088";}
        public String getName() {return "신한은행";}
    },
    BANK_KBANK      {
        public String getCode() {return "089";}
        public String getName() {return "K뱅크";}
    },
    BANK_KAKAO      {
        public String getCode() {return "090";}
        public String getName() {return "카카오뱅크";}
    },
    BANK_DAISHIN    {
        public String getCode() {return "267";}
        public String getName() {return "대신증권";}
    },
    BANK_NHQV       {
        public String getCode() {return "289";}
        public String getName() {return "NH투자증권";}
    },
    BANK_SMPOP      {
        public String getCode() {return "240";}
        public String getName() {return "삼성증권";}
    },
    BANK_MERITZ     {
        public String getCode() {return "287";}
        public String getName() {return "메리츠증권";}
    },
    BANK_SHINVEST   {
        public String getCode() {return "278";}
        public String getName() {return "신한투자증권";}
    };

    public String getCode() {
        return this.getCode();
    }

    public String getName() {
        return this.getName();
    }
}

public class AllScrap {
    private final String decUrl;
    private CabNFilterComponent nfilter;
    private ArrayList<String> scrapTypes;

    public HashMap<String, String> mapScrapOutput = new HashMap<String, String>();

    public List<Future<?>> futures = new ArrayList<Future<?>>();
    public List<ScrapRunnable> runnables = new ArrayList<ScrapRunnable>();

    private String fromDate;    //조회 시작일
    private String toDate;      //조회 종료일
    private String certNm;      //인증서 이름
    private String certPw;      //인증서 패스워드
    private String bizEndNo;    //암호화된 주번
    private String bizNo;       //복호화된 주번

    private ExecutorService executeService;

    private iftCoreEnV2 iftEncPw = null;

    private Activity activity;

    public AllScrap(String decUrl, CabNFilterComponent nfilter, ArrayList<String> scrapTypes, Activity activity) {
        this.decUrl = decUrl;
        this.nfilter = nfilter;
        this.scrapTypes = scrapTypes;
        this.activity = activity;

        init();
    }

    private void init() {
        this.iftEncPw = new iftCoreEnV2(null, null);
        this.bizEndNo = iftEncPw.iftEncPrarm(iftEncPw.iftDecryptExt("welcomeloan_kwic", getPaceUrlParam(decUrl, "jumin")));
        this.bizNo = iftEncPw.iftDecryptExt("welcomeloan_kwic", getPaceUrlParam(decUrl, "jumin"));
        Log.i("","");
        //this.bizEndNo = iftEncPw.iftEncPrarm(Crypto.decrypt(getPaceUrlParam(decUrl, "jumin"), "welcomeloan_kwic", "utf-8"));

        final String nfilterPublicKey = nfilter.getPublishKey();
        final String nfilterCoworkKey = NFilter.COWORKER_CODE;
        String aesenc = nfilter.aesencDataForId("certificatePass");
        this.certPw = iftEncPw.iftEncPrarm(iftEncPw.nFilterPassword(aesenc, nfilterPublicKey, nfilterCoworkKey));

        this.certNm = Base64.encodeToString(getPaceUrlParam(decUrl, "certnm").getBytes(), Base64.DEFAULT);
        this.fromDate = getPaceUrlParam(this.decUrl, "startdate");
        this.toDate = getPaceUrlParam(this.decUrl, "enddate");

        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = new Thread(r);
                return thread;
            }
        };

        int thCnt = calCountScrapTypes();
        executeService = Executors.newFixedThreadPool(thCnt, tf);

    }

    private int calCountScrapTypes() {
        int cnt = this.scrapTypes.size();
        if (this.scrapTypes.contains("BANK")) {
            cnt = cnt - 1 + 20;
        }
        return cnt;
    }

    public void runScrap() throws Exception {
        for (String scrapType : scrapTypes) {
            JSONObject inputObj;
            if (scrapType.equals("BANK")) {
                for (final BankCode bc : BankCode.values()) {
                    inputObj = this.getJsonObjectForScrap(scrapType, bc.getCode());
                    Log.i("AllScrap", "Scrap Object ::: " + inputObj.toString());
                    ScrapRunnable sr = new ScrapRunnable(inputObj, scrapType+bc.getCode(), this.activity);
//                    new Thread(sr).start();
                    runnables.add(sr);
                    futures.add(executeService.submit(sr));
                }
            } else {
                inputObj = this.getJsonObjectForScrap(scrapType, "");
                Log.i("AllScrap", "Scrap Object ::: " + inputObj.toString());
                ScrapRunnable sr = new ScrapRunnable(inputObj, scrapType, this.activity);
                //new Thread(sr).start();
                runnables.add(sr);
                futures.add(executeService.submit(sr));
            }
        }
    }

    //InfoTech Scraping을 위해 Input Json Object를 생성.
    private JSONObject getJsonObjectForScrap(String scrapType, String bankCode) throws Exception {
        //default options
        final JSONObject obj = new JSONObject();
        obj.put("appCd", "com.welcomeloan.mobile");
        obj.put("bizEncNo", this.bizEndNo);

        obj.put("certNm", this.certNm);
        obj.put("signEncPw",this.certPw);

        switch (scrapType) {
            case "NHIS":    //건강보험
                obj.put("svcCd", "B0001,B0002");
                obj.put("orgCd", "nhic");
                obj.put("devMode","R");
                obj.put("fromDate", this.fromDate);
                obj.put("toDate", this.toDate);
                break;

            case "BANK":    //은행내역
                obj.put("svcCd", "B0001");
                obj.put("orgCd", "bank");
                obj.put("bankCd", bankCode);
                obj.put("useChannel", "0");
                obj.put("loginMethod", "CERT");
                obj.put("curCd", "KRW");
                obj.put("sdate", this.fromDate);
                obj.put("edate", this.toDate);
                break;
            case "IROS":    //확정일자
                obj.put("orgCd", "iros");
                obj.put("svcCd", "B2001");
                obj.put("svcDivCd", "VIEW1");
                break;
            case "ONNARA":  //온나라 토지찾기
                obj.put("orgCd", "onnara");
                obj.put("svcCd", "B0001");
                obj.put("juminNo", bizNo);
                break;
        }
        return obj;
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
}
