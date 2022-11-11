package com.akqa.core.servlets;

import com.akqa.core.schedulers.capstoneScheduler.PagesUpdationScheduler;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.text.csv.Csv;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

@Component(service = { Servlet.class })
@SlingServletResourceTypes(
        resourceTypes="capstone/components/page",
        methods=HttpConstants.METHOD_GET,
        selectors = "importproducts",
        extensions="html")
@ServiceDescription("Products upload Servlet")
public class ProductImportServletCapstone extends SlingSafeMethodsServlet {
    String productsSheetUrl = "https://docs.google.com/spreadsheets/d/1WVOKxoQOuTs59tyO82pJs_BW8Cs9gh9x2W1X00ittIw/gviz/tq?tqx=out:csv&sheet=Sheet1";
    private static final String BASE_PATH = "/var/commerce/products";
    private Map<Integer, String> propertyNameMap;
    private Map<String, Map<String, String>> productsMap;
    private boolean firstRowFlag;

    private void toggleFlag() {
        firstRowFlag = false;
    }

    private Set<String> removedDataSet;
    public Set<String> removedDataSeoPath;
    private SlingHttpServletResponse responsee;
    Session session;
    Resource resource;

    @Override
    protected void doGet(final SlingHttpServletRequest req,
                         final SlingHttpServletResponse resp) throws ServletException, IOException {

        final Logger logger = LoggerFactory.getLogger(getClass());

        logger.info("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nentered in do get @57\n#####");

        resource = req.getResource();

        removedDataSet = new HashSet<>(); // to store the data which is removed from the csv
        removedDataSeoPath = PagesUpdationScheduler.removedDataSeoPath;

        resp.setContentType("text/plain");

        //Create a Session
        session = resource.getResourceResolver().adaptTo(Session.class);
        try {
            final Csv csv = new Csv();

            logger.info("!!!!!!!!!!!! entered in csvSection @60");


//            Resource res = resource.getResourceResolver().getResource("/content/dam/capstone/capstone.csv");
//            Asset asset = res.adaptTo(Asset.class);
//            Rendition rendition = asset.getOriginal();
//            InputStream inputStream = rendition.adaptTo(InputStream.class);

            logger.info("########## entered in csvSection @70");

            InputStream inputStream = null;

            try {
                inputStream = new URL(productsSheetUrl).openStream();
            } catch (Exception e) {
                e.printStackTrace();
                logger.info("########## @92 " + e.getMessage());
            }



            final Iterator<String[]> rows = csv.read(inputStream, "UTF-8");
            logger.info("################## " + csv.toString());

            if (Objects.nonNull(session) && session.nodeExists(BASE_PATH)) {
                logger.info("################## @74 " + BASE_PATH);
                final Node rootNode = session.getNode(BASE_PATH); // fetching node at where we need to add the data

                Node capstoneNode = JcrUtils.getOrAddNode(rootNode, "capstone", JcrConstants.NT_UNSTRUCTURED); // Add capstone node
                JcrUtils.getOrAddNode(capstoneNode, JcrConstants.JCR_CONTENT, JcrConstants.NT_UNSTRUCTURED);
                Node productsNode = JcrUtils.getOrAddNode(capstoneNode, "products", JcrConstants.NT_UNSTRUCTURED); // Add products node

                // backup for updation that which data is removed
                NodeIterator nodeIterator = productsNode.getNodes();
                while (productsNode.hasNodes() && nodeIterator.hasNext()) {
                    removedDataSet.add(nodeIterator.nextNode().getProperty("SKU").getValue().toString());
                }
                // map for which property lie at which index of csv
                propertyNameMap = new HashMap<>();
                // map for words to its properties
                productsMap = new HashMap<>();
                // for checking whether the first row(header) of the CSV is stored or not
                firstRowFlag = true;
                logger.info("############## @92");
                rows.forEachRemaining(row -> {
                    logger.info("############## @94");
                    final List<String> dataList = Arrays.asList(row);
                    logger.info("String row:", dataList);
                    // if header is not stored //
                    if (firstRowFlag) {
                        propertyNameMap.put(0, "jcr:title"); // productName is added as the title property
                        for (int index = 1; index < dataList.size(); index++) {
                            propertyNameMap.put(index, dataList.get(index));
                        }
                        // if header is stored then toggle so that next time we can store words
                        toggleFlag();
                    } else {
                        fillMap(dataList);
                    }
                });
                logger.info("############## @109");
                // importing products into a specific node
                for (String product : productsMap.keySet()) {
                    try {
                        importProducts(product, productsMap.get(product), productsNode);
                        session.save();
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    }
                }
                // deleting nodes
                for (String sku : removedDataSet) {
                    if (productsNode.hasNode(sku)) {
                        // adding removed nodes seo path so that page can be removed by scheduler
                        removedDataSeoPath.add(productsNode.getNode(sku).getProperty("SEO Path").getValue().toString());
                        productsNode.getNode(sku).remove(); // removing the node
                        session.save();
                    }
                }
            }
            resp.getWriter().write("Data added successfully\n");
        } catch (Exception e) {
            resp.getWriter().write("Error in adding data  ||  " + e.toString() + "\n");
            log(e.getMessage());
        }

        resp.getWriter().write(removedDataSeoPath.toString());
    }

