package com.android2ee.formation.restservice.sax.forecastyahoo.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android2ee.formation.restservice.sax.forecastyahoo.MotherActivity;
import com.android2ee.formation.restservice.sax.forecastyahoo.MyApplication;
import com.android2ee.formation.restservice.sax.forecastyahoo.R;
import com.android2ee.formation.restservice.sax.forecastyahoo.service.ForecastCallBack;
import com.android2ee.formation.restservice.sax.forecastyahoo.transverse.exceptions.ExceptionManaged;
import com.android2ee.formation.restservice.sax.forecastyahoo.transverse.exceptions.ExceptionManager;
import com.android2ee.formation.restservice.sax.forecastyahoo.transverse.interfaces.ConnectivityIsBackIntf;
import com.android2ee.formation.restservice.sax.forecastyahoo.transverse.model.YahooForcast;
import com.android2ee.formation.restservice.sax.forecastyahoo.view.arrayadpater.ForecastArrayAdapter;

public class MainActivity extends MotherActivity implements ConnectivityIsBackIntf,
		SwipeRefreshLayout.OnRefreshListener {
	/******************************************************************************************/
	/** Attributes **************************************************************************/
	/******************************************************************************************/

	/**
	 * The ArrayAdapter to use
	 */
	private ForecastArrayAdapter arrayAdapter;
	/**
	 * The ListView
	 */
	private ListView listView = null;
	/**
	 * The ImageView that displays the yahooLogo
	 */
	private ImageView imvYahooLogo;
	/**
	 * The last update textview displaying when the data was last updated
	 */
	private TextView txvLastUpdate;
	/**
	 * The SwipeLayout
	 */
	SwipeRefreshLayout swipeLayout = null;
	/**
	 * The displayed object
	 */
	private ArrayList<YahooForcast> forecasts;

	/**
	 * The connectivity status
	 */
	boolean isConnected;
	/**
	 * Data are loaded
	 */
	boolean dataLoaded = false;
	/**
	 * To know if we come from a destroy/create (device rotation) or from create
	 * And if the savedInstance contains the list of forecast (no need to reload them so)
	 */
	boolean recreationWithForecastsList = false;

	/******************************************************************************************/
	/** Managing Life Cycle **************************************************************************/
	/******************************************************************************************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		forecasts = new ArrayList<YahooForcast>();
		isConnected = MyApplication.instance.isConnected();
		if (savedInstanceState == null || !savedInstanceState.containsKey("forcasts_list")) {
			recreationWithForecastsList = true;
		}
		swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
		swipeLayout.setOnRefreshListener(this);
		swipeLayout.setColorSchemeResources(R.color.blue_pure,
				R.color.blue_pure_1,
				R.color.blue_pure_2,
				R.color.blue_pure_3);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		forecasts.clear();
		for (Parcelable parcel : savedInstanceState.getParcelableArrayList("forcasts_list")) {
			forecasts.add((YahooForcast) parcel);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList("forcasts_list", forecasts);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		Log.e("MainActivity", "onPause");
		MyApplication.instance.unregisterAsConnectivityBackListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		MyApplication.instance.registerAsConnectivityBackListener(this);
		isConnected = MyApplication.instance.isConnected();
		if (recreationWithForecastsList) {
			loadWeatherForecast();
		} else {
			updateGuiWithForecast();
		}
	}

	/******************************************************************************************/
	/** Managing connectivity **************************************************************************/
	/******************************************************************************************/
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.android2ee.formation.restservice.sax.forecastyahoo.transverse.interfaces.
	 * ConnectivityIsBackIntf#connectivityIsBack(boolean, int)
	 */
	@Override
	public void connectivityIsBack(boolean isWifi, int telephonyType) {
		// Ok so the connectivity is back, we should load the data
		if (!dataLoaded) {
			MyApplication.instance.getServiceManager().getForecastServiceData().getForecast(new ForecastCallBack() {
				@Override
				public void forecastLoaded(List<YahooForcast> forecasts) {
					forecastLoadedGuiUpdate(forecasts);
				}
			});
		}
		// else do nothing data already loaded

		// then insure the NoNetwork error message is hidden
		findViewById(R.id.txvNoNetwork).setVisibility(View.GONE);
	}

	/******************************************************************************************/
	/** Loading Forecast **************************************************************************/
	/******************************************************************************************/

	/**
	 * The call to the service to load weather forecast
	 * Iff there is network
	 */
	private void loadWeatherForecast() {
		// if connected ask for the weather
		if (isConnected) {
			MyApplication.instance.getServiceManager().getForecastServiceData().getForecast(new ForecastCallBack() {
				@Override
				public void forecastLoaded(List<YahooForcast> forecasts) {
					forecastLoadedGuiUpdate(forecasts);
				}
			});
		} else {
			// if not connected: say something to your user
			Toast.makeText(this, getString(R.string.no_network_message), Toast.LENGTH_LONG).show();
			// Hide the progressBar
			findViewById(R.id.progressBar1).setVisibility(View.GONE);
			findViewById(R.id.txvNoNetwork).setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Update the Gui using the List of Forecast retrieve by the service
	 * 
	 * @param forecasts
	 */
	private void forecastLoadedGuiUpdate(List<YahooForcast> forecasts) {
		// Instanciate the listView
		this.forecasts = (ArrayList<YahooForcast>) forecasts;
		if (this.forecasts == null || this.forecasts.size() == 0) {
			showNoConnectionMessage();
		} else {
			updateGuiWithForecast();
		}
		swipeLayout.setRefreshing(false);
	}

	/**
	 * Update the gui with the list of forecast
	 */
	private void updateGuiWithForecast() {
		// Instanciate the listView
		this.forecasts = (ArrayList<YahooForcast>) forecasts;
		if (listView == null) {
			listView = (ListView) findViewById(R.id.myListView);
			View viewFooter = LayoutInflater.from(this).inflate(R.layout.list_footer, null);
			imvYahooLogo = (ImageView) viewFooter.findViewById(R.id.imv_yahoo_logo);
			imvYahooLogo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					yahooRequirement();
				}

			});
			View viewHeader = LayoutInflater.from(this).inflate(R.layout.list_header, null);
			txvLastUpdate = (TextView) viewHeader.findViewById(R.id.txv_last_update);
			txvLastUpdate.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onRefresh();
					Toast.makeText(MainActivity.this, getString(R.string.txv_last_update_swipe), Toast.LENGTH_LONG).show();
				}

			});
			listView.addHeaderView(viewHeader);
			listView.addFooterView(viewFooter);
			arrayAdapter = new ForecastArrayAdapter(this, this.forecasts);
			listView.setAdapter(arrayAdapter);
		}
		// Hide the progressBar
		findViewById(R.id.progressBar1).setVisibility(View.GONE);
		// then insure the NoNetwork error message is hidden
		findViewById(R.id.txvNoNetwork).setVisibility(View.GONE);
		// Set the listView Visible
		listView.setVisibility(View.VISIBLE);
		arrayAdapter.notifyDataSetChanged();
		// update the last update textView
		SharedPreferences prefs = MyApplication.instance.getSharedPreferences(MyApplication.CONNECTIVITY_STATUS,
				Context.MODE_PRIVATE);
		txvLastUpdate.setText(getString(R.string.txv_last_update, prefs.getString(MyApplication.instance.getString(R.string.last_update), "null")));
		// ok data loaded
		dataLoaded = true;
	
	}

	/**
	 * Show the no connection message
	 */
	private void showNoConnectionMessage() {
		// if not connected: say something to your user
		Toast.makeText(this, getString(R.string.no_network_message), Toast.LENGTH_LONG).show();
		// Hide the progressBar
		findViewById(R.id.progressBar1).setVisibility(View.GONE);
		((TextView) findViewById(R.id.txvNoNetwork)).setText(getString(R.string.no_data_message));
		findViewById(R.id.txvNoNetwork).setVisibility(View.VISIBLE);
	}

	/**
	 * Open the YahooRequirement URL
	 */
	private void yahooRequirement() {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(getString(R.string.yahoo_requirment)));
		startActivity(i);
	}

	/******************************************************************************************/
	/** Managing SwipeToRefresh listener **************************************************************************/
	/******************************************************************************************/

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener#onRefresh()
	 */
	@Override
	public void onRefresh() {
		// call service updater
		if (isConnected) {
			MyApplication.instance.getServiceManager().getForecastServiceUpdater().updateForecastFromServer(new ForecastCallBack() {
				@Override
				public void forecastLoaded(List<YahooForcast> forecasts) {
					forecastLoadedGuiUpdate(forecasts);
				}
			});
		} else {
			// no connection dude
			showNoConnectionMessage();
		}
	}

	/******************************************************************************************/
	/** Managing Menu **************************************************************************/
	/******************************************************************************************/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
