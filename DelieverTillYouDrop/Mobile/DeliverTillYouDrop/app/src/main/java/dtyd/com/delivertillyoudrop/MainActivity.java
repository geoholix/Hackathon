package dtyd.com.delivertillyoudrop;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.data.FeatureCollectionTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.Symbol;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

  private final static String MSG_FAILED_ROUTE = "Failed to initialize delivery route";

  private final static Symbol ROUTE_SYMBOL = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 5);

  private MapView mMapView;

  private ArcGISMap mMap;

  private RouteTask mRouteTask;

  private GraphicsOverlay mGraphicsOverlay;

  private DrawerLayout mDrawerLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // TODO: Mark as delivered
      }
    });

    mDrawerLayout = (DrawerLayout) findViewById(R.id.content_main);

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
        mDrawerLayout.closeDrawers();
        return retVal;
      }
    });

    mMapView = (MapView) findViewById(R.id.map_view);
    mGraphicsOverlay = new GraphicsOverlay();
    mMapView.getGraphicsOverlays().add(mGraphicsOverlay);

    mMap = new ArcGISMap(Basemap.createNavigationVector());
    mMap.setInitialViewpoint(new Viewpoint(new Envelope(-13067866, 3843014, -13004499, 3871296, SpatialReferences.getWebMercator())));

    mMapView.setMap(mMap);
    mMap.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        setupDeliveryRoute();
      }
    });
  }

  private void setupDeliveryRoute() {

    mRouteTask = new RouteTask("http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route");
    final ListenableFuture<RouteParameters> routeParamsFuture = mRouteTask.createDefaultParametersAsync();
    routeParamsFuture.addDoneListener(new Runnable() {
      @Override public void run() {
        RouteParameters routeParams = null;
        try {
          routeParams = routeParamsFuture.get();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }

        if (routeParams == null) {
          Toast.makeText(MainActivity.this, MSG_FAILED_ROUTE, Toast.LENGTH_SHORT).show();
          return;
        }

        //routeParams.setReturnDirections(true);

        // get the stops from the first FeatureCollectionTable
        FeatureCollection deliveryRoutes = FeatureLoader.loadFeature("routes.json", getAssets());
        for (Feature feature : deliveryRoutes.getTables().get(0)) {
          routeParams.getStops().add(new Stop((Point) feature.getGeometry()));
        }

        final ListenableFuture<RouteResult> routeResultFuture = mRouteTask.solveRouteAsync(routeParams);
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
            mGraphicsOverlay.getGraphics().add(routeGraphic);

            mMapView.setViewpointGeometryAsync(route.getRouteGeometry().getExtent(), 20);
          }
        });
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
}
