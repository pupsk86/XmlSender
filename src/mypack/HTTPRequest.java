/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack;

import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class HTTPRequest
{
    //public static void main(String[] args) { 
        // TODO Auto-generated method stub 
      //  new HTTPRequest();
    //} 
    
    static String RequestResult = "";
    
    @SuppressWarnings("unused")
    public HTTPRequest(String body, String HostName, String SOAPAction, Boolean isSoap) {
        try {
            
            final HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
            HttpClient httpclient = new DefaultHttpClient(httpParams);

            //String body="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\r\n<soapenv:Body>\r\n<erc:Request xmlns:erc=\"http://schemas.radixware.org/erc.xsd\">\r\n<Operation_Name>NPK_Tarif</Operation_Name>\r\n</erc:Request>\r\n</soapenv:Body>\r\n</soapenv:Envelope>";
            //String bodyLength=new Integer(body.length()).toString();
            //System.out.println(bodyLength);

            //URI uri=new URI("http://trans-test.neyvabank.ru:27001");
            URI uri=new URI(HostName);
            HttpPost httpPost = new HttpPost(uri);

            //httpPost.setHeader("Host", "http://trans-test.neyvabank.ru");
            httpPost.setHeader("Host", HostName);
            httpPost.setHeader("Content-type", "text/xml; charset=utf-8");
            //httpPost.setHeader("SOAPAction", "http://schemas.radixware.org/erc.xsd");
            if (isSoap) httpPost.setHeader("SOAPAction", SOAPAction);

            StringEntity Req_entity = new StringEntity(body, "text/xml", HTTP.UTF_8);
            httpPost.setEntity(Req_entity);
            
            HttpResponse response = httpclient.execute(httpPost);
            //System.out.println(response);
            RequestResult = String.valueOf(response.getStatusLine().getStatusCode()) +
                            " " + response.getStatusLine().getReasonPhrase();
            
            HttpEntity Res_entity = response.getEntity();
            if (Res_entity != null & Res_entity.getContentLength() != -1 ) {
                System.out.println(EntityUtils.toString(Res_entity));
            }
            /*for (int i = 0; i <= 100; i++) {
                System.out.print("_");
                if (i == 100) { System.out.println(); }
            }*/
            

            //System.out.println(response.);
        } catch (Exception e) {
            //e.printStackTrace();
            RequestResult = e.toString();
        }
    }
    
    static String getResult() {
        return RequestResult;
    }
    
}