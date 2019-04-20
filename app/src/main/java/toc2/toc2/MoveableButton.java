package toc2.toc2;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.animation.DynamicAnimation;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class MoveableButton extends AppCompatImageButton {

    private float posX = 0;
    private float posY = 0;

    private float dXstart;
    private float dYstart;

    private boolean isMoving = false;

    private final Bundle properties = new Bundle();

    private final SpringAnimation springAnimationX = new SpringAnimation(this, DynamicAnimation.X).setSpring(
         new SpringForce()
                 .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY)
                 .setStiffness(SpringForce.STIFFNESS_HIGH));
    private final SpringAnimation springAnimationY = new SpringAnimation(this, DynamicAnimation.Y).setSpring(
         new SpringForce()
                 .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY)
                 .setStiffness(SpringForce.STIFFNESS_HIGH));

    private final ValueAnimator colorAnimation = ValueAnimator.ofObject(
            new ArgbEvaluator(),
            ContextCompat.getColor(getContext(), R.color.colorPrimary),
            ContextCompat.getColor(getContext(), R.color.colorPrimaryDark),
            ContextCompat.getColor(getContext(), R.color.colorPrimary));

    public interface PositionChangedListener {
        void onPositionChanged(MoveableButton button, float posX, float posY);

        void onStartMoving(MoveableButton button, float posX, float posY);

        void onEndMoving(MoveableButton button, float posX, float posY);
    }


    private PositionChangedListener positionChangedListener = null;

    private OnClickListener onClickListener = null;
    private final GestureDetector mTapDetector;

    private class GestureTap extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.v("testlayout", "Button clicked");
            animateColor();
            if(onClickListener != null) {
                onClickListener.onClick(MoveableButton.this);
                return true;
            }
            return false;
        }
    }


    MoveableButton(Context context){
        super(context);

        colorAnimation.setDuration(100); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                setBackgroundColor((int) animator.getAnimatedValue());
            }
        });

        mTapDetector = new GestureDetector(context, new GestureTap());

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();

                boolean clicked = mTapDetector.onTouchEvent(event);
                if(clicked)
                    v.performClick();

                switch(action){
                    case MotionEvent.ACTION_DOWN:
                        posX = v.getX();
                        posY = v.getY();
                        dXstart = v.getX() - event.getRawX();
                        dYstart = v.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dXstart;
                        float newY = event.getRawY() + dYstart;
                        if (isMoving) {
                            v.animate()
                                    .x(newX)
                                    .y(newY)
                                    .setDuration(0)
                                    .start();
                            if (positionChangedListener != null)
                                positionChangedListener.onPositionChanged(MoveableButton.this, getX(), getY());
                        } else {
                            float dx = posX - newX;
                            float dy = posY - newY;
                            final float resistanceDist = dp_to_px(10);
                            if (dx * dx + dy * dy > resistanceDist * resistanceDist) {
                                isMoving = true;
                                setElevation(10);
                                if(positionChangedListener != null)
                                    positionChangedListener.onStartMoving(MoveableButton.this, getX(), getY());
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if(isMoving) {
                            isMoving = false;
                            setElevation(2);
                            setNewPosition(posX, posY);
                            //clearColorFilter();
                            if (positionChangedListener != null)
                                positionChangedListener.onEndMoving(MoveableButton.this, getX(), getY());
                        }
                        break;
                }

                return false;
            }
        });

    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
        posX = marginLayoutParams.leftMargin;
        posY = marginLayoutParams.topMargin;
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener){
        this.onClickListener = onClickListener;
    }

    void setNewPosition(float newX, float newY) {
        posX = newX;
        posY = newY;

        if (isMoving) {
            return;
        }

        int dx = Math.round(posX - getX());
        int dy = Math.round(posY - getY());
        if(dx == 0 && dy == 0)
            return;

        springAnimationX.getSpring().setFinalPosition(posX);
        springAnimationY.getSpring().setFinalPosition(posY);

        springAnimationX.start();
        springAnimationY.start();
    }

    public void setOnPositionChangedListener(PositionChangedListener positionChangedListener){
        this.positionChangedListener = positionChangedListener;
    }

    public void animateColor() {
        colorAnimation.start();
    }

    public Bundle getProperties(){
        return properties;
    }

    public void setProperties(Bundle newProperties) {
        properties.putAll(newProperties);
        setImageResource(Sounds.getIconID(properties.getInt("soundid", 0)));
        Log.v("Metronome", "Setting new button properties " + properties.getFloat("volume",-1));
    }

    private int dp_to_px(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
