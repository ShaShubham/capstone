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
public class AllProductsModel {

    List<ImageDataBean> imageDataList = null;
    @Self
    Resource resource;


    @PostConstruct
    protected void getQueryBuild() {

        ResourceResolver resourceResolver = resource.getResourceResolver();

        Session session = resourceResolver.adaptTo(Session.class);
        QueryBuilder builder = resourceResolver.adaptTo(QueryBuilder.class);

        Map<String, String> predicate = new HashMap<>();
        predicate.put("path","/var/commerce/products/capstone/products" ); // path of the product
        predicate.put("property", "cq:commerceType");
        predicate.put("property.value", "product"); // search for the all the node which contains property of product
        predicate.put("p.limit","-1");

        Query query = null;

        try {
            query = builder.createQuery(PredicateGroup.create(predicate), session);

        } catch (Exception e) {

        }
        SearchResult searchResult = query.getResult();
        imageDataList = new ArrayList<ImageDataBean>();

        for (Hit hit : searchResult.getHits()) {

            ImageDataBean imageDataBean = new ImageDataBean();

            String path = null;

            try {
                path = hit.getPath();
                Resource imageResource = resourceResolver.getResource(path);

                String imgAsset = imageResource.getChild("defaultimage").getPath();
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