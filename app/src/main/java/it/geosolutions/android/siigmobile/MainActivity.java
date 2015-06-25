package it.geosolutions.android.siigmobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.vividsolutions.jts.geom.util.GeometryCollectionMapper;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;

import java.io.File;
import java.util.ArrayList;

import it.geosolutions.android.map.activities.MapActivityBase;
import it.geosolutions.android.map.control.CoordinateControl;
import it.geosolutions.android.map.control.LocationControl;
import it.geosolutions.android.map.control.MapControl;
import it.geosolutions.android.map.control.MapInfoControl;
import it.geosolutions.android.map.model.Layer;
import it.geosolutions.android.map.model.MSMMap;
import it.geosolutions.android.map.overlay.managers.MultiSourceOverlayManager;
import it.geosolutions.android.map.overlay.managers.OverlayManager;
import it.geosolutions.android.map.spatialite.SpatialiteLayer;
import it.geosolutions.android.map.utils.MapFilesProvider;
import it.geosolutions.android.map.utils.SpatialDbUtils;
import it.geosolutions.android.map.utils.ZipFileManager;
import it.geosolutions.android.map.view.AdvancedMapView;


public class MainActivity extends MapActivityBase
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    // default path for files
    private static File MAP_FILE;

    /**
     * Tag for Logging
     */
    private static final String TAG = "MainActivity";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;


    private MultiSourceOverlayManager layerManager;

    public AdvancedMapView mapView;
    public OverlayManager overlayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapFilesProvider.setBaseDir(Config.BASE_DIR_NAME);
        MAP_FILE = MapFilesProvider.getBackgroundMapFile();

        checkDefaults();

        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mapView =  (AdvancedMapView) findViewById(R.id.advancedMapView);

        // If MAP_FILE does not exists, it will be null
        // MAP_FILE.exists() will always be true
        if(MAP_FILE == null){

            askForDownload();

        }else {

            setupMap();
        }

    }

    private void setupMap() {

        if(MAP_FILE != null) {

            // Enforce Background and initial position
            MapPosition mp = mapView.getMapViewPosition().getMapPosition();
            if (mapView.getMapFile() == null || !mapView.getMapFile().exists()) {
                mapView.setMapFile(MAP_FILE);
                mp = new MapPosition(new GeoPoint(Config.INITIAL_LATITUDE, Config.INITIAL_LONGITUDE), (byte) Config.INITIAL_ZOOMLEVEL);
                mapView.getMapViewPosition().setMapPosition(mp);
            }

            if (mp.geoPoint.latitude == 0 && mp.geoPoint.longitude == 0) {
                mp = new MapPosition(new GeoPoint(Config.INITIAL_LATITUDE, Config.INITIAL_LONGITUDE), (byte) Config.INITIAL_ZOOMLEVEL);
                mapView.getMapViewPosition().setMapPosition(mp);
            }

            // Setup LayerManager
            layerManager =  new MultiSourceOverlayManager(mapView);

            // Enable Touch events
            mapView.setClickable(true);

            // Show ScaleBar
            mapView.getMapScaleBar().setShowMapScaleBar(true);

            // Coordinate Control
            mapView.addControl(new CoordinateControl(mapView, true));

            // Info Control
            MapInfoControl ic= new MapInfoControl(mapView,this);
            ic.setActivationButton((ImageButton) findViewById(R.id.ButtonInfo));
            ic.setMode(MapControl.MODE_VIEW);
            mapView.addControl(ic);

            // Location Control
            LocationControl lc  =new LocationControl(mapView);
            lc.setActivationButton((ImageButton)findViewById(R.id.ButtonLocation));
            mapView.addControl(lc);

            // Add Layers
            MSMMap mapConfig = SpatialDbUtils.mapFromDb(true);

            ArrayList<Layer> layers = new ArrayList<Layer>();
            for(Layer l : mapConfig.layers){
                Log.d(TAG, "Layer Title: " + l.getTitle());
                if(l.getTitle().startsWith(Config.LAYERS_PREFIX)){
                    layers.add(l);
                }
            }

            // Start with "Rischio Totale" theme
            for(Layer l : layers){
                if(l instanceof SpatialiteLayer){
                    Log.d(TAG, "Setting Style for layer: " + l.getTitle());
                    ((SpatialiteLayer) l).setStyleFileName(l.getTitle().replace(Config.LAYERS_PREFIX, Config.STYLES_PREFIX_ARRAY[3]));
                }
            }

            layerManager.setLayers(layers);

        }else{

            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.map_config_error))
                    .setPositiveButton(getString(R.string.ok_string), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();

        }
    }

    private void askForDownload() {
        File external_storage = Environment.getExternalStorageDirectory();

        if(external_storage.getFreeSpace() < Config.REQUIRED_SPACE){

            new AlertDialog.Builder(this)
                .setMessage(getString(R.string.not_enough_space))
                .setPositiveButton(getString(R.string.ok_string), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();
            return;
        }


        String dir_path = external_storage.getPath();
        ZipFileManager zfm = new ZipFileManager(
                this,
                dir_path,
                MapFilesProvider.getBaseDir(),
                Config.BASE_PACKAGE_URL,
                getString(R.string.download_base_data_title),
                getString(R.string.download_base_data)) {

            // TODO: This method is badly named, it should be a post-execute callback
            @Override
            public void launchMainActivity(boolean success) {
                if(success){
                    // Update the map file reference
                    MAP_FILE = MapFilesProvider.getBackgroundMapFile();
                    // Reconfigure the map
                    setupMap();
                }
            }
        };
    }

    /**
     * Check if the necessary options are set and eventually set the default values
     */
    private void checkDefaults() {

        //MAP_FILE is the file, can be null

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor ed = prefs.edit();
        if("not_found".equals(prefs.getString(MapView.MAPSFORGE_BACKGROUND_RENDERER_TYPE, "not_found"))){
            ed.putString(MapView.MAPSFORGE_BACKGROUND_RENDERER_TYPE, "0");
        }
        if(MAP_FILE != null && prefs.getString(MapView.MAPSFORGE_BACKGROUND_FILEPATH, null) == null){
            ed.putString(MapView.MAPSFORGE_BACKGROUND_FILEPATH, MAP_FILE.getAbsolutePath());
        }
        ed.commit();

        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        SharedPreferences.Editor mapPrefEditor = sharedPreferences.edit();

        if(!containsMapViewPosition(sharedPreferences)){
            mapPrefEditor.putFloat(KEY_LATITUDE, Config.INITIAL_LATITUDE);
            mapPrefEditor.putFloat(KEY_LONGITUDE, Config.INITIAL_LONGITUDE);
            mapPrefEditor.putInt(KEY_ZOOM_LEVEL, Config.INITIAL_ZOOMLEVEL);
        }
        mapPrefEditor.commit();
        //MapFilesProvider.setBackgroundFileName(MAP_FILE.getName());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        // Update Title
        onSectionAttached(position);

        // update the main content by replacing fragments
        if(layerManager == null && position < 4){
            // nothing to do
            return;
        }
       switch (position){
            case 0:
            case 1:
            case 2:
            case 3:
                // Set Layers style
                ArrayList<Layer> layers = layerManager.getLayers();
                for(Layer l : layers){
                    if(l instanceof SpatialiteLayer){
                        Log.d(TAG, "Setting Style for layer: " + l.getTitle());
                        ((SpatialiteLayer) l).setStyleFileName(l.getTitle().replace(Config.LAYERS_PREFIX, Config.STYLES_PREFIX_ARRAY[position]));
                    }
                }
                layerManager.setLayers(layers);
                mapView.redraw();
                break;
            case 4:
                Toast.makeText(getBaseContext(), "Starting Form...", Toast.LENGTH_SHORT).show();
                // Start the form activity
                Intent formIntent = new Intent(this, ComputeFormActivity.class);
                startActivity(formIntent);
                break;
            default:
                /*
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                        .commit();
                */
                break;
        }

    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                mTitle = getString(R.string.title_graph);
                break;
            case 1:
                mTitle = getString(R.string.title_risk_environment);
                break;
            case 2:
                mTitle = getString(R.string.title_risk_social);
                break;
            case 3:
                mTitle = getString(R.string.title_risk_total);
                break;
            case 4:
                mTitle = getString(R.string.title_elab_start);
                break;
            case 5:
                mTitle = getString(R.string.title_elab_load);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Controls can be refreshed getting the result of an intent, in this case
        // each control knows which intent he sent with their requestCode/resultCode
        for(MapControl control : mapView.getControls()){
            control.refreshControl(requestCode,resultCode, data);
        }

        // The user still don't have the data, ask for download
        if(MAP_FILE == null){
            askForDownload();
        }
    }
}
