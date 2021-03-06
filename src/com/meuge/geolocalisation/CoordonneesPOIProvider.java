package com.meuge.geolocalisation;


import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.db4o.ObjectSet;
import com.db4o.query.Constraint;
import com.db4o.query.Query;

import android.content.Context;

public class CoordonneesPOIProvider extends DbProvider<CoordonneesPOI> {
	private Context contextCreated;
	public static int ALLRECORDS = -1;
	public CoordonneesPOIProvider(Class<CoordonneesPOI> persistentClass, Context ctx) {
		super(persistentClass, ctx);
		this.contextCreated = ctx;
		db();
		// TODO Auto-generated constructor stub
	}
    //Trouve la derni�re coordonn�e selon la latitude et la longitude
	
	public List<CoordonneesPOI> findByLatLong (CoordonneesPOI coord)
	{
		List<CoordonneesPOI> retour = null;
		try {
			ObjectSet<CoordonneesPOI> resultat ;
			Query query = getQuery();
			Constraint lonT = query.descend("longitude").constrain(coord.getLongitude());
			query.descend("latitude").constrain(coord.getLatitude()).and(lonT);
			resultat = query.execute();
			retour = findMax(resultat,1);
		} catch (NullPointerException e) {
			//Base vide
			 retour = new ArrayList<CoordonneesPOI>();
		}
		return retour;
	}

	/**
	 * @param coord
	 * @return
	 */
	private Query getQuery() {
		Query query = db().query();
		query.constrain(CoordonneesPOI.class);
		return query;
	}
	
}
