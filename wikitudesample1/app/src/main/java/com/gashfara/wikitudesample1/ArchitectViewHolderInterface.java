//サンプルのインターフェース。そのまま使う。どのアプリでも同じ。
package com.gashfara.wikitudesample1;

import android.location.LocationListener;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;

public interface ArchitectViewHolderInterface {
    public static interface ILocationProvider {
        public void onResume();
        public void onPause();
    }
}
