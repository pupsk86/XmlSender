/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author gridasov
 */
//Не используется, перешел на использование библиотеки org.apache HttpClient
public class NewClass {
    
    public static void main(String[] args) throws MalformedURLException, IOException { 
        // TODO Auto-generated method stub 
        new NewClass();
    } 
    
    public NewClass() throws MalformedURLException, IOException
    {
        String body="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\r\n<soapenv:Body>\r\n<erc:Request xmlns:erc=\"http://schemas.radixware.org/erc.xsd\">\r\n<Operation_Name>NPK_Tarif</Operation_Name>\r\n</erc:Request>\r\n</soapenv:Body>\r\n</soapenv:Envelope>";
        String bodyLength=new Integer(body.length()).toString();
        
        URL oURL = new URL("http://trans-test.neyvabank.ru:27001");
        HttpURLConnection con = (HttpURLConnection) oURL.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Host", "http://trans-test.neyvabank.ru");
        con.setRequestProperty("Content-type", "text/xml; charset=utf-8");
        con.setRequestProperty("Content-length", bodyLength);
        con.setRequestProperty("SOAPAction", "http://schemas.radixware.org/erc.xsd");
        con.setDoOutput(true);
        
        OutputStream reqStream = con.getOutputStream();
        reqStream.write(body.getBytes());
        
        InputStream resStream = con.getInputStream();
        InputStreamReader is = new InputStreamReader(resStream);
        BufferedReader br = new BufferedReader(is);
        String read = br.readLine();

        while(read != null) { System.out.println(read); read = br.readLine(); }
        //System.out.println(resStream);
        

    }
}
