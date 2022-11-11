package com.akqa.core.Bean;
public class ImageDataBean {

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setTitle(String title) {
        this.title = title;
    }



    private String path;
    private String title;

    private String price;

    private String category;

    private String seopath;

    private String sku;


    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeopath() {
        return seopath;
    }

    public void setSeopath(String seopath) {
        this.seopath = seopath;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}