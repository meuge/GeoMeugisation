package com.meuge.geolocalisation;

import java.io.IOException;
import java.util.List;
 
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MeugeActivity extends Activity  implements OnClickListener, LocationListener {
	private LocationManager lManager;
    private Location location;
    private String choix_source = "";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        //On sp�cifie que l'on va avoir besoin de g�rer l'affichage du cercle de chargement
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 
        setContentView(R.layout.main);
 
        //On r�cup�re le service de localisation
        lManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
 
        //Initialisation de l'�cran
        reinitialisationEcran();
 
        //On affecte un �couteur d'�v�nement aux boutons
        findViewById(R.id.choix_source).setOnClickListener(this);
        findViewById(R.id.obtenir_adresse).setOnClickListener(this);
        findViewById(R.id.afficherAdresse).setOnClickListener(this);
        findViewById(R.id.obtenir_position).setOnClickListener(this);
        findViewById(R.id.refresh).setOnClickListener(this);
    }
 
        //M�thode d�clencher au clique sur un bouton
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.choix_source:
			choisirSource();
			break;
		case R.id.obtenir_position:
			obtenirPosition();
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
 
	private void choisirSource() {
		reinitialisationEcran();
 
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
				findViewById(R.id.obtenir_position).setEnabled(true);
				//on stock le choix de la source choisi
				choix_source = sources[which];
				//on ajoute dans la barre de titre de l'application le nom de la source utilis�
				setTitle(String.format("%s - %s", getString(R.string.app_name),
						choix_source));
			}
		})
		.create().show();
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
			String monAdresse = ((EditText)findViewById(R.id.adresse)).getText().toString();
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
		
			latitude  = Double.valueOf(((TextView)findViewById(R.id.latitude)).getText().toString()) ;
			longitude = Double.valueOf(((TextView)findViewById(R.id.longitude)).getText().toString()) ;
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
 
	public void onProviderEnabled(String provider) {
		Log.i("G�olocalisation", "La source a �t� activ�.");
	}
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i("G�olocalisation", "Le statut de la source a chang�.");
	}
 }