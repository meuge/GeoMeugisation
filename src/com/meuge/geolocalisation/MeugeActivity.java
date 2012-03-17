package com.meuge.geolocalisation;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;



import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
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
			saveDBCoordonnees();
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
                   choisirSource();
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
		final Bundle bundle = new Bundle();
		Intent myIntent = getParent().getIntent();
		double []arrayInfos = new double[2];
		arrayInfos[0] = getLatitude();
		arrayInfos[1] = getLongitude();
		bundle.putDoubleArray("GPSINFO", arrayInfos);
		bundle.putString("ADRESSEINFO", getAdresse());
		myIntent.putExtras(bundle);
		
		setIntent(myIntent);
	}
	//Fais un bean
	private Coordonnees getCoords()
	{
		Coordonnees retour = new Coordonnees();
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
		InitDB_POI();
		if (!(getLatitude()==(double) 0L && getLongitude()==(double) 0L))
		{
			CoordonneesProvider cp = new CoordonneesProvider(Coordonnees.class, this);
			//Coordonnee dans la base
			List<Coordonnees> coordonnee = cp.findByLatLong(getCoords());
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
		reinitialisationEcran();
	    if (!lManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){  
	          createGpsDisabledAlert();  
	    }
	    else choisirSource();
	}
 
	private void obtenirPosition() {
		//on d�marre le cercle de chargement
		setProgressBarIndeterminateVisibility(true);
 
		//On demande au service de localisation de nous notifier tout changement de position
		//sur la source (le provider) choisie, toute les minutes (60000millisecondes).
		//Le param�tre this sp�cifie que notre classe impl�mente LocationListener et recevra
		//les notifications.
		lManager.requestLocationUpdates(choix_source, 60000, 0, this);
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
		Log.i("Tuto g�olocalisation", "La position a chang�.");
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
		Log.i("Tuto g�olocalisation", "La source a �t� d�sactiv�");
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
		Log.i("G�olocalisation", "La source a �t� activ�.");
	}
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i("G�olocalisation", "Le statut de la source a chang�.");
	}
	
	//Initialise base de donn�e POI
	private void InitDB_POI()
	{
		if (!(getLatitude()==(double) 0L && getLongitude()==(double) 0L))
		{
			CoordonneesPOIProvider cp = new CoordonneesPOIProvider(CoordonneesPOI.class, this);
			Double latitude= 0.0, longitude= 0.0; String id_magasin= "", typePOI="Magasin But";

			if(cp.getNbLignes() <= 0)
			{
				 latitude= 43.30423032497021; longitude= 3.485597668647756; id_magasin= "205" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.18078071784649; longitude= 0.6341609325409081; id_magasin= "078" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 41.954772606219; longitude= 8.792348687172; id_magasin= "206" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.661449902672; longitude= 6.3874827976226; id_magasin= "124" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.89103322366536; longitude= 2.164943811571675; id_magasin= "079" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.107; longitude= 4.101; id_magasin= "207" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.961; longitude= 5.34; id_magasin= "125" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.86479944403039; longitude= 2.379527931213374; id_magasin= "003" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.467818681855; longitude= -0.52481982803351; id_magasin= "025" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.6245935759968; longitude= 0.1597161496762283; id_magasin= "020" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.946; longitude= 6.035; id_magasin= "126" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.60718928804504; longitude= 7.123715227791422; id_magasin= "208" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.663193081909; longitude= 4.6364686569203; id_magasin= "209" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.617; longitude= 4.383; id_magasin= "129" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.954605472443; longitude= 2.4801072883606; id_magasin= "188" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.909225398021; longitude= 2.4232410631026; id_magasin= "130" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.821; longitude= 3.555; id_magasin= "273" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.98648094576026; longitude= 4.883361474990807; id_magasin= "210" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.14735249778486; longitude= 4.614858582650754; id_magasin= "211" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 42.7; longitude= 9.449999999999999; id_magasin= "212" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.48988053973789; longitude= -1.494872550926175; id_magasin= "080" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.408195606114; longitude= 2.1150496687866; id_magasin= "004" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.483; longitude= 6.833; id_magasin= "169" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.83797996913118; longitude= 0.4574626886687838; id_magasin= "081" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.285; longitude= 5.993; id_magasin= "153" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 50.51467976875609; longitude= 2.623335289983515; id_magasin= "005" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.35036692509089; longitude= 3.252894577611642; id_magasin= "213" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.638890946538; longitude= -0.95668303078787; id_magasin= "272" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.571; longitude= 1.369; id_magasin= "103" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.9073068; longitude= 2.4249907999999; id_magasin= "189" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.86866888166; longitude= -0.49362717601787; id_magasin= "082" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.733; longitude= -0.534; id_magasin= "083" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.834973371124; longitude= -0.67068886375432; id_magasin= "084" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 50.6740731279315; longitude= 1.65515122110196; id_magasin= "006" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.2; longitude= 5.217; id_magasin= "131" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.09548209595389; longitude= 2.434249973910482; id_magasin= "104" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.588845999379; longitude= 5.2461755275726; id_magasin= "132" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.84607346962262; longitude= -0.5044642810897813; id_magasin= "064" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.41495368613352; longitude= -4.468569154568058; id_magasin= "026" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.315120713627; longitude= 3.3730321865082; id_magasin= "133" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.168104757674; longitude= 1.559642410836773; id_magasin= "065" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.1660882273421; longitude= -0.2957792719359986; id_magasin= "027" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.427614778201; longitude= 1.4417909048509; id_magasin= "085" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 50.939; longitude= 1.847; id_magasin= "007" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.548342568534; longitude= 6.963381021161; id_magasin= "214" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.205720454625; longitude= 2.3081068483132; id_magasin= "086" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.31142972386743; longitude= -1.256475556156602; id_magasin= "028" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.27417276713832; longitude= -3.580035605915214; id_magasin= "029" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.59156611817999; longitude= 2.20545709063299; id_magasin= "267" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.793081828115; longitude= 4.833984375; id_magasin= "105" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.94532105467934; longitude= 4.385042128260011; id_magasin= "154" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.73215744029901; longitude= 4.753926753997803; id_magasin= "155" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.451451674133; longitude= 1.5214739246369; id_magasin= "106" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.70378309478069; longitude= -1.393763178295899; id_magasin= "032" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.05933641712879; longitude= 1.328489452362078; id_magasin= "107" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.7762110525653; longitude= 1.649500897503003; id_magasin= "108" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.78913317767029; longitude= 0.5261945153442866; id_magasin= "066" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.12255585671263; longitude= 5.149299126580445; id_magasin= "156" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.6336923966165; longitude= -1.602629036462417; id_magasin= "033" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.0778769; longitude= -0.8391827055572776; id_magasin= "034" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.814; longitude= 3.108; id_magasin= "067" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.75428252585; longitude= 3.1317028245758; id_magasin= "068" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.05026585187113; longitude= 6.591379380226158; id_magasin= "135" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.68; longitude= -0.311; id_magasin= "069" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.235; longitude= 2.465; id_magasin= "262" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.726845567413; longitude= -1.0817322834671; id_magasin= "088" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.896; longitude= 1.077; id_magasin= "009" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.095; longitude= 6.24; id_magasin= "215" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.483151213087; longitude= 3.9792597595246; id_magasin= "110" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.28082815978402; longitude= 5.013284350262438; id_magasin= "256" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.44492639863445; longitude= -2.070548474278439; id_magasin= "036" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.06419370480322; longitude= 5.454168312072738; id_magasin= "158" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.74971777146213; longitude= 1.34753680534061; id_magasin= "111" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.06562211210457; longitude= 3.961718832016004; id_magasin= "159" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.198283010848; longitude= 6.4691479880228; id_magasin= "160" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.017; longitude= 1.15; id_magasin= "010" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.60317018595567; longitude= 2.012042915344182; id_magasin= "089" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 42.9497840433227; longitude= 1.625603033065772; id_magasin= "090" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.46583550476184; longitude= -0.8245416160152672; id_magasin= "037" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.35024307065; longitude= -1.226286096301; id_magasin= "038" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 50.023086163212; longitude= 4.034187754631; id_magasin= "011" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.44678157875484; longitude= 6.696884334349193; id_magasin= "216" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.75781638441172; longitude= 2.330587909787027; id_magasin= "192" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.539467595311; longitude= 6.064021376481; id_magasin= "217" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.83789445233781; longitude= -1.55565750222172; id_magasin= "039" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.43661174961133; longitude= 5.599965131282829; id_magasin= "162" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.154332051249; longitude= 5.7456781423278; id_magasin= "138" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.199535063948; longitude= 5.6743433227539; id_magasin= "137" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.167; longitude= 1.867; id_magasin= "070" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.55839691178907; longitude= -3.12346929091791; id_magasin= "040" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.83574384674526; longitude= 7.739650579934732; id_magasin= "163" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.781448575429; longitude= 2.5696790628433; id_magasin= "194" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.584344672312; longitude= -0.06959392457577; id_magasin= "091" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.693920388532; longitude= -1.4301109313965; id_magasin= "041" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.17447404426623; longitude= -1.120185791679432; id_magasin= "071" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.83134121948319; longitude= 5.336455356900046; id_magasin= "164" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.078765508061; longitude= -0.79813490531001; id_magasin= "043" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.50972611227827; longitude= 0.2244598464965293; id_magasin= "044" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.93980732424905; longitude= 0.2332713655456473; id_magasin= "045" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.022677676755; longitude= 3.8825039555677; id_magasin= "139" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 50.46393735598428; longitude= 2.826879937852254; id_magasin= "012" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.82647323935482; longitude= 1.969225305557302; id_magasin= "268" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.917; longitude= -0.233; id_magasin= "093" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.89715418742488; longitude= 1.282137792419803; id_magasin= "072" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.145; longitude= 0.27; id_magasin= "046" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.513714407051; longitude= 5.7357432143529; id_magasin= "165" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.67163558731622; longitude= 5.527246763229414; id_magasin= "166" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.77562507299656; longitude= -3.339881982803377; id_magasin= "047" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.714592566708; longitude= 4.966356754303; id_magasin= "140" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.253509136327; longitude= 4.7904565410614; id_magasin= "112" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.99; longitude= 1.669; id_magasin= "195" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.2911956367758; longitude= 5.592760862635373; id_magasin= "218" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.417; longitude= 5.361; id_magasin= "219" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 50.25298225601161; longitude= 3.935029283575432; id_magasin= "263" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.517; longitude= 3.5; id_magasin= "220" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.07757979519965; longitude= 6.09980688095095; id_magasin= "168" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.11528021901; longitude= 3.0705864232788; id_magasin= "222" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.689353046409; longitude= 4.3596751779462; id_magasin= "114" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.253; longitude= 1.762; id_magasin= "094" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.97830903819581; longitude= 2.732691615160206; id_magasin= "113" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.53233003890956; longitude= 4.7407525939484; id_magasin= "141" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.333; longitude= 2.6; id_magasin= "142" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.58096950663104; longitude= 3.9233439415558; id_magasin= "223" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.536848911636; longitude= 3.3462759219144; id_magasin= "143" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.79268118284018; longitude= 7.314231875491146; id_magasin= "170" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.91262276811431; longitude= 2.218388456344655; id_magasin= "266" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.22326133037686; longitude= -1.623947128697182; id_magasin= "050" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.183; longitude= 3.0; id_magasin= "224" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.25982692829135; longitude= 2.717306360821567; id_magasin= "199" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.01860416180993; longitude= 3.148347279229142; id_magasin= "115" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.809983623339; longitude= 4.3702105217042; id_magasin= "265" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.33321490356847; longitude= -0.4135342235565531; id_magasin= "073" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.32877786948443; longitude= 0.8288721571472024; id_magasin= "116" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.84496399730982; longitude= 1.915388456344544; id_magasin= "117" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.10091095677654; longitude= 1.623430579376191; id_magasin= "095" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.321955222133; longitude= -0.42246293055609; id_magasin= "258" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.19020793354147; longitude= 0.7670107288361123; id_magasin= "096" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 42.73224423156965; longitude= 2.888859638145959; id_magasin= "226" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 42.6757147153019; longitude= 2.889918505568858; id_magasin= "227" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.15829544709619; longitude= 2.264943167211868; id_magasin= "200" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.179812684864; longitude= 2.2577442090148; id_magasin= "118" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.64791395809787; longitude= 0.3633725389968276; id_magasin= "074" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.90388762351251; longitude= 6.33285888769683; id_magasin= "172" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.04377502543296; longitude= -2.959917659532607; id_magasin= "051" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.067; longitude= 2.067; id_magasin= "201" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.394297; longitude= 2.602043; id_magasin= "275" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.55187962993746; longitude= 3.299831348548878; id_magasin= "202" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.964; longitude= -4.084; id_magasin= "052" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.672542886601; longitude= -2.0658405829536; id_magasin= "053" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.21512167468182; longitude= 4.045906916484114; id_magasin= "173" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.0; longitude= 6.65; id_magasin= "174" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.085992832982; longitude= -1.6308712539673; id_magasin= "054" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.18984264446309; longitude= -1.727080466270422; id_magasin= "055" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.012481830035; longitude= 4.0941751171894; id_magasin= "144" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.927; longitude= -0.96; id_magasin= "260" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.057966923133; longitude= 5.0986722609648; id_magasin= "145" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.510583771884; longitude= 3.7164498073047; id_magasin= "175" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.3726875691543; longitude= 1.737871253967342; id_magasin= "119" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.8824797656351; longitude= 2.468028112411503; id_magasin= "274" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.527; longitude= 0.967; id_magasin= "015" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.32824550218225; longitude= 1.092790222167991; id_magasin= "016" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.74889144399759; longitude= -0.6749999999999545; id_magasin= "076" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.344279006983; longitude= 4.8054957447052; id_magasin= "128" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.48749612584164; longitude= 6.84376396560674; id_magasin= "178" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.1154461326058; longitude= 7.093123938900817; id_magasin= "179" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.231; longitude= -0.123; id_magasin= "061" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.280259909792; longitude= 7.4646712596634; id_magasin= "180" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.2156968176268; longitude= 3.276834557502752; id_magasin= "120" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.11518152048411; longitude= 6.709013148496524; id_magasin= "183" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.49419518968239; longitude= -2.725073831352233; id_magasin= "057" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.655; longitude= 4.967; id_magasin= "177" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.482648824342; longitude= 4.3480975313187; id_magasin= "146" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.038; longitude= 3.09; id_magasin= "147" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.11306768489012; longitude= 0.7801341267529551; id_magasin= "269" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.00623537377957; longitude= 1.125230669975281; id_magasin= "075" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.272398254675; longitude= 6.362119301297; id_magasin= "257" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.11633714341896; longitude= -1.047417784625281; id_magasin= "058" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.86022027275169; longitude= 3.255673091796893; id_magasin= "017" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.658319670622; longitude= -1.9703242773874; id_magasin= "059" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.25838351190313; longitude= -2.272231597900372; id_magasin= "060" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.622873899399; longitude= 2.350835064235; id_magasin= "203" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.65888977610565; longitude= 7.720972595770263; id_magasin= "181" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.24; longitude= 0.02; id_magasin= "097" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.35224404645442; longitude= 6.137000419011997; id_magasin= "184" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 46.355; longitude= 6.425; id_magasin= "149" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.20468540663474; longitude= 6.047239268807061; id_magasin= "236" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.549499668232; longitude= 1.5132467089844; id_magasin= "098" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.669433374369; longitude= 1.41117453139; id_magasin= "099" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 43.547086120524; longitude= 1.4197658920655; id_magasin= "264" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.34; longitude= 0.704; id_magasin= "121" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.296052692787; longitude= 4.1294016788977; id_magasin= "185" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.2531302866786; longitude= 1.762230669975224; id_magasin= "077" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.888851800521; longitude= 4.8795250698364; id_magasin= "150" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 50.34521389864577; longitude= 3.481118499475087; id_magasin= "018" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.66312064026388; longitude= -2.785265842367153; id_magasin= "062" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.76183927390906; longitude= 1.050700337634225; id_magasin= "122" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 49.163912637405; longitude= 5.4142468497375; id_magasin= "186" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 47.23953228560307; longitude= 2.091089794351205; id_magasin= "123" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.36068170614732; longitude= 2.012420597961409; id_magasin= "101" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.988282574127; longitude= 4.7436905033188; id_magasin= "259" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 44.402; longitude= 0.65; id_magasin= "102" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 48.85773217197603; longitude= -0.8768164909851066; id_magasin= "063" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.616845255155; longitude= 5.8878526599489; id_magasin= "271" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
				 latitude= 45.381143180358; longitude= 5.579820291996; id_magasin= "152" ;cp.store(getCoordsPOI(latitude,longitude,id_magasin,typePOI));cp.db().commit();
			}
	        cp.close();
	        cp.db().close();
		}
	}

	private CoordonneesPOI getCoordsPOI(Double latitude, Double longitude,
			String id_magasin, String typePOI) {
		// TODO Auto-generated method stub
		
		CoordonneesPOI retour = new CoordonneesPOI();
		retour.setAdresse("");
		retour.setLatitude(latitude);
		retour.setLongitude(longitude);
		retour.setType(typePOI);
		retour.setCategorie(id_magasin);
		return retour;
		
	}
 }
