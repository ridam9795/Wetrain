package com.adobe.training.core.listeners;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.adobe.training.core.StockDataWriterJob;
import com.adobe.training.core.schedulers.StockImportScheduler;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Sling listener listens to the StockDataWriterJob.STOCK_IMPORT_FOLDER location and creates a 
 * new scheduler config for each new stock folder added.
 * 
 * To add a symbol from the UI, go to AEM Navigation > Sites > stocks and click the blue Create > Folder
 * Add the Stock symbol as the Title. Dummy stock data is available for ADBE,MSFT,GOOG,AMZN,APPL,WDAY
 * 
 * Learn more about creating OSGi configurations programmatically:
 * http://www.nateyolles.com/blog/2015/10/updating-osgi-configurations-in-aem-and-sling
 */

@Component( immediate = true,
property = {"resource.paths=" + StockDataWriterJob.STOCK_IMPORT_FOLDER,
		"resource.change.types=ADDED",
		"resource.change.types=REMOVED"
		})

public class StockListener implements ResourceChangeListener{
	private final String stockImportSchedulerPID = "com.adobe.training.core.schedulers.StockImportScheduler";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	// Convenience string to find the log messages for this training example class
	// Logs can be found in crx-quickstart/logs/error.log
	private String searchableLogStr = "$$$$$";

	// Service to get OSGi configurations
    @Reference
    private ConfigurationAdmin configAdmin;
	// Service to add/remove resources if needed
	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Override
	public void onChange(List<ResourceChange> changes) {

		for (final ResourceChange change : changes) {
			logger.info(searchableLogStr + "Resource Change Detected: {}", change);

			//Get the folder name from the path. Ex: /content/stocks/adbe > adbe
			String folderName = change.getPath().substring(change.getPath().lastIndexOf("/")+1);
			//In this example a stock symbol must be 4 characters and not be the 'trade' node from the StockDataWriterJob
			if((folderName.length() == 4) && (folderName.matches("^[a-zA-Z]*$")) && !folderName.equals("trade")) {
				
				//Check if the added folder is uppercase. If it's not, autofix
				if(!folderName.equals(folderName.toUpperCase())&&change.getType().equals(ResourceChange.ChangeType.ADDED)){
					//Get the service user (training-user) that belongs to the training.core:training subservice
					Map<String, Object> serviceParams = new HashMap<>();
					serviceParams.put(ResourceResolverFactory.SUBSERVICE, "training");
					try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(serviceParams)) {
							logger.info(searchableLogStr + "Folder added is not uppercase. Recreating resource: "+ change.getPath());
							//Remove resource
							resourceResolver.delete(resourceResolver.getResource(change.getPath()));
							//Recreate resource as uppercase
							ResourceUtil.getOrCreateResource(resourceResolver, change.getPath().replace(folderName, folderName.toUpperCase()), "", "", false);
							resourceResolver.commit();
					} catch (LoginException | PersistenceException e) {
						logger.error(searchableLogStr + "Exception with updating resource to uppercase ", e);
					}
				}

				//Create a StockImportScheduler config for the symbol folder added
				else if(change.getType().equals(ResourceChange.ChangeType.ADDED)) {
					try{
							//Get the StockImportScheduler factory from the config admin
							Configuration config = configAdmin.createFactoryConfiguration(stockImportSchedulerPID);
								
							//Add the folder name to the configuration
							Dictionary<String, Object> properties = new Hashtable<String, Object>();
							properties.put(StockImportScheduler.JOB_PROP_SYMBOL, folderName);
							config.update(properties);
							logger.info(searchableLogStr + "Added " + folderName + " config with PID: "+ config.getPid());
					} catch (IOException e) {
						logger.error(searchableLogStr + "Could not add OSGi config for: " + folderName);
					}
				}
				//Remove the StockImportScheduler config for the symbol folder removed
				else if (change.getType().equals(ResourceChange.ChangeType.REMOVED)) {
					try {
						String filter = '(' + ConfigurationAdmin.SERVICE_FACTORYPID + '=' + stockImportSchedulerPID + ')';
						//Find all the StockImportScheduler configs
						Configuration[] configArray = configAdmin.listConfigurations(filter);
						//Find the config that matches the removed folder name and delete the config
						for( Configuration config :configArray) {
							Object configSymbolPropVal = config.getProperties().get(StockImportScheduler.JOB_PROP_SYMBOL);
							if(configSymbolPropVal.equals(folderName)) {
								logger.info(searchableLogStr + "Removed " + folderName + " config with PID: "+ config.getPid());
								config.delete();
							}
						}
					} catch (IOException e) {
						logger.error(searchableLogStr + "Could not delete OSGi config for: " + folderName);
					} catch (InvalidSyntaxException e) {
						logger.error(searchableLogStr + "Could not delete OSGi config for: " + folderName);
					}
				}
			}
		}
	}
}
