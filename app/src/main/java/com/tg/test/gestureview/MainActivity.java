package com.tg.test.gestureview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

/**
 * 自定义手势解锁
 */
public class MainActivity extends AppCompatActivity {
    GestureView gestureView;
    ShowGestureView showGestureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gestureView = findViewById(R.id.gesture);
        showGestureView = findViewById(R.id.show_gesture);

        gestureView.setGestureFinish(new GestureView.GestureFinish() {
            @Override
            public void onFinish(String password, int pointNumber) {
                Log.d("xudaha", "password is" + password);
                showGestureView.showGesture(-1);
                if(pointNumber < 4)
                    gestureView.showErrorUi();//连点小于4个
                else
                    gestureView.releasePoint(true,true);//表示手势成功

            }

            @Override
            public void onMoveIndex(int index) {
                showGestureView.showGesture(index);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gestureView.onDestory();
    }
}
