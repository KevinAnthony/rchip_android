package com.nosideracing.rchipremote;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import com.nosideracing.rchipremote.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Preferences extends PreferenceActivity {
	private CharSequence[] hostsListEntryValues = null;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		Log.d(Consts.LOG_TAG, "OnCreate: Preferences");
		addPreferencesFromResource(R.xml.preferences);
		ListPreference hostnameList = (ListPreference) findPreference("serverhostname");
		
		hostsListEntryValues = get_host_names().split("\\|");
		hostnameList.setEntries(hostsListEntryValues);
		hostnameList.setEntryValues(hostsListEntryValues);

	}

	protected String get_host_names() {
		try {
			SoapObject request = new SoapObject(Consts.NAMESPACE,
					Consts.METHOD_NAME_GETDAEMONS);
			;
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
					SoapEnvelope.VER11);
			envelope.setOutputSoapObject(request);
			HttpTransportSE androidHttpTransport = get_transport();

			androidHttpTransport.call(Consts.SOAP_ACTION, envelope);

			SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
			/* We get the result */
			return resultsRequestSOAP.getProperty("Result").toString();
			
		} catch (Exception e) {
			return null;
		}
	}

	private HttpTransportSE get_transport() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String URL_INT = settings.getString("serverurlinternal",
		"http://192.168.1.3:500/");
		String URL_EXT = settings.getString("serverurlexternal",
		"http://173.3.14.224:500/");
		Context f_context = getApplicationContext();
		NetworkInfo networkinfo = (NetworkInfo) ((ConnectivityManager) f_context
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		HttpTransportSE androidHttpTransport = null;
		if ((networkinfo != null) && (networkinfo.isConnected())) {
			int itype = networkinfo.getType();
			if (itype == 1) {
				androidHttpTransport = new HttpTransportSE(URL_INT);
			} else {
				androidHttpTransport = new HttpTransportSE(URL_EXT);
			}
		} else {
			androidHttpTransport = new HttpTransportSE(URL_EXT);
		}
		return androidHttpTransport;
	}
}
