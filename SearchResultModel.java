package com.akqa.core.models;

import com.akqa.core.Bean.ImageDataBean;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Model(adaptables = Resource.class)
public class SearchResultModel {

    List<ImageDataBean> imageDataList = null;
    @Self
    Resource resource;


    @PostConstruct
    protected void getFilterResult() {

        String currentPagePath = resource.getPath();  // gets current page path (url)
        String[] currentPageElements= currentPagePath.split("/");

        int indexOfProduct=0;
        for (int index = 0; index < currentPageElements.length; index++) {
            String s = "jcr:content";

            if (s.equals(currentPageElements[index])) {
                indexOfProduct=index;
            }
        }
        String result = currentPageElements[indexOfProduct-1];  // contains the category that has to  be searched

        ResourceResolver resourceResolver = resource.getResourceResolver();

        Session session = resourceResolver.adaptTo(Session.class);
        QueryBuilder builder = resourceResolver.adaptTo(QueryBuilder.class); // inheriting properties of query builder class

        Map<String, String> predicate = new HashMap<>();
        predicate.put("path","/var/commerce/products/capstone/products" ); // path of the product
        predicate.put("property", "Category"); // sorting products on category
        predicate.put("property.value", result);
        predicate.put("p.limit","-1"); // shows all the results from the query at once

        Query query = null;

        try {
            query = builder.createQuery(PredicateGroup.create(predicate), session); // creates query from the input in the map

        } catch (Exception e) {

        }
        SearchResult searchResult = query.getResult();  // stores the result for the query
        imageDataList = new ArrayList<ImageDataBean>();

        for (Hit hit : searchResult.getHits()) { // gets the hits got from the query as a result

            ImageDataBean imageDataBean = new ImageDataBean();

            String path = null;

            try {
                path = hit.getPath();  // gets the path of the hit
                Resource imageResource = resourceResolver.getResource(path);  // gets the resource from the path of the hits

                String imgAsset = imageResource.getChild("defaultimage").getPath(); // gets the child "defaultimage" from the hit resource
                Resource imagePath=resourceResolver.getResource(imgAsset);
                ValueMap valueMapOfImg=imagePath.getValueMap();

                ValueMap valueMap = imageResource.getValueMap();


                if(valueMap==null) {
                    throw new NullPointerException();
                }
                imageDataBean.setPath((String) valueMapOfImg.get("fileReference"));
                imageDataBean.setTitle((String) valueMap.get("jcr:title"));
                imageDataBean.setPrice((String) valueMap.get("Price"));
                imageDataBean.setCategory((String) valueMap.get("Category"));
                imageDataBean.setSeopath((String) valueMap.get("SEO Path"));
                imageDataBean.setSku((String) valueMap.get("SKU"));

                imageDataList.add(imageDataBean);

            } catch (RepositoryException e) {

            }

        }

    }

    public List<ImageDataBean> getImageDataList() {
        return imageDataList;
    }
}