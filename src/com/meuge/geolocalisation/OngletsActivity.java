package com.meuge.geolocalisation;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import com.google.code.microlog4android.Logger;

public class OngletsActivity extends TabActivity {
	private static Logger logger = LogPersos.getLoggerPerso();
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        TabHost tabHost =  getTabHost(); 
        //initilatise avant l'affectation des activit�s
        tabHost.setup(); 
        // Tab for Photos
        TabSpec meugespec = tabHost.newTabSpec("Meuge");
        meugespec.setIndicator("Meuge", getResources().getDrawable(R.drawable.icon_meuge_tab));
        Intent photosIntent = new Intent(this, MeugeActivity.class);
//        photosIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        meugespec.setContent(photosIntent);
        
        // Tab for Songs
        TabSpec songspec = tabHost.newTabSpec("Datas");
        // setting Title and Icon for the Tab
        songspec.setIndicator("Carte", null);//getResources().getDrawable(R.drawable.icon_songs_tab));
        Intent songsIntent = new Intent(this, SongsActivity.class);
        songsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        songspec.setContent(songsIntent);
        
        // Tab for Videos
        TabSpec videospec = tabHost.newTabSpec("Carte");
        videospec.setIndicator("Infos", null);//getResources().getDrawable(R.drawable.icon_videos_tab));
        Intent videosIntent = new Intent(this, VideosActivity.class);
//        videosIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        videospec.setContent(videosIntent);
        
        // Adding all TabSpec to TabHost
        tabHost.addTab(songspec); // Adding songs tab
        tabHost.addTab(videospec); // Adding videos tab
        tabHost.addTab(meugespec); // Adding meuge tab
        tabHost.setCurrentTab(2);
        


    }
    
    
    @Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

}
