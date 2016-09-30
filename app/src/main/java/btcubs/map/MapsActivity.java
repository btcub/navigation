package btcubs.map;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private final static String TAG = "btcubs";
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        String data = "";
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(22.631392, 120.301803);
        mMap.addMarker(new MarkerOptions().position(sydney).title("美麗島站"));
       // mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.setMyLocationEnabled(true); // 右上角的定位功能；這行會出現紅色底線，不過仍可正常編譯執行
        mMap.getUiSettings().setZoomControlsEnabled(true);  // 右下角的放大縮小功能
        mMap.getUiSettings().setCompassEnabled(true);       // 左上角的指南針，要兩指旋轉才會出現
        mMap.getUiSettings().setMapToolbarEnabled(true);    // 右下角的導覽及開啟 Google Map功能




        Log.d(TAG, "最高放大層級："+mMap.getMaxZoomLevel());
        Log.d(TAG, "最低放大層級："+mMap.getMinZoomLevel());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));     // 放大地圖到 16 倍大

        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute("https://maps.googleapis.com/maps/api/directions/json?origin=23.979548,120.696745&destination=22.631392,120.301803&sensor=false");
    }

    private String downloadUrl(String strUrl)  throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
       //     Log.d(TAG, "downloadUrl data："+ data);
            br.close();

        } catch (Exception e) {
             Log.d(TAG, "downloadUrl Exception："+ e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        Log.d(TAG, " \ndecodePoly dlat："+ encoded);
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;


            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            Log.d(TAG, " \ndecodePoly dlat："+ dlat);
            Log.d(TAG, " decodePoly dlng："+ dlng);
            Log.d(TAG, " decodePoly lat："+ lat);
            Log.d(TAG, " decodePoly lng："+ lng);
            Log.d(TAG, " \n：");
            poly.add(p);
        }

        return poly;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            String data = "";

            try {
                Log.d(TAG, "\n doInBackground url："+ url);

                // Fetching the data from web service
                data = downloadUrl(url[0]);

                Log.d(TAG, "\n doInBackground data："+ data);

            } catch (Exception e) {
                Log.d(TAG, "doInBackground Exception："+ e.toString());

            }
            return data;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);

            JSONObject jObject;

            JSONArray jRoutes = null;
            JSONArray jLegs = null;
            JSONArray jSteps = null;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(data);
                jRoutes = jObject.getJSONArray("routes");
                /** Traversing all routes */
                for(int i=0;i<jRoutes.length();i++){
                    jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
                    List path = new ArrayList<HashMap<String, String>>();

                    /** Traversing all legs */
                    for(int j=0;j<jLegs.length();j++){
                        jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");

                        /** Traversing all steps */
                        for(int k=0;k<jSteps.length();k++){
                            String polyline = "";
                            polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);
                            //Log.d(TAG, "\n\n\n onPostExecute polyline："+ polyline);
                            /** Traversing all points */

                        }
                        Log.d(TAG, "\n\n\n onPostExecute path："+ path.toString());
                        routes.add(path);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }catch (Exception e){
            }



        }
    }
}
