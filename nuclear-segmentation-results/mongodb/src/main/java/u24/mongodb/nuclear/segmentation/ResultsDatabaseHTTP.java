package u24.mongodb.nuclear.segmentation;

import org.apache.http.HttpHost;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

/**
 * Node.js service to see how fast we can push data to the database.
 * HTTP protocol - Allow people to load small amount of data
 * without having direct access to the database.
 */
public class ResultsDatabaseHTTP {

    private String resultDbName;
    private String dbHost;
    private int dbPort;
    private CloseableHttpClient client;
    private CredentialsProvider credsProvider;

    public ResultsDatabaseHTTP(String dbURI, String username, String password) {
        String[] tokens = dbURI.split("://|:|/");
        this.dbHost = tokens[1];
        this.dbPort = Integer.parseInt(tokens[2]);
        this.resultDbName = tokens[3];

        credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(this.dbHost, this.dbPort),
                new UsernamePasswordCredentials(username, password));
    }

    private void submitDocument(String doc, String collection) {
        this.client = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        try {
            HttpHost httpHost = new HttpHost(dbHost, dbPort, "http");
            HttpPost httpPost = new HttpPost("/api/v1/database/"
                    + resultDbName + "/" + collection);

            StringEntity entity = new StringEntity(doc);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-type", "application/json");

            CloseableHttpResponse response = client.execute(httpHost, httpPost);
            int status = response.getStatusLine().getStatusCode();
            if (status < 200 || status > 300) {
                System.err.println("ERROR: " + status + " Msg: " + response.getStatusLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkMetadataExists(String caseid, String execid) {
        client = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        boolean retVal = false;
        try {
            HttpHost target = new HttpHost("localhost", 3000, "http");

            // specify the get request
            String query = "/api/v1/database/" + resultDbName
                    + "/metadata?filter[where][image.caseid]=" + caseid
                    + "&filter[where][provenance.analysis_execution_id]=" + execid;

            System.out.println("QUERY: " + query);
            HttpGet getRequest = new HttpGet(query);

            CloseableHttpResponse httpResponse = client.execute(target, getRequest);
            HttpEntity entity = httpResponse.getEntity();

            if (entity != null) {
                String resValue = EntityUtils.toString(entity);
                retVal = !resValue.equals("null");
            } else {
                retVal = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Returning : " + retVal);
        return retVal;
    }

    public String getImageObject(String caseid) {
        client = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        try {
            HttpHost target = new HttpHost("localhost", 3000, "http");

            // specify the get request
            // String query = "/api/v1/database/" + resultDbName + "/images?caseid=" + caseid;
            // loopback
            String query = "/api/v1/database/" + resultDbName + "/images/findOne?filter[where][caseid]=" + caseid;
            System.out.println("Query: " + query);
            HttpGet getRequest = new HttpGet(query);

            CloseableHttpResponse httpResponse = client.execute(target, getRequest);
            HttpEntity entity = httpResponse.getEntity();

            if (entity != null) {
                return EntityUtils.toString(entity);
                // loopback
                // String entity_str = EntityUtils.toString(entity);
                // System.out.println("RECEIVED: " + entity_str);
                // return entity_str;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void submitObjectsDocument(String doc) {
        String objectsCollection = "objects";
        submitDocument(doc, objectsCollection);
    }

    public void submitMetadataDocument(String doc) {
        String metadataCollection = "metadata";
        submitDocument(doc, metadataCollection);
    }
}
