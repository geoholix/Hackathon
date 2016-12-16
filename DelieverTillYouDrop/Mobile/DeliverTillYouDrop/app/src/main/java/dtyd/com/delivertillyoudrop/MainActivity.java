package dtyd.com.delivertillyoudrop;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Symbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.PointBarrier;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

  private final static String MSG_FAILED_ROUTE = "No other alternative route available";

  private final static Symbol ROUTE_SYMBOL = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.argb(200,0,209,92), 5);

  private final static Symbol DELIVER_SYMBOL = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.argb(255,1,77,100), 25);
  private final static Symbol DELIVER_CURRENT_SYMBOL = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.argb(255,201,77,1), 27);

  private MapView mMapView;

  private ArcGISMap mMap;

  private final RouteTask mRouteTask = new RouteTask("http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route");;

  private final GraphicsOverlay mGraphicsOverlay = new GraphicsOverlay();
  private int mDeliveredCount;
  private boolean mLongPressDown;

  RouteParameters mRouteParams = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //DELIVER_SYMBOL = new PictureMarkerSymbol((BitmapDrawable)ContextCompat.getDrawable(this, R.drawable.ic_stop));

    setContentView(R.layout.activity_main);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);


    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getDeliveryOptionsDialog().show();
      }
    });

    NavigationView navigationView = (NavigationView) findViewById(R.id.right_drawer);

    navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        boolean retVal = true;
        switch(item.getItemId()){
          case R.id.action_menu_barrier:
            break;
          default:
            retVal = false;
            break;
        }
        return retVal;
      }
    });

    mMapView = (MapView) findViewById(R.id.map_view);
    mMapView.setOnTouchListener(new MapViewOnTouchListener(this, mMapView));
    mMapView.getGraphicsOverlays().add(mGraphicsOverlay);

    mMap = new ArcGISMap(Basemap.createNavigationVector());
    mMap.setInitialViewpoint(new Viewpoint(new Envelope(-13067866, 3843014, -13004499, 3871296, SpatialReferences.getWebMercator())));

    mMapView.setMap(mMap);
    mMap.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        setupDeliveryRoute();
      }
    });

    mMapView.setMagnifierEnabled(true);
  }

  private void setupDeliveryRoute() {

    final ListenableFuture<RouteParameters> routeParamsFuture = mRouteTask.createDefaultParametersAsync();
    routeParamsFuture.addDoneListener(new Runnable() {
      @Override public void run() {

        try {
          mRouteParams = routeParamsFuture.get();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }

        if (mRouteParams == null) {
          Toast.makeText(MainActivity.this, MSG_FAILED_ROUTE, Toast.LENGTH_SHORT).show();
          return;
        }

        //routeParams.setReturnDirections(true);

        // get the stops from the first FeatureCollectionTable
        FeatureCollection deliveryRoutes = FeatureLoader.loadFeature("routes.json", getAssets());
        for (Feature feature : deliveryRoutes.getTables().get(0)) {
          mRouteParams.getStops().add(new Stop((Point) feature.getGeometry()));
        }

        router();
      }
    });
  }

  private void router() {
    final ListenableFuture<RouteResult> routeResultFuture = mRouteTask.solveRouteAsync(mRouteParams);

    routeResultFuture.addDoneListener(new Runnable() {
      @Override public void run() {

        RouteResult routeResult = null;
        try {
          routeResult = routeResultFuture.get();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }

        if (routeResult == null || routeResult.getRoutes().size() == 0) {
          Toast.makeText(MainActivity.this, MSG_FAILED_ROUTE, Toast.LENGTH_SHORT).show();
          return;
        }

        Route route = routeResult.getRoutes().get(0);

        if (route == null) {
          Toast.makeText(MainActivity.this, MSG_FAILED_ROUTE, Toast.LENGTH_SHORT).show();
          return;
        }

        Graphic routeGraphic = new Graphic(route.getRouteGeometry(), ROUTE_SYMBOL);

        mGraphicsOverlay.getGraphics().clear();
        mGraphicsOverlay.getGraphics().add(routeGraphic);

        // Delivery graphics
        for (int i = 0; i < mRouteParams.getStops().size(); i++) {
          Graphic routeSteerPoint = new Graphic(mRouteParams.getStops().get(i).getGeometry(),
                  (i == 0) ? DELIVER_CURRENT_SYMBOL : DELIVER_SYMBOL);
          mGraphicsOverlay.getGraphics().add(routeSteerPoint);

          //create text symbols
          TextSymbol text =
                  new TextSymbol(
                          18, String.valueOf(mDeliveredCount + i + 1), (i == 0) ? Color.argb(255,255,144,10) : Color.argb(200,0,209,92),
                          TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
          Graphic textGraphic = new Graphic(mRouteParams.getStops().get(i).getGeometry(), text);

          //add the text to the graphics overlay
          mGraphicsOverlay.getGraphics().add(textGraphic);
        }

        mMapView.setViewpointGeometryAsync(route.getRouteGeometry().getExtent(), 20);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
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

  private Dialog getDeliveryOptionsDialog(){
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Delivery Options")
            .setItems(R.array.deliver_options, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                switch (i){
                  case 0:
                    break;
                }
                // Mark as delivered
                if (mRouteParams.getStops().size() > 2)
                {
                  mMapView = (MapView) findViewById(R.id.map_view);
                  mRouteParams.getStops().remove(0);
                  router();
                }else if (mRouteParams.getStops().size() > 0)
                {
                  mRouteParams.getStops().clear();
                  mGraphicsOverlay.getGraphics().clear();
                }

                ++mDeliveredCount;
              }
            });

    return builder.create();
  }

  /**
   * Custom MapViewOnTouchListener that listens to long press events on the map. If one occurs a new PointBarrier is created.
   */
  private class MapViewOnTouchListener extends DefaultMapViewOnTouchListener {

    public MapViewOnTouchListener(Context context, MapView mapView) {
      super(context, mapView);
    }

    @Override public void onLongPress(MotionEvent e) {
      super.onLongPress(e);

      mLongPressDown = true;
    }

    @Override
    public boolean onUp(MotionEvent e) {
      super.onUp(e);
      
      if (mLongPressDown) {
        Point mapPoint = mMapView.screenToLocation(new android.graphics.Point((int) e.getX(), (int) e.getY()));
        if (mapPoint != null) {

          mLongPressDown = false;

          Vibrator v = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
          // Vibrate for 100 milliseconds
          v.vibrate(100);

          mRouteParams.getPointBarriers().add(new PointBarrier(mapPoint));

          // once we have added a new barrier we need to recalculate the route
          router();
        }
      }
      return true;
    }
  }
}