    // filling the product and it's properties
    private void fillMap(List<String> row) {
        Map<String, String> propertyMap = new HashMap<>(); // temporary map for mapping the properties
        for (int index = 0; index < row.size(); index++) {
            propertyMap.put(propertyNameMap.get(index), row.get(index));
        }
        productsMap.put(row.get(0), propertyMap); // adding the tempMap(this product's properties) infront of itself
    }

    // importing products into a specific node
    private void importProducts(String product, Map<String, String> propertyMap, Node rootNode) throws RepositoryException, IOException {
        try {
            String sku = propertyMap.get("SKU");

            if (removedDataSet.contains(sku)) {
                removedDataSet.remove(sku);
            }// removing nodes which are deleted
            final Node productNode = JcrUtils.getOrAddNode(rootNode, sku, JcrConstants.NT_UNSTRUCTURED); // creating a node for a specific word

            for (String property : propertyMap.keySet()) {
                if (property.isEmpty()) continue;
                String value = propertyMap.get(property);

                // for adding images path
                if (property.equals("Pipe Seperated image path")) {
                    Node defaultImageNode = JcrUtils.getOrAddNode(productNode, "defaultimage", JcrConstants.NT_UNSTRUCTURED);
                    String[] imagesArray = value.split("\\|");
                    // first image is default image
                    defaultImageNode.setProperty("fileReference", imagesArray[0]);
                    Node imageNodes = JcrUtils.getOrAddNode(productNode, "images", JcrConstants.NT_UNSTRUCTURED);

                    int i = 1;
                    for (String path : imagesArray) { //remaining are additional images
                        Node imageNode = JcrUtils.getOrAddNode(imageNodes, "image" + i++, JcrConstants.NT_UNSTRUCTURED);
                        imageNode.setProperty("fileReference", path);
                    }
                }
                // for Bulk discount property
                else if (property.equals("Bulk Discount")) {
                    String[] discountArr = value.split("\\|");
                    Node bulkDiscounts = JcrUtils.getOrAddNode(productNode, "bulkdiscounts", JcrConstants.NT_UNSTRUCTURED);

                    int i = 1;
                    for (String discountMap : discountArr) {
                        String[] discountMapArray = discountMap.split("~");
                        Node discount = JcrUtils.getOrAddNode(bulkDiscounts, "discount" + i++, JcrConstants.NT_UNSTRUCTURED);
                        discount.setProperty("quantity", Integer.parseInt(discountMapArray[0]));
                        discount.setProperty("discountpercentage", Integer.parseInt(discountMapArray[1]));
                    }
                }
                // for other properties
                else {
                    productNode.setProperty(property, value);  // adding its properties
                }
            }
        } catch (Exception e) {
            responsee.getWriter().write(e.toString() + "line 259\n");
        }
    }
}