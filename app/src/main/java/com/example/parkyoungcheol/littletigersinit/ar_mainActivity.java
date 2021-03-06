package com.example.parkyoungcheol.littletigersinit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.parkyoungcheol.littletigersinit.Model.ArmsgData;
import com.example.parkyoungcheol.littletigersinit.Model.AppHelper;
import com.example.parkyoungcheol.littletigersinit.Model.DataSet;
import com.example.parkyoungcheol.littletigersinit.Model.DataSource;
import com.example.parkyoungcheol.littletigersinit.Model.GeoPoint;
import com.example.parkyoungcheol.littletigersinit.Model.ResultMSG;
import com.example.parkyoungcheol.littletigersinit.Navigation.AR.AR_navigationActivity;
import com.example.parkyoungcheol.littletigersinit.Navigation.AR.ARmessageActivity;
import com.example.parkyoungcheol.littletigersinit.Navigation.AR.Nav_searchActivity;
import com.example.parkyoungcheol.littletigersinit.Navigation.AR.UnityPlayerActivity;
import com.example.parkyoungcheol.littletigersinit.util.ArmsgListAdapter;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;
import com.xw.repo.BubbleSeekBar;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.val;


public class ar_mainActivity extends FragmentActivity implements OnMapReadyCallback {
    private MapView mapView;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private Context context;
    private static final int REQUEST_CAMERA = 2000;
    private FirebaseDatabase mFirebaseDb;
    private DatabaseReference mARMessageRef;
    private AlertDialog.Builder alertDialogBuilder;
    private BubbleSeekBar bubbleSeekBar;
    private int progressStatus = 500;

    //private Button menu,ar_nav,info,poi;
    @BindView(R.id.fab_menu_btn)
    FloatingActionMenu fab_menu;
    @BindView(R.id.ar_nav_btn)
    com.github.clans.fab.FloatingActionButton ar_nav_btn;
    @BindView(R.id.poi_browser_btn)
    com.github.clans.fab.FloatingActionButton poi_browser_btn;
    @BindView(R.id.ar_message_btn)
    com.github.clans.fab.FloatingActionButton ar_message_btn;

