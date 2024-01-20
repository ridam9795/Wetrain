package com.adobe.training.core.models.impl;

import com.adobe.training.core.models.Stockplex;
import com.adobe.training.core.StockDataWriterJob;
import com.day.cq.wcm.api.designer.Style;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//these imports support the annotations used in this sling model
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ResourcePath;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import javax.annotation.PostConstruct;

// the following import statements were added to support data layer enablement and population
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.ComponentContext;
import org.apache.sling.models.annotations.injectorspecific.Self;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * This model is used as the backend logic for the stockplex component. Using a Sling model allows the component
 * to be exportable via JSON for a headless scenarios. Stock data that this model uses is imported into the JCR
 * via StockImportScheduler.java
 * 
 * The stock data that is expected is in the form:
 * /content/stocks
 * + ADBE [sling:OrderedFolder]
 *   + lastTrade [nt:unstructured]
 *     - companyName = <value>
 *     - sector = <value>
 *     - lastTrade = <value
 *     - ..
 */

@Model(adaptables=SlingHttpServletRequest.class,		
		adapters= {Stockplex.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL,
        resourceType = StockplexImpl.RESOURCE_TYPE)
//the Exporter will format the json output of this component
@Exporter(name="jackson", extensions = "json")
public class StockplexImpl implements Stockplex{
	protected static final String RESOURCE_TYPE = "wetrain/components/stockplex";
    private static final Logger LOGGER = LoggerFactory.getLogger(StockplexImpl.class);
	
    //Annotations to support data layer enablement and population
    @Self
    private SlingHttpServletRequest request;
    @ScriptVariable
    protected ComponentContext componentContext;

	//HTL global objects in the model
	//Learn more  at Helpx > HTL Global Objects
    @ScriptVariable
    private Page currentPage;
    @ScriptVariable
    private Style currentStyle;
	
	//Properties on the current resource saved from the dialog of a component
    @ValueMapValue
    private String symbol;
    @ValueMapValue
    private String summary;

    //content root of for stock data. /content/stocks
    @ResourcePath(path = StockDataWriterJob.STOCK_IMPORT_FOLDER)
    private Resource stocksRoot;
    
    private double currentPrice;
    private Map<String,Object> stockInfo;    

    @PostConstruct
    public void init() {
        ValueMap tradeValues = null;
        
        //Check to see if stock data has been imported into the JCR
        if(stocksRoot != null) {
	        Resource stockResource = stocksRoot.getChild(symbol);
	    	if(stockResource != null) {
	        	Resource lastTradeResource = stockResource.getChild("trade");
	        	if(lastTradeResource != null){
                    tradeValues = lastTradeResource.getValueMap();
                } 
	    	}
        }
        
        stockInfo = new HashMap<>();
        //If stock information is in the JCR, display the data
        if(tradeValues != null) {
        	currentPrice = tradeValues.get(StockDataWriterJob.LASTTRADE, Double.class);   	
            stockInfo.put("Request Date", tradeValues.get(StockDataWriterJob.DAYOFUPDATE, String.class));
            stockInfo.put("Request Time", tradeValues.get(StockDataWriterJob.UPDATETIME, String.class));
            stockInfo.put("UpDown", tradeValues.get(StockDataWriterJob.UPDOWN, Double.class));
            stockInfo.put("Open Price", tradeValues.get(StockDataWriterJob.OPENPRICE, Double.class));
            stockInfo.put("Range High", tradeValues.get(StockDataWriterJob.RANGEHIGH, Double.class));
            stockInfo.put("Range Low", tradeValues.get(StockDataWriterJob.RANGELOW, Double.class));
            stockInfo.put("Volume",  tradeValues.get(StockDataWriterJob.VOLUME, Integer.class));
            stockInfo.put("Company", tradeValues.get(StockDataWriterJob.COMPANY, String.class));
            stockInfo.put("Sector", tradeValues.get(StockDataWriterJob.SECTOR, String.class));
            stockInfo.put("52 Week Low", tradeValues.get(StockDataWriterJob.WEEK52LOW, Double.class));
        } else {
        	stockInfo.put(symbol,"No import config found. New stock symbols can be added in the Sites console under the stocks folder.");
        }
    }
    
    /**
     * All getter methods below will be apart of the output by the JSON Exporter
     */ 
    //Getter for dialog input
    @Override
    public String getSymbol() {
        return symbol;
    }
    //Getter for dialog input
    @Override
    public String getSummary() {
        return summary;
    }
    
    //Calculated current price based on imported stock info
    @Override
    public Double getCurrentPrice() {
        return currentPrice;
    }

    //Getter for dialog input
    public String getShowStockInfo() {
        return currentStyle.get("showStockInfo").toString();
    }

    //Calculated trade values based on imported stock info 
    @Override
    public Map<String,Object> getStockInfo() {
        return stockInfo;
    }

    //required by Exporter and its value populates the `:type` key in the JSON object
    @Override
	public String getExportedType() {
        return request.getResource().getResourceType();
    } 

    // Return data about the Stockplex Component to populate the data layer
    @Override
    public String getData() {
        Resource stockplexResource = request.getResource();
        // Use ComponentUtils to verify if the DataLayer is enabled
        if (ComponentUtils.isDataLayerEnabled(stockplexResource)) {
            //Create a map of properties we want to expose to the data layer
            Map<String, Object> stockplexProperties = new HashMap<String,Object>();
            stockplexProperties.put("@type", stockplexResource.getResourceType());
            stockplexProperties.put("symbol", this.getSymbol());
            stockplexProperties.put("summary", this.getSummary());
            stockplexProperties.put("showStockInfo", this.getShowStockInfo());
            stockplexProperties.put("currentPrice", this.getCurrentPrice());
            stockplexProperties.put("stockInfo", this.getStockInfo());
            
            //Use AEM Core Component utils to get a unique identifier for the stockplex component (in case multiple are on the page)
            String stockplexComponentID = ComponentUtils.getId(stockplexResource, this.currentPage, this.componentContext);

            // Return the stockplexProperties as a JSON String with a key of the stockplexResource's ID
            try {
                return String.format("{\"%s\":%s}",
                    stockplexComponentID,
                    // Use the ObjectMapper to serialize the stockplexProperties to a JSON string
                    new ObjectMapper().writeValueAsString(stockplexProperties));
            } catch (JsonProcessingException e) {
                LOGGER.error("Unable to generate dataLayer JSON string", e);
            }
        }
        // return null if the Data Layer is not enabled
        return null;
    }
}