package com.github.paolorotolo.appintro;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Vector;

public abstract class AppIntro extends AppCompatActivity {
    private PagerAdapter mPagerAdapter;
    private ViewPager pager;
    private List<Fragment> fragments = new Vector<>();
    private List<ImageView> dots;
    private int slidesNumber;
    private Vibrator mVibrator;
    private IndicatorController mController;
    private boolean isVibrateOn = false;
    private int vibrateIntensity = 20;
    private boolean showSkip = true;
    private boolean showDone = true;

    static enum TransformType {
        FLOW,
        DEPTH,
        ZOOM,
        SLIDE_OVER,
        FADE
    }

    @Override
    final protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.intro_layout);

        final TextView skipButton = (TextView) findViewById(R.id.skip);
        final ImageView nextButton = (ImageView) findViewById(R.id.next);
        final TextView doneButton = (TextView) findViewById(R.id.done);
        mVibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View v) {
                if (isVibrateOn) {
                    mVibrator.vibrate(vibrateIntensity);
                }
                onSkipPressed();
            }
        });

        int nextDrawable = R.drawable.ic_navigate_next_white_24dp;
        if (isRtlLayout()) {
            nextDrawable = R.drawable.ic_navigate_back_white;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            nextButton.setImageDrawable(getResources().getDrawable(nextDrawable));
        } else {
            nextButton.setImageDrawable(getResources().getDrawable(nextDrawable, this.getTheme()));
        }
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View v) {
                if (isVibrateOn) {
                    mVibrator.vibrate(vibrateIntensity);
                }
                if (isRtlLayout()) {
                    pager.setCurrentItem(pager.getCurrentItem() - 1);
                } else {
                    pager.setCurrentItem(pager.getCurrentItem() + 1);
                }
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View v) {
                if (isVibrateOn) {
                    mVibrator.vibrate(vibrateIntensity);
                }
                onDonePressed();
            }
        });

        if (isRtlLayout()) {
            mPagerAdapter = new RtlPagerAdapter(super.getSupportFragmentManager(), fragments);
        } else {
            mPagerAdapter = new PagerAdapter(super.getSupportFragmentManager(), fragments);
        }
        pager = (ViewPager) findViewById(R.id.view_pager);

        pager.setAdapter(this.mPagerAdapter);
        /**
         *  ViewPager.setOnPageChangeListener is now deprecated. Use addOnPageChangeListener() instead of it.
         */
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (slidesNumber > 1)
                    if (isRtlLayout()) {
                        mController.selectPosition(slidesNumber - 1 - position);
                    } else {
                        mController.selectPosition(position);
                    }
                if ((!isRtlLayout() && position == slidesNumber - 1) || (isRtlLayout() && position == 0)) {
                    skipButton.setVisibility(View.INVISIBLE);
                    nextButton.setVisibility(View.GONE);
                    if (showDone) {
                        doneButton.setVisibility(View.VISIBLE);
                    } else {
                        doneButton.setVisibility(View.INVISIBLE);
                    }
                } else {
                    skipButton.setVisibility(View.VISIBLE);
                    doneButton.setVisibility(View.GONE);
                    nextButton.setVisibility(View.VISIBLE);
                }

                if (!showSkip) {
                    skipButton.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        init(savedInstanceState);
        slidesNumber = fragments.size();

        if (slidesNumber == 1) {
            nextButton.setVisibility(View.GONE);
            doneButton.setVisibility(View.VISIBLE);
        } else {
            initController();
        }


        if (isRtlLayout()) {
            pager.setCurrentItem(slidesNumber - 1);
        }
    }

    public ViewPager getPager() {
        return pager;
    }


    private void initController() {
        if (mController == null)
            mController = new DefaultIndicatorController();

        FrameLayout indicatorContainer = (FrameLayout) findViewById(R.id.indicator_container);
        indicatorContainer.addView(mController.newInstance(this));

        mController.initialize(slidesNumber);
    }
    public void selectDot(int index) {
        if (fragments.size() == 0) {
            return;
        }

        Resources res = getResources();
        for (int i = 0; i < fragments.size(); i++) {
            int drawableId = (i == index) ? (R.drawable.indicator_dot_white) : (R.drawable.indicator_dot_grey);
            Drawable drawable = res.getDrawable(drawableId);
            dots.get(i).setImageDrawable(drawable);
        }
        onDotSelected(index);
    }

    public void addSlide(@NonNull Fragment fragment) {
        fragments.add(fragment);
        mPagerAdapter.notifyDataSetChanged();
        slidesNumber = fragments.size();

        if (mController != null) {
            mController.initialize(fragments.size());
        }
    }

    @NonNull
    public List<Fragment> getSlides() {
        return mPagerAdapter.getFragments();
    }

    public void setBarColor(@ColorInt final int color) {
        LinearLayout bottomBar = (LinearLayout) findViewById(R.id.bottom);
        bottomBar.setBackgroundColor(color);
    }

    public void setSeparatorColor(@ColorInt final int color) {
        TextView separator = (TextView) findViewById(R.id.bottom_separator);
        separator.setBackgroundColor(color);
    }

    public void setSkipText(@Nullable final String text) {
        TextView skipText = (TextView) findViewById(R.id.skip);
        skipText.setText(text);
    }

    public void setDoneText(@Nullable final String text) {
        TextView doneText = (TextView) findViewById(R.id.done);
        doneText.setText(text);
    }

    public void showSkipButton(boolean showButton) {
        this.showSkip = showButton;
        if (!showButton) {
            TextView skip = (TextView) findViewById(R.id.skip);
            skip.setVisibility(View.INVISIBLE);
        }
    }

    public void showDoneButton(boolean showDone) {
        this.showDone = showDone;
        if (!showDone) {
            TextView done = (TextView) findViewById(R.id.done);
            done.setVisibility(View.GONE);
        }
    }

    public void setVibrate(boolean vibrate) {
        this.isVibrateOn = vibrate;
    }

    public void setVibrateIntensity(int intensity) {
        this.vibrateIntensity = intensity;
    }

    public void setFadeAnimation() {
        pager.setPageTransformer(true, new ViewPageTransformer(ViewPageTransformer.TransformType.FADE));
    }
    public void setZoomAnimation() {
        pager.setPageTransformer(true, new ViewPageTransformer(ViewPageTransformer.TransformType.ZOOM));
    }
    public void setFlowAnimation() {
        pager.setPageTransformer(true, new ViewPageTransformer(ViewPageTransformer.TransformType.FLOW));
    }
    public void setSlideOverAnimation() {
        pager.setPageTransformer(true, new ViewPageTransformer(ViewPageTransformer.TransformType.SLIDE_OVER));
    }
    public void setDepthAnimation() {
        pager.setPageTransformer(true, new ViewPageTransformer(ViewPageTransformer.TransformType.DEPTH));
    }


    public void setCustomTransformer(@Nullable ViewPager.PageTransformer transformer) {
        pager.setPageTransformer(true, transformer);
    }

    public void setOffScreenPageLimit(int limit) {
        pager.setOffscreenPageLimit(limit);
    }

    /**
     * Set a progress indicator instead of dots. This is recommended for a large amount of slides. In this case there
     * could not be enough space to display all dots on smaller device screens.
     */
    public void setProgressIndicator() {
        mController = new ProgressIndicatorController();
    }

    /**
     * Set a custom {@link IndicatorController} to use a custom indicator view for the {@link AppIntro} instead of the
     * default one.
     *
     * @param controller The controller to use
     */
    public void setCustomIndicator(@NonNull IndicatorController controller) {
        mController = controller;
    }

    public abstract void init(@Nullable Bundle savedInstanceState);

    public abstract void onSkipPressed();

    public abstract void onDonePressed();
    
    public void onDotSelected(int index) {}

    @Override
    public boolean onKeyDown(int code, KeyEvent kvent) {
        if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_BUTTON_A || code == KeyEvent.KEYCODE_DPAD_CENTER) {
            ViewPager vp = (ViewPager) this.findViewById(R.id.view_pager);
            if (vp.getCurrentItem() == vp.getAdapter().getCount() - 1) {
                onDonePressed();
            } else {
                vp.setCurrentItem(vp.getCurrentItem() + 1);
            }
            return false;
        }
        return super.onKeyDown(code, kvent);
    }

    protected boolean isRtlLayout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return (TextUtils.getLayoutDirectionFromLocale(getResources().getConfiguration().locale) == View.LAYOUT_DIRECTION_RTL);
        }
        return false;
    }
}
