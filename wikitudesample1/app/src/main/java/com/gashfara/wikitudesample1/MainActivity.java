//メイン画面。UI以外はほぼそのまま毎回使う。
package com.gashfara.wikitudesample1;

import android.Manifest;
import android.content.Context;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;
import com.wikitude.architect.StartupConfiguration;
import com.wikitude.architect.StartupConfiguration.CameraPosition;

import java.io.IOException;
//ArchitectViewHolderInterfaceを実装
public class MainActivity extends AppCompatActivity implements ArchitectViewHolderInterface {
    protected ArchitectView					 architectView;
    protected SensorAccuracyChangeListener      sensorAccuracyListener;
    protected Location 						 lastKnownLocaton;
    protected ArchitectViewHolderInterface.ILocationProvider locationProvider;
    protected LocationListener 				 locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mogi Android6対応。権限をアプリで実装しないとエラーになる。SDKのターゲットを23にしちゃうと必要。
        //requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CAMERA}, 0);
        //ARのビュー
        this.architectView = (ArchitectView)this.findViewById( R.id.architectView );
        //wikitude初期設定
        final StartupConfiguration config = new StartupConfiguration(this.getWikitudeSDKLicenseKey(), StartupConfiguration.Features.Geo, this.getCameraPosition());
        try {
            //設定
            this.architectView.onCreate( config );
        } catch (RuntimeException ex)
        {
            this.architectView = null;
            Toast.makeText(getApplicationContext(), "can't create Architect View", Toast.LENGTH_SHORT).show();
        }
        //方位のトラッキングのリスナークラス。wikitudeのクラス。
        this.sensorAccuracyListener = this.getSensorAccuracyListener();
        //位置情報のリスナー
        this.locationListener = new LocationListener() {

            @Override
            public void onStatusChanged( String provider, int status, Bundle extras ) {
            }

            @Override
            public void onProviderEnabled( String provider ) {
            }

            @Override
            public void onProviderDisabled( String provider ) {
            }
            //位置がUpdateしたときの処理
            @Override
            public void onLocationChanged( final Location location ) {
                // forward location updates fired by LocationProvider to architectView, you can set lat/lon from any location-strategy
                if (location!=null) {
                    // sore last location as member, in case it is needed somewhere (in e.g. your adjusted project)
                    //位置をセット
                    MainActivity.this.lastKnownLocaton = location;
                    if ( MainActivity.this.architectView != null ) {
                        // check if location has altitude at certain accuracy level & call right architect method (the one with altitude information)
                        //誤差が7m未満の時.hasAltitudeは標高。
                        //位置情報をARのビューの位置情報に設定。これでJavaScriptで設定した。AR.context.onLocationChangedの関数が動く。
                        if ( location.hasAltitude() && location.hasAccuracy() && location.getAccuracy()<7) {
                            MainActivity.this.architectView.setLocation( location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy() );
                        } else {
                            MainActivity.this.architectView.setLocation( location.getLatitude(), location.getLongitude(), location.hasAccuracy() ? location.getAccuracy() : 1000 );
                        }
                    }
                }
            }
        };
        //位置のプロバイダーをセット。getLocationProviderは以下で定義
        this.locationProvider = getLocationProvider(this.locationListener);
    }
    //onCreate直後に実行
    @Override
    protected void onPostCreate( final Bundle savedInstanceState ) {
        super.onPostCreate(savedInstanceState);

        if ( this.architectView != null ) {
       // call mandatory live-cycle method of architectView
            this.architectView.onPostCreate();
            try {
                //inde.htmlのパスからコンテンツをロード
                //描画処理は、index.htmlとなかにあるpoiatlocation.jsに記述する。
                this.architectView.load( this.getARchitectWorldPath() );
                //50km以内のものだけ表示
                this.architectView.setCullingDistance(50 * 1000); /* 50km */
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ( this.architectView != null ) {
            this.architectView.onResume();
            if (this.sensorAccuracyListener!=null) {
                //registしなおす
                this.architectView.registerSensorAccuracyChangeListener( this.sensorAccuracyListener );
            }
        }
        if ( this.locationProvider != null ) {
            this.locationProvider.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if ( this.architectView != null ) {
            this.architectView.onPause();
            if ( this.sensorAccuracyListener != null ) {
                //regist解除
                this.architectView.unregisterSensorAccuracyChangeListener(this.sensorAccuracyListener);
            }
        }
        if ( this.locationProvider != null ) {
            this.locationProvider.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // call mandatory live-cycle method of architectView
        if ( this.architectView != null ) {
            this.architectView.onDestroy();
        }
    }
    //ライセンス返す
    protected String getWikitudeSDKLicenseKey() {
        return WikitudeSDKConstants.WIKITUDE_SDK_KEY;
    }
    //index.htmlのパス
    protected String getARchitectWorldPath() {
        return "wikitude/index.html"; /* assets folder */
    }
    //カメラの場所
    protected StartupConfiguration.CameraPosition getCameraPosition() {
        //背面カメラを使う
        return StartupConfiguration.CameraPosition.BACK;
    }

    public ArchitectViewHolderInterface.ILocationProvider getLocationProvider(final LocationListener locationListener) {
        return new LocationProvider(this, locationListener);
    }

    private long lastCalibrationToastShownTimeMillis = System.currentTimeMillis();

    //センサーの精度が悪い時にワーニングを表示
    public ArchitectView.SensorAccuracyChangeListener getSensorAccuracyListener() {
        return new ArchitectView.SensorAccuracyChangeListener() {
            @Override
            public void onCompassAccuracyChanged( int accuracy ) {
				/* UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3 */
                if ( accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && MainActivity.this != null && !MainActivity.this.isFinishing() && System.currentTimeMillis() - MainActivity.this.lastCalibrationToastShownTimeMillis > 5 * 1000) {
                    Toast.makeText( MainActivity.this, R.string.compass_accuracy_low, Toast.LENGTH_LONG ).show();
                    MainActivity.this.lastCalibrationToastShownTimeMillis = System.currentTimeMillis();
                }
            }
        };
    }
}
