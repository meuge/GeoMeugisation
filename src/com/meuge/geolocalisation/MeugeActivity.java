package com.meuge.geolocalisation;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.code.microlog4android.Logger;



import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MeugeActivity extends Activity  implements OnClickListener, LocationListener {
	private LocationManager lManager;
    private Location location;
    private String choix_source = "";
    final Handler handler = new Handler();
	private static Logger logger = LogPersos.getLoggerPerso();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //On sp�cifie que l'on va avoir besoin de g�rer l'affichage du cercle de chargement
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 
        setContentView(R.layout.meuge_layout);
 
        //On r�cup�re le service de localisation
        lManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        //Initialisation de l'�cran
        reinitialisationEcran();
 
        //On affecte un �couteur d'�v�nement aux boutons
        findViewById(R.id.choix_source).setOnClickListener(this);
        findViewById(R.id.obtenir_adresse).setOnClickListener(this);
        findViewById(R.id.afficherAdresse).setOnClickListener(this);
        findViewById(R.id.refresh).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.world).setOnClickListener(this);
        obtenirPosition();
    }
    
    //On cree le menu Quitter
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.icontext : finish(); break; 
    	}
    	return true;
    }
    
        //M�thode d�clencher au clique sur un bouton
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.choix_source:
			choisirSaSource();
			break;
		case R.id.afficherAdresse:
			afficherAdresse();
			break;
		case R.id.obtenir_adresse:
			afficherLatitude();
			break;
		case R.id.refresh:
			reinitialisationEcran();
			break;
		case R.id.save:
			//saveDBCoordonnees();
			break;
		case R.id.world:
			tacheDeFond();
			break;
		default:
			break;
		}
	}
 
	//R�initialisation de l'�cran
	private void reinitialisationEcran(){
		BundleTools.storeGPSStatus(false);
		((TextView)findViewById(R.id.latitude)).setText("0.0");
		((TextView)findViewById(R.id.longitude)).setText("0.0");
		((TextView)findViewById(R.id.altitude)).setText("0.0");
		((TextView)findViewById(R.id.adresse)).setText("");
		((TextView)findViewById(R.id.adresse_etat)).setText("");
 
	}
    //Va a la page GPS (independant de nous)
    private void showGpsOptions(){  
        Intent gpsOptionsIntent = new Intent(  
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);  
        startActivity(gpsOptionsIntent);  
    }  
    //Affiche la toolbox fdu GPS et ensuite realise l'action associee
	private void createGpsDisabledAlert(){  
		BundleTools.storeGPSStatus(true);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);  
    builder.setMessage("Votre GPS est eteint! Voulez vous l'allumer ?")  
         .setCancelable(false)  
         .setPositiveButton("GPS OK",  
              new DialogInterface.OnClickListener(){  
              public void onClick(DialogInterface dialog, int id){ 
                   showGpsOptions();
              }  
         });  
         builder.setNegativeButton("GPS KO",  
              new DialogInterface.OnClickListener(){  
              public void onClick(DialogInterface dialog, int id){  
                   dialog.cancel();
                   obtenirPosition();
              //     choisirSource();
              }  
         });  
    AlertDialog alert = builder.create(); 
    alert.show(); 
    }
	// J'ai perdu le focus
	@Override
	public void onStop()
	{
	    super.onStop();
	    saveCoordonnees();
	}

	/** Called when the activity looses focus **/
	@Override
	public void onPause()
	{
		super.onPause();
		saveCoordonnees();
	}
	
	//Sauvegarde 
	private void saveCoordonnees() {
		Intent myIntent = getParent().getIntent();
		BundleTools.storeLatitude(getLatitude());
		BundleTools.storeLongitude(getLongitude());
		BundleTools.storeAdresse(getAdresse());
		BundleTools.commitExtras(myIntent);
		setIntent(myIntent);
	}
	//Fais un bean
	private CoordonneesPOI getCoords()
	{
		CoordonneesPOI retour = new CoordonneesPOI();
		retour.setAdresse(getAdresse());
		retour.setLatitude(getLatitude());
		retour.setLongitude(getLongitude());
		retour.setUUID(getUUID());
		retour.setPositions(CalculLatLong.calculate(getLatitude(), getLongitude()));
		return retour;
	}
	//Sauvegarde en base
	private void saveDBCoordonnees()
	{
		if (!(getLatitude()==(double) 0L && getLongitude()==(double) 0L))
		{
			CoordonneesPOIProvider cp = new CoordonneesPOIProvider(CoordonneesPOI.class, this);
			//Coordonnee dans la base
			List<CoordonneesPOI> coordonnee = cp.findByLatLong(getCoords());
			if(coordonnee.size() == 0)
			{
		        cp.store(getCoords());
		        cp.db().commit();
			}
	        cp.close();
	        cp.db().close();
		}
	}
	

	//Affiche les sources possibles
	private void choisirSource() {
		//On demande au service la liste des sources disponibles.
		List <String> providers = lManager.getProviders(true);
		final String[] sources = new String[providers.size()];
		int i =0;
		//on stock le nom de ces source dans un tableau de string
		for(String provider : providers)
			sources[i++] = provider;
 
		//On affiche la liste des sources dans une fen�tre de dialog
		//Pour plus d'infos sur AlertDialog, vous pouvez suivre le guide
		//http://developer.android.com/guide/topics/ui/dialogs.html
		new AlertDialog.Builder(MeugeActivity.this)
		.setItems(sources, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//on stock le choix de la source choisi
				choix_source = sources[which];
				//on ajoute dans la barre de titre de l'application le nom de la source utilis�
				setTitle(String.format("%s - %s", getString(R.string.app_name),
						choix_source));
				obtenirPosition();
			}
		})
		.create().show();
	}

	/**
	 * 
	 */
	private void choisirSaSource() {
		//reinitialisationEcran();
	    if (!lManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&  !BundleTools.GPSStatus()){  
	          createGpsDisabledAlert();  
	    }
	    else obtenirPosition();
	    //else choisirSource();
	}
 
	private void obtenirPosition() {
		
		//on ajoute dans la barre de titre de l'application le nom de la source utilis�
		//on d�marre le cercle de chargement
		setProgressBarIndeterminateVisibility(true);
 
		//On demande au service de localisation de nous notifier tout changement de position
		//sur la source (le provider) choisie, toute les minutes (60000millisecondes).
		//Le param�tre this sp�cifie que notre classe impl�mente LocationListener et recevra
		//les notifications.
	      //Choix du service de localisation en fonction de crit�res
        Criteria crit = new Criteria();
        crit.setCostAllowed(false);
     //   crit.setAccuracy(100); //pr�cis � 1km pret

        String choix_s = lManager.getBestProvider(crit, true);
        setTitle(String.format("%s - %s", getString(R.string.app_name),
        		choix_source));
		lManager.requestLocationUpdates(choix_s, 60000, 0, this);
	}
 
	
	private void afficherLocation() {
		//On affiche les informations de la position a l'�cran
		((TextView)findViewById(R.id.latitude)).setText(String.valueOf(location.getLatitude()));
		((TextView)findViewById(R.id.longitude)).setText(String.valueOf(location.getLongitude()));
		((TextView)findViewById(R.id.altitude)).setText(String.valueOf(location.getAltitude()));
	}
 
	private void afficherAdresse() {
		setProgressBarIndeterminateVisibility(true);
 
		geoCoderInformation();
		//on stop le cercle de chargement
		setProgressBarIndeterminateVisibility(false);
	}
	private void afficherLatitude() {
		setProgressBarIndeterminateVisibility(true);
		
		geoCoderAdresseInformation();
		//on stop le cercle de chargement
		setProgressBarIndeterminateVisibility(false);
	}
	/**
	 *  Obtenir les coordonnees avec Adresse
	 */
	private void geoCoderAdresseInformation() {
		//Le geocoder permet de r�cup�rer ou chercher des adresses
		//gr�ce � un mot cl� ou une position
		Geocoder geo = new Geocoder(MeugeActivity.this);
		try {
			//Ici on r�cup�re la premiere adresse trouv� gr�ce � la position que l'on a r�cup�r�
			String monAdresse = getAdresse();
			List
			<Address> adresses = geo.getFromLocationName(monAdresse, 1);
			if(adresses != null && adresses.size() == 1){
				Address adresse = adresses.get(0);
				
				//Si le geocoder a trouver une adresse, alors on l'affiche
				
				((TextView)findViewById(R.id.adresse_etat)).setText(String.format("Latitude : %s - Longitude :%s",
						adresse.getLatitude(),
						adresse.getLongitude()));
			}
			else {
				//sinon on affiche un message d'erreur
				((TextView)findViewById(R.id.adresse_etat)).setText("L'adresse n'a pu �tre d�termin�e");
			}
		} catch (IOException e) {
			e.printStackTrace();
			((TextView)findViewById(R.id.adresse_etat)).setText("L'adresse n'a pu �tre d�termin�e");
		}
	}
 	
	/**
	 *  Obtenir les coordonnees avec latitude et longitude
	 */
	private void geoCoderInformation() {
		//Le geocoder permet de r�cup�rer ou chercher des adresses
		//gr�ce � un mot cl� ou une position
		double latitude = 0L;
		double longitude = 0L;
		Geocoder geo = new Geocoder(MeugeActivity.this);
		try {
			//Ici on r�cup�re la premiere adresse trouv� gr�ce � la position que l'on a r�cup�r�
		
			latitude  = getLatitude() ;
			longitude = getLongitude() ;
			List
<Address> adresses = geo.getFromLocation(latitude,
					longitude,1);
 
			if(adresses != null && adresses.size() == 1){
				Address adresse = adresses.get(0);
				//Si le geocoder a trouver une adresse, alors on l'affiche
				((EditText)findViewById(R.id.adresse)).setText(String.format("%s %s %s ,%s",
						adresse.getAddressLine(0),
						adresse.getPostalCode(),
						adresse.getLocality(),
						adresse.getCountryName()));
			}
			else {
				//sinon on affiche un message d'erreur
				((TextView)findViewById(R.id.adresse_etat)).setText("L'adresse n'a pu �tre d�termin�e");
			}
		} catch (IOException e) {
			e.printStackTrace();
			((TextView)findViewById(R.id.adresse_etat)).setText("L'adresse n'a pu �tre d�termin�e");
		} catch (NumberFormatException e) {
			((TextView)findViewById(R.id.adresse_etat)).setText("Coordonn�es entr�es invalides");
		}
	}

	/**
	 * Obtenir la longitude
	 */
	private Double getLongitude() {
		Double retour = null;
		try {
		return Double.valueOf(((TextView)findViewById(R.id.longitude)).getText().toString());
		} catch (NumberFormatException e) {
			retour = (double) 0L;
		}
		return retour;
	}

	/**
	 * Obtenir la latitude
	 */
	private Double getLatitude() {
		Double retour = null;
		try {
			retour = Double.valueOf(((TextView)findViewById(R.id.latitude)).getText().toString());
		} catch (NumberFormatException e) {
			retour = (double) 0L;
		}
		return retour;
	}
    /**
     * Obtenir l'adresse
     */
	private String getAdresse() {
		return ((EditText)findViewById(R.id.adresse)).getText().toString();
	}
	
	/**
     * Obtenir L'UUID
     */
	private String getUUID() {
		TelephonyManager tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		return tManager.getDeviceId();
		
	}
	
	
	public void onLocationChanged(Location location) {
		//Lorsque la position change...
		logger.info("Geolocalisation : La position a chang�.");
		//... on stop le cercle de chargement
		setProgressBarIndeterminateVisibility(false);
		//... on active le bouton pour afficher l'adresse
		findViewById(R.id.afficherAdresse).setEnabled(true);
		//... on sauvegarde la position
		this.location = location;
		//... on l'affiche
		afficherLocation();
		//... et on sp�cifie au service que l'on ne souhaite plus avoir de mise � jour
		lManager.removeUpdates(this);
	}
 
	public void onProviderDisabled(String provider) {
		//Lorsque la source (GSP ou r�seau GSM) est d�sactiv�
		logger.info("G�olocalisation : La source a �t� d�sactiv�");
		//...on affiche un Toast pour le signaler � l'utilisateur
		Toast.makeText(MeugeActivity.this,
				String.format("La source \"%s\" a �t� d�sactiv�", provider),
				Toast.LENGTH_SHORT).show();
		//... et on sp�cifie au service que l'on ne souhaite plus avoir de mise � jour
		lManager.removeUpdates(this);
		//... on stop le cercle de chargement
		setProgressBarIndeterminateVisibility(false);
	}
	//Demarrage en tache de fond
	public void tacheDeFond() {
		ToggleButton tgb = (ToggleButton) findViewById(R.id.world);
		if (tgb.isChecked() && choix_source.length()==0)
		{
			Toast.makeText(this, "Choississez au moins une source !! ", Toast.LENGTH_SHORT).show();
			tgb.setChecked(false);
		}
		else
			startThread();
	    
	}
	/**
	 * 
	 */
	private void startThread() {
		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
		@Override
		public void run() {
			ToggleButton tgb = (ToggleButton) findViewById(R.id.world);
			if (!tgb.isChecked())
			{
				timer.cancel();
			}
			else
				handler.post(checkCoords());
		}
		}, 10000, 10000); //delay, //periode
	}

	/**
	 * @return
	 */
	private Runnable checkCoords() {
		return new Runnable() {
			  @Override
			  public void run() {
				  	obtenirPosition();
				  	geoCoderInformation();
			    	handler.removeCallbacks(this);
			  }
			  
			};
	}
	
	public void onProviderEnabled(String provider) {
		logger.info("G�olocalisation : La source a �t� activ�.");
	}
	public void onStatusChanged(String provider, int status, Bundle extras) {
		logger.info("G�olocalisation : Le statut de la source a chang�.");
	}
	
 }
