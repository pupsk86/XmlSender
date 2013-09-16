/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
class Params {
    @XmlElement
    public String root = "/";
    @XmlElement
    public boolean isHighlightEnabled = true;
    @XmlElement
    public boolean isLineNumbersEnabled = true;
    @XmlElement
    private int framePositionX = 300;
    @XmlElement
    private int framePositionY = 300;
    @XmlElement
    private int frameWidth = 600;
    @XmlElement
    private int frameHeight = 450;
    @XmlElement
    public int dividerLocation = 200;
    @XmlElement
    public int lastDividerLocation = 200;
    @XmlElement
    public List<String> hostList = new ArrayList<String>(Arrays.asList(new String("http://localhost:8080")));
    @XmlElement
    public int hostIdx = 0;
    
    public void setFrameBounds(Rectangle re){
        if(re != null) {
            framePositionX = re.x;
            framePositionY = re.y;
            frameHeight    = re.height;
            frameWidth     = re.width;
        }
    }
    public Rectangle getFrameBounds(){
        return new Rectangle(framePositionX, framePositionY, frameWidth, frameHeight);
    }
}