/**<ul>
 * <li>ForecastRestYahooSax</li>
 * <li>com.android2ee.formation.restservice.sax.forecastyahoo.service</li>
 * <li>22 nov. 2013</li>
 * 
 * <li>======================================================</li>
 *
 * <li>Projet : Mathias Seguy Project</li>
 * <li>Produit par MSE.</li>
 *
 /**
 * <ul>
 * Android Tutorial, An <strong>Android2EE</strong>'s project.</br> 
 * Produced by <strong>Dr. Mathias SEGUY</strong>.</br>
 * Delivered by <strong>http://android2ee.com/</strong></br>
 *  Belongs to <strong>Mathias Seguy</strong></br>
 ****************************************************************************************************************</br>
 * This code is free for any usage but can't be distribute.</br>
 * The distribution is reserved to the site <strong>http://android2ee.com</strong>.</br>
 * The intelectual property belongs to <strong>Mathias Seguy</strong>.</br>
 * <em>http://mathias-seguy.developpez.com/</em></br> </br>
 * 
 * *****************************************************************************************************************</br>
 *  Ce code est libre de toute utilisation mais n'est pas distribuable.</br>
 *  Sa distribution est reservée au site <strong>http://android2ee.com</strong>.</br> 
 *  Sa propriété intellectuelle appartient à <strong>Mathias Seguy</strong>.</br>
 *  <em>http://mathias-seguy.developpez.com/</em></br> </br>
 * *****************************************************************************************************************</br>
 */
package com.android2ee.formation.restservice.sax.forecastyahoo.service.forecast;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android2ee.formation.restservice.sax.forecastyahoo.MyApplication;
import com.android2ee.formation.restservice.sax.forecastyahoo.R;
import com.android2ee.formation.restservice.sax.forecastyahoo.dao.forecast.ForecastDAO;
import com.android2ee.formation.restservice.sax.forecastyahoo.service.ServiceManager;
import com.android2ee.formation.restservice.sax.forecastyahoo.service.forecast.saxparser.ForcastSaxHandler;
import com.android2ee.formation.restservice.sax.forecastyahoo.transverse.exceptions.ExceptionManaged;
import com.android2ee.formation.restservice.sax.forecastyahoo.transverse.exceptions.ExceptionManager;
import com.android2ee.formation.restservice.sax.forecastyahoo.transverse.model.YahooForcast;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * @author Mathias Seguy (Android2EE)
 * @goals
 *        This class aims to retrieve YahooForecast from the internet and then save them in DB
 */
public class ForecastServiceUpdater {

	/******************************************************************************************/
	/** Attributes **************************************************************************/
	/******************************************************************************************/

	/**
	 * The url to use
	 */
	private String url;
	/**
	 * The object used to communicate with http
	 */
	private HttpClient client;
	/**
	 * The raw xml answer
	 */
	private String responseBody;
	/**
	 * The forecasts to display
	 */
	private List<YahooForcast> forecasts;
	/**
	 * The logCat's tag
	 */
	private final String tag = "ForecastServiceUpdater";
	/**
	 * The callBack to update activity
	 */
	private ForecastCallBack callback;
	/**
	 * The Dao
	 */
	private ForecastDAO forcastDao;
	/**
	 * The date parser used to set the last update date format in the preference
	 */
	public SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
	/**
	 * The woeid of the city corresponding to the forecast managed
	 */
	private String woeid;

	/******************************************************************************************/
	/** Constructors **************************************************************************/
	/******************************************************************************************/

	/**
	 * Constructor
	 */
	public ForecastServiceUpdater(ServiceManager srvManager) {
		// NOthing to initialize
		// the parameter is to ensure only srvManager cant create it
	}

	/******************************************************************************************/
	/** Public method **************************************************************************/
	/******************************************************************************************/

	/**
	 * Return the forecast
	 * 
	 * @param callback
	 *            The callback to use to deliver the data when data updated
	 * @param woeid
	 *            The id of the city associated with the forecasts
	 */
	public void updateForecastFromServer(ForecastCallBack callback,String woeid) {
		this.callback = callback;
		this.woeid=woeid;
		if (MyApplication.instance.isConnected()) {
			// then load data from network
			// retrieve the url
			url = MyApplication.instance.getString(R.string.forcast_url,this.woeid) + "&"
					+ MyApplication.instance.getString(R.string.forcast_url_degres);
			//then link the Handler with the handler of the runnable
			if(restCallRunnable.restCallHandler==null) {
				restCallRunnable.restCallHandler = restCallHandler;
			}
			//then launch it
			MyApplication.instance.getServiceManager().getCancelableThreadsExecutor().submit(restCallRunnable);
		} else {
			// else use the callback to return null to the client
			callback.forecastLoaded(null);
		}
	}

