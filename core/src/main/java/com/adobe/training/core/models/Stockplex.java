package com.adobe.training.core.models;

import java.util.Map;

/**
 * Interface for stockplex model. Methods for retrieving:
 * * dialog and content policy properties
 * * component resourcetype for json export
 * * data for the Adobe Client Data Layer
 */
public interface Stockplex {
    /**
     * All getter methods below will be a part of the output by the JSON Exporter
     */ 
    //Getter for dialog input
    String getSymbol();

    //Getter for dialog input
    String getSummary();

    //Getter for dialog input
    String getShowStockInfo();
    
    //Calculated current price based on imported stock info
    Double getCurrentPrice();

     //Calculated trade values based on imported stock info 
    Map<String,Object> getStockInfo();

    //required by Exporter and its value populates the `:type` key in the JSON object
	String getExportedType(); 

    // Getter to return data about the Stockplex Component to populate the data layer 
    String getData();
}