    public ResultMSG resultMSG = new ResultMSG();
    public ResultMSG resultMSG_for_BUSSTOP = new ResultMSG();
    private ProgressDialog pDialog;
    public boolean errorState = false; // JSON 객체 받아왔을때 에러 발생시 상태표시
    public boolean loopStopper = true; // JSON 객체 받아왔을때 에러 발생시 반복문 종료 위한 변수
    private String[] categoryAry = {"CAFE", "BUSSTOP", "CONVENIENCE", "RESTAURANT", "BANK", "ACCOMMODATION", "HOSPITAL"};
    public int loopShareInt = 0; // for루프와 핸들러 사이의 공유변수
    private double current_longitude = 0, current_latitude = 0;
    double subDistance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_main);
        ButterKnife.bind(this);

        // 카메라, 위치정보활용 권환 받기
        checkCameraAndLocationPermission();

        // 네이버 맵 띄우기
        NaverMapSdk.getInstance(this).setClient(
                new NaverMapSdk.NaverCloudPlatformClient("0yfv84wqze"));

        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        // ar 메시지 남기기로 이동 버튼
        ar_message_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ar_mainActivity.this, UnityPlayerActivity.class);
                intent.putExtra("SELECT", 3);
                startActivity(intent);
                overridePendingTransition(R.anim.push_up_in, R.anim.non_anim);
            }
        });

        // ar 네비게이션으로 이동버튼
        ar_nav_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ar_mainActivity.this, AR_navigationActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.push_up_in, R.anim.non_anim);
            }
        });


        if (AppHelper.requestQueue == null) {
            //리퀘스트큐 생성 (MainActivit가 메모리에서 만들어질 때 같이 생성이 될것이다.
            AppHelper.requestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        // ar 주변검색으로 이동 버튼
        poi_browser_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder popDialog = new AlertDialog.Builder(v.getContext());
                View innerView = getLayoutInflater().inflate(R.layout.custom_seekbar, null);
                TextView count = innerView.findViewById(R.id.count);

                popDialog.setView(innerView);
                popDialog.setTitle("어디까지 보고싶으세요?\n");
                bubbleSeekBar = (BubbleSeekBar) innerView.findViewById(R.id.bubble_seekbar);
                TextView tip = innerView.findViewById(R.id.tip);
                tip.setVisibility(View.GONE);
                count.setText(progressStatus + "m");
                bubbleSeekBar.getConfigBuilder()
                        .min(500)
                        .max(2500)
                        .progress(progressStatus)
                        .sectionCount(4)
                        .trackColor(ContextCompat.getColor(v.getContext(), R.color.colorLightGray))
                        .secondTrackColor(ContextCompat.getColor(v.getContext(), R.color.main_mint))
                        .thumbColor(ContextCompat.getColor(v.getContext(), R.color.mainColor))
                        .showSectionText()
                        .sectionTextColor(ContextCompat.getColor(v.getContext(), R.color.tw__composer_deep_gray))
                        .sectionTextSize(12)
                        .showThumbText()
                        .touchToSeek()
                        .thumbTextColor(ContextCompat.getColor(v.getContext(), R.color.main_red))
                        .thumbTextSize(15)
                        .hideBubble()
                        .showSectionMark()
                        .seekBySection()
                        .sectionTextPosition(BubbleSeekBar.TextPosition.BELOW_SECTION_MARK)
                        .build();
                bubbleSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
                    @Override
                    public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                        progressStatus = progress;
                        count.setText(progress + "m");
                    }

                    @Override
                    public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

                    }

                    @Override
                    public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

                    }
                });


                popDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int k) {
                        pDialog = new ProgressDialog(ar_mainActivity.this);
                        // Showing progress dialog before making http request
                        pDialog.setMessage("AR Loading...");
                        pDialog.show();

                        // 반경을 설정할 수 있도록 해주는 변수 (현재위치 좌표에 subDistance를 빼면됨)
                        subDistance = LatitudeInDifference(progressStatus - 100);
                        Log.v("LatLon", "currentLat=" + current_latitude + "currentLon=" + current_longitude + "//" + (current_latitude - subDistance) + ", " + (current_longitude - subDistance));


                        Intent intent = new Intent(ar_mainActivity.this, UnityPlayerActivity.class);
                        intent.putExtra("SELECT", 2);

                        int delayTime = 0;
                        loopShareInt = 0;
                        for (int i = 0; i < categoryAry.length; i++) {
                            final Handler handler = new Handler() {
                                @Override
                                public void handleMessage(Message msg) {
                                    pDialog.setMessage(categoryAry[loopShareInt] + " Loading..");
                                    POIreceiver(categoryAry[loopShareInt]);
                                    loopShareInt++;
                                }
                            };
                            delayTime += 300;
                            handler.sendEmptyMessageDelayed(0, delayTime);
                        }

                        final Handler handler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                if (errorState) {
                                    Toast.makeText(ar_mainActivity.this, "주변 정보를 받아오지 못했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                    hidePDialog();
                                } else {
                                    intent.putExtra(categoryAry[0], resultMSG.getCAFE());
                                    intent.putExtra(categoryAry[1], resultMSG_for_BUSSTOP.getBUSSTOP());
                                    intent.putExtra(categoryAry[2], resultMSG.getCONVENIENCE());
                                    intent.putExtra(categoryAry[3], resultMSG.getRESTAURANT());
                                    intent.putExtra(categoryAry[4], resultMSG.getBANK());
                                    intent.putExtra(categoryAry[5], resultMSG.getACCOMMODATION());
                                    intent.putExtra(categoryAry[6], resultMSG.getHOSPITAL());
                                    hidePDialog();
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.push_up_in, R.anim.non_anim);
                                }
                            }
                        };
                        handler.sendEmptyMessageDelayed(0, 2500);

                    }
                })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                popDialog.create();
                popDialog.show();
            }
        });
    }

    // 주변검색 API 결과값 파싱
    // NaverHttpHandler에 Naver에서 요구하는 API URL형식을 맞춰 보내고 전달받은 JSON 값을 파싱한다
    // 파싱해서 저장하는 형태 : 위치이름,경도,위도|
    public void POIreceiver(String category) {
        String createdURL;
        GeoPoint myGeo = findMyLocation();

        if (current_latitude != 0 && current_longitude != 0) {
            createdURL = DataSource.createRequestCategoryURL(category, current_longitude, current_latitude, subDistance);
        } else {
            createdURL = DataSource.createRequestCategoryURL(category, myGeo.getX(), myGeo.getY(), subDistance);
        }
        Log.i("만들어진 주소", createdURL);

        JsonObjectRequest localRequest = new JsonObjectRequest(Request.Method.GET, createdURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //에러처리

                        StringBuffer sb = new StringBuffer();

                        // response
                        Log.d("리스폰스", response.toString());
                        JSONObject result;
                        JSONArray site = null;
                        JSONArray station = null;

                        boolean errorFinder = response.toString().contains("error");
                        Log.i("확인", String.valueOf(errorFinder));
                        try {
                            if (errorFinder) {
                                errorState = true;
                            } else {
                                result = response.getJSONObject("result");

                                if (category.equals("BUSSTOP")) {
                                    station = result.getJSONArray("station");
                                    for (int i = 0; i < station.length(); i++) {
                                        JSONObject obj = station.getJSONObject(i);
                                        String lonX = obj.getString("x");
                                        String latY = obj.getString("y");
                                        String name = obj.getString("stationDisplayName");

                                        sb.append(name);
                                        sb.append(",");
                                        sb.append(lonX);
                                        sb.append(",");
                                        sb.append(latY);
                                        sb.append("|");
                                    }
                                } else {
                                    site = result.getJSONArray("site");
                                    //busstop을 제외하고 json 반환 양식이 동일함
                                    for (int i = 0; i < site.length(); i++) {
                                        JSONObject obj = site.getJSONObject(i);
                                        String lonX = obj.getString("x");
                                        String latY = obj.getString("y");
                                        String name = obj.getString("name");

                                        sb.append(name);
                                        sb.append(",");
                                        sb.append(lonX);
                                        sb.append(",");
                                        sb.append(latY);
                                        sb.append("|");
                                    }
                                }

                                Log.i("파싱결과", sb.toString());

                                switch (category) {
                                    case "CAFE":
                                        resultMSG.setCAFE(sb.toString());
                                        break;
                                    case "BUSSTOP":
                                        resultMSG_for_BUSSTOP.setBUSSTOP(sb.toString());
                                        break;
                                    case "CONVENIENCE":
                                        resultMSG.setCONVENIENCE(sb.toString());
                                        break;
                                    case "RESTAURANT":
                                        resultMSG.setRESTAURANT(sb.toString());
                                        break;
                                    case "BANK":
                                        resultMSG.setBANK(sb.toString());
                                        break;
                                    case "ACCOMMODATION":
                                        resultMSG.setACCOMMODATION(sb.toString());
                                        break;
                                    case "HOSPITAL":
                                        resultMSG.setHOSPITAL(sb.toString());
                                        break;
                                }
                            }
                        } catch (JSONException e) {
                            errorState = true;
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("ERROR_RESPONSE =>", error.toString());
                        errorState = true;
                        Toast.makeText(ar_mainActivity.this, "통신에러", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        localRequest.setShouldCache(false);
        AppHelper.requestQueue.add(localRequest);
    }


    // 현재 내 위치 반환
    private GeoPoint findMyLocation() {
        GeoPoint myGeo = null;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // 밑줄 권한때문에 그런거임
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ar_mainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        } else {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {

                pDialog = new ProgressDialog(ar_mainActivity.this);
                pDialog.setMessage("위치정보를 받아오고 있습니다...");
                pDialog.show();
                while (true) {
                    location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location == null) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                1000,
                                1,
                                gpsLocationListener);
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                1000,
                                1,
                                gpsLocationListener);
                        continue;
                    } else {
                        break;
                    }
                }
                //로딩메시지제거
                if (pDialog != null) {
                    pDialog.dismiss();
                    pDialog = null;
                }

            } else {
                String provider = location.getProvider();
                Double lon_X = location.getLongitude();
                Double lat_Y = location.getLatitude();
                current_longitude = lon_X;
                current_latitude = lat_Y;

                myGeo = new GeoPoint(lon_X, lat_Y);
            }
        }
        return myGeo;
    }

    // 현재위치가 바뀔때마다 좌표를 바꾸는 리스너
    final LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            current_longitude = location.getLongitude();
            current_latitude = location.getLatitude();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };


    // 카메라 권한 받아오기
    public void checkCameraAndLocationPermission() {
        int permissionCamera = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        int permissionLocation = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionLocation == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(ar_mainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else if (permissionCamera == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(ar_mainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {

        }
    }

    // 권한설정
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 위치 추적 권한
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        checkCameraAndLocationPermission();
                    } else {
                        Toast.makeText(this, "ar기능을 이용하려면 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                        finish();
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                }
            }
        }

        // 카메라 권한
        if (requestCode == REQUEST_CAMERA) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                if (permission.equals(Manifest.permission.CAMERA)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        checkCameraAndLocationPermission();
                    } else {
                        Toast.makeText(this, "ar기능을 이용하려면 카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                        finish();
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                }
            }
        }
    }

    // 사전설정
    @Override
    public void onMapReady(@NonNull final NaverMap naverMap) {
        InfoWindow infoWindow = new InfoWindow();
        //정보창 안띄어짐
        infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(ar_mainActivity.this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return "정보 창 내용";
            }
        });

        mFirebaseDb = FirebaseDatabase.getInstance();
        mARMessageRef = mFirebaseDb.getReference("ARMessages");


        //지하철 역 정보 까지 띄어주는 레이어그룹
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, true);
        //건물 내부정보까지 보여지게하는 옵션
        naverMap.setIndoorEnabled(false);

        ArrayList<ArmsgData> oData = new ArrayList<ArmsgData>();
        ArmsgData oItem = new ArmsgData();
        mARMessageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String msg;
                oData.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Marker marker = new Marker();
                    ArmsgData abc = snapshot.getValue(ArmsgData.class); // 컨버팅되서 Bbs로........
                    marker.setPosition(new LatLng(abc.getLatitude(), abc.getLongitude()));
                    if (abc.getLabel().length() >= 10) {
                        msg = abc.getLabel().substring(0, 10) + "...";
                    } else {
                        msg = abc.getLabel();
                    }
                    marker.setCaptionText(msg);
                    marker.setCaptionText(msg);
                    marker.setWidth(80);
                    marker.setHeight(80);
                    marker.setIcon(OverlayImage.fromResource(R.drawable.ar_marker));
                    marker.setCaptionColor(Color.WHITE);
                    marker.setCaptionHaloColor(Color.BLACK);
                    marker.setCaptionTextSize(16);
                    marker.setMap(naverMap);
                    marker.setOnClickListener(new Overlay.OnClickListener() {
                        @Override
                        public boolean onClick(@NonNull Overlay overlay) {
                            alertDialogBuilder = new AlertDialog.Builder(ar_mainActivity.this);

                            alertDialogBuilder.setTitle("AR 메시지 내용");
                            alertDialogBuilder
                                    .setMessage(abc.getLabel())
                                    .setCancelable(true)
                                    .setPositiveButton("확인",
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            });

                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                            return false;
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // 사용자 현위치 버튼 활성화
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);
        // 사용자 위치 오버레이 활성화
        LocationOverlay locationOverlay = naverMap.getLocationOverlay();
        locationOverlay.setVisible(true);
        // 위치 오버레이 반경
        locationOverlay.setCircleRadius(100);

        naverMap.setLocationSource(locationSource);

        naverMap.setLocationTrackingMode(LocationTrackingMode.Face);

        // 카메라 이동 .. 단, 위치 퍼미션이 허가되어있을 때만
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            GeoPoint mGeo = findMyLocation();
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(mGeo.getY(), mGeo.getX()));
            naverMap.moveCamera(cameraUpdate);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = getSharedPreferences("sFile", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
        Log.i("destroy", "SharedPreferences 데이터 삭제");
    }

    private void hidePDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }
    }

    // WGS좌표계 반경구하기 ( 몇 m거리의 위도,경도 구하기 )
    //반경 m이내의 위도차(degree)
    public double LatitudeInDifference(int diff) {
        //지구반지름
        final int earth = 6371000;    //단위m

        return (diff * 360.0) / (2 * Math.PI * earth);
    }

    //반경 m이내의 경도차(degree)
    public double LongitudeInDifference(double _latitude, int diff) {
        //지구반지름
        final int earth = 6371000;    //단위m

        double ddd = Math.cos(0);
        double ddf = Math.cos(Math.toRadians(_latitude));

        return (diff * 360.0) / (2 * Math.PI * earth * Math.cos(Math.toRadians(_latitude)));
    }
}
