package com.akqa.core.servlets;

import com.akqa.core.schedulers.capstoneScheduler.PagesUpdationScheduler;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Objects;

public class PagesAddHelp {
    public PagesAddHelp(Resource resource) {
        this.resource = resource;
    }
    Resource resource;
    private final String BASE_PATH = "/content/capstone";
    private PageManager pageManager;
    public void updatePages() throws Exception {
        final Logger logger = LoggerFactory.getLogger(getClass());

        //Create a Session
        session = resource.getResourceResolver().adaptTo(Session.class);
        try {
            ResourceResolver resourceResolver = resource.getResourceResolver();
            if (Objects.nonNull(session) && session.nodeExists(BASE_PATH)) {
                pageManager = resourceResolver.adaptTo(PageManager.class);

                for(String seo : PagesUpdationScheduler.removedDataSeoPath) {
                    Node node = pageManager.getPage(indPagePath+seo).adaptTo(Node.class);
                    Node nodeParent = node.getParent();
                    node.remove();
                    session.save();
                    if(nodeParent.getNodes().getSize()==1) { // only jcr:content remaining
                        nodeParent.remove();
                        session.save();
                    }
                }
                final Node rootNode = session.getNode("/var/commerce/products/capstone/products"); // fetching node at where we need to add the pages
                addPages(rootNode);
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
    }
    private final String capstonePagePath = "/content/capstone";
    String template = "/conf/capstone/settings/wcm/templates/home-page";
    Session session;
    private final String indPagePath = capstonePagePath+"/ind";
    private final String IND = "ind";
    private void addPages(Node dataNode) throws RepositoryException, WCMException {

        NodeIterator nodeIterator = dataNode.getNodes();
        while(nodeIterator.hasNext()) {
            Node productNode = nodeIterator.nextNode();
            String seoPath = productNode.getProperty("SEO Path").getValue().toString();
            Page capstonePage = resource.getResourceResolver().getResource(capstonePagePath).adaptTo(Page.class);

            String[] pageNodes = seoPath.split("/");
            Page indPage;
            if(capstonePage.hasChild(IND)){
                indPage = resource.getResourceResolver().getResource(indPagePath).adaptTo(Page.class);
            } else {
                indPage = pageManager.create(capstonePage.getPath(), IND, template, IND);
            }
            Page tempPage = indPage;
            String tempPath = indPagePath;
            for (String path_ : pageNodes) {
                if(!path_.isEmpty()) {
                    if(!tempPage.adaptTo(Node.class).hasNode(path_)) {
                        Page page = pageManager.create(tempPage.getPath(), path_, template, path_);
                        Node pageNode = page.adaptTo(Node.class);

                        if(pageNodes[pageNodes.length-1].equals(path_)) { // when ypou are at final product node
                            Node jcr = JcrUtils.getOrAddNode(pageNode, "jcr:content", "cq:PageContent");
                            jcr.setProperty("jcr:title", productNode.getProperty("jcr:title").getValue().toString());
                            Node produ = JcrUtils.getOrAddNode(jcr, "product", "nt:unstructured");
                            produ.setProperty("productData", productNode.getPath());
                            Node img = JcrUtils.getOrAddNode(jcr, "image", "nt:unstructured");
                            img.setProperty("fileReference", productNode.getNode("defaultimage").getProperty("fileReference").getValue().toString());
                        }
                    } else { // if titile has been changed
                        if(pageNodes[pageNodes.length-1].equals(path_)) {
                            tempPath+="/"+path_;
                            Node pageNode = session.getNode(tempPath);
                            Node jcr = JcrUtils.getOrAddNode(pageNode, "jcr:content", "cq:PageContent");
                            jcr.setProperty("jcr:title", productNode.getProperty("jcr:title").getValue().toString());
                        }
                    }
                    tempPath+="/"+path_;
                    tempPage = pageManager.getPage(tempPath);
                    session.save();
                }
            }
        }
    }
}