    /**
     * Return the last update date for the specific woeid
     * Use the getDateFormatForLastUpdate to parse that date
     * @param woeid The woeid of the city we are looking for the last update
     * @return the last update date formatted using the new SimpleDateFormat("dd MMM yyyy HH:mm:ss")
     *  or "null" if not found
     */
    public String getLastUpdate(String woeid){
        SharedPreferences prefs = MyApplication.instance.getSharedPreferences(
                MyApplication.CONNECTIVITY_STATUS, Context.MODE_PRIVATE);
        return prefs.getString(MyApplication.instance.getString(R.string.last_update)+woeid, "null");
    }

    /**
     *
     * @return The simpleDateFormat to use for decoding the LastUpdate Date
     */
    public SimpleDateFormat getDateFormatForLastUpdate(){
        return sdf;
    }

	/******************************************************************************************/
	/** Private methods: Managing the Update **************************************************************************/
	/******************************************************************************************/

	
	/**
	 * The runnable to execute when requesting update from the server
	 */
	private RestCallRunnable restCallRunnable = new RestCallRunnable();
	
	/**
	 * @author Mathias Seguy (Android2EE)
	 * @goals
	 * This class aims to implements a Runnable with an Handler
	 */
	private class RestCallRunnable implements Runnable {
		/**
		 * The handler to use to communicate outside the runnable
		 */
		public Handler restCallHandler=null;
		@Override
		public void run() {
			// Do the rest http call
			// Parse the element
			buildForecasts(getForecast());
			// store the data in DAO
			forcastDao = new ForecastDAO();
			forcastDao.saveAll((ArrayList<YahooForcast>) forecasts,woeid);
			forcastDao = null;
			restCallHandler.sendMessage(restCallHandler.obtainMessage());
		}
		/**
		 * Retrieve the forecast
		 */
		private String getForecast() {
			// The HTTP get method send to the URL
			HttpGet getMethod = new HttpGet(url);
			// The basic response handler
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			// instantiate the http communication
			client = new DefaultHttpClient();
			// Call the URL and get the response body
			try {
				responseBody = client.execute(getMethod, responseHandler);
			} catch (ClientProtocolException e) {
				ExceptionManager.manage(new ExceptionManaged(this.getClass(), R.string.exc_client_protocol, e));
			} catch (IOException e) {
				ExceptionManager.manage(new ExceptionManaged(this.getClass(), R.string.exc_http_get_error, e));
			}
			if (responseBody != null) {
				Log.d(tag, responseBody);
			}
			// parse the response body
			return responseBody;
		}

		/**
		 * Build the Forecasts list by parsing the xml response using SAX
		 * 
		 * @param raw
		 *            the xml response of the web server
		 */
		private void buildForecasts(String raw) {

			try {
				// Create a new instance of the SAX parser
				SAXParserFactory saxPF = SAXParserFactory.newInstance();
				SAXParser saxP = saxPF.newSAXParser();
				// The xml reader
				XMLReader xmlR = saxP.getXMLReader();
				// Create the Handler to handle each of the XML tags.
				ForcastSaxHandler forecastHandler = new ForcastSaxHandler();
				xmlR.setContentHandler(forecastHandler);
				// then parse
				xmlR.parse(new InputSource(new StringReader(raw)));
				// and retrieve the parsed forecasts
				forecasts = forecastHandler.getForecasts();
			} catch (ParserConfigurationException e) {
				ExceptionManager.manage(new ExceptionManaged(this.getClass(), R.string.exc_parsing, e));
			} catch (SAXException e) {
				ExceptionManager.manage(new ExceptionManaged(this.getClass(), R.string.exc_parsing, e));
			} catch (IOException e) {
				ExceptionManager.manage(new ExceptionManaged(this.getClass(), R.string.exc_parsing, e));
			}
		}
	}

	/**
	 * The handler awoke when the Runnable has finished it's execution
	 */
	private Handler restCallHandler=new Handler() {
		/* (non-Javadoc)
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			returnForecast();
		}
		
	};

	/**
	 * Called when the forecast are built
	 * Return that list to the calling Activity using the ForecastCallBack
	 */
	private void returnForecast() {
		// set the date of the last update:
		SharedPreferences prefs = MyApplication.instance.getSharedPreferences(MyApplication.CONNECTIVITY_STATUS,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(MyApplication.instance.getString(R.string.last_update)+this.woeid, sdf.format(new Date()));
		editor.commit();
		// use the callback to prevent the client
		for (YahooForcast forcast : forecasts) {
			Log.e("ForcastServiceUpdater ", "Found " + forcast);
		}
		callback.forecastLoaded(forecasts);
	}
}
