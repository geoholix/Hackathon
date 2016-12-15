package com.esri.routing;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.data.FeatureCollectionTable;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;

public class CreateDeliveries extends Application {

  private GraphicsOverlay mGraphicsOverlay = new GraphicsOverlay();
  private MapView mMapView;
  private GraphicsOverlay mGraphics = new GraphicsOverlay();
  private FeatureCollectionTable mRoute;
  private ArrayList<FeatureCollectionTable> mRoutes = new ArrayList<>();

  @Override
  public void start(Stage stage) throws Exception {
    try {
      Platform.setImplicitExit(true);

      BorderPane bp = new BorderPane();

      javafx.scene.Scene fxScene = new javafx.scene.Scene(bp);

      stage.setTitle("Create delivery routes");
      stage.setWidth(1000);
      stage.setHeight(700);

      stage.setScene(fxScene);
      stage.show();

      ArcGISMap map = new ArcGISMap(Basemap.createStreets());
      mMapView = new MapView();

      bp.setCenter(mMapView);

      mMapView.setMap(map);
      mMapView.getGraphicsOverlays().add(mGraphicsOverlay);

      mMapView.setViewpointGeometryAsync(
          new Envelope(-13067866, 3843014, -13004499, 3871296, SpatialReferences.getWebMercator()));

      // create a table to use for the test
      final ArrayList<Field> fields = new ArrayList<Field>();
      fields.add(Field.createString("Name", "Name", 50));

      mRoute = new FeatureCollectionTable(fields, GeometryType.POINT, SpatialReferences.getWebMercator());
      mRoutes.add(mRoute);

      mMapView.getGraphicsOverlays().add(mGraphics);
      mGraphics.setRenderer(new SimpleRenderer(new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CROSS, 0xFFFF0000, 10)));

      // add deliveries on mouse clicks
      mMapView.setOnMouseClicked(event -> {
        if (event.isStillSincePress()) {
          // add features to the table
          Feature feature = mRoute.createFeature();
          Point deliveryPoint = mMapView.screenToLocation(new Point2D(event.getX(), event.getY()));
          feature.setGeometry(deliveryPoint);
          feature.getAttributes().put("Name", "delivery" + mRoute.getTotalFeatureCount());
          try {
            mRoute.addFeatureAsync(feature).get();
            mGraphics.getGraphics().add(new Graphic(deliveryPoint));
          } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
          }
        }
      });

      //serialize the deliveries so they can be transferred to devices
      Button saveRoutes = new Button("Save routes");
      saveRoutes.setOnAction(e -> {
        FeatureCollection collection = new FeatureCollection(mRoutes);
        try {
          PrintWriter out = new PrintWriter("c:/temp/routes.txt");
          out.append(collection.toJson());
          out.flush();
        } catch (FileNotFoundException e1) {
          e1.printStackTrace();
        }
      });

      // button to start new route
      Button newRoute = new Button("New route");
      newRoute.setOnAction(e -> {
        mGraphics.getGraphics().clear();
        mRoute = new FeatureCollectionTable(fields, GeometryType.POINT, SpatialReferences.getWebMercator());
        mRoutes.add(mRoute);
      });

      HBox hbox = new HBox();
      bp.setBottom(hbox);
      hbox.getChildren().add(newRoute);
      hbox.getChildren().add(saveRoutes);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void stop() throws Exception {
    super.stop();
    mMapView.dispose();
  }

  /**
   * Opens and runs application.
   *
   * @param args arguments passed to this application
   */
  public static void main(String[] args) {

    Application.launch(args);
  }
}
