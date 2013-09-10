/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author pupsk
 */
public class Settings {
    private final File xmlFile = new File(".xmlsender/settings.xml");
    private JAXBContext jaxbCtx = null;
    public Params params = new Params();
    
    Settings(){
        importXml();
    }
    
    public void exportXml(){
        try {
            jaxbCtx = JAXBContext.newInstance(Params.class);
            jaxbCtx.createMarshaller().marshal(params, xmlFile);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    public void importXml(){
        try {
            jaxbCtx = JAXBContext.newInstance(Params.class);
            params = (Params) jaxbCtx.createUnmarshaller().unmarshal(xmlFile);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    
}
@XmlRootElement(name="Params")
@XmlAccessorType(XmlAccessType.FIELD)
class Params {
    @XmlElement(name="host")
    public String host = "localhost:8080";
    @XmlElement(name="root")
    public String root = "/";
    @XmlElement(name="isHighlightEnabled")
    public boolean isHighlightEnabled = true;
    @XmlElement(name="isLineNumbersEnabled")
    public boolean isLineNumbersEnabled = true;
}