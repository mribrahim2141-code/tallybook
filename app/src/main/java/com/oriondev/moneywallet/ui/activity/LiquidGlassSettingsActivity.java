package com.oriondev.moneywallet.ui.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.ui.view.theme.LiquidGlassSettings;
import java.util.Locale;

/** Native Android Liquid Glass controls; no WebView, CSS or additional dependency. */
public class LiquidGlassSettingsActivity extends AppCompatActivity {
    private static final String[] GLASS_KEYS = {"blur","refraction_height","refraction_amount","tint","vibrancy","brightness","gloss","corner_radius"};
    private static final String[] MOTION_KEYS = {"bounce","speed","compression","pull_stretch","long_press","haptics_strength"};
    private static final int[] DEFAULTS = {42,34,45,28,72,100,48,28};
    private static final String[] AR = {"التمويه","ارتفاع الانكسار","مقدار الانكسار","الصباغة","الحيوية","السطوع","اللمعة","نصف قطر الزوايا","الارتداد","السرعة","عمق الضغط","التمدد عند السحب","التضخم عند الضغط المطول","الاهتزاز عند الإفلات"};
    private static final String[] EN = {"Blur","Refraction height","Refraction amount","Tint","Vibrancy","Brightness","Gloss","Corner radius","Bounce","Speed","Compression depth","Pull stretch","Long-press inflation","Release haptics"};
    private View preview, ambient;
    private TextView previewCaption;
    private final int purple = Color.rgb(151, 112, 255);
    private final java.util.Map<String, Integer> previewValues = new java.util.HashMap<>();
    private int motionPreset = 0;

