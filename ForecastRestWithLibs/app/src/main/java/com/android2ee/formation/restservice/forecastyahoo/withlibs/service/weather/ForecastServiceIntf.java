/**
 * <ul>
 * <li>ForecastServiceIntf</li>
 * <li>com.android2ee.formation.restservice.forecastyahoo.withlibs.service.weather</li>
 * <li>10/04/2016</li>
 * <p/>
 * <li>======================================================</li>
 * <p/>
 * <li>Projet : Mathias Seguy Project</li>
 * <li>Produit par MSE.</li>
 * <p/>
 * /**
 * <ul>
 * Android Tutorial, An <strong>Android2EE</strong>'s project.</br>
 * Produced by <strong>Dr. Mathias SEGUY</strong>.</br>
 * Delivered by <strong>http://android2ee.com/</strong></br>
 * Belongs to <strong>Mathias Seguy</strong></br>
 * ***************************************************************************************************************</br>
 * This code is free for any usage but can't be distribute.</br>
 * The distribution is reserved to the site <strong>http://android2ee.com</strong>.</br>
 * The intelectual property belongs to <strong>Mathias Seguy</strong>.</br>
 * <em>http://mathias-seguy.developpez.com/</em></br> </br>
 * <p/>
 * *****************************************************************************************************************</br>
 * Ce code est libre de toute utilisation mais n'est pas distribuable.</br>
 * Sa distribution est reservée au site <strong>http://android2ee.com</strong>.</br>
 * Sa propriété intellectuelle appartient à <strong>Mathias Seguy</strong>.</br>
 * <em>http://mathias-seguy.developpez.com/</em></br> </br>
 * *****************************************************************************************************************</br>
 */
package com.android2ee.formation.restservice.forecastyahoo.withlibs.service.weather;

import com.android2ee.formation.restservice.forecastyahoo.withlibs.service.MotherBusinessServiceIntf;
import com.android2ee.formation.restservice.forecastyahoo.withlibs.transverse.event.CityForecastDownloadedEvent;

import org.greenrobot.eventbus.Subscribe;

/**
 * Created by Mathias Seguy - Android2EE on 10/04/2016.
 */
public interface ForecastServiceIntf extends MotherBusinessServiceIntf {

    /**
     * Load the WeatherData in an asynchronous way
     *
     * @param cityId
     *            The id of the city associated with the forecasts
     */
    void loadForecastAsync(int cityId);

    /***********************************************************
     *  Listening for downloaded data events
     **********************************************************/
    @Subscribe
    void onEvent(CityForecastDownloadedEvent event);
}
