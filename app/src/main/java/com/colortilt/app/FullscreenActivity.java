package com.colortilt.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.colortilt.app.util.SystemUiHider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity implements SensorEventListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
    private static final float EPSILON = 1/100;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private View mainLayout;
    private View contentView;

    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getActionBar().hide();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        contentView = findViewById(R.id.fullscreen_content);
        //here I set it to a view because I still get the setBackgroundColor method.
        mainLayout = findViewById(R.id.mainLayout);


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });


    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample, not normalized yet.
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];


            Boolean isCounterColor = false;

            String xAxis = convertToColorString(axisX, isCounterColor);
            String yAxis = convertToColorString(axisY, isCounterColor);
            String zAxis = convertToColorString(axisZ, isCounterColor);

            isCounterColor = true;

            String xAxisOpp = convertToColorString(axisX, isCounterColor);
            String yAxisOpp = convertToColorString(axisY, isCounterColor);
            String zAxisOpp = convertToColorString(axisZ, isCounterColor);

            Log.e("DEBUG", "X-axis: " + xAxis);
            Log.e("DEBUG","Y-axis: " + yAxis);
            Log.e("DEBUG","Z-axis: " + zAxis);
            String color = "#" + xAxis + yAxis + zAxis;
            String colorOpp = "#" + xAxisOpp + yAxisOpp + zAxisOpp;
            Log.e("DEBUG","color: " + color);
            Log.e("DEBUG","color num: " +((Integer)Color.parseColor(color)).toString());
            Log.e("DEBUG","Textcolor: " + colorOpp);
            Log.e("DEBUG","Textcolor num: " +((Integer)Color.parseColor(colorOpp)).toString());

            mainLayout.setBackgroundColor(Color.parseColor(color));
            ((TextView) contentView).setTextColor(Color.parseColor(colorOpp));
//            contentView.setBackgroundColor(Color.parseColor(colorOpp));

            // Calculate the angular speed of the sample
            float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

            // Normalize the rotation vector if it's big enough to get the axis
            // (that is, EPSILON should represent your maximum allowable margin of error)
                        if (omegaMagnitude > EPSILON) {
                            axisX /= omegaMagnitude;
                            axisY /= omegaMagnitude;
                            axisZ /= omegaMagnitude;
                        }

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            float thetaOverTwo = omegaMagnitude * dT / 2.0f;
            float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
            float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
            deltaRotationVector[0] =  axisX;
            deltaRotationVector[1] =  axisY;
            deltaRotationVector[2] = axisZ;
            deltaRotationVector[3] = cosThetaOverTwo;
//            Log.e("DEBUG","X-axis: " + Float.toString(deltaRotationVector[0]));
//            Log.e("DEBUG","Y-axis: " + Float.toString(deltaRotationVector[1]));
//            Log.e("DEBUG","Z-axis: " + Float.toString(deltaRotationVector[2]));
        }
        timestamp = event.timestamp;
        float[] deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        // rotationCurrent = rotationCurrent * deltaRotationMatrix;
    }



    private String convertToColorString(float flert, Boolean isCounterColor) {
        flert = Math.abs(flert);
        flert = flert*10000;
        int integer = Math.round(flert);

        // this sets up the opposition of colors.
        if (isCounterColor) {
            integer = 100 - integer;
            integer = Math.abs(integer);
        }

        Integer blah = integer;

        String poop = blah.toString();

        if (poop.length() == 1) {
            poop = "0" + poop;
        }

        if (poop.length() == 3) {
            poop =  poop.substring(1);
        }

        if (poop.length() == 4) {
            poop =  poop.substring(2);
        }

        if (poop.length() == 5) {
            poop =  poop.substring(3);
        }

        if (poop.length() == 6) {
            poop =  poop.substring(4);
        }
        return poop;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
