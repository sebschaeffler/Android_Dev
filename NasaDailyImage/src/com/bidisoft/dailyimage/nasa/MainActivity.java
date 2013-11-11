package com.bidisoft.dailyimage.nasa;

import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bidisoft.nasadailyimage.R;
import com.bidisoft.utils.IotdHandler;
import com.bidisoft.utils.IotdHandlerListener;

public class MainActivity extends Activity implements IotdHandlerListener {

	private static final String URL = "http://www.nasa.gov/rss/image_of_the_day.rss";

	private static Handler handler;
	private ProgressDialog dialog;
	private Bitmap image;
	private String imageUrl;
	private Thread imageThread;

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Bitmap getImage() {
		return image;
	}

	public void setImage(Bitmap image) {
		this.image = image;
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
		setContentView(R.layout.iotd);
	}

	public void onStart() {
		super.onStart();
		refreshFromFeed();
	}

	private void refreshFromFeed() {
		if (isNetworkAvailable()) {
			dialog = ProgressDialog.show(this, getText(R.string.loading),
					getText(R.string.loading_desc));
			Thread th = new Thread(new Runnable() {
				public void run() {
					IotdHandler iotdHandler = new IotdHandler();
					iotdHandler.setListener(MainActivity.this);

					try {
						iotdHandler.processFeed(null, new URL(URL));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			});
			th.start();
		} else {
			handler.post(new Runnable() {
				public void run() {
					Toast.makeText(MainActivity.this,
							getText(R.string.networkNotAvailable),
							Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	public void onRefreshButtonClicked(View view) {
		refreshFromFeed();
	}

	public void onAboutClicked(View view) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle(getText(R.string.about));
		alertDialog.setMessage(getText(R.string.bidisoftDescription));
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

					}
				});
		alertDialog.show();
	}

	public void iotdParsed(final String url, final String title,
			final String description, final String date) {
		handler.post(new Runnable() {
			public void run() {
				TextView titleView = (TextView) findViewById(R.id.imageTitle);
				titleView.setText(title);

				TextView dateView = (TextView) findViewById(R.id.imageDate);
				dateView.setText(date);

				ImageView imageView = (ImageView) findViewById(R.id.imageDisplay);
				setImageUrl(url);
				imageThread = new RefreshImageThread();
				imageThread.start();
				while (imageThread.isAlive()) {
					// //Just wait. The thread needs to be finished, otherwise
					// getImage() will return null.
					// //If you want to see LogCat be filled up with messages,
					// just put a System.out.println() here,
					// //it'll give you an idea on how long the thread is
					// running.
					// //Please note: this is NOT the best way to work in
					// Android, since your application will
					// //actually be paused until the thread finishes. In this
					// case, that is not bad, actually:
					// //we are showing the user a dialog. You could, however,
					// implement behavior here that after
					// //a certain amount of time, the thread is stopped, just
					// to make sure the application will not
					// //hang completely on a slow connection.
				}
				imageView.setImageBitmap(getImage());

				TextView descriptionView = (TextView) findViewById(R.id.imageDescription);
				descriptionView.setText(description);
			}
		});
		dialog.dismiss();
	}

	private Bitmap getBitmap(String url) throws IOException {

		HttpUriRequest request = new HttpGet(url.toString());
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse response = httpClient.execute(request);

		StatusLine statusLine = response.getStatusLine();
		int statusCode = statusLine.getStatusCode();
		if (statusCode == 200) {
			HttpEntity entity = response.getEntity();
			byte[] bytes = EntityUtils.toByteArray(entity);

			Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0,
					bytes.length);
			return bitmap;
		} else {
			throw new IOException("Download failed, HTTP response code "
					+ statusCode + " - " + statusLine.getReasonPhrase());
		}
	}

	public class RefreshImageThread extends Thread {

		@Override
		public void run() {
			try {
				setImage(getBitmap(getImageUrl()));
			} catch (IOException e) {
				Log.e("", "Getting the bitmap failed!");
			} finally {

			}
		}
	}

	public void onSetWallpaper(View view) {
		Thread th = new Thread() {
			public void run() {
				try {
					WallpaperManager wallpaperManager = WallpaperManager
							.getInstance(MainActivity.this);
					wallpaperManager.setBitmap(image);
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this,
									getText(R.string.wallpaperSet),
									Toast.LENGTH_SHORT).show();
						}
					});
				} catch (Exception e) {
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this,
									"Error setting wallpaper",
									Toast.LENGTH_SHORT).show();
						}
					});
					e.printStackTrace();
				}
			}
		};
		th.start();
	}

}
