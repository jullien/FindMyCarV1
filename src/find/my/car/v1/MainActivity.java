package find.my.car.v1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MainActivity extends MapActivity implements LocationListener {
	private LocationManager locationManager;
	private Criteria criteria;
	private String provider, line, result, encodedString;
	private Location location, human, car;
	private LocationListener locationlistener;
	private int lat, lng, aux_lat, aux_lng;
	private GeoPoint point, p;
	private MapView mapView;
	private MapController mapController;
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	private MyOverlays itemizedoverlay;
	private OverlayItem overlayitem;
	private MyLocationOverlay myLocationOverlay;
	private float distance;
	private Handler updateLocation;
	private Button c, h;
	private SharedPreferences mPrefs;
	private Editor editor;
	private DefaultHttpClient httpclient;
	private HttpPost httppost;
	private HttpResponse response;
	private HttpEntity entity;
	private InputStream is;
	private BufferedReader reader;
	private StringBuilder sb;
	private JSONObject jsonObject, routes, overviewPolylines;
	private JSONArray routeArray;
	private List<GeoPoint> pointToDraw, poly;
	private URL url;
	private HttpURLConnection urlConnection;
	private Toast toast;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);
		
		mPrefs = getPreferences(MODE_PRIVATE);
		
		c = (Button) findViewById(R.id.car);
		c.setVisibility(View.VISIBLE);
		h = (Button) findViewById(R.id.human);
		h.setVisibility(View.INVISIBLE);
		
		updateLocation = new Handler();
		
		// Configure the Map
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setSatellite(true);
						
		mapController = mapView.getController();
		mapController.setZoom(16); // Zoom 1 is world view
		
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		mapView.getOverlays().add(myLocationOverlay);
		
		mapOverlays = mapView.getOverlays();
		drawable = this.getResources().getDrawable(R.drawable.ic_launcher);
		itemizedoverlay = new MyOverlays(this,drawable);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void buttonCar (View v) {
	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    criteria = new Criteria();
	    criteria.setAccuracy(Criteria.ACCURACY_FINE);
	    //criteria.setAccuracy(criteria.ACCURACY_COARSE);
	    //criteria.setAccuracy(criteria.ACCURACY_HIGH);
	    //criteria.setAccuracy(criteria.ACCURACY_MEDIUM);
	    //criteria.setAccuracy(criteria.ACCURACY_LOW);
	    provider = locationManager.getBestProvider(criteria, false);
	    locationManager.requestLocationUpdates(provider, 0, 0, this);
	    //locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, this);
	    //locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, this);
	    location = locationManager.getLastKnownLocation(provider);

	    // Initialize the location fields
	    if (location != null) {
	      onLocationChanged(location);
	      overlayitem = new OverlayItem(point, "", "");
	  	  itemizedoverlay.addOverlay(overlayitem);
	  	  mapOverlays.add(itemizedoverlay);
	      c.setVisibility(View.INVISIBLE);
	      h.setVisibility(View.VISIBLE);
	      
	      car = new Location("My car is here!");
		  car.setLatitude(point.getLatitudeE6());
		  car.setLongitude(point.getLongitudeE6());
	    }
	}
	
	public void buttonHuman (View v) {
	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    criteria = new Criteria();
	    criteria.setAccuracy(Criteria.ACCURACY_FINE);
	    //criteria.setAccuracy(criteria.ACCURACY_COARSE);
	    //criteria.setAccuracy(criteria.ACCURACY_HIGH);
	    //criteria.setAccuracy(criteria.ACCURACY_MEDIUM);
	    //criteria.setAccuracy(criteria.ACCURACY_LOW);
	    provider = locationManager.getBestProvider(criteria, false);
	    locationManager.requestLocationUpdates(provider, 0, 0, this);
	    //locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, this);
	    //locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, this);
	    location = locationManager.getLastKnownLocation(provider);

	    // Initialize the location fields
	    if (location != null) {
	      onLocationChanged(location);
	      overlayitem = new OverlayItem(point, "", "");
	  	  itemizedoverlay.addOverlay(overlayitem);
	  	  mapOverlays.add(itemizedoverlay);
	      h.setVisibility(View.INVISIBLE);
	      
	      //Tarefa executada em background para tra�ar rota entre usu�rio e carro
	      String strUrl = "http://maps.googleapis.com/maps/api/directions/json?"
	      + "origin=" + (itemizedoverlay.getItem(1).getPoint().getLatitudeE6()/1.0E6) + ","
	      + (itemizedoverlay.getItem(1).getPoint().getLongitudeE6()/1.0E6)
	      + "&destination=" + (itemizedoverlay.getItem(0).getPoint().getLatitudeE6()/1.0E6) + ","
	      + (itemizedoverlay.getItem(0).getPoint().getLongitudeE6()/1.0E6)
	      + "&sensor=false&mode=walking";
	      new TraceRouteTask().execute(strUrl.toString());
	      new TraceRouteTask().cancel(true);
	      
	      //Tarefa que monitora a posi��o atual do usu�rio para checar proximidade usu�rio-carro
	      updateLocation.removeCallbacks(UpdateLocationTask);
	      updateLocation.post(UpdateLocationTask);
	    }
	}
	
	@Override
	public void onLocationChanged(Location location) {
  	   lat = (int) (location.getLatitude() * 1E6);
  	   lng = (int) (location.getLongitude() * 1E6);
  	   point = new GeoPoint (lat,lng);
  	   mapView.getController().animateTo(point);
	}

	@Override
	public void onProviderDisabled(String provider) {
		c.setEnabled(false);
		h.setEnabled(false);
	}

	@Override
	public void onProviderEnabled(String provider) {
		c.setEnabled(true);
		h.setEnabled(true);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	protected void onResume() {
		super.onResume();
		myLocationOverlay.enableMyLocation();
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    criteria = new Criteria();
	    criteria.setAccuracy(Criteria.ACCURACY_FINE);
	    //criteria.setAccuracy(criteria.ACCURACY_COARSE);
	    //criteria.setAccuracy(criteria.ACCURACY_HIGH);
	    //criteria.setAccuracy(criteria.ACCURACY_MEDIUM);
	    //criteria.setAccuracy(criteria.ACCURACY_LOW);
	    provider = locationManager.getBestProvider(criteria, false);
	    locationManager.requestLocationUpdates(provider, 0, 0, this);
	    //locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, this);
	    //locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, this);
	    location = locationManager.getLastKnownLocation(provider);

	    // Initialize the location fields
	    if (location != null) {
	      onLocationChanged(location);
	    }
		
		//Carrega a posi��o do carro caso o aplicativo volte ao primeiro plano
		//Se nenhum valor tenha sido salvo em latitude ou longitude n�o faz nada
		//Se h� valor salvo para latitude e longitude carrega eles para o mapa e
		//Seta variavel com esses valores para utiliza��o na verifica��o de pr�ximidade usu�rio-carro
		aux_lat = mPrefs.getInt("latdest", 1000);
		aux_lng = mPrefs.getInt("lngdest", 1000);
		if (aux_lat != 1000 && aux_lng != 1000) {
			point = new GeoPoint (aux_lat,aux_lng);
			overlayitem = new OverlayItem(point, "", "");
		  	itemizedoverlay.addOverlay(overlayitem);
		  	mapOverlays.add(itemizedoverlay);
		  	c.setVisibility(View.INVISIBLE);
		    h.setVisibility(View.VISIBLE);
		    
		    car = new Location("My car is here!");
			car.setLatitude(aux_lat);
			car.setLongitude(aux_lng);
		}
		
		//Carrega a posi��o inicial do usu�rio caso o aplicativo volte ao primeiro plano
		//Se nenhum valor tenha sido salvo em latitude ou longitude n�o faz nada
		//Se h� valor salvo para latitude e longitude carrega eles para o mapa,
		//Executa tarefa assincrona em background para tra�ado da rota e
		//Inicializa tarefa para monitoramento da posi��o atual do usu�rio
		aux_lat = mPrefs.getInt("latorig", 1000);
		aux_lng = mPrefs.getInt("lngorig", 1000);
		if (aux_lat != 1000 && aux_lng != 1000) {
			point = new GeoPoint (aux_lat,aux_lng);
			overlayitem = new OverlayItem(point, "", "");
		  	itemizedoverlay.addOverlay(overlayitem);
		  	mapOverlays.add(itemizedoverlay);
		  	h.setVisibility(View.INVISIBLE);
		  	
		  	String strUrl = "http://maps.googleapis.com/maps/api/directions/json?"
		  	+ "origin=" + (itemizedoverlay.getItem(1).getPoint().getLatitudeE6()/1.0E6) + ","
		  	+ (itemizedoverlay.getItem(1).getPoint().getLongitudeE6()/1.0E6)
		  	+ "&destination=" + (itemizedoverlay.getItem(0).getPoint().getLatitudeE6()/1.0E6) + ","
		  	+ (itemizedoverlay.getItem(0).getPoint().getLongitudeE6()/1.0E6)
		  	+ "&sensor=false&mode=walking";
		  	new TraceRouteTask().execute(strUrl.toString());
		  	new TraceRouteTask().cancel(true);
		  	
		  	updateLocation.removeCallbacks(UpdateLocationTask);
		    updateLocation.post(UpdateLocationTask);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
		//Salva posi��o inicial do usu�rio e posi��o do carro caso o aplicativo saia do primeiro plano
		editor = mPrefs.edit();
		System.out.println(itemizedoverlay.size());
		if (itemizedoverlay.size() == 2) {
			editor.putInt("latorig", itemizedoverlay.getItem(1).getPoint().getLatitudeE6());
			editor.putInt("lngorig", itemizedoverlay.getItem(1).getPoint().getLongitudeE6());
			System.out.println("Meu carro no pause");
			editor.putInt("latdest", itemizedoverlay.getItem(0).getPoint().getLatitudeE6());
			editor.putInt("lngdest", itemizedoverlay.getItem(0).getPoint().getLongitudeE6());
		}
		else if (itemizedoverlay.size() == 1) {
			editor.putInt("latdest", itemizedoverlay.getItem(0).getPoint().getLatitudeE6());
			editor.putInt("lngdest", itemizedoverlay.getItem(0).getPoint().getLongitudeE6());
		}
		else if (itemizedoverlay.size() == 0) {
			editor.putInt("latorig", 1000);
			editor.putInt("lngorig", 1000);
			editor.putInt("latdest", 1000);
			editor.putInt("lngdest", 1000);
		}
		editor.apply();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	//Tarefa que monitora a movimenta��o do usu�rio e verifica se chegou pr�ximo do seu carro
	private Runnable UpdateLocationTask = new Runnable() {
		   public void run() {
			   locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			   criteria = new Criteria();
			   criteria.setAccuracy(Criteria.ACCURACY_FINE);
			   //criteria.setAccuracy(criteria.ACCURACY_COARSE);
			   //criteria.setAccuracy(criteria.ACCURACY_HIGH);
			   //criteria.setAccuracy(criteria.ACCURACY_MEDIUM);
			   //criteria.setAccuracy(criteria.ACCURACY_LOW);
			   provider = locationManager.getBestProvider(criteria, false);
			   locationlistener = new LocationListener() {
				   @Override
				   public void onLocationChanged(Location location) {
					   lat = (int) (location.getLatitude() * 1E6);
					   lng = (int) (location.getLongitude() * 1E6);
					   point = new GeoPoint (lat,lng);
					   //mapView.getController().setCenter(point);
					   mapView.getController().animateTo(point);
					   
					   human = new Location ("I'm here!");
					   human.setLatitude(lat);
					   human.setLongitude(lng);
					   
					   distance = human.distanceTo(car);
					   if (distance<=5) {
						   toast = Toast.makeText(getApplicationContext(), "You found your car.\nDrive safe!", Toast.LENGTH_LONG);
						   toast.setGravity(Gravity.CENTER, 0, 0);
						   toast.show();
						   itemizedoverlay.clearOverlay();
						   mapOverlays.clear();
						   c.setVisibility(View.VISIBLE);
						   human.reset();
						   car.reset();
						   locationManager.removeUpdates(locationlistener);
						   updateLocation.removeCallbacks(UpdateLocationTask);
					   }
				   }

				   public void onProviderDisabled(String provider) {}
				   public void onProviderEnabled(String provider) {}
				   public void onStatusChanged(String provider, int status, Bundle extras) {}
			   };
			   locationManager.requestLocationUpdates(provider, 0, 0, locationlistener);
			   //locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, this);
			   //locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, this);
		   }
	};
	
	//Tarefa assincrona em background que far� conex�o com site do google directions,
	//Obter� os dados codificados da rota,
	//Decodificar� os dados da rota e
	//Tra�ar� a rota no mapa para o usu�rio
	private class TraceRouteTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... urls) {
			try {
				/*httpclient = new DefaultHttpClient();
				httppost = new HttpPost(urls[0]);
				response = httpclient.execute(httppost);
				entity = response.getEntity();
				is = entity.getContent();
				httpclient.getConnectionManager().shutdown();
				reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);*/
				//Conex�o com o site
				url = new URL(urls[0]);
	            urlConnection = (HttpURLConnection) url.openConnection();
	            urlConnection.connect();
	            is = urlConnection.getInputStream();
	            //Leitura dos dados obtidos do site
	            reader = new BufferedReader(new InputStreamReader(is));
				sb = new StringBuilder();
				sb.append(reader.readLine() + "\n");
				line = "";
				while ((line = reader.readLine()) != null) {
				    //sb.append(line + "\n");
					sb.append(line);
				}
				result = sb.toString();
				reader.close();
				//Tratando os dados obtidos para serem decodificados
				jsonObject = new JSONObject(result);
				routeArray = jsonObject.getJSONArray("routes");
				routes = routeArray.getJSONObject(0);
				overviewPolylines = routes.getJSONObject("overview_polyline");
				encodedString = overviewPolylines.getString("points");
				return encodedString;
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
					urlConnection.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}		
			}
			return null;
		}
		
		protected void onPostExecute(String result) {
			if (result != null) {
				//Decodifica os dados pegos no google directions para obter os pontos da rota a ser tra�ada
				pointToDraw = decodePoly(encodedString);
				
				//Desenha a routa do usu�rio at� o carro utilzando os dados decodificados
				mapOverlays.add(new RoutePathOverlay(pointToDraw));
			}
		}
	}
	
	//Transforma os dados codificados do google directions em uma lista GeoPoint(latitude,longitude)
	private List<GeoPoint> decodePoly(String encoded) {
	    poly = new ArrayList<GeoPoint>();
	    int index = 0, len = encoded.length();
	    int lat = 0, lng = 0;
	    
	    //User Location - esse ponto pode ser diferente do inicial obtido do google directions
	    //Adicionamos esse ponto para ter o ponto do usu�rio como o inicial
	    p = new GeoPoint(itemizedoverlay.getItem(1).getPoint().getLatitudeE6(),itemizedoverlay.getItem(1).getPoint().getLongitudeE6());
	    poly.add(p);
	    		
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

	        p = new GeoPoint((int) (((double) lat / 1E5) * 1E6), (int) (((double) lng / 1E5) * 1E6));
	        poly.add(p);
	    }
	    //Car Location - esse ponto pode ser diferente do final obtido do google directions
	    //Adicionamos esse ponto para ter o ponto do usu�rio como o final
	    p = new GeoPoint(itemizedoverlay.getItem(0).getPoint().getLatitudeE6(),itemizedoverlay.getItem(0).getPoint().getLongitudeE6());
	    poly.add(p);
	    
	    return poly;
	}
}