    private boolean ar() { return "ar".equals(Locale.getDefault().getLanguage()); }
    private String s(String arabic, String english) { return ar() ? arabic : english; }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(20, 24, 54));
        getWindow().setNavigationBarColor(Color.rgb(20, 24, 54));
        buildScreen();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(28));
        GradientDrawable page = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(27, 31, 75), Color.rgb(71, 42, 104), Color.rgb(26, 77, 113)});
        root.setBackground(page);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView back = text("‹", 30, Color.WHITE);
        back.setPadding(dp(8), 0, dp(16), 0);
        back.setOnClickListener(v -> finish());
        top.addView(back);
        TextView title = text(s("تخصيص الزجاج السائل","Liquid Glass Studio"), 23, Color.WHITE);
        title.setTypeface(null, 1);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(top);
        TextView subtitle = text(s("تحكم بالمظهر والحركة مع معاينة فورية","Tune appearance and motion with a live preview"), 13, 0xFFD9D7F4);
        subtitle.setPadding(dp(4), dp(4), dp(4), dp(14));
        root.addView(subtitle);

        addSection(root, s("معاينة مباشرة","Live preview"));
        ambient = new View(this);
        GradientDrawable glow = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF9D72FF,0xFF4DA9E8,0xFFD86CB8});
        glow.setCornerRadius(dp(28));
        ambient.setBackground(glow);
        LinearLayout previewWrap = new LinearLayout(this);
        previewWrap.setPadding(dp(14), dp(14), dp(14), dp(14));
        previewWrap.addView(ambient, new LinearLayout.LayoutParams(-1, dp(44)));
        preview = new LinearLayout(this);
        ((LinearLayout)preview).setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(dp(18), dp(18), dp(18), dp(18));
        previewCaption = text(s("بطاقة زجاجية تفاعلية","Interactive glass surface"), 16, Color.WHITE);
        previewCaption.setTypeface(null, 1);
        ((LinearLayout)preview).addView(previewCaption);
        TextView sample = text(s("تظهر تغييراتك هنا فوراً","Your adjustments appear here instantly"), 13, 0xFFECEBFF);
        sample.setPadding(0, dp(8), 0, 0);
        ((LinearLayout)preview).addView(sample);
        previewWrap.addView(preview, new LinearLayout.LayoutParams(-1, dp(116)));
        root.addView(previewWrap);
        applyPreview();

        addSection(root, s("أنماط جاهزة","Presets"));
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER);
        String[] names = {s("الافتراضي","Default"),s("سهل القراءة","Readable"),s("كريستال","Crystal"),s("الأداء","Performance")};
        for (int i=0;i<names.length;i++) {
            final int index=i;
            Button b=button(names[i]);
            b.setTextSize(11);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1);
            lp.setMargins(dp(2),0,dp(2),0);
            presets.addView(b,lp);
            b.setOnClickListener(v -> applyPreset(index));
        }
        root.addView(presets);

        addSection(root, s("خصائص الزجاج","Glass properties"));
        for(int i=0;i<GLASS_KEYS.length;i++) addSlider(root, GLASS_KEYS[i], AR[i], EN[i], false);
        addToggle(root,s("تأثير العمق","Depth effect"),"depth");
        addToggle(root,s("الزيغ اللوني","Chromatic aberration"),"chromatic");

        addSection(root,s("الحركة والفيزياء","Motion & physics"));
        RadioGroup group=new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        String[] motionNames={s("عادي","Normal"),s("الأداء","Performance"),s("مرن","Elastic"),s("مرن جداً","Very elastic")};
        for(int i=0;i<motionNames.length;i++){
            RadioButton rb=new RadioButton(this); rb.setText(motionNames[i]); rb.setTextColor(Color.WHITE); rb.setId(100+i);
            group.addView(rb,new RadioGroup.LayoutParams(0,-2,1));
        }
        group.check(100);
        group.setOnCheckedChangeListener((g,id)->applyMotionPreset(id-100));
        root.addView(group);
        Button tryButton=button(s("جربها","Try it"));
        tryButton.setOnClickListener(v->playMotion());
        tryButton.setOnLongClickListener(v->{if (!motionAllowed()) return true; float inflation=1f+LiquidGlassSettings.get(this,"long_press")/350f;preview.animate().scaleX(inflation).scaleY(inflation).setDuration(260).withEndAction(()->preview.animate().scaleX(1f).scaleY(1f).setDuration(220).start()).start();return true;});
        root.addView(tryButton,new LinearLayout.LayoutParams(-1,dp(48)));
        for(int i=0;i<MOTION_KEYS.length;i++) addSlider(root,MOTION_KEYS[i],AR[i+8],EN[i+8],true);
        addToggle(root,s("الاهتزاز عند الإفلات","Release haptics"),"release_haptics");

        addSection(root,s("إعادة الضبط","Reset"));
        Button resetGlass=button(s("↺  إعادة إلى الافتراضي","↺  Reset appearance"));
        resetGlass.setOnClickListener(v->confirmReset(false));
        root.addView(resetGlass,new LinearLayout.LayoutParams(-1,dp(46)));
        Button resetMotion=button(s("↺  إعادة ضبط الحركة","↺  Reset motion"));
        resetMotion.setOnClickListener(v->confirmReset(true));
        LinearLayout.LayoutParams mr=new LinearLayout.LayoutParams(-1,dp(46)); mr.topMargin=dp(8); root.addView(resetMotion,mr);
    }

    private void addSection(LinearLayout root,String label){
        TextView t=text(label,15,0xFFF4F1FF); t.setTypeface(null,1);
        t.setPadding(dp(4),dp(22),dp(4),dp(9)); root.addView(t);
    }
    private TextView text(String value,int size,int color){
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private Button button(String label){
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0x553FFFFF); bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1),0x77FFFFFF); b.setBackground(bg); return b;
    }
    private void addSlider(LinearLayout root,String key,String arabic,String english,boolean motion){
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(13),dp(9),dp(13),dp(7));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0x22FFFFFF); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1),0x33FFFFFF);
        panel.setBackground(bg);
        LinearLayout line=new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text(s(arabic,english),13,Color.WHITE);
        TextView value=text(String.valueOf(LiquidGlassSettings.get(this,key)),13,0xFFE7DCFF);
        value.setGravity(Gravity.END);
        line.addView(label,new LinearLayout.LayoutParams(0,-2,1)); line.addView(value);
        panel.addView(line);
        SeekBar bar=new SeekBar(this); bar.setMax(100); bar.setProgress(LiquidGlassSettings.get(this,key));
        panel.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar seek,int progress,boolean user){
                previewValues.put(key, progress); value.setText(String.valueOf(progress)); applyPreview();
                if(motion) updateMotionPreview(progress);
            }
            public void onStartTrackingTouch(SeekBar seek){}
            public void onStopTrackingTouch(SeekBar seek){LiquidGlassSettings.put(LiquidGlassSettingsActivity.this,key,seek.getProgress()); com.oriondev.moneywallet.ui.view.theme.ThemeEngine.refresh();}
        });
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(7); root.addView(panel,lp);
    }
    private void addToggle(LinearLayout root,String label,String key){
        Switch sw=new Switch(this); sw.setText(label); sw.setTextColor(Color.WHITE); sw.setPadding(dp(12),dp(8),dp(12),dp(8));
        sw.setChecked(LiquidGlassSettings.isEnabled(this,key));
        sw.setOnCheckedChangeListener((CompoundButton b,boolean checked)->{LiquidGlassSettings.put(this,key,checked);applyPreview();com.oriondev.moneywallet.ui.view.theme.ThemeEngine.refresh();});
        root.addView(sw);
    }
    private void applyPreset(int index){
        int[][] presets={
            {42,34,45,28,72,100,48,28},
            {8,15,15,12,85,100,22,20},
            {70,62,78,42,95,108,88,38},
            {0,0,0,8,65,100,10,18}
        };
        for(int i=0;i<GLASS_KEYS.length;i++) LiquidGlassSettings.put(this,GLASS_KEYS[i],presets[index][i]);
        previewValues.clear();
        if(index==3) LiquidGlassSettings.put(this,"depth",false);
        else LiquidGlassSettings.put(this,"depth",true);
        com.oriondev.moneywallet.ui.view.theme.ThemeEngine.refresh();
        buildScreen();
    }
    private void applyMotionPreset(int index){
        int[][] values={{42,100,18,55,45,30},{8,125,8,20,12,0},{68,82,36,75,65,45},{90,70,52,95,88,75}};
        motionPreset=index;
        for(int i=0;i<MOTION_KEYS.length;i++) LiquidGlassSettings.put(this,MOTION_KEYS[i],values[index][i]);
        buildScreen();
    }
    private int current(String key){ return previewValues.containsKey(key) ? previewValues.get(key) : LiquidGlassSettings.get(this,key); }
    private void applyPreview(){
        if(preview==null)return;
        int tint=current("tint"), gloss=current("gloss");
        int radius=current("corner_radius"), brightness=current("brightness"), vibrancy=current("vibrancy");
        int base=ColorUtils.blendARGB(0xAAFFFFFF,0xFF9876FF,tint/100f*0.34f);
        base=ColorUtils.blendARGB(base,Color.WHITE,Math.max(0,brightness-100)/100f*0.16f);
        base=ColorUtils.blendARGB(base,0xFF9876FF,(100-vibrancy)/100f*0.12f);
        GradientDrawable glass=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{ColorUtils.setAlphaComponent(base,0xD8),ColorUtils.setAlphaComponent(ColorUtils.blendARGB(base,0xFF60BFEF,0.22f),0xB8)});
        glass.setCornerRadius(dp(12+radius*.20f));
        glass.setStroke(dp(1),0x99FFFFFF);
        preview.setBackground(glass);
        preview.setElevation(dp(LiquidGlassSettings.isEnabled(this,"depth")?8:1));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
            float blur=current("blur")/18f;
            ambient.setRenderEffect(blur>0?RenderEffect.createBlurEffect(blur,blur,Shader.TileMode.CLAMP):null);
        }
        float glossAlpha=0.12f+gloss/100f*0.35f;
        preview.setAlpha(0.90f+glossAlpha*0.1f);
        preview.setTranslationY(-current("refraction_height")/25f);
        float refraction=current("refraction_amount")/100f;
        preview.setScaleX(1f+refraction*0.025f);
        preview.setScaleY(1f-refraction*0.012f);
        if(LiquidGlassSettings.isEnabled(this,"chromatic")){
            preview.setRotationY(0.4f);
        }else preview.setRotationY(0f);
    }
    private boolean motionAllowed(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return ValueAnimator.areAnimatorsEnabled();
        try { return Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f; }
        catch (RuntimeException ignored) { return true; }
    }
    private void playMotion(){
        if (!motionAllowed()) { preview.setScaleX(1f); preview.setScaleY(1f); return; }
        float bounce=0.85f+LiquidGlassSettings.get(this,"bounce")/100f*0.55f;
        float compression=LiquidGlassSettings.get(this,"compression")/100f*0.16f;
        float stretch=LiquidGlassSettings.get(this,"pull_stretch")/100f*0.12f;
        long duration=Math.max(180,650-LiquidGlassSettings.get(this,"speed")*4L);
        ObjectAnimator x=ObjectAnimator.ofFloat(preview,"scaleX",1f,1f-compression,1f+stretch,1f);
        ObjectAnimator y=ObjectAnimator.ofFloat(preview,"scaleY",1f,1f-compression*1.5f,1f+stretch*0.45f,1f);
        x.setDuration(duration); y.setDuration(duration);
        x.setInterpolator(new OvershootInterpolator(bounce)); y.setInterpolator(new OvershootInterpolator(bounce));
        x.start(); y.start();
        if(LiquidGlassSettings.isEnabled(this,"release_haptics") && LiquidGlassSettings.get(this,"haptics_strength") > 0) preview.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);
    }
    private void updateMotionPreview(int value){ if(preview!=null) preview.setTranslationY(-value/35f); }
    private void confirmReset(boolean motion){
        new AlertDialog.Builder(this).setMessage(s("هل تريد إعادة ضبط هذه الإعدادات؟","Reset these settings?"))
            .setNegativeButton(android.R.string.cancel,null)
            .setPositiveButton(s("إعادة الضبط","Reset"),(d,w)->{LiquidGlassSettings.reset(this,motion);previewValues.clear();com.oriondev.moneywallet.ui.view.theme.ThemeEngine.refresh();buildScreen();}).show();
    }
}
