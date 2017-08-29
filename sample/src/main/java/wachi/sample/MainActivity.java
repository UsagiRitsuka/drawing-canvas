package wachi.sample;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import wachi.drawingview.WachiDrawingView;

public class MainActivity extends AppCompatActivity {
    private WachiDrawingView drawingView;
    private Button eraser;
    private Button brush;
    private Button undo;
    private Button redo;
    private Button strok1;
    private Button strok2;
    private Button strok3;
    private Button red;
    private Button yellow;
    private Button black;
    private Button loadImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();
        setupComponent();
    }

    private void findView(){
        drawingView = findViewById(R.id.wachi_drawing_view);
        eraser = findViewById(R.id.eraser);
        brush = findViewById(R.id.brush);
        undo = findViewById(R.id.undo);
        redo = findViewById(R.id.redo);
        strok1 = findViewById(R.id.pen_stroke_1);
        strok2 = findViewById(R.id.pen_stroke_2);
        strok3 = findViewById(R.id.pen_stroke_3);
        red = findViewById(R.id.pen_color_1);
        yellow = findViewById(R.id.pen_color_2);
        black = findViewById(R.id.pen_color_3);
        loadImg = findViewById(R.id.load_img);
    }

    private void setupComponent(){
        drawingView.setConfig(800, 800);

        eraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.change2Eraser();
            }
        });

        brush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.change2Brush();
            }
        });

        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.undo();
            }
        });

        redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.redo();
            }
        });

        strok1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.setStrokeWidth(10);
            }
        });

        strok2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.setStrokeWidth(20);
            }
        });

        strok3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.setStrokeWidth(40);
            }
        });


        black.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.setColor(Color.BLACK);
            }
        });

        red.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.setColor(Color.RED);
            }
        });

        yellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.setColor(Color.YELLOW);
            }
        });

        loadImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawingView.loadImg(R.drawable.test);
            }
        });
    }
}
