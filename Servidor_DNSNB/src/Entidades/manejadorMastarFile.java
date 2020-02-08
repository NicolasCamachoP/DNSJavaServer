package Entidades;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import Entidades.retornoMasterFile;

public class manejadorMastarFile
{

	retornoMasterFile leerMasterFile(String ruta) throws FileNotFoundException, IOException 
	{

        BufferedReader buffReader = new BufferedReader(new FileReader("m.txt"));
        ArrayList<String> records = new ArrayList<String>();
        retornoMasterFile retornar = new retornoMasterFile();
        Map<String, String> mapAux = new HashMap<String, String>();
        try {
            String line = buffReader.readLine();
            while (line != null) {
                records.add(line);
                line = buffReader.readLine();
            }
            System.out.println("Numero de resgistros leidos :" + (records.size()-16));
        } 
        catch (IOException ex) {
            java.util.logging.Logger.getLogger(servidorDNS.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error al leer el archivo");
        } 
        finally {
            buffReader.close();
            String domain = "", IN = "", TYPE = "", ipDir = "";
            if (records.size() > 0) 
            {
            	//LOGICA PARA DETECTAR DIRECCIONES 
                StringTokenizer tokenizer;
                String recIt;
                for (int i = 0;i < records.size() ; i++) 
                {
                	recIt = records.get(i);
                	if(recIt.length() > 0)
                	{
                		if(recIt.charAt(0) == '(')//Inicio descripcion de caracteristicas generales
                    	{
                			ArrayList<String> caracteristicasDNS = new ArrayList<>();
                    		while(!recIt.equals(")"))
                    		{
                    			i++;
                    			recIt = records.get(i);
                    			tokenizer = new StringTokenizer(recIt, ";");
                    			caracteristicasDNS.add(tokenizer.nextToken());
                    		}
                    		retornar.setDirDnsAux(caracteristicasDNS.get(0));
                    		retornar.setDirMailBoxEncharge(caracteristicasDNS.get(1));
                    		retornar.setVersionMasterFile(Integer.parseInt(caracteristicasDNS.get(2)));
                    		retornar.setRefreshInterval(Long.parseLong(caracteristicasDNS.get(3)));
                    		retornar.setExpireInterval(Long.parseLong(caracteristicasDNS.get(4)));
                    		retornar.setTTL(Long.parseLong(caracteristicasDNS.get(5)));
                    	}
                		else if(recIt.charAt(0) != ' ' && recIt.charAt(0) != ';' && recIt.charAt(0) != ')' && recIt.charAt(0) != '\r')//Direcciones Ip y Dominios
                		{
                			tokenizer = new StringTokenizer(recIt, "\t");
                            domain = tokenizer.nextToken();
                            IN = tokenizer.nextToken();
                            TYPE = tokenizer.nextToken();
                            ipDir = tokenizer.nextToken();
                            mapAux.put(domain, ipDir);
                		}
                	}
                    
                }
            } 
            else 
            {
                System.out.println("Archivo vacio");
            }
        }
        if (mapAux.size() > 0) 
        {
            System.out.println("MasterFile leido correctamente");
            retornar.setZonas(mapAux);
            return retornar;
        } 
        else 
        {
            return null;
        }
    }
}
