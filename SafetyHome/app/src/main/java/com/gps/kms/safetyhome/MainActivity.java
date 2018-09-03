package com.gps.kms.safetyhome;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;


public class MainActivity extends AppCompatActivity

        implements OnMapReadyCallback,

        GoogleApiClient.ConnectionCallbacks,

        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        SensorEventListener,

        LocationListener {


    private GoogleApiClient mGoogleApiClient = null;

    private GoogleMap mGoogleMap = null;

    private Marker currentMarker = null;

    private Circle mCircle;
    private Marker mMarker;


    private static final String TAG = "googlemap_example";

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2002;

    private static final int UPDATE_INTERVAL_MS = 2000;  // 2초

    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초


    private AppCompatActivity mActivity;

    boolean askPermissionOnceAgain = false;

    boolean mRequestingLocationUpdates = false;

    Location mCurrentLocation;

    boolean mMoveMapByUser = true;

    boolean mMoveMapByAPI = true;


    LocationRequest locationRequest = new LocationRequest()

            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            .setInterval(UPDATE_INTERVAL_MS)

            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

    ArrayList<MarkerItem> markerList;

    View marker_root_view;

    private Button btn_callHelp;
    private Button btn_sms;
    private Button btn_record;

    private String currentAddresss;

    //가속도 센서 적용
    private long lastTime;
    private float speed;
    private float lastX;
    private float lastY;
    private float lastZ;
    private float x, y, z;

    private static final int SHAKE_THRESHOLD = 8000;
    private static final int DATA_X = SensorManager.DATA_X;
    private static final int DATA_Y = SensorManager.DATA_Y;
    private static final int DATA_Z = SensorManager.DATA_Z;

    private SensorManager sensorManager;
    private Sensor accelerormeterSensor;

    private int count = 0;


    // 녹음된 오디오 저장할 위치
    // 내장 메모리를 사용하려면 permission.WRITE_EXTERNAL_STORAGE 를 추가해야한다.
    // Environment.getExternalStorageDirectory()로 각기 다른 핸드폰의 내장메모리의 디렉토리를 알수있다.
    final private static File RECORDED_FILE = Environment.getExternalStorageDirectory();
    String filename;
    // MediaPlayer 클래스에 재생에 관련된 메서드와 멤버변수가 저장어되있다.
    MediaPlayer player;
    // MediaRecorder 클래스에  녹음에 관련된 메서드와 멤버 변수가 저장되어있다.
    MediaRecorder recorder;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


        Log.d(TAG, "onCreate");

        mActivity = this;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


        MapFragment mapFragment = (MapFragment) getFragmentManager()

                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);


        markerList = new ArrayList<>();

        btn_callHelp = (Button) findViewById(R.id.btn_callHelp);
        btn_callHelp.setOnClickListener(this);

        btn_sms = (Button) findViewById(R.id.btn_sms);
        btn_sms.setOnClickListener(this);

        //엑셀 파일 읽어오기
        Excel();

        //가속도 센서 적용
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerormeterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        btn_record = (Button) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);

        // 저장할 파일 위치를 String 으로 처리했다.
        // RECORDED_FILE.getAbsolutePath() == /mnt/sdcard 뒤에 저장할 파일엔 '/' 가 필요하다.
        filename = RECORDED_FILE.getAbsolutePath() + "/evidenceRecord.mp4";

        recorder = null;

        Toast.makeText(getApplicationContext(),"안전귀가 서비스를 시작합니다.", Toast.LENGTH_LONG).show();


    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_callHelp:
                onCall();
                break;
            case R.id.btn_sms:
                String message = "구조요청 메세지" + "\n";
                //message += "위도,경도 : " + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude() + "\n";
                //message += " : " + mCurrentLocation.getLongitude() + "\n";
                currentAddresss.trim();
                message += "현재 위치 : " + currentAddresss + "\n";

                sendSMS("010-5343-8719", message);
                sendSMS("010-6744-5932", message);
                break;

            case R.id.btn_record:
                onPermissionCheck();

                break;
        }
    }

    @Override
    public void onResume() {


        super.onResume();


        if (mGoogleApiClient.isConnected()) {


            Log.d(TAG, "onResume : call startLocationUpdates");

            if (!mRequestingLocationUpdates) startLocationUpdates();

        }


        //앱 정보에서 퍼미션을 허가했는지를 다시 검사해봐야 한다.

        if (askPermissionOnceAgain) {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                askPermissionOnceAgain = false;


                checkPermissions();

            }

        }

    }


    private void startLocationUpdates() {


        if (!checkLocationServicesStatus()) {


            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");

            showDialogForLocationServiceSetting();

        } else {


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED

                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


                Log.d(TAG, "startLocationUpdates : 퍼미션 안가지고 있음");

                return;

            }


            Log.d(TAG, "startLocationUpdates : call FusedLocationApi.requestLocationUpdates");

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

            mRequestingLocationUpdates = true;


            mGoogleMap.setMyLocationEnabled(true);


        }


    }


    private void stopLocationUpdates() {


        Log.d(TAG, "stopLocationUpdates : LocationServices.FusedLocationApi.removeLocationUpdates");

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        mRequestingLocationUpdates = false;

    }


    @Override

    public void onMapReady(GoogleMap googleMap) {


        Log.d(TAG, "onMapReady :");


        mGoogleMap = googleMap;


        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에

        //지도의 초기위치를 서울로 이동

        setDefaultLocation();


        //mGoogleMap.getUiSettings().setZoomControlsEnabled(false);

        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {


            @Override

            public boolean onMyLocationButtonClick() {


                Log.d(TAG, "onMyLocationButtonClick : 위치에 따른 카메라 이동 활성화");

                mMoveMapByAPI = true;

                return true;

            }

        });

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {


            @Override

            public void onMapClick(LatLng latLng) {


                Log.d(TAG, "onMapClick :");

            }

        });


        mGoogleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {


            @Override

            public void onCameraMoveStarted(int i) {


                if (mMoveMapByUser == true && mRequestingLocationUpdates) {


                    Log.d(TAG, "onCameraMove : 위치에 따른 카메라 이동 비활성화");

                    mMoveMapByAPI = false;

                }


                mMoveMapByUser = true;


            }

        });


        mGoogleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {


            @Override

            public void onCameraMove() {

            }

        });


        //CCTV 마커 그리기
        setCustomMarkerView();
        getSampleMarkerItems();

        //원 그리기
        mGoogleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (mCircle == null || mMarker == null) {
                    drawMarkerWithCircle(latLng);
                } else {
                    updateMarkerWithCircle(latLng);
                }
            }
        });


    }


    @Override

    public void onLocationChanged(Location location) {


        Log.d(TAG, "onLocationChanged : ");

        String markerTitle = getCurrentAddress(location);
        String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                + " 경도:" + String.valueOf(location.getLongitude());

        //현재 위치에 마커 생성하고 이동
        setCurrentLocation(location, markerTitle, markerSnippet);
        mCurrentLocation = location;

    }


    @Override

    protected void onStart() {


        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() == false) {


            Log.d(TAG, "onStart: mGoogleApiClient connect");

            mGoogleApiClient.connect();

        }

        if (accelerormeterSensor != null) {
            sensorManager.registerListener(this, accelerormeterSensor, sensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "onStart 호출");
        }
        super.onStart();

    }


    @Override
    protected void onStop() {


        if (mRequestingLocationUpdates) {


            Log.d(TAG, "onStop : call stopLocationUpdates");

            stopLocationUpdates();

        }


        if (mGoogleApiClient.isConnected()) {


            Log.d(TAG, "onStop : mGoogleApiClient disconnect");

            mGoogleApiClient.disconnect();

        }

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }


        super.onStop();

    }


    @Override

    public void onConnected(Bundle connectionHint) {


        if (mRequestingLocationUpdates == false) {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


                int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,

                        Manifest.permission.ACCESS_FINE_LOCATION);


                if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED) {

                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                } else {


                    Log.d(TAG, "onConnected : 퍼미션 가지고 있음");
                    Log.d(TAG, "onConnected : call startLocationUpdates");
                    startLocationUpdates();

                    mGoogleMap.setMyLocationEnabled(true);

                }


            } else {


                Log.d(TAG, "onConnected : call startLocationUpdates");
                startLocationUpdates();
                mGoogleMap.setMyLocationEnabled(true);

            }

        }

    }


    @Override

    public void onConnectionFailed(ConnectionResult connectionResult) {


        Log.d(TAG, "onConnectionFailed");
        setDefaultLocation();

    }


    @Override

    public void onConnectionSuspended(int cause) {


        Log.d(TAG, "onConnectionSuspended");

        if (cause == CAUSE_NETWORK_LOST)

            Log.e(TAG, "onConnectionSuspended(): Google Play services " +

                    "connection lost.  Cause: network lost.");

        else if (cause == CAUSE_SERVICE_DISCONNECTED)

            Log.e(TAG, "onConnectionSuspended():  Google Play services " +

                    "connection lost.  Cause: service disconnected");

    }


    public String getCurrentAddress(Location location) {


        //지오코더... GPS를 주소로 변환

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());


        List<Address> addresses;


        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);

        } catch (IOException ioException) {

            //네트워크 문제

            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();

            return "지오코더 서비스 사용불가";

        } catch (IllegalArgumentException illegalArgumentException) {

            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();

            return "잘못된 GPS 좌표";


        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        } else {

            Address address = addresses.get(0);

            return address.getAddressLine(0).toString();

        }


    }

    public String getCurrentAddress(LatLng location) {


        //지오코더... GPS를 주소로 변환

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());


        List<Address> addresses;


        try {
            addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1);

        } catch (IOException ioException) {

            //네트워크 문제

            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();

            return "지오코더 서비스 사용불가";

        } catch (IllegalArgumentException illegalArgumentException) {

            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();

            return "잘못된 GPS 좌표";


        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        } else {

            Address address = addresses.get(0);

            return address.getAddressLine(0).toString();

        }


    }


    public boolean checkLocationServicesStatus() {

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

    }


    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {


        mMoveMapByUser = false;


        if (currentMarker != null) currentMarker.remove();


        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());


        //구글맵의 디폴트 현재 위치는 파란색 동그라미로 표시

        //마커를 원하는 이미지로 변경하여 현재 위치 표시하도록 수정해야함.

        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(currentLatLng);

        markerOptions.title(markerTitle);

        markerOptions.snippet(markerSnippet);

        markerOptions.draggable(true);

        markerOptions.icon(BitmapDescriptorFactory

                .defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        //이동마커
        //currentMarker = mGoogleMap.addMarker(markerOptions);


        if (mMoveMapByAPI) {


            Log.d(TAG, "setCurrentLocation :  mGoogleMap moveCamera "

                    + location.getLatitude() + " " + location.getLongitude());

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 17);
            //로그 찍어보기
            //Toast.makeText(getApplicationContext(),
            //        "latitude : " +currentLatLng.latitude + "\n" + "longitude : " +currentLatLng.longitude,Toast.LENGTH_SHORT).show();

            //CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);

            mGoogleMap.moveCamera(cameraUpdate);

        }

    }

    public void setDefaultLocation() {


        mMoveMapByUser = false;
        //디폴트 위치, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 요부 확인하세요";


        if (currentMarker != null) currentMarker.remove();


        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        //currentMarker = mGoogleMap.addMarker(markerOptions);


        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);

        mGoogleMap.moveCamera(cameraUpdate);


    }


    //여기부터는 런타임 퍼미션 처리을 위한 메소드들

    @TargetApi(Build.VERSION_CODES.M)

    private void checkPermissions() {

        boolean fineLocationRationale = ActivityCompat

                .shouldShowRequestPermissionRationale(this,

                        Manifest.permission.ACCESS_FINE_LOCATION);

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,

                Manifest.permission.ACCESS_FINE_LOCATION);


        if (hasFineLocationPermission == PackageManager

                .PERMISSION_DENIED && fineLocationRationale)

            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");


        else if (hasFineLocationPermission

                == PackageManager.PERMISSION_DENIED && !fineLocationRationale) {

            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +

                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");

        } else if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {


            Log.d(TAG, "checkPermissions : 퍼미션 가지고 있음");


            if (mGoogleApiClient.isConnected() == false) {


                Log.d(TAG, "checkPermissions : 퍼미션 가지고 있음");

                mGoogleApiClient.connect();

            }

        }

    }


    @Override

    public void onRequestPermissionsResult(int permsRequestCode,

                                           @NonNull String[] permissions,

                                           @NonNull int[] grantResults) {


        if (permsRequestCode

                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0) {


            boolean permissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;


            if (permissionAccepted) {


                if (mGoogleApiClient.isConnected() == false) {


                    Log.d(TAG, "onRequestPermissionsResult : mGoogleApiClient connect");

                    mGoogleApiClient.connect();

                }


            } else {


                checkPermissions();

            }

        } else if (permsRequestCode == 2000) {


        }

    }


    @TargetApi(Build.VERSION_CODES.M)

    private void showDialogForPermission(String msg) {


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("알림");

        builder.setMessage(msg);

        builder.setCancelable(false);

        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                ActivityCompat.requestPermissions(mActivity,

                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},

                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            }

        });


        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                finish();

            }

        });

        builder.create().show();

    }


    private void showDialogForPermissionSetting(String msg) {


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("알림");

        builder.setMessage(msg);

        builder.setCancelable(true);

        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {


                askPermissionOnceAgain = true;


                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,

                        Uri.parse("package:" + mActivity.getPackageName()));

                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);

                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                mActivity.startActivity(myAppSettings);

            }

        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                finish();

            }

        });

        builder.create().show();

    }


    //여기부터는 GPS 활성화를 위한 메소드들

    private void showDialogForLocationServiceSetting() {


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("위치 서비스 비활성화");

        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"

                + "위치 설정을 수정하실래요?");

        builder.setCancelable(true);

        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int id) {

                Intent callGPSSettingIntent

                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);

                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);

            }

        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int id) {

                dialog.cancel();

            }

        });

        builder.create().show();

    }


    @Override

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {

                    if (checkLocationServicesStatus()) {

                        Log.d(TAG, "onActivityResult : 퍼미션 가지고 있음");
                        if (mGoogleApiClient.isConnected() == false) {

                            Log.d(TAG, "onActivityResult : mGoogleApiClient connect ");
                            mGoogleApiClient.connect();

                        }

                        return;

                    }

                }


                break;

        }

    }


    /**
     * 엑셀 파일 읽어오기
     */
    public void Excel() {
        Workbook workbook = null;
        Sheet sheet = null;
        try {
            InputStream inputStream = getBaseContext().getResources().getAssets().open("mini.xls");
            workbook = Workbook.getWorkbook(inputStream);
            sheet = workbook.getSheet(0);
            int MaxColumn = 2, RowStart = 0, RowEnd = sheet.getColumn(MaxColumn - 1).length - 1, ColumnStart = 0, ColumnEnd = sheet.getRow(2).length - 1;
            for (int row = RowStart; row <= RowEnd; row++) {
                double excelload = Double.parseDouble(sheet.getCell(ColumnStart, row).getContents());
                double excelload2 = Double.parseDouble(sheet.getCell(ColumnStart + 1, row).getContents());

                MarkerItem markerItem = new MarkerItem(excelload, excelload2, 0);
                markerList.add(markerItem);

            }

            //디버깅 용
            MarkerItem markerItem = new MarkerItem(37.242304, 127.070238,0);
            markerList.add(markerItem);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        } finally {
            workbook.close();
        }
    }


    /**
     * 실제 마커를 추가한다.
     */
    private Marker addMarker(Marker marker) {

        double lat = marker.getPosition().latitude;
        double lon = marker.getPosition().longitude;
        int title = Integer.parseInt(marker.getTitle());

        MarkerItem temp = new MarkerItem(lat, lon, title);

        return addMarker(temp);
    }

    private Marker addMarker(MarkerItem markerItem) {

        LatLng position = new LatLng(markerItem.getLat(), markerItem.getLon());


        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title("CCTV");
        markerOptions.position(position);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(this, marker_root_view)));

        return mGoogleMap.addMarker(markerOptions);
    }


    /**
     * View를 Bitmap으로 변환
     */
    private Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }


    /**
     * 실제로는 서버에서 해당 정보들의 목록을 받아오고 그 목록을 마커에 추가한다.
     */
    private void getSampleMarkerItems() {


        for (MarkerItem markerItem : markerList)
            addMarker(markerItem);
    }

    /**
     * 마커 정보로부터 하나씩 마커 추가
     */

    private void setCustomMarkerView() {
        marker_root_view = LayoutInflater.from(this).inflate(R.layout.marker_layout, null);

    }

    private void updateMarkerWithCircle(LatLng position) {

        int shadeColor;
        int strokeColor;

        if (isSafe(position)) {
            shadeColor = 0x4400ff00; //opaque green fill
            strokeColor = 0xff00ff00;
            mMarker.setIcon(BitmapDescriptorFactory
                    .fromResource(R.drawable.safe));

        } else {
            shadeColor = 0x44ff0000; //opaque red fill (주위에 CCTV가 없다면);
            strokeColor = 0xffff0000;
            mMarker.setIcon(BitmapDescriptorFactory
                    .fromResource(R.drawable.danger));
        }

        mCircle.setFillColor(shadeColor);
        mCircle.setStrokeColor(strokeColor);

        mCircle.setCenter(position);
        mMarker.setPosition(position);
    }

    private void drawMarkerWithCircle(LatLng position) {
        double radiusInMeters = 80.0;
        int strokeColor = 0xff00ff00; //green outline
        int shadeColor;


        MarkerOptions markerOptions = new MarkerOptions().position(position);

        markerOptions.title("현재위치 주소");


        currentAddresss = getCurrentAddress(position);
        markerOptions.snippet(currentAddresss);
        markerOptions.draggable(true);

        if (isSafe(position)) {
            shadeColor = 0x4400ff00; //opaque green fill
            strokeColor = 0xff00ff00;
            markerOptions.icon(BitmapDescriptorFactory
                    .fromResource(R.drawable.safe));
        } else {
            shadeColor = 0x44ff0000; //opaque red fill (주위에 CCTV가 없다면),
            strokeColor = 0xffff0000;
            markerOptions.icon(BitmapDescriptorFactory
                    .fromResource(R.drawable.danger));
        }

        CircleOptions circleOptions = new CircleOptions().center(position).radius(radiusInMeters).fillColor(shadeColor).strokeColor(strokeColor).strokeWidth(8);
        mCircle = mGoogleMap.addCircle(circleOptions);


        mMarker = mGoogleMap.addMarker(markerOptions);
    }

    boolean isSafe(LatLng position) {

        for (int i = 0; i < markerList.size(); i++) {

            MarkerItem item = markerList.get(i);
            double distance = getDistance(position.latitude, position.longitude, item.getLat(), item.getLon());
            if (distance < 80) {
                return true;
            }
        }
        return false;
    }

    /**
     * 두 위경도 좌표 사이의 거리 구하는 함수
     *
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    public double getDistance(double lat1, double lng1, double lat2, double lng2) {
        double distance;

        Location locationA = new Location("point A");
        locationA.setLatitude(lat1);
        locationA.setLongitude(lng1);

        Location locationB = new Location("point B");
        locationB.setLatitude(lat2);
        locationB.setLongitude(lng2);

        distance = locationA.distanceTo(locationB);

        return distance;
    }

    public void onCall() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //사용자 단말기의 권한 중 "전화 걸기"권한이 허용 되어있는지 체크
            int permissionResult = checkSelfPermission(Manifest.permission.CALL_PHONE);

            if (permissionResult == PackageManager.PERMISSION_DENIED) {

                if (shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("권한 필요")
                            .setMessage("기능을 사용하기 위해 단말기의 '전화걸기' 권한이 필요합니다.")
                            .setPositiveButton("네", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 2000);
                                    }
                                }
                            })
                            .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getApplicationContext(), "기능 취소", Toast.LENGTH_SHORT).show();
                                }
                            }).create().show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 2000);
                }
            } else {
                //권한 존재
                Toast.makeText(getApplicationContext(), "구조 요청", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:123456789"));
                try {
                    startActivity(intent);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        } else { //마시멜로우 이전 버젼이라면,
            //권한 존재
            Toast.makeText(getApplicationContext(), "구조 요청", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:123456789"));
            try {
                startActivity(intent);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }


    }

    public void sendSMS(String phoneNumber, String message) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //사용자 단말기의 권한 중 "전화 걸기"권한이 허용 되어있는지 체크

            int permissionResult = checkSelfPermission(Manifest.permission.SEND_SMS);

            if (permissionResult == PackageManager.PERMISSION_DENIED) {

                if (shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("권한 필요")
                            .setMessage("기능을 사용하기 위해 단말기의 '메세지 전송' 권한이 필요합니다.")
                            .setPositiveButton("네", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 3000);
                                    }
                                }
                            })
                            .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getApplicationContext(), "기능 취소", Toast.LENGTH_SHORT).show();
                                }
                            }).create().show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 3000);
                }
            } else {
                //권한 존재
                String SENT = "SMS_SENT";
                String DELIVERED = "SMS_DELIVERED";

                PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
                PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

                registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        switch (getResultCode()) {
                            case Activity.RESULT_OK:
                                //Toast.makeText(getBaseContext(), "알림 메시지 발송", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                }, new IntentFilter(SENT));

                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
                Toast.makeText(getApplicationContext(), "구조요청 메세지 전송", Toast.LENGTH_SHORT).show();
            }
        } else { //마시멜로우 이전 버젼이라면,
            //권한 존재
            String SENT = "SMS_SENT";
            String DELIVERED = "SMS_DELIVERED";

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            //Toast.makeText(getBaseContext(), "알림 메시지 발송", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }, new IntentFilter(SENT));

            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
            Toast.makeText(getApplicationContext(), "구조요청 메세지 전송", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long gabOfTime = (currentTime - lastTime);
            if (gabOfTime > 100) {
                lastTime = currentTime;
                x = event.values[SensorManager.DATA_X];
                y = event.values[SensorManager.DATA_Y];
                z = event.values[SensorManager.DATA_Z];

                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / gabOfTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    //Toast.makeText(getApplicationContext(),"count : " + count,Toast.LENGTH_SHORT).show();
                    if (count == 1) {
                        //Toast.makeText(getApplicationContext(),"count " + count,Toast.LENGTH_SHORT).show();
                        //긴급 문자 메세지 전송
                        String message = "긴급구조 요청 메세지" + "\n";
                        //message += "위도,경도 : " + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude() + "\n";
                        //message += " : " + mCurrentLocation.getLongitude() + "\n";
                        currentAddresss.trim();
                        message += "현재 위치 : " + currentAddresss + "\n";
                        message += "위험상황이니 빠른 구조 바랍니다!!" + "\n";
                        sendSMS("010-5343-8719", message);
                        sendSMS("010-6744-5932", message);
                        count = 0;
                    }
                    count++;
                }


                lastX = event.values[DATA_X];
                lastY = event.values[DATA_Y];
                lastZ = event.values[DATA_Z];
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onRecord() {





        // 녹음 시작 버튼

        if (recorder == null) {

            // 녹음 시작을 위해  MediaRecorder 객체  recorder를 생성한다.
            recorder = new MediaRecorder();
            // 오디오 입력 형식 설정
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // 음향을 저장할 방식을 설정
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            // 오디오 인코더 설정
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            // 저장될 파일 지정
            recorder.setOutputFile(filename);


            try {
                Toast.makeText(getApplicationContext(), "녹음을 시작합니다.", Toast.LENGTH_LONG).show();

                // 녹음 준비,시작
                recorder.prepare();
                recorder.start();
            } catch (Exception ex) {
                Log.e("SampleAudioRecorder", "Exception : ", ex);
            }
        } else {
            // 녹음을 중지
            recorder.stop();

            // 오디오 녹음에 필요한  메모리를 해제한다
            recorder.release();
            recorder = null;

            Toast.makeText(getApplicationContext(), "녹음을 저장합니다.", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    protected void onPause() {

        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        super.onPause();
    }

    public void onPermissionCheck() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //사용자 단말기의 권한 중 "외장 메모리쓰기"권한이 허용 되어있는지 체크
            int permissionResult = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permissionResult == PackageManager.PERMISSION_DENIED) {

                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("권한 필요")
                            .setMessage("기능을 사용하기 위해 단말기의 '외장 메모리쓰기' 권한이 필요합니다.")
                            .setPositiveButton("네", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 5000);
                                    }
                                }
                            })
                            .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getApplicationContext(), "기능 취소", Toast.LENGTH_SHORT).show();
                                }
                            }).create().show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 5000);
                }
            } else {


            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //사용자 단말기의 권한 중 "내장 녹음"권한이 허용 되어있는지 체크
            int permissionResult = checkSelfPermission(Manifest.permission.RECORD_AUDIO);

            if (permissionResult == PackageManager.PERMISSION_DENIED) {

                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("권한 필요")
                            .setMessage("기능을 사용하기 위해 단말기의 '내장 녹음' 권한이 필요합니다.")
                            .setPositiveButton("네", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 6000);
                                    }
                                }
                            })
                            .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getApplicationContext(), "기능 취소", Toast.LENGTH_SHORT).show();
                                }
                            }).create().show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 6000);
                }
            } else {
                //권한 존재

                onRecord();
            }
        }
    }
}

