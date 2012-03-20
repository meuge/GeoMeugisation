package com.meuge.geolocalisation;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

public class LectureFichierPOI {
	
	private static boolean testNumber (String s){
		Pattern pattern = Pattern.compile("\\d");
		Matcher matcher = pattern.matcher(s);
		if (matcher.find()){
			return true; 
		} 
		return false; 
	}
	public static void LectureFichier (InputStream fichier, String nomFichier, Context ctx)
	{
	    try{
	    	 String separator = ",";
			 CoordonneesPOIProvider cp = new CoordonneesPOIProvider(CoordonneesPOI.class, ctx);	
	    	  BufferedReader br = new BufferedReader(new InputStreamReader(fichier));
	    	  
	    	  String strLine = br.readLine();
	    	  int compteur = 0;
	    	  //File Ligne Par Ligne
	    	  while (strLine != null)   {
	    		  compteur++;
	    		  strLine = strLine.replaceAll("\"\"", "\"");
	    		  if (strLine.trim().length() >0)
	    		  {
	    			  StringTokenizer temp = new StringTokenizer(strLine,separator);
		              if (temp.countTokens()==3)
		              {
		            	  String longitude = ((String) temp.nextElement()).trim();
		            	  String latitude = ((String) temp.nextElement()).trim();
		            	  String infos = ((String) temp.nextElement()).trim();
		            	  String  magasin = "";
		            	  if (infos.startsWith("\"") && infos.endsWith("\""))
		            		  infos = infos.substring(1, infos.length()-1);
		            	  if (infos.startsWith("[") && (infos.indexOf("]") < infos.length()))
		            	  {
		            		  magasin = infos.substring(1, infos.indexOf("]"));
		            		  infos = infos.substring(infos.indexOf("]")+1).trim();
		            	  }
		            	  if (testNumber(latitude)  && testNumber(longitude))
		            		  cp.store(getCoordsPOI(Double.valueOf(latitude), Double.valueOf(longitude), magasin, infos, "Grands Magasins"));
		            	  else 
		            		  Log.e("MAGASINS",compteur+ " : Erreur ligne :" + strLine);
		              }
	    		  }
	              strLine = br.readLine();
	    	  }
	    	  //Fermeture
	    	  fichier.close();
	    	  if (compteur>0)
	    	  {
	    		  cp.db().commit();
	    		  cp.db().close();
	    	  }
	    	}catch (Exception e){//Catch exception if any
	    		Log.e("MAGASINS"," Erreur Avec le fichier :" + nomFichier);
	    	}
		}
	private static CoordonneesPOI getCoordsPOI(double latitude, double longitude,
			String id_magasin, String adresse, String typePOI) {
		
		CoordonneesPOI retour = new CoordonneesPOI();
		retour.setAdresse(adresse);
		retour.setLatitude(latitude);
		retour.setLongitude(longitude);
		retour.setType(typePOI);
		retour.setCategorie(id_magasin);
		retour.setPositions(CalculLatLong.calculate(retour.getLatitude(), retour.getLongitude()));
		return retour;
		
	}
}