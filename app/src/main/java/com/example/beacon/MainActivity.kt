package com.example.beacon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import org.altbeacon.beacon.*

class MainActivity : AppCompatActivity(), BeaconConsumer {

    private var beaconManager: BeaconManager? = null

    private val uuidString: String = "00000000-0000-0000-0000-000000000000"
    private val uuid = Identifier.parse(uuidString)

    private val IBEACON_FORMAT: String = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"

    // Handlerクラスの変数の宣言(追加)
    private val handler: Handler = Handler()
    //private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            // 未対応の場合、Toast表示
            showToast("このデバイスはBLE未対応です", Toast.LENGTH_LONG)
        }

        // API 23以上かのチェック
        if (Build.VERSION.SDK_INT >= 23) {

            // パーミッションの要求
            checkPermission()
        }

        // ビーコンマネージャのインスタンスを生成
        beaconManager = BeaconManager.getInstanceForApplication(this)

        // BeaconManagerの設定
        beaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout(IBEACON_FORMAT))
    }

    // onResume
    override fun onResume() {
        super.onResume()

        // ビーコンサービスの開始
        beaconManager!!.bind(this)
    }

    // onPause
    override fun onPause() {
        super.onPause()

        // ビーコンサービスの停止
        beaconManager!!.unbind(this)
    }


    /**************************************************
     * BeaconConsumer内のメソッドをoverride
     **************************************************/
    // onBeaconServiceConnect
    override fun onBeaconServiceConnect() {

        try {
            // ビーコン情報の監視を開始、第3,4引数はmajor・minor値を指定する時に使用
            beaconManager!!.startMonitoringBeaconsInRegion(Region("ここは適用な文字列", uuid, null, null))
        }
        catch (e: RemoteException) {
            e.printStackTrace()
        }

        // モニタリングの通知受取り処理
        beaconManager!!.addMonitorNotifier(object: MonitorNotifier {

            // 領域内に侵入した時に呼ばれる
            override fun didEnterRegion(region: Region) {

                // レンジングの開始
                beaconManager!!.startRangingBeaconsInRegion(region)
            }

            // 領域外に退出した時に呼ばれる
            override fun didExitRegion(region: Region) {

                // レンジングの停止
                beaconManager!!.stopRangingBeaconsInRegion(region)
            }

            // 領域への侵入/退出のステータスが変化した時に呼ばれる
            override fun didDetermineStateForRegion(i: Int, region: Region) {
                //
            }
        })

        // レンジングの通知受け取り処理
        beaconManager!!.addRangeNotifier(object: RangeNotifier {

            // 範囲内のビーコン情報を受け取る
            override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region){

                var maxMajor: Int?
                var maxMinor: Int?

                // 範囲内の複数のビーコン情報を保持させる変数
                var getMajorList: ArrayList<Int> = ArrayList()
                var getMinorList: ArrayList<Int> = ArrayList()
                var getRssiList: ArrayList<Int> = ArrayList()

                // 範囲内にビーコンがある時の処理
                if (beacons.size > 0) {

                    // 範囲内のビーコンの数だけ繰り返す
                    for (beacon in beacons) {
                        // 複数のビーコン情報をArrayListに分割
                        getMajorList.add(beacon.id2.toInt())
                        getMinorList.add(beacon.id3.toInt())
                        getRssiList.add(beacon.rssi)
                    }

                    // RSSIが最も大きいインデックスを取得
                    var indexRssi: Int = getRssiList.indexOf(getRssiList.max())

                    // 取得したインデックスのmajor値・minor値を取得
                    maxMajor = getMajorList[indexRssi]
                    maxMinor = getMinorList[indexRssi]

                    Log.d("Test_Major:", maxMajor.toString())
                    Log.d("Test_Minor", maxMinor.toString())

                    // メインスレッドで実装(追加)
                    handler.post {
                        // 空の引数を渡して、Viewの更新
                        viewUpdate(maxMajor, maxMinor)
                    }
                }
            }
        })
    }


    /**************************************************
     * メソッド
     **************************************************/
    // トースト表示のメソッド
    fun showToast(text: String, length: Int) {

        // トーストの生成と表示
        var toast: Toast = Toast.makeText(this, text, length)
        toast.show()
    }

    // パーミッションの許可チェック
    @RequiresApi(Build.VERSION_CODES.M)
    fun checkPermission() {

        // パーミッション未許可の時
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // パーミッションの許可ダイアログの表示
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }
    }

    fun viewUpdate(major: Int?, minor: Int?) {

        // Viewの取得
        var majorTextView: TextView = findViewById(R.id.major) as TextView
        var minorTextView: TextView = findViewById(R.id.minor) as TextView

        majorTextView.text = "major:" + major
        minorTextView.text = "minor:" + minor
    }

}
