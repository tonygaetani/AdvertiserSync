package com.tonygaetani;

import com.mongodb.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Main {

    private static class AdvertiserSyncHandler extends DefaultHandler {
        private static MongoClient mongoClient;
        private static DB db;
        private static DBCollection dbCollection;
        private static String currentElement;
        private static Map<String, String> currentProduct = new HashMap<>();
        private static Set<String> updatedUPCs = new HashSet<>();

        public AdvertiserSyncHandler() {
            try {
                mongoClient = new MongoClient();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            db = mongoClient.getDB( "AdSync" );
            dbCollection = db.getCollection("advertisements");
        }

        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            currentElement = qName;
        }

        public void endElement(String uri, String localName,
                               String qName) throws SAXException {
            if(qName.equals("product")) {
                markPreviousInactive();
                addProduct(currentProduct);
                currentProduct = new HashMap<>();
                updatedUPCs = new HashSet<>();
            }
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if(!currentElement.equals("product")) {
                currentProduct.put(currentElement, new String(ch, start, length));
            }
        }

        private void addProduct(final Map<String, String> product) {
            final BasicDBObject basicDBObject = product2DBObject(product);
            // mark the added product as active
            basicDBObject.append("active", true);
            dbCollection.insert(basicDBObject);
            // get the UPC for the added product
            updatedUPCs.add(product.get("upc"));
        }

        private void markPreviousInactive() {
            for(String upc : updatedUPCs) {
                final DBCursor cursor = dbCollection.find(new BasicDBObject("upc", upc));
                for(DBObject o : cursor) {
                    dbCollection.update(o, new BasicDBObject("active", false));
                }

            }
        }

        private BasicDBObject product2DBObject(final Map<String, String> product) {
            BasicDBObject ret = new BasicDBObject();
            for(Map.Entry<String, String> entry : product.entrySet()) {
                ret.append(entry.getKey(), entry.getValue());
            }
            return ret;
        }
    }

    public static void main(String argv[]) {
        int ret = 0;

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = null;
        try {
            saxParser = factory.newSAXParser();
        } catch (ParserConfigurationException e) {
            ret = 100;
        } catch (SAXException e) {
            ret = 101;
        }

        if(ret == 0) {
            DefaultHandler handler = new AdvertiserSyncHandler();

            try {
                saxParser.parse("data.xml", handler);
            } catch (SAXException e) {
                ret = 102;
            } catch (IOException e) {
                ret = 103;
            }
        }

        System.exit(ret);

    }

}
