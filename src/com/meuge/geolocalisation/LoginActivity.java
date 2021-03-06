package com.meuge.geolocalisation;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnClickListener {
	private Button ok;
	private Button exit;
	private TextView result;
	private SharedPreferences settings;
	//private Context Classd = LoginActivity.this;
	    /** Called when the activity is first created. */
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	    	getMich();
	    	LogPersos.initLog(getApplicationContext(), getSharedPreferences("appinfos",MODE_PRIVATE));
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.login);
	        // Login button clicked
	        ok = (Button)findViewById(R.id.btn_login);
	        exit = (Button)findViewById(R.id.deconnection);
	        ok.setOnClickListener(this);
	        exit.setOnClickListener(this);
	        settings = getSharedPreferences("userdetails",MODE_WORLD_READABLE);
	        result = (TextView)findViewById(R.id.lbl_result);
	    }
	    private void getMich() {
	    	String NAMESPACE = "http://webservices.meuge.com/" ;
	    	String METHOD_NAME = "getPrice";
	    	String SOAP_ACTION = NAMESPACE + METHOD_NAME;
	    	String URL = "http://localhost:8080/WebServiceProject/services/StockQuoteService";


//	    		String endPoint    = "http://localhost:8080/WebServiceProject/services/StockQuoteService";
//	    		String nameSpace   = "http://webservices.meuge.com";
//	    		String nameService = "StockQuoteServiceService";
//	    		URL url = new URL (endPoint+"?wsdl");
//	    		String operation ="getPrice";
	    		
	    	SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
	    	SoapSerializationEnvelope envelope =
	    		    new SoapSerializationEnvelope(SoapEnvelope.VER11);
	    		    envelope.dotNet =  false;
	    		    envelope.setOutputSoapObject(request);
	    		    HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);

	    		    try
	    		    {

	    		    androidHttpTransport.call(SOAP_ACTION, envelope);

	    		    SoapPrimitive Result = (SoapPrimitive)envelope.getResponse();

	    		   String hresult = Result.toString();
	    		   hresult.toCharArray();
	    		    }
	    		    catch(Exception e) 
	    		    {
	    		    	e.getClass();
	    		    }
	    
	    }
	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.menu, menu);
	        return true;
	    }
	    
	    public boolean onPrepareOptionsMenu(Menu menu){
	     
	    	boolean result = super.onPrepareOptionsMenu(menu);
	    	MenuItem tt = menu.findItem(R.id.icontext);
	    	tt.setVisible(true);
	    	return result;
	    }
	    private String getChampbyId(int id)
	    {
	    	EditText uname = (EditText)findViewById(id);
	    	return uname.getText().toString();
	    }
	    private void postLoginData() {
	       	 SharedPreferences.Editor prefsEditor;
	       	 SharedPreferences myPrefs = getSharedPreferences("userdetails", MODE_WORLD_READABLE);
	       	 prefsEditor = myPrefs.edit();
	       	 
	         
	         prefsEditor.putString("login",    getChampbyId(R.id.txt_username));
	         prefsEditor.putString("password", getChampbyId(R.id.txt_password));
	         prefsEditor.commit();
	         result.setText(LoginMetier.postLoginData(getChampbyId(R.id.txt_username), getChampbyId(R.id.txt_password)));
	         //TODO: PassageValue
	         passageVue(OngletsActivity.class);
	    }
	    private String retournePrefs() 
	    {
	       	String retour = settings.getString("login", "") + " " +settings.getString("password", "");
	        return retour.trim();

	    }
	    private void clearPrefs(String myPreference)
	    {
        	SharedPreferences.Editor prefsEditor;
        	SharedPreferences myPrefs = getSharedPreferences(myPreference, MODE_WORLD_READABLE);
        	prefsEditor = myPrefs.edit();
        	prefsEditor.clear();
        	prefsEditor.commit();
	    }
	    public void popIt( String title, String message ){
	        new AlertDialog.Builder(this)
	        .setTitle( title )
	        .setMessage( message )
	        .setPositiveButton("YES", new android.content.DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface arg0, int arg1) {
	            	clearPrefs("userdetails");
	            	finish();
	            }
	        })
	        .setNegativeButton("NO", new android.content.DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface arg0, int arg1) {
	                Toast.makeText(getBaseContext(), "Retour au programme", Toast.LENGTH_SHORT).show();
	                // TODO: Passage � ImageGrid
	                //passageVue(ImageGrid.class);
	            }
	        }).show();
	    }
	    @Override
	    public void onClick(View view) {
	       switch (view.getId())
	       {
	       		case R.id.btn_login : postLoginData(); break;
	       		case R.id.deconnection : popIt(retournePrefs(), "Etes vous sur de quitter ?" ) ; break;
	       }
		}
	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	    	switch (item.getItemId()) {
	        	case R.id.icontext: Toast.makeText(this, "Inscription", Toast.LENGTH_LONG).show();
	        						passageVue(InscriptionActivity.class);break;
	        }
	    	return true;
	    }
	    private void passageVue(Class goToActivity) {
	    	Intent intent = LoginMetier.passageVue(this,
                    goToActivity);
			intent.putExtra("login", getChampbyId(R.id.txt_username));
			startActivity(intent);
	    }
	    public String inscription() {
	    	try {
	            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	                NetworkInterface intf = en.nextElement();
	                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                    InetAddress inetAddress = enumIpAddr.nextElement();
	                    if (!inetAddress.isLoopbackAddress()) {
	                        return "Ton ip est :" +inetAddress.getHostAddress().toString();
	                    }
	                }
	            }
	        } catch (SocketException ex) {
	            Log.e("ipError", ex.toString());
	        }
	        return "Aucune ip trouvee";
	    }
}