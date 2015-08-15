package org.owasp.pubkeypin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.net.URL;

// http://android-developers.blogspot.com/2009/05/painless-threading.html
public class FetchSecretTask extends AsyncTask<Void, Void, Object> {

	@Override
	protected void onPreExecute() {

		assert (null != MainActivity.m_secret);
		if (null != MainActivity.m_secret) {
			MainActivity.m_secret.setText("    Fetching...");
		}

		assert (null != MainActivity.m_button);
		if (null != MainActivity.m_button) {
			MainActivity.m_button.setEnabled(false);
		}

		assert (null != MainActivity.m_progress1);
		if (null != MainActivity.m_progress1) {
			MainActivity.m_progress1.setVisibility(ProgressBar.VISIBLE);
			MainActivity.m_progress1.setProgress(0);
		}

		assert (null != MainActivity.m_progress2);
		if (null != MainActivity.m_progress2) {
			MainActivity.m_progress2.setVisibility(ProgressBar.VISIBLE);
			MainActivity.m_progress2.setProgress(0);
		}
	}

    @Override
	protected Object doInBackground(Void... params) {

		Object result = null;

		try {

			byte[] secret = null;

            //Getting the keystore
			KeyPinStore keystore = KeyPinStore.getInstance();

            // Tell the URLConnection to use a SocketFactory from our SSLContext
			URL url = new URL( "https://www.random.org/integers/?num=16&min=0&max=255&col=16&base=10&format=plain&rnd=new");
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setSSLSocketFactory(keystore.getContext().getSocketFactory());
            InputStream instream = urlConnection.getInputStream();

            // Following OWASP example https://www.owasp.org/index.php/Certificate_and_Public_Key_Pinning
			StreamTokenizer tokenizer = new StreamTokenizer(instream);
			assert (null != tokenizer);

			secret = new byte[16];
			assert (null != secret);

			int idx = 0, token;
			while (idx < secret.length) {
				token = tokenizer.nextToken();
				if (token == StreamTokenizer.TT_EOF)
					break;
				if (token != StreamTokenizer.TT_NUMBER)
					continue;

				secret[idx++] = (byte) tokenizer.nval;
			}

			// Prepare return value
			result = (Object) secret;

		} catch (Exception ex) {

			// Log error
			Log.e("doInBackground", ex.toString());

			// Prepare return value
			result = (Object) ex;
		}

		return result;
	}

	@Override
	protected void onPostExecute(Object result) {

		assert (null != result);

		assert (null != MainActivity.m_button);
		if (null != MainActivity.m_button) {
			MainActivity.m_button.setEnabled(true);
		}

		assert (null != MainActivity.m_progress1);
		if (null != MainActivity.m_progress1) {
			MainActivity.m_progress1.setVisibility(ProgressBar.INVISIBLE);
		}

		assert (null != MainActivity.m_progress2);
		if (null != MainActivity.m_progress2) {
			MainActivity.m_progress2.setVisibility(ProgressBar.INVISIBLE);
		}

		assert (null != MainActivity.m_secret);
		if (null != MainActivity.m_secret) {
			MainActivity.m_secret.setText("");
		}

		assert (null != result);
		if (null == result)
			return;

		assert (result instanceof Exception || result instanceof byte[]);
		if (!(result instanceof Exception || result instanceof byte[]))
			return;

		if (result instanceof Exception) {
			ExitWithException((Exception) result);
			return;
		}

		ExitWithSecret((byte[]) result);
	}

	protected void ExitWithException(Exception ex) {

		assert (null != ex);

		if (null != MainActivity.m_secret) {
			MainActivity.m_secret.setText("    Error fetching secret");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(
				MainActivity.m_this);
		builder.setMessage(ex.toString()).setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		AlertDialog alert = builder.create();
		alert.show();

	}

	protected void ExitWithSecret(byte[] secret) {

		assert (null != secret);

		StringBuilder sb = new StringBuilder(secret.length * 3 + 1);
		assert (null != sb);

		for (int i = 0; i < secret.length; i++) {
			sb.append(String.format("%02X ", secret[i]));
			secret[i] = 0;
		}

		MainActivity.m_secret.setText(sb.toString());
	}
}
