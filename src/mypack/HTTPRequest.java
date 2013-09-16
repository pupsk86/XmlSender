/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class HTTPRequest implements Runnable
{
    private HttpClient httpclient;
    private HttpResponse response;
    private HttpUriRequest httpurirequest;
    private String RequestResult = "";
    private String RequestEntity = "";
    
    public void run() {
        try {
            response = httpclient.execute(httpurirequest);
            RequestResult = String.valueOf(response.getStatusLine().getStatusCode()) +
                            " " + response.getStatusLine().getReasonPhrase();
            HttpEntity Res_entity = response.getEntity();
            if (Res_entity != null) {
                RequestEntity = EntityUtils.toString(Res_entity);
            }
        } catch (Exception e) {
            RequestResult = e.toString();
        }
    }
    
    @SuppressWarnings("unused")
    public HTTPRequest(String body, String HostName) throws URISyntaxException, UnsupportedEncodingException {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
        httpclient = new DefaultHttpClient(httpParams);
        
        URI uri=new URI(HostName);
        StringEntity Req_entity = new StringEntity(body, "text/xml", HTTP.UTF_8);
        
        //post or get
        HttpPost httpPost;
        httpPost = new HttpPost(uri);
        httpPost.setHeader("Host", uri.getHost());
        httpPost.setHeader("Content-type", "text/xml; charset=utf-8");
        httpPost.setEntity(Req_entity);
        httpurirequest = (HttpUriRequest)httpPost;
        //post or get
    }
    
    public int getResultCode() {
        return response.getStatusLine().getStatusCode();
    }
    
    public String getResult() {
        return (RequestResult == null ? "" : RequestResult);
    }
    
    public String getResultEntity() {
        return RequestEntity == null ? "" : prettyFormat(RequestEntity);
    }
    
    public static String prettyFormat(String input, int indent) {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer(); 
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setErrorListener(null);
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            return input;
        }
    }

    public static String prettyFormat(String input) {
        return prettyFormat(input, 2);
    }
    
}