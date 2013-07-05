package com.maistora.facedetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("NewApi")
public class FaceDetector extends Activity implements SurfaceHolder.Callback {

	private Camera camera;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private boolean previewing = false;

	private ImageButton buttonTakePicture;
	private TextView facesCountView;
//	private TextView imageSaved;

	final int RESULT_SAVEIMAGE = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_face_detector);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().setFormat(PixelFormat.UNKNOWN);
		
		surfaceInit();
		
		addContentView();

		addClickListenerToTakePicButton();

		addClickListenerToBackground();

		facesCountView = (TextView) findViewById(R.id.count);
//		imageSaved = (TextView) findViewById(R.id.imageSaved);
	}

	private void addClickListenerToTakePicButton() {
		buttonTakePicture = (ImageButton) findViewById(R.id.takepicture);
		buttonTakePicture.setOnClickListener(new ImageButton.OnClickListener() {
		
			@Override
			public void onClick(View arg0) {
				camera.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);
			}
		});
	}

	private void addClickListenerToBackground() {
		final LinearLayout layoutBackground = (LinearLayout) findViewById(R.id.background);
		
		layoutBackground.setOnClickListener(new LinearLayout.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				buttonTakePicture.setEnabled(false);
				camera.autoFocus(myAutoFocusCallback);
			}
		});
	}

	@SuppressWarnings("deprecation")
	private void addContentView() {
		final LayoutInflater controlInflater = LayoutInflater.from(getBaseContext());
		final View viewControl = controlInflater.inflate(R.layout.control, null);
		final LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		this.addContentView(viewControl, layoutParamsControl);
	}

	@SuppressWarnings("deprecation")
	private void surfaceInit() {
		surfaceView = (SurfaceView) findViewById(R.id.camerapreview);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	private FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {

		@Override
		public void onFaceDetection(Face[] faces, Camera camera) {

			if (faces.length == 0) {
				facesCountView.setText("0");
			} else {
				facesCountView.setText(String.valueOf(faces.length));
			}

		}
	};

	private AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			buttonTakePicture.setEnabled(true);
		}
	};

	private ShutterCallback myShutterCallback = new ShutterCallback() {

		@Override
		public void onShutter() {
		}
	};

	private PictureCallback myPictureCallback_RAW = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] arg0, Camera arg1) {
		}
	};

	private PictureCallback myPictureCallback_JPG = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] byteArr, Camera cam) {
			final File albumStorageDir = getAlbumStorageDir("FaceDetector", getImageName());

			OutputStream imageFileOS;
			try {
				imageFileOS = new FileOutputStream(albumStorageDir);
				imageFileOS.write(byteArr);
				imageFileOS.flush();
				imageFileOS.close();
				
//				imageSaved.setText("Image saved: " + albumStorageDir.getAbsolutePath());
				
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
			
			camera.startPreview();
			camera.startFaceDetection();
		}

		@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
		private String getImageName() {
			final String currentDate = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(new Date());
			final int faceNum = Integer.parseInt(facesCountView.getText().toString());
			
			return String.format("%s-%d-face%s.jpg", currentDate, faceNum, (faceNum == 1 ? "" : "s"));
		}
		
		private File getAlbumStorageDir(String albumName, String imageName) {
		    final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);
		    if (!file.getParentFile().mkdirs()) {
		    	System.err.println("Directories not created.");
		    }
		    
			return new File(file, imageName);
		}
	};

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		if (previewing) {
			camera.stopFaceDetection();
			camera.stopPreview();
			previewing = false;
		}

		if (camera != null) {
			try {
				camera.setPreviewDisplay(surfaceHolder);
				camera.startPreview();

				facesCountView.setText(String.valueOf("Max Face: " + camera.getParameters().getMaxNumDetectedFaces()));
				camera.startFaceDetection();
				previewing = true;
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();
		camera.setFaceDetectionListener(faceDetectionListener);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		camera.stopFaceDetection();
		camera.stopPreview();
		camera.release();
		camera = null;
		previewing = false;
	}

}
