package com.example.facecapture.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import androidx.constraintlayout.widget.ConstraintLayout;


public class CircleOverlayView extends ConstraintLayout {
    private Bitmap bitmap;

    public CircleOverlayView(Context context) { super(context); }
    public CircleOverlayView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        bitmap = null;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (bitmap == null) {
            createWindowFrame();
        }
        canvas.drawBitmap(bitmap, 0f, 0f, null);
    }

    private void createWindowFrame() {
        float margin = 30f;
        float radius = (getWidth() - 2 * margin) / 2f;
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float textViewY = centerY - radius - 20;
        String title = "CUBOX";

        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        RectF outerRectangle = new RectF(0f, 0f, getWidth(), getHeight()); // 뷰의 백그라운드를 나타내는 사각형을 정의
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(Color.BLACK);
        paint.setAlpha(180);

        canvas.drawRect(outerRectangle, paint);

        // 텍스트 추가
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);

        canvas.drawText(title, centerX, textViewY, paint);

        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)); // punch 해주는 효과

        canvas.drawCircle(centerX, centerY, radius, paint);
    }
}
