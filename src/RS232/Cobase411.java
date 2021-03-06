/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package RS232;


import configuration.xmlparser;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import log.DisplayMessageType;


/**
 *
 * @author BLIS
 */
public class Cobase411 extends Thread {
    
     
     private static List<String> testIDs = new ArrayList<String>();
     static final char Start_Block = (char)2;
     static final char End_Block = (char)3;
     static final char CARRIAGE_RETURN = 13; 
     private static StringBuilder datarecieved = new StringBuilder();
    
  @Override
    public void run() {
        log.AddToDisplay.Display("Cobas e 411 handler started...", DisplayMessageType.TITLE);
        log.AddToDisplay.Display("Checking available ports on this system...", DisplayMessageType.INFORMATION);
        String[] ports = Manager.getSerialPorts();
        log.AddToDisplay.Display("Avaliable ports:", DisplayMessageType.TITLE);
       for(int i = 0; i < ports.length; i++){           
           log.AddToDisplay.Display(ports[i],log.DisplayMessageType.INFORMATION);
        }            
       log.AddToDisplay.Display("Now connecting to port "+RS232Settings.COMPORT , DisplayMessageType.TITLE);
       if(Manager.openPortforData("Cobas e411"))
       {
           log.AddToDisplay.Display("Connected sucessfully",DisplayMessageType.INFORMATION);   
           setTestIDs();
       }      
      
    }
    
    public static void HandleDataInput(String data)
    {
       
            if(data.charAt(0) == Start_Block)
            {
                datarecieved = new StringBuilder();                
            }
            if(data.contains(String.valueOf(End_Block)))
            {
                int endindex = data.indexOf(String.valueOf(End_Block));
                datarecieved.append(data.substring(0, endindex));
                processMessage();
                datarecieved = new StringBuilder();
                if(data.substring(endindex).length()> 1)
                {
                    if(data.startsWith(String.valueOf(End_Block)))
                        HandleDataInput(data.substring(endindex+2));
                    else                        
                        HandleDataInput(data.substring(endindex));
                }
            }
            else
            {
                datarecieved.append(data);
            }
            //datarecieved.append(data);
            /*if(data.charAt(data.length()-1) == End_Block)
            {
                processMessage();
            }  */        
       
           
    }
    private static void processMessage()
    {
        if(null == datarecieved.toString() || datarecieved.toString().isEmpty())
            return;
        String[] DataParts = datarecieved.toString().split(";");
        if(DataParts.length > 5)
        {
            String Type  = DataParts[2].trim();
            int mID=0;
            float value = 0;
            boolean flag = false;
                           
                    String specimen_id = DataParts[3].trim();
                    for(int i=10;i<DataParts.length;i+=4)
                    {
                        mID = getMeasureID(DataParts[i].trim());
                        if(mID > 0)
                        {
                            try
                            {
                                value = Float.parseFloat(DataParts[i+1].trim());
                            }catch(NumberFormatException e){
                                try{
                                value = Float.parseFloat(DataParts[i+1].trim());
                                }catch(NumberFormatException ex){}
                            
                            }
                            if(SaveResults(specimen_id, mID,value))
                            {
                                flag = true;
                            }
                        }

                    }
                    if(flag)
                    {
                         log.AddToDisplay.Display("Results with Code: "+specimen_id +" sent to BLIS sucessfully",DisplayMessageType.INFORMATION);
                    }
                    else
                    {
                         log.AddToDisplay.Display("Test with Code: "+specimen_id +" not Found on BLIS",DisplayMessageType.WARNING);
                    }
                
            
        }
       
    }
    
    public void Stop()
    {
        if(Manager.closeOpenedPort())
        {
            log.AddToDisplay.Display("Port Closed sucessfully", log.DisplayMessageType.INFORMATION);
        }
    }
    
    private void setTestIDs()
     {
         String equipmentid = getSpecimenFilter(3);
         String blismeasureid = getSpecimenFilter(4);
        
         String[] equipmentids = equipmentid.split(",");
         String[] blismeasureids = blismeasureid.split(",");
         for(int i=0;i<equipmentids.length;i++)
         {
             testIDs.add(equipmentids[i]+";"+blismeasureids[i]);             
         }
        
     }
    
    private static String getSpecimenFilter(int whichdata)
    {
        String data = "";
        xmlparser p = new xmlparser("configs/selectrajunior/selectrajunior.xml");
        try {
            data = p.getMicros60Filter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(MICROS60.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return data;        
    }
    
     private static int getMeasureID(String equipmentID)
     {
         int measureid = 0;
         for(int i=0;i<testIDs.size();i++)
         {
             if(testIDs.get(i).split(";")[0].equalsIgnoreCase(equipmentID))
             {
                 measureid = Integer.parseInt(testIDs.get(i).split(";")[1]);
                 break;
             }
         }
         
         return measureid;
     }
     
    private static boolean SaveResults(String barcode,int MeasureID, float value)
     {
         
         
          boolean flag = false;       
          if("1".equals(BLIS.blis.saveResults(barcode,MeasureID,value,0)))
           {
              flag = true;
            }
                          
         return flag;
         
     }    
   
}
