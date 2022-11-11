package com.akqa.aem.training.aem201.core.servlets;


import com.akqa.aem.training.aem201.core.Bean.ImageDataBean;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = { Servlet.class })
@SlingServletResourceTypes(
        resourceTypes="cq:Page",
        methods= HttpConstants.METHOD_GET,
        selectors = "search.page",
        extensions="html")

@ServiceDescription("Search Product Servlet")
public class FilterProductServlet extends SlingAllMethodsServlet {
//    @Self
//    Resource resource;
    public static String searchedPage = "";

    private static final long serialVersionUID = 1L;

    List<ImageDataBean> imageDataList = null;

    @Override
    protected void doGet(final SlingHttpServletRequest req,
                         final SlingHttpServletResponse resp) throws ServletException, IOException {

//        if(searchedPage == null){
//            searchedPage +="0";
//        }
        searchedPage = req.getParameter("selectPage");

        String pagePath = req.getRequestURI();
        Resource resource = req.getResourceResolver().resolve(pagePath);


        ResourceResolver resourceResolver = resource.getResourceResolver();

        Session session = resourceResolver.adaptTo(Session.class);
        QueryBuilder builder = resourceResolver.adaptTo(QueryBuilder.class);

        Map<String, String> predicate = new HashMap<>();
        predicate.put("path", "/var/commerce/products/capstone/products"); // path of the product
        predicate.put("property", "cq:commerceType");
        predicate.put("property.value", "product"); // search for the all the node which contains property of product
        predicate.put("p.offset", "12");
        predicate.put("p.limit" , "4");

        Query query = null;

        try {
            query = builder.createQuery(PredicateGroup.create(predicate), session);

        } catch (Exception e) {

        }
        SearchResult searchResult = query.getResult();
        double totalMatches = (double) searchResult.getTotalMatches();
        double pages=totalMatches/4;
        double numberOfPages=  Math.ceil(pages);
        imageDataList = new ArrayList<ImageDataBean>();

        for (Hit hit : searchResult.getHits()) {

            ImageDataBean imageDataBean = new ImageDataBean();

            String path = null;

            try {
                path = hit.getPath();
                Resource imageResource = resourceResolver.getResource(path);

                String imgAsset = imageResource.getChild("defaultimage").getPath();
                Resource imagePath = resourceResolver.getResource(imgAsset);
                ValueMap valueMapOfImg = imagePath.getValueMap();

                ValueMap valueMap = imageResource.getValueMap();

                imageDataBean.setPath((String) valueMapOfImg.get("fileReference"));
                imageDataBean.setTitle((String) valueMap.get("jcr:title"));
                imageDataBean.setPrice((String) valueMap.get("Price"));
                imageDataBean.setCategory((String) valueMap.get("Category"));
                imageDataBean.setSeopath((String) valueMap.get("SEO Path"));
                imageDataBean.setSku((String) valueMap.get("SKU"));

                imageDataList.add(imageDataBean);

                JSONArray arr= new JSONArray();
                arr.put(imageDataList);

                System.out.println(arr);

            } catch (RepositoryException e) {

            }

        }

    }

}
