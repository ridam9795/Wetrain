package com.adobe.training.core.models;

import javax.annotation.PostConstruct;

//This Hero model extends the core Image component
import com.adobe.cq.wcm.core.components.models.Image;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.ComponentContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Imports added to support Adobe Client Data Layer enablement and population
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;

@Model(
    adaptables=SlingHttpServletRequest.class,		
    adapters= Image.class,
    resourceType = Hero.RESOURCE_TYPE 
)
public class Hero implements Image{
    protected static final String RESOURCE_TYPE = "wetrain/components/hero";
    private static final Logger LOGGER = LoggerFactory.getLogger(Hero.class);
    
    //Annotations to help extend the data layer enablement and population
    @Self
    private SlingHttpServletRequest request;
    
    // @OSGiService
    // private ModelFactory modelFactory;
    
    //Core Image model we are extending
	@Self 
    @Via(type = ResourceSuperType.class)
	private Image coreImage;

    @ScriptVariable
    protected ComponentContext componentContext;
    @ScriptVariable
    private Page currentPage;
    @ScriptVariable
	private Resource resource;

    //property on the current resource saved from the dialog of a component
	@ValueMapValue @Default(values="")
	private String linkText;

    //A method to make any calculations before values are returned
    @PostConstruct
    private void init() {
        // Add any needed business logic here.
    }
    
    public String getLinkText() {
        return linkText;
    }

    @Override
    public String getLink() {
        return coreImage.getLink();
    }

    @Override
    public String getTitle() {
        return coreImage.getTitle();
    }
    
    @Override
    public String getSrc() {
        return coreImage.getSrc();
    }

    @Override
    public String getAlt() {
        return coreImage.getAlt();
    }

    public boolean isEmpty() {
        String src = coreImage.getSrc();
        if(src != null && !src.isEmpty()){
            return false;
        }
        LOGGER.error("Hero component has no content");
        return true;
    }

    @Override
    public String getExportedType() {
        return request.getResource().getResourceType();
    }

    /**
     * This is the method that will return relevant data from the Hero component 
     * to the Adobe Client Data Layer
     */
    @Override
    public ComponentData getData() {
        Resource heroResource = request.getResource();
        if (ComponentUtils.isDataLayerEnabled(heroResource)) {
            return DataLayerBuilder.extending(coreImage.getData()).asImageComponent()
                .withId(this::getId)
                .withTitle(this::getTitle)
                .withLinkUrl(this::getLink)
                .withType(() -> RESOURCE_TYPE)
                .build();
        }
        return null;
    }

    /**
     * Get the uniqueID of the hero component
     * This method is used by the getData() method to uniquely identify the output to the data layer
     */
    @Override
    public String getId() {
        Resource heroResource = this.request.getResource();
        return ComponentUtils.getId(heroResource, this.currentPage, this.componentContext);
    }
}
  